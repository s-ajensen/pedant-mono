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
