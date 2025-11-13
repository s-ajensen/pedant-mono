(ns pedant.fs-spec
  (:require [pedant.fs :as sut]
            [speclj.core :refer :all])
  (:import (java.io File)
           (pedant.fs FileStore MemoryStore)))

(declare store)
(declare temp-dir)

(describe "file persistence"

  (context "mem store"

    (with store (MemoryStore. (atom {})))

    (it "writes content to path"
      (sut/write! @store "/tmp/test.edn" "{:foo :bar}")
      (should= "{:foo :bar}" (sut/read @store "/tmp/test.edn")))

    (it "overwrites existing content"
      (sut/write! @store "/tmp/test.edn" "first")
      (sut/write! @store "/tmp/test.edn" "second")
      (should= "second" (sut/read @store "/tmp/test.edn")))

    (it "returns nil for non-existent paths"
      (should-be-nil (sut/read @store "/does/not/exist")))

    (it "lists all paths"
      (sut/write! @store "/tmp/a.edn" "a")
      (sut/write! @store "/tmp/b.edn" "b")
      (should= #{"/tmp/a.edn" "/tmp/b.edn"} (set (sut/paths @store))))

    (it "deletes content at path"
      (sut/write! @store "/tmp/test.edn" "data")
      (sut/delete! @store "/tmp/test.edn")
      (should-be-nil (sut/read @store "/tmp/test.edn"))))

  (context "file store"
    (with temp-dir (str "/tmp/persistence-test-" (System/currentTimeMillis)))
    (with store (FileStore. @temp-dir))

    (after
      (sut/delete-all! @store))

    (it "writes and reads from actual files"
      (let [path (str @temp-dir "/test.edn")]
        (sut/write! @store path "{:real :file}")
        (should= "{:real :file}" (sut/read @store path))
        (should (.exists (File. path)))))))