(ns atemoia.client
  (:require [reagent.core :as r]
            [atemoia.note :as note]
            [reagent.dom.client :as rdc]))

(defonce *state (r/atom {}))

(defn fetch-todos
  []
  (-> (js/fetch "/todo")
    (.then (fn [response]
             (when-not (.-ok response)
               (throw (ex-info (.-statusText response)
                        {:response response})))
             (swap! *state dissoc :error)
             (.json response)))
    (.then (fn [todos]
             (swap! *state assoc :todos (js->clj todos
                                          :keywordize-keys true))))
    (.catch (fn [ex]
              (swap! *state assoc :error (ex-message ex))))))

(defn ui-root
  []
  (let [{:keys [error todos]} @*state]
    [:<>
     [:p "This is a sample clojure app to demonstrate how to use "
      [:a {:href "https://clojure.org/guides/tools_build"}
       "tools.build"]
      " to create and deploy a full-stack clojure app."]
     [:p "Checkout our "
      [:a {:href "https://github.com/souenzzo/atemoia"}
       "README"]]
     [:form
      {:on-submit (fn [^js evt]
                    (.preventDefault evt)
                    (let [note-el (-> evt
                                    .-target
                                    .-elements
                                    .-note)
                          _ (when-not (note/valid? (.-value note-el))
                              (.setCustomValidity note-el "Invalid note")
                              (.reportValidity note-el)
                              (throw (ex-info "Invalid note" {})))
                          json-body #js{:note (.-value note-el)}
                          unlock (fn [success?]
                                   (fetch-todos)
                                   (when success?
                                     (set! (.-value note-el) ""))
                                   (set! (.-disabled note-el) false))]
                      (set! (.-disabled note-el) true)
                      (-> (js/fetch "/todo" #js{:method "POST"
                                                :body   (js/JSON.stringify json-body)})
                        (.then (fn [response]
                                 (unlock (.-ok response))))
                        (.catch (fn [ex]
                                  (unlock false))))))}
      [:label
       "note: "
       [:input {:on-change (fn [evt]
                             (-> evt .-target
                               (.setCustomValidity "")))
                :name "note"}]]]
     (when error
       [:<>
        [:pre (str error)]
        [:button {:on-click (fn []
                              (js/fetch "/install-schema"
                                #js{:method "POST"}))}
         "install schema"]])
     [:ul
      (for [{:todo/keys [id note]} todos]
        [:li {:key id}
         note])]]))

(defonce *root (atom nil))

(defn after-load
  []
  (some-> @*root
    (rdc/render [ui-root])))

(defn start
  []
  (let [container (js/document.getElementById "atemoia")
        root (rdc/create-root container)]
    (fetch-todos)
    (rdc/render root [ui-root])
    (reset! *root root)))
