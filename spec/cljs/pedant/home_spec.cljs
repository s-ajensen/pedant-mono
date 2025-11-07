(ns pedant.home-spec
  (:require-macros [c3kit.wire.spec-helperc :refer [should-not-select should-select]]
                   [speclj.core :refer [before describe context it should= should-contain with-stubs]])
  (:require [pedant.home :as sut]
            [pedant.layout :as layout]
            [pedant.page :as page]
            [c3kit.wire.spec-helper :as wire-helper]
            [c3kit.wire.spec-helper :as wire]
            [speclj.core]
            ))

(defn mock-parse [s]
  (str s "-parsed"))
(defn mock-valid? [s]
  (> 2 (count s)))

(describe "Home page"

  (wire-helper/with-root-dom)

  (before (reset! sut/num sut/default)
          (page/install-page! :home)
          (wire/render [layout/default]))

  (context "number fields"

    (context "binary"

      (it "label"
        (should= "Binary " (wire/text! "#binary-label")))

      (it "default value"
        (should= "0" (wire/value "#binary")))

      (it "inputs 0 & 1"
        (wire/change! "#binary" "0110")
        (should= "0110" (wire/value "#binary")))

      (it "only allows 0 & 1"
        (wire/change! "#binary" "02")
        (should= "0" (wire/value "#binary")))

      (it "segments every 3 characters"
        (wire/change! "#binary" "011001")
        (should= "01 1001" (wire/value "#binary")))

      (it "removes spaces"
        (wire/change! "#binary" "0110 1001")
        (should= "0110 1001" (wire/value "#binary")))

      (it "updates others"
        (wire/change! "#binary" "11")
        (should= "11" (wire/value "#binary"))
        (should= "3" (wire/value "#decimal"))))

    (context "decimal"

      (it "label"
        (should= "Decimal " (wire/text! "#decimal-label")))

      (it "default value"
        (should= "0" (wire/value "#decimal")))

      (it "inputs 0 - 9"
        (wire/change! "#decimal" "9876543210")
        (should= "9876543210" (wire/value "#decimal")))

      (it "disallows leading 0's"
        (wire/change! "#decimal" "01")
        (should= "0" (wire/value "#decimal")))

      (it "only allows 0 - 9"
        (wire/change! "#decimal" "asdf")
        (should= "0" (wire/value "#decimal")))

      (it "allows 0"
        (wire/change! "#decimal" "")
        (wire/change! "#decimal" "0")
        (should= "0" (wire/value "#decimal")))

      ; TODO - allow negatives & change "binary" to 2's comp
      )

    (context "hexadecimal"

      (it "label"
        (should= "Hexadecimal " (wire/text! "#hexadecimal-label")))

      (it "default value"
        (should= "0x00" (wire/value "#hexadecimal")))

      (it "values less than 10"
        (wire/change! "#decimal" "9")
        (should= "0x09" (wire/value "#hexadecimal")))

      (it "values less than 16"
        (wire/change! "#decimal" "10")
        (should= "0x0a" (wire/value "#hexadecimal")))

      (it "values greater than 16"
        (wire/change! "#decimal" "26")
        (should= "0x1a" (wire/value "#hexadecimal")))

      (it "accepts 0x as 0"
        (wire/change! "#hexadecimal" "0x")
        (should= "0" (wire/value "#decimal")))

      (it "requires '0x' prefix"
        (wire/change! "#hexadecimal" "1234")
        (should= "0x00" (wire/value "#hexadecimal")0)))))
