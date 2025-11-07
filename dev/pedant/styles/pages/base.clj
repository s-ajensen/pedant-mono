(ns pedant.styles.pages.base
  (:refer-clojure :exclude [rem])
  (:require [pedant.styles.core :refer :all]
            [garden.def :refer [defstyles]]))

(defstyles screen

  [:.not-found {:text-align "center"
                :display    "block"}

   [:img {:width (px 400)
          :height (px 400)
          :margin ["ato"]}]]

  )
