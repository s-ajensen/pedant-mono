(ns pedant.routes
  (:require [c3kit.apron.log :as log]
            [c3kit.apron.util :as util]
            [compojure.core :as compojure :refer [defroutes routes]]
            [org.httpkit.server :as http]
            [pedant.config :as config]
            [pedant.sse :as sse]))

(def resolve-handler
  (if config/development?
    (fn [handler-sym] (util/resolve-var handler-sym))
    (memoize (fn [handler-sym] (util/resolve-var handler-sym)))))

(defn lazy-handle
  "Reduces load burden of this ns, which is useful in development.
  Runtime errors will occur for missing handlers, but all the routes should be tested in routes_spec.
  Assumes all handlers take one parameter, request."
  [handler-sym request]
  (let [handler (resolve-handler handler-sym)]
    (handler request)))

(defmacro lazy-routes
  "Creates compojure route for each entry where the handler is lazily loaded.
  Why are params a hash-map instead of & args? -> Intellij nicely formats hash-maps as tables :-)"
  [table]
  `(routes
     ~@(for [[[path method] handler-sym] table]
         (let [method (if (= :any method) nil method)]
           (compojure/compile-route method path 'req `((lazy-handle '~handler-sym ~'req)))))))

(def web-routes-handlers
  (lazy-routes
    {
     ["/" :get] pedant.layouts/web-rich-client
     }))

(def sse-route-handlers
  (-> (lazy-routes
        {
         ;["/sse" :get] pedant.routes/test-handler
         })
      ;sse/wrap-sse
      ))

(defroutes handler
  web-routes-handlers
  sse-route-handlers
  )