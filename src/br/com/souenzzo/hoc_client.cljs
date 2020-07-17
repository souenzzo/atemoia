(ns br.com.souenzzo.hoc-client
  (:require ["react" :as r]
            ["react-dom" :as rd]
            [goog.object :as gobj]
            [goog.dom :as gdom]
            [com.wsscode.pathom.connect :as pc]
            [clojure.edn :as edn]
            [cljs.core.async.interop :as async.p]
            [clojure.core.async :as async]
            [edn-query-language.core :as eql]))


(defn hoc-form
  [{::keys [sym]}]
  (fn
    ([env] (vec (::pc/params (pc/mutation-data env sym))))
    ([{::keys [remote]} tree]
     (r/createElement
       "form"
       #js {:action   (str "/" sym)
            :onSubmit (fn [e]
                        (.preventDefault e)
                        (async/put! remote `[(~sym {})]))
            :method   "POST"}
       (r/createElement "input"
                        #js{:type  "submit"
                            :value (str sym)})))))


(defn hoc-kv-table
  [{::keys [display-props labels]}]
  (fn
    ([env] display-props)
    ([env tree]
     (r/createElement
       "table"
       #js{}
       (apply r/createElement
              "tbody"
              #js{}
              (for [prop display-props]
                (r/createElement
                  "tr"
                  #js{}
                  (r/createElement
                    "th"
                    #js {} (get labels prop))
                  (r/createElement
                    "td"
                    #js {} (get tree prop)))))))))

(def ui-table-counter
  (hoc-kv-table {::display-props [:br.com.souenzzo.atemoia/counter]
                 ::labels        {:br.com.souenzzo.atemoia/counter "Counter"}}))

(def ui-inc-form
  (hoc-form {::sym 'br.com.souenzzo.atemoia/increment}))

(defn hello
  []
  (let [[{:>/keys [inc-form
                   table-counter]}
         setState] (r/useState nil)
        remote (async/chan)
        env {::remote remote}
        query [{:>/inc-form (ui-inc-form env)}
               {:>/table-counter (ui-table-counter env)}]
        fetch! (fn []
                 (-> (js/fetch "/api"
                               #js{:method "POST"
                                   :body   (pr-str query)})
                     (.then (fn [e]
                              (.text e)))
                     (.then (fn [text]
                              (setState (edn/read-string text))))))]
    (async/go-loop
      []
      (let [{:keys [dispatch-key]
             :as   op} (-> remote
                           async/<!
                           eql/query->ast
                           :children
                           first)
            body (-> {:type     :root
                      :children [(merge (eql/query->ast query)
                                        op)]}
                     eql/ast->query
                     pr-str)
            next-state (-> (js/fetch "/api" (js-obj "method" "POST"
                                                    "body" body))
                           async.p/<p!
                           .text
                           async.p/<p!
                           edn/read-string
                           (get dispatch-key))]
        (setState next-state)
        (recur)))

    (r/createElement "div"
                     nil
                     (r/createElement "button" #js{:onClick fetch!}
                                      "fetch!")
                     (ui-inc-form env inc-form)
                     (ui-table-counter env table-counter))))

(def app (r/createElement hello))

(def target (gdom/getElement "app"))

(defn ^:export main
  []
  (rd/render app target)
  #_(.then (js/fetch "/")
           (fn [e]
             (.log js/console e))))

(defn after-load
  []
  (rd/render app target))


