(ns br.com.souenzzo.hoc-client)

(defn ^:export main
  []
  (.then (js/fetch "/")
         (fn [e]
           (.log js/console e))))

(defn after-load
  []
  (.log js/console "after-load"))


