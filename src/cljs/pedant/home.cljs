(ns pedant.home
  (:require [c3kit.wire.js :as wjs]
            [clojure.string :as str]
            [oxbow.core :as ox]
            [pedant.page :as page]
            [reagent.core :as r]))

(defn num-field [num-ratom name from-fn to-fn valid?-fn]
  (let [label (str (str/upper-case (subs name 0 1))
                   (subs name 1)
                   " ")]
    [:div.num-field
     [:label {:id (str name "-label") :for name} label]
     [:input {:id        name
              :type      "text"
              :name      name
              :value     (when-not (str/blank? @num-ratom)
                           (from-fn @num-ratom))
              :style     {:text-align "right"}
              :on-change (fn [e]
                           (let [val (wjs/e-text e)]
                             (when (valid?-fn val)
                               (reset! num-ratom (to-fn val)))))}]]))

(def default "0")
(def num (r/atom default))

; region binary
(defn present-binary [s]
  (->> (str/reverse s)
       (partition-all 4)
       (map (partial apply str))
       (str/join " ")
       (str/reverse)))
(defn coerce-binary [s]
  (str/replace s " " ""))
(defn valid-binary? [s]
  (str/blank? (str/replace-all s #"1|0" "")))               ; endregion
; region decimal
(defn binary->decimal [s]
  (js/parseInt s 2))
(defn decimal->binary [s]
  (when-not (str/blank? s)
    (js-invoke (js/parseInt s) "toString" 2)))
(defn valid-decimal? [s]
  (or (= "0" s)
      (and (str/blank? (str/replace-all s #"[0-9]" ""))
           (not (str/starts-with? s "0")))))                ;endregion
;region hex
(defn binary->hex [s]
  (let [parsed (js/parseInt s 2)
        hex    (.toString parsed 16)
        hex    (if (< parsed 16)
                 (str "0" hex)
                 hex)]
    (str "0x" hex)))
(defn hex->binary [s]
  (let [n (subs s 2 (count s))
        n (if (str/blank? n) "0" n)]
    (-> n
        (js/parseInt 16)
        (.toString 2))))
(defn valid-hex? [s]
  (str/starts-with? s "0x"))

(defn home []
  (fn []
    [:div.homepage-container
     [:h1 "pedant - The Simple Base Converter"]
     [num-field num "binary" present-binary coerce-binary valid-binary?]
     [num-field num "decimal" binary->decimal decimal->binary valid-decimal?]
     [num-field num "hexadecimal" binary->hex hex->binary valid-hex?]]))

(defmethod page/render :home [_]
  [home])