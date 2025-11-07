(ns pedant.styles.layout.document
  (:refer-clojure :exclude [rem])
  (:require [pedant.styles.core :refer :all]))

(def screen
  (list

    [:body :html
     {:font-family "'Courier New', Courier, monospace"
      :background-color "#ecf0f1" ;; Light grayish blue background
      :color "#2c3e50" ;; Dark blue text color
      :display "flex"
      :justify-content "center"
      :align-items "center"
      :height "100vh"
      :margin "0"}]

    [:.homepage-container
     {:width "100%"
      :max-width "1000px"
      :margin "0 auto"}]

    [:h1
     {:font-family "'Courier New', Courier, monospace"
      :font-size "24px"
      :color "#3498db" ;; Bright blue for the heading
      :margin-bottom "20px"
      :text-align "center"}]

    [:form
     {:display "flex"
      :flex-direction "column"
      :align-items "flex-start"}]

    [:.num-field
     {:display "flex"
      :align-items "center"
      :justify-content "space-between"
      :margin-bottom "15px"}]

    [:label
     {:font-size "18px"
      :margin-right "10px"
      :color "#2c3e50"}] ;; Dark blue text color for labels

    [:input
     {:width "50%"
      :padding "10px"
      :border "2px solid #2c3e50" ;; Dark blue border
      :border-radius "5px"
      :font-family "'Courier New', Courier, monospace"
      :font-size "18px"
      :text-align "right"
      :background-color "#ffffff" ;; White background for input
      :color "#2c3e50"}] ;; Dark blue text color for input

    ["input[type=\"text\"]::placeholder"
     {:color "#e74c3c"}] ;; Red color for placeholder text

    )




  )