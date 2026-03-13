(ns k16.c
  (:require
   [k16.b :as b]))

(defn return-one-via-b []
  (b/also-return-one))
