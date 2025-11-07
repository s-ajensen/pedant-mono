(ns pedant.room
  (:require [pedant.core :as cc]
            [pedant.page :as page]))

(defmethod page/render :room [_]
  [:h1 (str "hello!" @page/state)])