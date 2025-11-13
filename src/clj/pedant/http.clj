(ns pedant.http
  (:require [aleph.http :as aleph]
            [pedant.config :as config]
            [pedant.layouts :as layouts]
            [c3kit.apron.log :as log]
            [c3kit.apron.util :as util]
            [c3kit.wire.assets :refer [wrap-asset-fingerprint]]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.head :refer [wrap-head]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]])
  (:import (java.util.concurrent TimeUnit)))

(defn refreshable [handler-sym]
  (if config/development?
    (fn [request] (@(util/resolve-var handler-sym) request))
    (util/resolve-var handler-sym)))

(defroutes web-handler
  (refreshable 'pedant.routes/handler)
  (route/not-found (layouts/not-found)))

(defn app-handler []
  (if config/development?
    (let [wrap-verbose    (util/resolve-var 'c3kit.apron.verbose/wrap-verbose)
          refresh-handler (util/resolve-var 'c3kit.apron.refresh/refresh-handler)]
      (-> (refresh-handler 'pedant.http/web-handler)
          wrap-verbose))
    (util/resolve-var 'pedant.http/web-handler)))

(defonce root-handler
  (-> (app-handler)
      wrap-keyword-params
      wrap-multipart-params
      wrap-nested-params
      wrap-params
      wrap-cookies
      (wrap-resource "public")
      wrap-asset-fingerprint
      wrap-content-type
      wrap-not-modified
      wrap-head
      ))

(defn start [app]
  (let [port (or (some-> "PORT" System/getenv Integer/parseInt) 8124)]
    (log/info (str "Starting HTTP server: http://localhost:" port))
    (let [server (aleph/start-server root-handler {:port port})]
      (assoc app :http server))))

(defn stop [app]
  (when-let [server (:http app)]
    (log/info "Stopping HTTP server")
    (.close server)
    (.awaitTermination server 1000 TimeUnit/MILLISECONDS))
  (dissoc app :http))


