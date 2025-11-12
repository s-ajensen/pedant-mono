(ns pedant.llm.claude
  (:require [aleph.http :as aleph]
            [c3kit.apron.corec :as ccc]
            [c3kit.apron.env :as env]
            [c3kit.apron.log :as log]
            [c3kit.apron.utilc :as utilc]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [manifold.stream :as s]
            [medley.core :as medley]))

(defn api-key [] (env/env "ANTHROPIC_API_KEY"))

(defn parse-sse-event [raw-event]
  (when (str/starts-with? raw-event "data: ")
    (-> raw-event
        (subs 6)
        str/trim
        (utilc/<-json-kw))))

(defn- handle-streaming-response [response ch]
  (let [body-stream (:body response)
        buffer      (atom "")]
    @(s/consume
       (fn [chunk]
         (let [text  (str @buffer (String. chunk "UTF-8"))
               lines (str/split text #"\n" -1)]
           (reset! buffer (last lines))
           (doseq [line (butlast lines)
                   :when (str/starts-with? line "data: ")
                   :let [parsed (parse-sse-event line)]
                   :when parsed]
             (async/>!! ch parsed))))
       body-stream)))

(defn- handle-json-response [response ch]
  (let [body-str @(s/reduce (fn [acc chunk]
                              (str acc (String. chunk "UTF-8")))
                            ""
                            (:body response))]
    (async/>!! ch (utilc/<-json-kw body-str))))

(defn- do-request! [merged-opts streaming?]
  (let [headers (cond-> (:headers merged-opts)
                        streaming? (assoc "accept" "text/event-stream"))
        body    (utilc/->json (:body merged-opts))]
    @(aleph/post "https://api.anthropic.com/v1/messages"
                 (cond-> {:headers headers :body body}
                         streaming? (assoc :raw-stream? true)))))

(def default-request
  {:headers {"anthropic-version" "2023-06-01"
             "content-type"      "application/json"}
   :body    {:model      "claude-sonnet-4-5-20250929"
             :max_tokens 20480
             :thinking   {:type "enabled" :budget_tokens 8192}}})

(defn prompt! [system messages & opts]
  (log/info "-> claude")
  (let [req         {:body    {:system system :messages messages}
                     :headers {"x-api-key" (api-key)}}
        merged-opts (medley/deep-merge default-request req (ccc/->options opts))
        streaming?  (get-in merged-opts [:body :stream])
        ch          (async/chan 100)]
    (try
      (let [response (do-request! merged-opts streaming?)]
        (if streaming?
          (handle-streaming-response response ch)
          (handle-json-response response ch)))
      (catch Exception e
        (log/error "Request error:" e))
      (finally
        (async/close! ch)
        (log/info "<- claude")))
    ch))