(ns br.com.souenzzo.atemoia
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]))

(defonce counter
         (atom 0))

(defn current
  [req]
  {:body    (->> [:html
                  [:head
                   [:meta {:charset "utf-8"}]
                   [:link {:href "data:image/svg+xml;utf8"
                           :rel  "icon"}]
                   [:title "atemoia"]]
                  [:body
                   [:main
                    [:table
                     [:tbody
                      [:tr
                       [:th "counter: "]
                       [:td @counter]]]]
                    [:form
                     {:action "/inc" :method "POST"}
                     [:input {:type "submit" :value "+"}]]]
                   [:footer
                    [:pre
                     (with-out-str (pp/pprint (into {} (System/getenv))))]]]]
                 (h/html {:mode :html})
                 str)
   :headers {"Content-Type" "text/html;UTF=8"}
   :status  200})

(defn increment
  [req]
  (swap! counter inc)
  {:headers {"Location" "/"}
   :status  302})

(def routes `#{["/" :get current]
               ["/inc" :post increment]})
(def port (edn/read-string (System/getenv "PORT")))

(def service-map
  {::http/port   port
   ::http/host   "0.0.0.0"
   ::http/routes routes
   ::http/type   :jetty
   ::http/join?  false})

(defonce state
         (atom nil))

(defn -main
  [& _]
  (prn :-main)
  (swap! state
         (fn [st]
           (prn :st)
           (when st
             (prn :stop)
             (http/stop st))
           (prn :starting)
           (-> service-map
               http/default-interceptors
               http/create-server
               http/start)
           (prn :started))))
