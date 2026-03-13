(ns k16.kmono.core.deps-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is use-fixtures]]
   [k16.kmono.core.config :as core.config]
   [k16.kmono.core.deps :as core.deps]
   [k16.kmono.core.fs :as core.fs]
   [k16.kmono.core.packages :as core.packages]
   [k16.kmono.test.helpers.repo :refer [*repo* with-test-repo] :as helpers.repo]
   [matcher-combinators.test]))

(use-fixtures :each with-test-repo)

(deftest resolve-package-aliases-test
  (fs/write-bytes (fs/file *repo* "deps.local.edn")
                  (.getBytes (prn-str {:aliases {:local {:extra-paths ["local"]}}})))

  (let [config (core.config/resolve-workspace-config *repo*)

        packages (core.packages/resolve-packages *repo* config)

        sdeps (core.deps/generate-sdeps-aliases *repo* packages)]

    (is (match? {:local {:extra-paths ["local"]}

                 :kmono/packages {:extra-deps {'com.kepler16/a {:local/root "packages/a"}
                                               'com.kepler16/b {:local/root "packages/b"}}}

                 :a/test
                 {:extra-paths ["packages/a/test"]
                  :extra-deps {'local/excluded {:local/root "packages/excluded"}}}}
                sdeps))))

(deftest resolve-package-aliases-from-subdir-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)
        base-dir (str (fs/file *repo* "packages/a"))

        sdeps (core.deps/generate-sdeps-aliases *repo* base-dir packages)]

    (is (match? {:kmono/packages {:extra-deps {'com.kepler16/a {:local/root "."}
                                                'com.kepler16/b {:local/root "../b"}}}

                 :a/test
                 {:extra-paths ["test"]
                  :extra-deps {'local/excluded {:local/root "../excluded"}}}}
                sdeps))))

(deftest root-aliases-injected-into-sdeps-from-subdir-test
  ;; Add a workspace-level alias to root deps.edn — should appear in -Sdeps
  ;; with paths rewritten relative to base-dir when running from a package subdir.
  (let [existing-root (core.fs/read-edn-file! (fs/file *repo* "deps.edn"))]
    (fs/write-bytes
      (fs/file *repo* "deps.edn")
      (.getBytes (prn-str (assoc existing-root
                                 :aliases {:workspace/shared {:extra-paths ["shared"]}})))))

  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)
        base-dir (str (fs/file *repo* "packages/a"))

        sdeps (core.deps/generate-sdeps-aliases *repo* base-dir packages)]

    (is (match? {:workspace/shared {:extra-paths ["../../shared"]}}
                sdeps))

    ;; Root aliases are NOT injected into -Sdeps when running from project root
    ;; (clojure reads the root deps.edn directly in that case).
    (let [sdeps-from-root (core.deps/generate-sdeps-aliases *repo* packages)]
      (is (not (contains? sdeps-from-root :workspace/shared))))))

(deftest filter-package-aliases-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)]

    (is (= {'com.kepler16/a #{:test}}
           (core.deps/filter-package-aliases [:*/*] packages)))

    (is (= {'com.kepler16/a #{:test}}
           (core.deps/filter-package-aliases [:a/*] packages)))

    (is (= {'com.kepler16/a #{:test}}
           (core.deps/filter-package-aliases [:*/test] packages)))

    (is (= {'com.kepler16/a #{:test}}
           (core.deps/filter-package-aliases [:a/test] packages)))

    (is (= {}
           (core.deps/filter-package-aliases [:*/unknown] packages)))))
