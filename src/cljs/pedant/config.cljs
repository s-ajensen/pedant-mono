(ns pedant.config
  (:require [reagent.core :as reagent]))

(def state (reagent/atom {}))

(defn install! [new-config]
  (when new-config
    (assert (map? new-config) "Config must come as a map")
    (swap! state merge new-config)))

(def environment (reagent/track #(:environment @state)))
(def development? (reagent/track #(= "development" @environment)))
(def production? (reagent/track #(= "production" @environment)))
