(ns pedant.llm.claude
  (:require [aleph.http :as aleph]
            [c3kit.apron.corec :as ccc]
            [c3kit.apron.env :as env]
            [c3kit.apron.log :as log]
            [c3kit.apron.utilc :as utilc]
            [clojure.string :as str]))

(defn api-key [] (env/env "ANTHROPIC_API_KEY"))

(defn parse-sse-event [raw-event]
  (when (str/starts-with? raw-event "data: ")
    (-> raw-event
        (subs 6)
        str/trim
        (utilc/<-json-kw))))

(defn- base-req [system messages]
  {:request-method :post
   :url            "https://api.anthropic.com/v1/messages"
   :as             :stream
   :headers        {"x-api-key"         (api-key)
                    "anthropic-version" "2023-06-01"
                    "content-type"      "application/json"}
   :body           {:system     system
                    :messages   messages
                    :model      "claude-sonnet-4-5-20250929"
                    :max_tokens 20480
                    :thinking   {:type "enabled" :budget_tokens 8192}
                    :stream     true}})

(defn prompt! [system messages & opts]
  (as-> (ccc/->options opts) $
        (merge (base-req system messages) $)
        (update $ :body utilc/->json)
        (aleph/request $)))