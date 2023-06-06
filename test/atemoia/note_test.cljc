(ns atemoia.note-test
  (:require [clojure.test :refer [deftest is testing]]
            [atemoia.note :as note]))


(deftest valid?
  (is (= false
        (note/valid? "")))
  (is (= true
        (note/valid? "hello"))))
