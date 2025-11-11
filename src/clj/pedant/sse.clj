(ns pedant.sse
  (:require [c3kit.apron.log :as log]
            [org.httpkit.server :as http]))

(defn ->event
  ([] (->event nil))
  ([data] (->event nil data))
  ([event-type data]
   (let [event-str (if event-type (str "event: " event-type "\n") "")]
     (if (or event-type data)
       (str event-str "data: " data "\n\n")
       ": comment\n\n"))))

; (http/send! channel (->event event-type data) false)

(def sse-headers
  {"Content-Type"      "text/event-stream"
   "Cache-Control"     "no-cache"
   "Connection"        "keep-alive"
   "X-Accel-Buffering" "no"})

(defn wrap-sse [sse-handler & opts]
  (fn [request]
    (http/as-channel request
      {:on-open  (fn [ch]
                   (http/send! ch
                               {:status  200
                                :headers sse-headers}
                               false)
                   (sse-handler ch))
       :on-close (fn [ch status]
                   (log/debug "SSE connection closed:" status))})))

; captain hook type character: "lost boys"
; ---
; parable about how nothing we think we own we do own