(ns pedant.sse-spec
  (:require [org.httpkit.server :as http]
            [pedant.sse :as sut]
            [speclj.core :refer :all]
            [speclj.stub :as stub]))

(declare sse-handler)
(def channel (atom nil))

(describe "Server Side Events API"

  (with-stubs)

  (context "->event"

    (it "comment"
      (should= ": comment\n\n" (sut/->event)))

    (it "with data"
      (should= "data: foo\n\n" (sut/->event "foo")))

    (it "with data and type"
      (should= "event: foo\ndata: bar\n\n" (sut/->event "foo" "bar")))
    )

  (context "wrap-sse"

    (with sse-handler (fn [chan] (reset! chan :foo)))
    (redefs-around [http/as-channel (stub :as-chan {:return channel})
                    http/send! (stub :http/send!)])
    (before (reset! channel nil))

    (it "sends initial request by default"
      (let [handler (sut/wrap-sse @sse-handler)
            _       (handler {})
            [_ {:keys [on-open]}] (stub/last-invocation-of :as-chan)
            _       (on-open channel)
            [chan {:keys [headers status] :as _msg} close?] (stub/last-invocation-of :http/send!)]
        (should= channel chan)
        (should= 200 status)
        (should= "text/event-stream" (get headers "Content-Type"))
        (should= "no-cache" (get headers "Cache-Control"))
        (should= "keep-alive" (get headers "Connection"))
        (should= "no" (get headers "X-Accel-Buffering"))
        (should-not close?)))

    (it "invokes handler by default"
      (let [handler (sut/wrap-sse @sse-handler)
            _       (handler {})
            [_ {:keys [on-open]}] (stub/last-invocation-of :as-chan)
            _       (on-open channel)]
        (should= :foo @channel)))
    )
  )