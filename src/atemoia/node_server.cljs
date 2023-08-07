(ns atemoia.node-server
  (:require [clojure.string :as string]
            [com.fulcrologic.fulcro.dom :as dom]
            ["node:fs/promises" :as fs]
            ["node:http" :as http]
            ["node:url" :as url]
            ["react" :as r]
            ["react-dom/server" :as rds]))


;; see https://book.fulcrologic.com/#RawReactHooks

(defn Root []
  (dom/html {:lang "en"}
    (dom/head
      (dom/meta {:charSet "UTF-8"})
      (dom/link {:rel "icon" :href "data:"})
      (dom/meta {:name    "viewport"
                 :content "width=device-width, initial-scale=1.0"})
      (dom/meta {:name    "theme-color"
                 :content "#000000"})
      (dom/meta {:name    "description"
                 :content "A simple full-stack clojure app"})
      (dom/title "atemoia"))
    (dom/body
      (dom/div {:id "atemoia"} "loading ...")
      (dom/script {:src "/atemoia/main.js"}))))

(defn fs-handler
  [prefix]
  (fn [{:keys [uri]}]
    (-> (fs/readFile (str prefix uri))
      (.then (fn [x]
               {:status 200
                :body   x})))))

(def static-handler
  (fs-handler "./target/classes/public"))

(defn handler-impl
  [{:keys [uri request-method]
    :as   ring-request}]
  (js/console.log #js[(pr-str request-method)
                      uri])
  (case uri
    "/" {:headers {"Content-Type" "text/html"}
         :body    (str "<!DOCTYPE html>\n"
                    (rds/renderToString (r/createElement Root)))
         :status  200}
    "/todo" {:headers {"Content-Type" "application/json"}
             :body    (js/JSON.stringify #js[])
             :status  200}
    (.catch (static-handler ring-request)
      (fn [_ex]
        {:status 404}))))

(defn ring-handler->request-listener
  [handler]
  (fn [req ^js res]
    (let [url (url/parse (.-url req))]
      (-> {:uri            (.-pathname url)
           :query-string   (.-query url)
           :request-method (keyword (string/lower-case (.-method req)))
           :headers        (js->clj (.-headers req))
           #_#_:server-port -1
           #_#_:server-name "localhost"
           #_#_:remote-addr "127.0.0.1"
           #_#_:scheme :http
           #_#_:protocol "HTTP/1.1"
           #_#_:body nil}
        handler
        js/Promise.resolve
        (.then (fn [{:keys [status body headers]}]
                 (.writeHead res status (clj->js headers))
                 (.end res body)))))))

(defn start
  [& _]
  (.listen (http/createServer (fn [req res]
                                ((ring-handler->request-listener handler-impl)
                                 req res)))

    3000))
