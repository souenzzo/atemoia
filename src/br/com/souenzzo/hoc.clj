(ns br.com.souenzzo.hoc
  (:require [com.wsscode.pathom.connect :as pc]
            [io.pedestal.http.route :as route]
            [clojure.edn :as edn]))


(defn dispatch!
  [{:keys [parser]
    :as   env} tx]
  (parser env tx))


(defn routes
  [{::keys    [interceptors]
    ::pc/keys [indexes]
    :or       {interceptors []}}]
  (route/expand-routes (into `#{["/api" :post
                                 ~(conj interceptors
                                        (fn [{:keys [body] :as env}]
                                          (let [query (edn/read-string (slurp body))]
                                            {:status 200
                                             :body (pr-str (dispatch! env query))})))
                                 :route-name ::eql-api]}
                             cat
                             [(for [{::pc/keys [output sym]
                                     ::keys    [path]} (vals (::pc/index-resolvers indexes))
                                    :when path]
                                [path :get (conj interceptors
                                                 (fn [env]
                                                   (let [tree (dispatch! env output)]
                                                     (first (keep tree output)))))
                                 :route-name (keyword sym)])
                              (for [{::pc/keys [sym]} (vals (::pc/index-mutations indexes))]
                                [(str "/" sym)
                                 :post (conj interceptors
                                             (fn [env]
                                               (dispatch! env
                                                          `[{(~sym {})
                                                             []}])
                                               {:headers {"Location" "/"}
                                                :status  302}))
                                 :route-name (keyword sym)])])))


(defn kv-table
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

(defn form
  [{::keys [sym]}]
  (fn
    ([env] (vec (::pc/params (pc/mutation-data env sym))))
    ([env tree]
     [:form
      {:action (str "/" sym)
       :method "POST"}
      [:input {:type "submit" :value (str sym)}]])))

