(ns br.com.souenzzo.atemoia
  (:require [clojure.edn :as edn]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [hiccup2.core :as h]
            [io.pedestal.http :as http]
            [br.com.souenzzo.hoc :as hoc])
  (:import (java.nio.charset StandardCharsets)))

(set! *warn-on-reflection* true)

(def ui-table-counter
  (hoc/kv-table {::hoc/display-props [::counter]
                 ::hoc/labels        {::counter "Counter"}}))

(def ui-table-app-info
  (hoc/kv-table {::hoc/display-props [::current-commit
                                      ::total-memory
                                      ::max-memory
                                      ::free-memory]
                 ::hoc/labels        {::current-commit "Current commit"
                                      ::total-memory   "Total memory"
                                      ::max-memory     "Max memory"
                                      ::free-memory    "Free memory"}}))

(def ui-inc-form
  (hoc/form {::hoc/sym `increment}))

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
    {:onload "br.com.souenzzo.hoc_client.main()"}
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
     (ui-table-app-info env table-app-info)]
    [:div {:id "app"}]
    [:script {:src "/main.js"}]]))

(defn ui-head
  ([env] [::title])
  ([env {::keys [title]}]
   [:head
    [:meta {:charset (str StandardCharsets/UTF_8)}]
    [:link {:href "data:image/svg+xml;utf8"
            :rel  "icon"}]
    [:title title]]))

(defn ui-html
  ([env] [{:>/head (ui-head env)}
          {:>/body (ui-body env)}])
  ([env {:>/keys [head body]}]
   [:html
    (ui-head env head)
    (ui-body env body)]))


(pc/defresolver index-page [env input]
  {::hoc/path  "/"
   ::pc/output [::index-page]}
  (let [tree (hoc/dispatch! env (ui-html env))]
    {::index-page {:body    (str (h/html {:mode :html}
                                         (ui-html env tree)))
                   :headers {"Content-Security-Policy" ""
                             "Cache-Control"           "no-store"
                             "Content-Type"            "text/html"}
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
           (pc/constantly-resolver ::title "Atemoia")
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

(def req->env
  {:name  ::req->env
   :enter (fn [ctx]
            (update ctx :request (fn [req]
                                   (assoc req
                                     :parser parser
                                     ::p/reader [p/map-reader
                                                 pc/reader3
                                                 pc/open-ident-reader
                                                 p/env-placeholder-reader]
                                     ::counter-state counter-state
                                     ::p/placeholder-prefixes #{">"}
                                     ::pc/indexes indexes))))})

(def port (edn/read-string (System/getenv "PORT")))

(defonce state
         (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (when st
             (http/stop st))
           (-> {::http/port      port
                ::http/file-path "target"
                ::http/host      "0.0.0.0"
                ::http/type      :jetty
                ::http/join?     false}
               (assoc ::http/routes (fn []
                                      (hoc/routes {::pc/indexes       indexes
                                                   ::hoc/interceptors [req->env]})))
               http/default-interceptors
               #_http/dev-interceptors
               http/create-server
               http/start))))
(comment
  (require
    '[shadow.cljs.devtools.api :as shadow.api]
    '[shadow.cljs.devtools.server :as shadow.server])
  (shadow.server/start!)
  (shadow.api/watch :web))