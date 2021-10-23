(ns atemoia.client
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defonce *state
  (r/atom {:todos []}))

(defn ui-root
  []
  (let [{:keys [todos]} @*state]
    [:div
     [:form
      {:on-submit (fn [^js evt]
                    (.preventDefault evt)
                    (let [el (-> evt
                               .-target
                               .-elements
                               .-note)]
                      (swap! *state update :todos (fn [todos]
                                                    (conj todos
                                                      {:todo/id   (count todos)
                                                       :todo/note (.-value el)})))
                      (set! (.-value el) "")))}
      [:label
       "note: " [:input {:name "note"}]]]
     [:ul
      (for [{:todo/keys [id note]} todos]
        [:li {:key id}
         note])]]))

(defn start
  []
  (some->> (js/document.getElementById "atemoia")
    (rd/render [ui-root])))

(defn after-load
  []
  (some->> (js/document.getElementById "atemoia")
    (rd/render [ui-root])))
