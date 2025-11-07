(ns pedant.home
  (:require [pedant.page :as page]
            [reagent.core :as r]))

(defn home []
  (fn []
    [:div.homepage-container
     [:h1 "pedant - The Simple Base Converter"]]))

(defmethod page/render :home [_]
  [home])