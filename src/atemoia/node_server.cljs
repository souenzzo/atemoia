(ns atemoia.node-server
  (:require ["node:http" :as http]
            ["react" :as r]
            ["react-dom/server" :as rds]))

(defn ui-index
  [props]
  (r/createElement "html" #js{}
    #js[(r/createElement "head" #js{}
          #js[(r/createElement "title" #js{} "hello")])

        (r/createElement "body" #js{}
          (r/createElement "div" #js{} "ok!"))]))


(defn handler-impl
  [req ^js res]
  (.writeHead res 200 #js{"Content-Type" "text/html"})
  (.end res (str "<!DOCTYPE html>\n"
              (rds/renderToString (r/createElement ui-index)))))

(defn start
  [& _]
  (.listen (http/createServer (fn [req res]
                                (handler-impl req res)))
    3000))




