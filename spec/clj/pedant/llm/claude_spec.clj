(ns pedant.llm.claude-spec
  (:require [pedant.llm.claude :as sut]
            [c3kit.apron.env :as env]
            [c3kit.apron.utilc :as utilc]
            [aleph.http :as aleph]
            [clojure.core.async :as async]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [speclj.core :refer :all]
            [speclj.stub :as stub]))

(def system-prompt "You are helpful")
(def test-messages [{:role "user" :content "Hi"}])

(def claude-response {:id      "msg_123"
                      :content [{:type "text" :text "Hello there"}]
                      :usage   {:input_tokens 10 :output_tokens 5}})

(def streaming-chunk-1 "data: {\"type\":\"content_block_delta\",\"delta\":{\"text\":\"Hello\"}}\n\n")
(def streaming-chunk-2 "data: {\"type\":\"content_block_delta\",\"delta\":{\"text\":\" there\"}}\n\n")
(def streaming-chunk-3 "data: {\"type\":\"message_stop\"}\n\n")

(defn json-stream []
  (let [stream (s/stream)]
    (s/put! stream (.getBytes (utilc/->json claude-response) "UTF-8"))
    (s/close! stream)
    stream))

(defn sse-stream []
  (let [stream (s/stream)]
    (s/put! stream (.getBytes streaming-chunk-1 "UTF-8"))
    (s/put! stream (.getBytes streaming-chunk-2 "UTF-8"))
    (s/put! stream (.getBytes streaming-chunk-3 "UTF-8"))
    (s/close! stream)
    stream))

(describe "prompt!"

  (with-stubs)

  (redefs-around [env/env (stub :env {:return "test-key"})])

  (context "non-streaming"

    (redefs-around [aleph/post (stub :aleph/post
                                     {:invoke (fn [_ _]
                                                (d/success-deferred
                                                  {:status 200 :body (json-stream)}))})])

    (focus-it "makes request with default configuration"
      (let [ch     (sut/prompt! system-prompt test-messages)
            result (async/<!! ch)
            [url opts] (stub/last-invocation-of :aleph/post)]
        (prn "result: " result)
        (should= "msg_123" (:id result))
        (should= "https://api.anthropic.com/v1/messages" url)
        (should= {"x-api-key"         "test-key"
                  "anthropic-version" "2023-06-01"
                  "content-type"      "application/json"}
          (:headers opts))
        (should= {:model      "claude-sonnet-4-5-20250929"
                  :max_tokens 20480
                  :system     system-prompt
                  :messages   test-messages
                  :thinking   {:type "enabled" :budget_tokens 8192}}
          (utilc/<-json-kw (:body opts)))))

    (it "merges opts as keyword args"
      (let [ch (sut/prompt! system-prompt test-messages :body {:temperature 0.5})]
        (async/<!! ch)
        (let [[_ opts] (stub/last-invocation-of :aleph/post)
              body (utilc/<-json-kw (:body opts))]
          (should= 0.5 (:temperature body))
          (should= 20480 (:max_tokens body)))))

    (it "merges opts as map"
      (let [ch (sut/prompt! system-prompt test-messages {:body {:temperature 0.7}})]
        (async/<!! ch)
        (let [[_ opts] (stub/last-invocation-of :aleph/post)
              body (utilc/<-json-kw (:body opts))]
          (should= 0.7 (:temperature body)))))

    (it "allows overriding model"
      (let [ch (sut/prompt! system-prompt test-messages :body {:model "claude-opus-4-20250514"})]
        (async/<!! ch)
        (let [[_ opts] (stub/last-invocation-of :aleph/post)
              body (utilc/<-json-kw (:body opts))]
          (should= "claude-opus-4-20250514" (:model body)))))

    (it "allows disabling thinking"
      (let [ch (sut/prompt! system-prompt test-messages :body {:thinking nil})]
        (async/<!! ch)
        (let [[_ opts] (stub/last-invocation-of :aleph/post)
              body (utilc/<-json-kw (:body opts))]
          (should-be-nil (:thinking body)))))

    (it "closes channel after response"
      (let [ch (sut/prompt! system-prompt test-messages)]
        (async/<!! ch)
        (Thread/sleep 50)
        (should-be-nil (async/<!! ch)))))

  (context "streaming"

    (redefs-around [aleph/post (stub :aleph/post
                                     {:invoke (fn [_ opts]
                                                (d/success-deferred
                                                  {:status 200 :body (sse-stream)}))})])

    (it "sets accept header for streaming"
      (let [ch (sut/prompt! system-prompt test-messages :body {:stream true})]
        (async/<!! ch)
        (let [[_ opts] (stub/last-invocation-of :aleph/post)]
          (should= "text/event-stream" (get-in opts [:headers "accept"]))
          (should= true (:raw-stream? opts)))))

    (it "parses SSE events and puts them on channel"
      (let [ch      (sut/prompt! system-prompt test-messages :body {:stream true})
            event1  (async/<!! ch)
            event2  (async/<!! ch)
            event3  (async/<!! ch)]
        (should= "content_block_delta" (:type event1))
        (should= "Hello" (get-in event1 [:delta :text]))
        (should= "content_block_delta" (:type event2))
        (should= " there" (get-in event2 [:delta :text]))
        (should= "message_stop" (:type event3))))

    (it "closes channel after streaming completes"
      (let [ch (sut/prompt! system-prompt test-messages :body {:stream true})]
        (async/<!! ch)
        (async/<!! ch)
        (async/<!! ch)
        (Thread/sleep 50)
        (should-be-nil (async/<!! ch)))))

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