(ns atemoia.server
  (:gen-class)
  (:require [atemoia.note :as note]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [hiccup2.core :as h]
            [io.pedestal.connector :as conn]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.jetty :as jetty]
            [io.pedestal.http.ring-middlewares :as ring-middlewares]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.secure-headers :as secure-headers]
            [io.pedestal.http.tracing :as tracing]
            [io.pedestal.service.interceptors :as interceptors]
            [next.jdbc :as jdbc]))

(set! *warn-on-reflection* true)

(defn index
  [_]
  (let [html [:html {:lang "en"}
              [:head
               [:meta {:charset "UTF-8"}]
               [:link {:rel "icon" :href "data:"}]
               [:meta {:name    "viewport"
                       :content "width=device-width, initial-scale=1.0"}]
               [:meta {:name    "theme-color"
                       :content "#000000"}]
               [:meta {:name    "description"
                       :content "A simple full-stack clojure app"}]
               [:title "atemoia"]]
              [:body
               [:div {:id "atemoia"} "loading ..."]
               [:script {:src "/atemoia/main.js"}]]]]
    {:body    (->> html
                (h/html {:mode :html})
                (str "<!DOCTYPE html>\n"))
     :headers {"Content-Type" "text/html"}
     :status  200}))

(defn list-todo
  [{::keys [atm-conn]}]
  (let [response (jdbc/execute! atm-conn
                   ["SELECT * FROM todo"])]
    {:body    (json/generate-string response)
     :headers {"Content-Type" "application/json"}
     :status  200}))

(defn create-todo
  [{::keys [atm-conn]
    :keys  [body]}]
  (let [note (some-> body
               io/reader
               (json/parse-stream true)
               :note)]
    (when-not (note/valid? note)
      (throw (ex-info "Invalid note" {})))
    (jdbc/execute! atm-conn
      ["INSERT INTO todo (note) VALUES (?);
        DELETE FROM todo WHERE id IN (SELECT id FROM todo ORDER BY id DESC OFFSET 10)"
       note])
    {:status 201}))

(defn install-schema
  [{::keys [atm-conn]}]
  (jdbc/execute! atm-conn
    ["CREATE TABLE todo (id serial, note text)"])
  {:status 202})

(def routes
  `#{["/" :get index]
     ["/todo" :get list-todo]
     ["/todo" :post create-todo]
     ["/install-schema" :post install-schema]})

(defonce *state
  (atom nil))

(defn create-connector
  [{::keys [atm-db-url] :as connector-map}]
  (-> connector-map
    ;; TODO use with-default-interceptors onde secure-headers is supported
    #_conn/with-default-interceptors
    (conn/with-interceptors
      [(tracing/request-tracing-interceptor)
       interceptors/log-request
       interceptors/not-found
       (ring-middlewares/content-type)
       route/query-params
       (body-params/body-params)
       (secure-headers/secure-headers {:content-security-policy-settings ""})])
    conn/optionally-with-dev-mode-interceptors
    (conn/with-interceptor
      {:name  ::with-context
       :enter (fn [ctx]
                (-> ctx
                  (assoc-in [:request ::atm-conn] {:jdbcUrl atm-db-url})))})
    (conn/with-interceptor
      {:name  ::resource-path
       :leave (fn [ctx]
                (case (int (get-in ctx [:response :status] 404))
                  404 (let [file-path (->> ctx :request :path-info
                                        (str "public"))
                            resource (io/resource file-path)]
                        (if resource
                          (assoc ctx :response {:status 200
                                                :body   (slurp resource)})
                          ctx))
                  ctx))})
    (conn/with-routes routes)
    (jetty/create-connector {})))

(defn -main
  [& _]
  (let [port (Long/getLong "atemoia.server.http-port" 8080)
        atm-db-url (System/getProperty "atemoia.server.atm-db-url"
                     "jdbc:postgresql://0:5432/postgres?user=postgres&password=postgres")]
    (swap! *state
      (fn [st]
        (some-> st conn/stop!)
        (-> "0"
          (conn/default-connector-map port)
          (assoc ::atm-db-url atm-db-url)
          create-connector
          conn/start!)))))

(comment
  (System/setProperty "io.pedestal.dev-mode" "true")
  (-> `shadow.cljs.devtools.server/start!
    requiring-resolve
    (apply []))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:atemoia]))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:node-test]))
  (-main))
