(ns pedant.config
  (:require [c3kit.apron.app :as app]))

(def ^:private base
  {
   :log-level      :info})

(def development
  (assoc base
    :host "http://localhost:8123"
    :log-level :trace))

(def staging
  (assoc base
    ;:host "https://pedant-staging.cleancoders.com"
    :log-level :trace))

(def production
  ;(assoc
    base
    ;:host "https://pedant.cleancoders.com")
    )

(def environment (app/find-env "cc.env" "CC_ENV"))
(def development? (= "development" environment))
(def production? (= "production" environment))

(def env
  (case environment
    "staging" staging
    "production" production
    development))

(def host (:host env))

(defn link [& parts] (apply str host parts))
