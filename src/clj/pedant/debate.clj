(ns pedant.debate
  (:require [c3kit.apron.log :as log]
            [c3kit.apron.utilc :as utilc]
            [clj-commons.byte-streams :as bs]
            [clojure.string :as str]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [pedant.llm.claude :as claude]
            [pedant.sse :as sse]))

(defn handle-line [stream line]
  (when (str/starts-with? line "data:")
    (let [payload (utilc/<-json-kw (subs line 6))]
      (s/put! stream
              (sse/->event
                (or (:text (:delta payload))
                    (:thinking (:delta payload))))))))

(defn handle-lines [x stream]
  (d/chain x
    (fn [{:keys [body]}]
      (let [line-stream (bs/to-line-seq body)]
        (s/consume
          (partial handle-line stream)
          line-stream)))
    (fn [_]
      (s/close! stream))))

(defn wrap-errors [x stream]
  (letfn [(log-n-close! [e]
            (log/error e "Error in SSE stream")
            (s/close! stream))]
    (d/catch x log-n-close!)))

(defn api-debate [_request]
  (clojure.pprint/pprint _request)
  (let [response-stream (s/stream)]
    (-> (claude/prompt!
          "You're grumpy but ultimately kind-hearted Eastern Orthodox Priest"
          [{:role "user" :content "I spent too much time poasting on OrthoTwitter and missed vigil last night."}])
        (handle-lines response-stream)
        (wrap-errors response-stream))
    {:status  200
     :headers sse/sse-headers
     :body    response-stream}))