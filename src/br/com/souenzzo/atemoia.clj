(ns br.com.souenzzo.atemoia
  (:require [io.pedestal.http :as http]
            [hiccup2.core :as h]
            [clojure.edn :as edn]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc])
  (:import (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(def register
  (concat [(pc/resolver `current-commit
                        {::pc/output [::current-commit]}
                        (fn [_ _]
                          {::current-commit (System/getenv "CURRENT_COMMIT")}))
           (pc/resolver `counter
                        {::pc/output [::counter]}
                        (fn [{::keys [counter-state]} _]
                          {::counter @counter-state}))
           (pc/mutation `increment
                        {}
                        (fn [{::keys [counter-state]} _]
                          (swap! counter-state inc)
                          {}))
           (pc/resolver `memory
                        {::pc/output [::total-memory
                                      ::max-memory
                                      ::free-memory]}
                        (fn [_ _]
                          (let [rt (Runtime/getRuntime)]
                            {::total-memory (.maxMemory rt)
                             ::max-memory   (.maxMemory rt)
                             ::free-memory  (.freeMemory rt)})))
           pc/index-explorer-resolver]
          pc/connect-resolvers))

(def indexes (pc/register {} register))

(defn hoc-table
  [{::keys [display-props labels]}]
  (fn
    ([env] display-props)
    ([env tree]
     [:table
      [:tbody
       (for [prop display-props]
         [:tr
          [:th (get labels prop)]
          [:td (get tree prop)]])]])))

(defn hoc-form
  [{::keys [sym]}]
  (fn
    ([env] (vec (::pc/params (pc/mutation-data env sym))))
    ([env tree]
     [:form
      {:action (str "/" sym)
       :method "POST"}
      [:input {:type "submit" :value (str sym)}]])))


(def ui-table-counter
  (hoc-table {::display-props [::counter]
              ::labels        {::counter "Counter"}}))

(def ui-table-app-info
  (hoc-table {::display-props [::current-commit
                               ::total-memory
                               ::max-memory
                               ::free-memory]
              ::labels        {::current-commit "Current commit"
                               ::total-memory   "Total memory"
                               ::max-memory     "Max memory"
                               ::free-memory    "Free memory"}}))

(def ui-inc-form
  (hoc-form {::sym `increment}))

(defn ui-body
  ([env] [{:>/table-counter (ui-table-counter env)}
          {:>/inc-form (ui-inc-form env)}
          {:>/table-app-info (ui-table-app-info env)}])
  ([env {:>/keys [inc-form table-counter table-app-info]}]
   [:body
    [:header]
    [:main
     (ui-table-counter env table-counter)
     (ui-inc-form env inc-form)]
    [:footer
     (ui-table-app-info env table-app-info)]]))

(defn ui-head
  ([env] [])
  ([env tree]
   [:head
    [:meta {:charset (str StandardCharsets/UTF_8)}]
    [:link {:href "data:image/svg+xml;utf8"
            :rel  "icon"}]
    [:title "atemoia"]]))

(defn ui-html
  ([env] [{:>/head (ui-head env)}
          {:>/body (ui-body env)}])
  ([env {:>/keys [head body]}]
   [:html
    (ui-head env head)
    (ui-body env body)]))


(defonce counter-state
         (atom 0))


(def parser
  (p/parser {::p/plugins [(pc/connect-plugin {::pc/indexes (atom indexes)})]
             ::p/mutate  pc/mutate
             ::p/env     {::p/reader               [p/map-reader
                                                    pc/reader3
                                                    pc/open-ident-reader
                                                    p/env-placeholder-reader]
                          ::counter-state          counter-state
                          ::p/placeholder-prefixes #{">"}}}))


(defn current
  [req]
  (let [req (assoc req ::pc/indexes indexes)
        tree (parser req (ui-html req))]
    {:body    (->> (ui-html req tree)
                   (h/html {:mode :html})
                   str)
     :headers {"Content-Type" "text/html"}
     :status  200}))

(def routes
  (into `#{["/" :get current]}
        (for [{::pc/keys [sym]} (vals (::pc/index-mutations indexes))]
          [(str "/" sym)
           :post (fn [req]
                   (parser req `[{(~sym {})
                                  []}])
                   {:headers {"Location" "/"}
                    :status  302})
           :route-name (keyword sym)])))
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
  (swap! state
         (fn [st]
           (when st
             (http/stop st))
           (-> service-map
               http/default-interceptors
               http/create-server
               http/start))))
