(ns br.com.souenzzo.atemoia
  (:require [io.pedestal.http :as http]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]))

(defn index
  [req]
  (prn (keys req))
  {:body    "ok"
   :headers {"Content-Type" "text/plain"}
   :status  200})

(def routes `#{["/*" :any index]})
(def port (edn/read-string (System/getenv "PORT")))

(def service-map
  {::http/port   port
   ::http/routes routes
   ::http/type   :jetty
   ::http/join?  false})

(defonce state
         (atom nil))

(defn debug
  [x y]
  (prn y)
  x)

(defn -main
  [& _]
  (prn [:starting!!])
  (pp/pprint (into {} (System/getenv)))
  (swap! state
         (fn [st]
           (prn :Ok)
           (when st
             (prn :sopt)
             (http/stop st))
           (prn :xxx)
           (-> service-map
               (doto (debug :a))
               http/default-interceptors
               (doto (debug :b))
               http/create-server
               (doto (debug :c))
               http/start
               (doto pp/pprint)))))
