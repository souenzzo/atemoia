(ns atemoia.node-server
  (:require [com.fulcrologic.fulcro.dom :as dom]
            ["node:http" :as http]
            ["react" :as r]
            ["react-dom/server" :as rds]))


;; see https://book.fulcrologic.com/#RawReactHooks

(defn Root []
  (dom/html
    (dom/head
      (dom/title "hello"))
    (dom/body
      (dom/div "world"))))

(defn handler-impl
  [req ^js res]
  (.writeHead res 200 #js{"Content-Type" "text/html"})
  (.end res (str "<!DOCTYPE html>\n"
              (rds/renderToString (r/createElement Root)))))

(defn start
  [& _]
  (.listen (http/createServer (fn [req res]
                                (handler-impl req res)))
    3000))
