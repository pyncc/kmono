(ns k16.c-test
  (:require
   [clojure.test :refer [deftest is]]
   [k16.c :as c]))

(deftest should-return-one-via-b
  (is (= 1 (c/return-one-via-b))))
