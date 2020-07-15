(ns br.com.souenzzo.atemoia
  (:require [clojure.edn :as edn]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [hiccup2.core :as h]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route])
  (:import (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(defn dispatch!
  [{:keys [parser]
    :as   env} tx]
  (parser env tx))

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
  ([env] [{::nav-links [::nav-label
                        ::nav-href
                        ::nav-active?]}
          {:>/table-counter (ui-table-counter env)}
          {:>/inc-form (ui-inc-form env)}
          {:>/table-app-info (ui-table-app-info env)}])
  ([env {::keys  [nav-links]
         :>/keys [inc-form table-counter table-app-info]}]
   [:body
    [:header
     [:nav
      [:ul
       (for [{::keys [nav-label
                      nav-href
                      nav-active?]} nav-links]
         [:li
          [:a
           {:href    nav-href
            :disable nav-active?}
           nav-label]])]]]
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


(pc/defresolver index-page [env input]
  {::path      "/"
   ::pc/output [::index-page]}
  (let [tree (dispatch! env (ui-html env))]
    {::index-page {:body    (str (h/html {:mode :html}
                                         (ui-html env tree)))
                   :headers {"Content-Type" "text/html"}
                   :status  200}}))


(def register
  (concat [index-page
           (pc/resolver `current-commit
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
           (pc/resolver `nav-links
                        {::pc/output [::nav-links]}
                        (fn [env _]
                          {::nav-links (for [{::pc/keys [output sym]
                                              ::keys    [path]} (-> env
                                                                    ::pc/indexes
                                                                    ::pc/index-resolvers
                                                                    vals)
                                             :when path]
                                         {::nav-label (pr-str sym)
                                          ::nav-href  path})}))
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


(def parser
  (p/parser {::p/plugins [(pc/connect-plugin {::pc/indexes (atom indexes)})]
             ::p/mutate  pc/mutate}))

(defonce counter-state
         (atom 0))
(defn req->env
  [req]
  (assoc req
    :parser parser
    ::p/reader [p/map-reader
                pc/reader3
                pc/open-ident-reader
                p/env-placeholder-reader]
    ::counter-state counter-state
    ::p/placeholder-prefixes #{">"}
    ::pc/indexes indexes))

(def routes
  (into `#{}
        cat
        [(for [{::pc/keys [output sym]
                ::keys    [path]} (vals (::pc/index-resolvers indexes))
               :when path]
           [path :get (fn [req]
                        (let [env (req->env req)
                              tree (dispatch! env output)]
                          (first (keep tree output))))
            :route-name (keyword sym)])
         (for [{::pc/keys [sym]} (vals (::pc/index-mutations indexes))]
           [(str "/" sym)
            :post (fn [req]
                    (dispatch! (req->env req)
                               `[{(~sym {})
                                  []}])
                    {:headers {"Location" "/"}
                     :status  302})
            :route-name (keyword sym)])]))
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
               #_(assoc ::http/routes (fn []
                                        (route/expand-routes routes)))
               http/default-interceptors
               #_http/dev-interceptors
               http/create-server
               http/start))))
