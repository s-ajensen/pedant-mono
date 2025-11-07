(ns pedant.sse
  (:require [c3kit.apron.log :as log]
            [c3kit.apron.utilc :as utilc]
            [org.httpkit.server :as http]))

(defn sse-headers []
  {"Content-Type"      "text/event-stream"
   "Cache-Control"     "no-cache"
   "Connection"        "keep-alive"
   "X-Accel-Buffering" "no"})

(defn format-sse-event
  "Formats data as an SSE event"
  ([data] (format-sse-event nil data))
  ([event-type data]
   (let [data-str  (if (string? data) data (utilc/->json data))
         event-str (if event-type (str "event: " event-type "\n") "")]
     (str event-str "data: " data-str "\n\n"))))

(defn send-sse-event!
  "Sends an SSE event to the client"
  ([channel data] (send-sse-event! channel nil data))
  ([channel event-type data]
   (http/send! channel (format-sse-event event-type data) false)))

(defn sse-response
  "Returns an SSE response using as-channel.
   handler-fn should be a function that takes [channel] and sets up event streaming."
  [handler-fn]
  (fn [request]
    (http/as-channel request
      {:on-open  (fn [ch]
                   (http/send! ch
                               {:status  200
                                :headers (sse-headers)}
                               false)
                   (handler-fn ch))
       :on-close (fn [ch status]
                   (log/debug "SSE connection closed:" status))})))