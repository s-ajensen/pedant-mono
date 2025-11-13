(ns pedant.llm.claude-spec
  (:require [clj-commons.byte-streams :as bs]
            [pedant.llm.claude :as sut]
            [c3kit.apron.env :as env]
            [c3kit.apron.utilc :as utilc]
            [aleph.http :as aleph]
            [clojure.core.async :as async]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [speclj.core :refer :all]
            [speclj.stub :as stub]))

(declare response)

(describe "prompt!"

  (with-stubs)

  (context "parse-sse-event"

    (it "parses valid SSE data line"
      (should= {:type "test" :data "value"}
        (sut/parse-sse-event "data: {\"type\":\"test\",\"data\":\"value\"}")))

    (it "returns nil for non-data lines"
      (should-be-nil (sut/parse-sse-event "event: message"))
      (should-be-nil (sut/parse-sse-event ": comment")))

    (it "trims whitespace"
      (should= {:type "test"}
        (sut/parse-sse-event "data:   {\"type\":\"test\"}  "))))
  )