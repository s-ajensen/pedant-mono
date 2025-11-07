(ns pedant.main
  (:require [pedant.config :as config]
            [c3kit.apron.app :as app]
            [c3kit.apron.log :as log]
            [c3kit.apron.util :as util]
            [c3kit.bucket.api :as db])
  (:import (java.lang Runtime Thread)))

(defn start-env [app] (app/start-env app "cc.env" "CC_ENV"))

(def env (app/service 'pedant.main/start-env 'c3kit.apron.app/stop-env))
(def http (app/service 'pedant.http/start 'pedant.http/stop))

(def all-services [env http])
(def refresh-services [])

(defn maybe-init-dev []
  (when config/development?
    (let [refresh-init (util/resolve-var 'c3kit.apron.refresh/init)]
      (refresh-init refresh-services "pedant" ['pedant.http 'pedant.main]))))

(def start-db #(app/start! [db/service]))
(def start-all #(app/start! all-services))
(def stop-all #(app/stop! all-services))

(defn -main []
  (log/report "----- STARTING pedant SERVER -----")
  (log/report "pedant environment: " config/environment)
  (log/set-level! (config/env :log-level :warn))
  (maybe-init-dev)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-all))
  (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown-agents))
  (start-all))
