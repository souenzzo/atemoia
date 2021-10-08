(ns atemoia.client
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defonce state (r/atom {}))

(defn fetch-todos
  []
  (-> (js/fetch "/todo")
    (.then (fn [response]
             (if (.-ok response)
               (.json response)
               (swap! state assoc :error? true))))
    (.then (fn [{:keys [error?]
                 :as   todos}]
             (when-not error?
               (swap! state assoc :todos (js->clj todos
                                           :keywordize-keys true)))))))

(defn ui-root
  []
  (let [{:keys [error? todos]} @state]
    [:div
     [:p "This is a sample clojure app to demonstrate how to use "
      [:a {:href "https://clojure.org/guides/tools_build"}
       "tools.build"]
      " to create and deploy a full-stack clojure app."]
     [:p "Checkout our "
      [:a {:href "https://github.com/souenzzo/atemoia"}
       "README"]]
     [:ul
      (for [{:todo/keys [id note]} todos]
        [:li {:key id}
         note])]
     [:form
      {:on-submit (fn [evt]
                    (.preventDefault evt)
                    (let [el (-> ^js evt
                               .-target
                               .-elements
                               .-note)
                          json-body #js{:note (.-value el)}
                          unlock (fn [success?]
                                   (fetch-todos)
                                   (when success?
                                     (set! (.-value el) ""))
                                   (set! (.-disabled el) false))]
                      (set! (.-disabled el) true)
                      (-> (js/fetch "/todo" #js{:method "POST"
                                                :body   (js/JSON.stringify json-body)})
                        (.then (fn [response]
                                 (unlock (.-ok response))))
                        (.catch (fn [ex]
                                  (unlock false))))))}
      [:label
       "note: "
       [:input {:name "note"}]]]
     (when error?
       [:button {:on-click (fn []
                             (js/fetch "/install-schema"
                               #js{:method "POST"}))}
        "install schema"])]))

(defn start
  []
  (some->> (js/document.getElementById "atemoia")
    (rd/render [ui-root]))
  (fetch-todos))

(defn after-load
  []
  (some->> (js/document.getElementById "atemoia")
    (rd/render [ui-root])))
