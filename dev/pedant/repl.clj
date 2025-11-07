(ns pedant.repl
  (:require
   [pedant.init :as init]
   [pedant.main :as main]))

(println "Welcome to the pedant REPL!")
(println "Initializing")
(init/install-legend!)
(main/start-db)
(require '[c3kit.bucket.api :as db])
