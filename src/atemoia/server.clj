(ns atemoia.server
  (:gen-class)
  (:require [clojure.edn :as edn]
            [hiccup2.core :as h]
            [io.pedestal.http :as http]
            [ring.util.mime-type :as mime]
            [next.jdbc :as jdbc]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.route :as route]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.nio.charset StandardCharsets)
           (java.net URI)))

(set! *warn-on-reflection* true)

(defn index
  [{::keys [jdbc-database-url]}]
  (let [html [:html
              [:head
               [:meta {:charset (str StandardCharsets/UTF_8)}]
               [:title "atemoia"]]
              [:body
               [:div {:id "atemoia"} "loading ..."]
               [:script
                {:src "/atemoia/main.js"}]]]]
    {:body    (->> html
                (h/html {:mode :html})
                   (str "<!DOCTYPE html>\n"))
     :headers {"Content-Security-Policy" ""
               "Content-Type"            (mime/default-mime-types "html")}
     :status  200}))

(defn list-todo
  [{::keys [jdbc-url]}]
  (let [response (jdbc/execute! {:jdbcUrl jdbc-url}
                   ["SELECT * FROM todo"])]
    {:body    (->> response
                (json/generate-string))
     :headers {"Content-Type" (mime/default-mime-types "json")}
     :status  200}))

(defn create-todo
  [{::keys [jdbc-url]
    :keys  [body]}]
  (let [note (some-> body
               io/reader
               (json/parse-stream true)
               :note)]
    (jdbc/with-transaction [conn (jdbc/get-connection {:jdbcUrl jdbc-url})]
      (jdbc/execute! conn
        ["INSERT INTO todo (note) VALUES (?)"
         note])
      (when (< 10 (:count (first (jdbc/execute! conn
                                   ["SELECT count(id) FROM todo"]))))
        (jdbc/execute! conn
          ["DELETE FROM todo WHERE id = (SELECT min(id) FROM todo)"])))
    {:status 201}))

(defn install-schema
  [{::keys [jdbc-url]}]
  (jdbc/execute! {:jdbcUrl jdbc-url}
    ["CREATE TABLE todo (id serial, note text)"])
  {:status 202})

(def routes
  `#{["/" :get index]
     ["/todo" :get list-todo]
     ["/todo" :post create-todo]
     ["/install-schema" :post install-schema]})


(defonce state
  (atom nil))

(defn database->jdbc-url
  [database-url]
  (let [jdbc-url* (URI/create database-url)
        creds (.getUserInfo jdbc-url*)
        [u p] (string/split creds #":" 2)
        jdbc-url (str "jdbc:postgresql://"
                   (.getHost jdbc-url*)
                   ":" (.getPort jdbc-url*)
                   (.getPath jdbc-url*)
                   "?user=" u "&password=" p)]
    jdbc-url))

(defn -main
  [& _]
  (let [port (or (edn/read-string (System/getenv "PORT"))
               8080)
        database-url (or (System/getenv "DATABASE_URL")
                       "postgres://postgres:postgres@127.0.0.1:5432/postgres")
        jdbc-url (database->jdbc-url database-url)]
    (swap! state
      (fn [st]
        (some-> st http/stop)
        (-> {::http/port          port
             ::http/file-path     "target/classes/public"
             ::http/resource-path "public"
             ::http/host          "0.0.0.0"
             ::http/type          :jetty
             ::http/routes        (fn []
                                    (-> @#'routes
                                      route/expand-routes))
             ::http/join?         false}
          http/default-interceptors
          (update ::http/interceptors
            (partial cons
              (interceptor/interceptor {:enter (fn [ctx]
                                                 (-> ctx
                                                   (assoc-in [:request
                                                              ::jdbc-url]
                                                     jdbc-url)))})))
          http/dev-interceptors
          http/create-server
          http/start)))
    (println "started: " port)))

(defn dev-main
  [& _]
  ;; docker run --name my-postgres --env=POSTGRES_PASSWORD=postgres --rm -p 5432:5432 postgres:alpine
  (-> `shadow.cljs.devtools.server/start!
    requiring-resolve
    (apply []))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:atemoia]))
  (-main))
