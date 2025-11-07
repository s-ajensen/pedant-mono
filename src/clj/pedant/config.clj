(ns pedant.config
  (:require [c3kit.apron.app :as app]))

(def ^:private base
  {
   :analytics-code "console.log('google analytics would have loaded for this page');"
   :log-level      :info})

(def development
  (assoc base
    :host "http://localhost:8123"
    :log-level :trace))

(def staging
  (assoc base
    :host "https://pedant-staging.cleancoders.com"
    :log-level :trace))

(def production
  (assoc base
    :host "https://pedant.cleancoders.com"
    :analytics-code "console.log('Replace me with Real Google Analytics Code.');"))

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
