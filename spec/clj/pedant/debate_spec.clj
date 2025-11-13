(ns pedant.debate-spec
  (:require [c3kit.apron.log :as log]
            [clj-commons.byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [pedant.debate :as sut]
            [pedant.llm.claude :as claude]
            [speclj.core :refer :all]))

(declare stream)

(describe "Debate API"

  (context "handle-line"

    (with stream (s/stream))

    (it "puts text delta events onto stream"
      (let [line "data: {\"delta\":{\"text\":\"Hello\"}}"]
        (sut/handle-line @stream line)
        (should-contain "Hello" @(s/take! @stream))))

    (it "puts thinking delta events onto stream"
      (let [line "data: {\"delta\":{\"thinking\":\"Pondering...\"}}"]
        (sut/handle-line @stream line)
        (should-contain "Pondering..." @(s/take! @stream))))

    (it "prefers text over thinking when both present"
      (let [line "data: {\"delta\":{\"text\":\"Text\",\"thinking\":\"Think\"}}"]
        (sut/handle-line @stream line)
        (should-contain "Text" @(s/take! @stream))))

    (it "handles neither text nor thinking"
      (let [line "data: {\"delta\":{\"other\":\"stuff\"}}"]
        (sut/handle-line @stream line)
        (should-contain ": comment" (deref (s/try-take! @stream 10)))))

    (it "ignores non-data lines"
      (let [line "event: ping"]
        (sut/handle-line @stream line)
        (should-be-nil (deref (s/try-take! @stream 10)))))

    (it "ignores lines without data: prefix"
      (let [line "{\"delta\":{\"text\":\"Hello\"}}"]
        (sut/handle-line @stream line)
        (should-be-nil (deref (s/try-take! @stream 10))))))

  (context "handle-lines"

    (it "processes all lines from body and closes stream"
      (with-redefs [bs/to-line-seq (fn [body]
                                     (s/->source
                                       ["data: {\"delta\":{\"text\":\"A\"}}"
                                        "data: {\"delta\":{\"text\":\"B\"}}"
                                        "data: {\"delta\":{\"text\":\"C\"}}"]))]
        (let [stream   (s/stream)
              deferred (d/success-deferred {:body ::mock-body})]
          @(sut/handle-lines deferred stream)

          (should-contain "A" @(s/take! stream))
          (should-contain "B" @(s/take! stream))
          (should-contain "C" @(s/take! stream))

          (should (s/closed? stream)))))

    (it "closes stream even with empty body"
      (with-redefs [bs/to-line-seq (fn [_] (s/->source []))]
        (let [stream   (s/stream)
              deferred (d/success-deferred {:body ::mock-body})]
          @(sut/handle-lines deferred stream)
          (should (s/closed? stream)))))

    #_(it "handles malformed JSON gracefully"
        (with-redefs [bs/to-line-seq (fn [_]
                                       (s/->source
                                         ["data: not-json"
                                          "data: {\"delta\":{\"text\":\"OK\"}}"]))]
          (let [stream   (s/stream)
                deferred (d/success-deferred {:body ::mock-body})]
            (should-throw @(sut/handle-lines deferred stream))))))

  (context "wrap-errors"

    #_(it "logs errors and closes stream on failure"
        (let [logged-errors (atom [])
              stream        (s/stream)
              error         (ex-info "Boom!" {:reason :test})]
          (with-redefs [log/error (fn [e msg] (swap! logged-errors conj [e msg]))]
            (let [failed-deferred (d/error-deferred error)]
              @(sut/wrap-errors failed-deferred stream)

              ;(should= 1 (count @logged-errors))
              ;(should= error (ffirst @logged-errors))
              ;(should= "Error in SSE stream" (second (first @logged-errors)))

              (should (s/closed? stream))))))

    (it "passes through successful results"
      (let [stream           (s/stream)
            success-deferred (d/success-deferred ::result)]
        (should= ::result @(sut/wrap-errors success-deferred stream)))))

  (context "api-debate"

    (it "returns SSE response with streaming body"
      (with-redefs [claude/prompt! (fn [& _]
                                     (d/success-deferred
                                       {:body (bs/to-input-stream
                                                "data: {\"delta\":{\"text\":\"Hi\"}}\n\n")}))]
        (let [response (sut/api-debate {})]
          (should= 200 (:status response))
          (should= "text/event-stream" (get-in response [:headers "Content-Type"]))
          (should= "no-cache" (get-in response [:headers "Cache-Control"]))

          (should (s/stream? (:body response)))

          (Thread/sleep 100)

          (should-contain "Hi" @(s/take! (:body response) 1000)))))

    #_(it "handles claude API errors gracefully"
      (let [logged-errors (atom [])]
        (with-redefs [claude/prompt! (fn [& _]
                                       (d/error-deferred
                                         (ex-info "API Error" {:status 500})))
                      log/error      (stub :log/error {:invoke (fn [e msg] (swap! logged-errors conj [e msg]))})]
          (let [response (sut/api-debate {})]
            (should= 200 (:status response))

            (Thread/sleep 100)

            (should= 1 (count @logged-errors))

            (should (s/closed? (:body response)))))))

    (it "closes stream when client disconnects"
      (with-redefs [claude/prompt! (fn [& _]
                                     (let [body-stream (s/stream)]
                                       (future
                                         (Thread/sleep 5000)
                                         (s/put! body-stream "data: late\n\n"))
                                       (d/success-deferred {:body body-stream})))]
        (let [response    (sut/api-debate {})
              body-stream (:body response)]
          (s/close! body-stream)

          (should (s/closed? body-stream))))))

  #_(context "integration: full SSE flow"

      (it "streams multiple events from claude to client"
        (with-redefs [claude/prompt! (fn [& _]
                                       (d/success-deferred
                                         {:body (bs/to-input-stream
                                                  (str "data: {\"delta\":{\"text\":\"Hello\"}}\n\n"
                                                       "data: {\"delta\":{\"text\":\" \"}}\n\n"
                                                       "data: {\"delta\":{\"text\":\"world\"}}\n\n"))}))]
          (let [response    (sut/api-debate {})
                body-stream (:body response)]
            (Thread/sleep 100)

            (let [events (atom [])]
              (s/consume #(swap! events conj %) body-stream)
              (Thread/sleep 100)

              (should-contain "data: Hello\n\n" @events)
              (should-contain "data:  \n\n" @events)
              (should-contain "data: world\n\n" @events)))))))