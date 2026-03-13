(ns k16.c.dev
  "Dev utilities for package c, available when :c-dev alias is active.

  This namespace is only on the classpath when package c is targeted
  (via -F or by running kmono from packages/c), demonstrating how
  per-package aliases can scope dev tooling to specific packages."
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [k16.c :as c]))

(defn go []
  (refresh)
  (println "Package c reloaded. return-one-via-b =" (c/return-one-via-b)))
