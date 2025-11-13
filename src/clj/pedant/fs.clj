(ns pedant.fs
  (:require [clojure.java.io :as io]))

(defprotocol Store
  (read [this path])
  (write! [this path contents])
  (delete! [this path])
  (paths [this])
  (delete-all! [this]))

(defrecord MemoryStore [data]
  Store
  (read [_ path]
    (get @data path))

  (write! [_ path contents]
    (swap! data assoc path contents))

  (delete! [_ path]
    (swap! data dissoc path))

  (paths [_]
    (keys @data))

  (delete-all! [_]
    (reset! data {})))

(defrecord FileStore [base-dir]
  Store
  (read [_ path]
    (when (.exists (io/file path))
      (slurp path)))

  (write! [_ path contents]
    (io/make-parents path)
    (spit path contents))

  (delete! [_ path]
    (io/delete-file path true))

  (paths [_]
    (->> (io/file base-dir)
         file-seq
         (filter #(.isFile %))
         (map #(.getPath %))))

  (delete-all! [this]
    (doseq [path (paths this)]
      (delete! this path))))
