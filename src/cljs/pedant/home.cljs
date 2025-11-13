(ns pedant.home
  (:require [pedant.page :as page]
            [reagent.core :as r]))

(def url "http://localhost:8124/sse")

(defn sse
  [url handler]
  (let [es (js/EventSource. url)]
    (.addEventListener es "message" #(handler (.-data %)))
    #(.close es)))

(defn home []
  (fn []
    [:div.homepage-container
     [:h1 "pedant - for now, a Claude wrapper"]
     (r/with-let [system   (r/atom "")
                  prompt   (r/atom "")
                  close-fn (atom nil)
                  response (r/atom "")]
       [:div
        [:textarea {:value       @system
                    :placeholder "System"
                    :on-change   #(reset! system (.. % -target -value))}]
        [:textarea {:value       @prompt
                    :placeholder "Prompt"
                    :on-change   #(reset! prompt (.. % -target -value))}]
        [:button {:on-click (fn []
                              (when @close-fn (@close-fn))
                              (let [sse-url (str url "?system=" (js/encodeURIComponent @system)
                                                 "&prompt=" (js/encodeURIComponent @prompt))]
                                (reset! close-fn (sse sse-url #(do
                                                                 (prn "%: " %)
                                                                 (swap! response str %))))))}
         "Submit"]
        [:p @response]]
       (finally
         (when @close-fn (@close-fn))))]))

(defmethod page/render :home [_]
  [home])