(ns br.com.souenzzo.hoc-client
  (:require ["react" :as r]
            ["react-dom" :as rd]
            [goog.object :as gobj]
            [goog.dom :as gdom]
            [com.wsscode.pathom.connect :as pc]
            [clojure.edn :as edn]))

(defn hoc-form
  [{::keys [sym]}]
  (fn
    ([env] (vec (::pc/params (pc/mutation-data env sym))))
    ([env tree]
     (r/createElement
       "form"
       #js {:action (str "/" sym)
            :method "POST"}
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
        query [{:>/inc-form (ui-inc-form {})}
               {:>/table-counter (ui-table-counter {})}]
        fetch! (fn []
                 (-> (js/fetch "/api"
                               #js{:method "POST"
                                   :body   (pr-str query)})
                     (.then (fn [e]
                              (.text e)))
                     (.then (fn [text]
                              (setState (edn/read-string text))))))]
    (r/createElement "div"
                     nil
                     (r/createElement "button" #js{:onClick fetch!}
                                      "fetch!")
                     (ui-inc-form {} inc-form)
                     (ui-table-counter {} table-counter))))

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


