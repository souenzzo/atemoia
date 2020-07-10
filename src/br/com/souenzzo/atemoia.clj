(ns br.com.souenzzo.atemoia
  (:require [io.pedestal.http :as http]
            [clojure.edn :as edn]))

(defn index
  [req]
  (prn (keys req))
  {:body    "ok"
   :headers {"Content-Type" "text/plain"}
   :status  200})

(def routes `#{["/" :get index]})
(def port (edn/read-string (System/getenv "PORT")))

(def service-map
  {::http/port   port
   ::http/routes routes
   ::http/type   :jetty
   ::http/join?  false})

(defonce state
         (atom nil))

(defn -main
  [& _]
  (prn [:starting!!])
  (swap! state
         (fn [st]
           (when st
             (http/stop st))
           (-> service-map
               http/default-interceptors
               http/create-server
               http/start))))
