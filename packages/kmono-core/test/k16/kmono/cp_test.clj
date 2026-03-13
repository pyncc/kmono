(ns k16.kmono.cp-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [k16.kmono.core.config :as core.config]
   [k16.kmono.core.packages :as core.packages]
   [k16.kmono.cp :as kmono.cp]
   [k16.kmono.test.helpers.repo :refer [*repo* with-test-repo] :as helpers.repo]
   [matcher-combinators.test]))

(use-fixtures :once with-test-repo)

(deftest collect-aliases-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        ;; The duplication here is intentional - it's to test the unique
        ;; filtering behavior.
        config (merge config {:aliases [:local :local :another]})

        aliases (kmono.cp/collect-aliases config packages)]

    (is (= [:kmono/packages :local :another] aliases))))

(deftest generate-cp-command-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        cmd (kmono.cp/generate-classpath-command *repo* config packages)]

    (is (= "clojure -Sdeps '{:aliases {:a/test {:extra-paths [\"packages/a/test\"], :extra-deps #:local{excluded #:local{:root \"packages/excluded\"}}}, :kmono/packages {:extra-deps #:com.kepler16{a #:local{:root \"packages/a\"}, b #:local{:root \"packages/b\"}}}}}' -A:kmono/packages -Spath"
           cmd))))

(deftest generate-cp-command-with-aliases-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        cmd (kmono.cp/generate-classpath-command
             *repo*
             (merge config
                    {:aliases [:local]})
             packages)]

    (is (= "clojure -Sdeps '{:aliases {:a/test {:extra-paths [\"packages/a/test\"], :extra-deps #:local{excluded #:local{:root \"packages/excluded\"}}}, :kmono/packages {:extra-deps #:com.kepler16{a #:local{:root \"packages/a\"}, b #:local{:root \"packages/b\"}}}}}' -A:kmono/packages:local -Spath"
           cmd))))

(deftest collect-aliases-with-per-package-aliases-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        ;; Simulate per-package alias injection (as run-clojure does)
        pkg-aliases (->> (vals packages)
                         (mapcat :aliases)
                         (remove nil?)
                         vec)
        config (cond-> config
                 (seq pkg-aliases) (update :aliases into pkg-aliases))

        aliases (kmono.cp/collect-aliases config packages)]

    (is (some #{:workspace/shared} aliases))))

(deftest run-command-per-package-alias-injection-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        ;; Simulate the command-fn that run-command builds per package
        aliases [:test]
        flag "-M"
        build-cmd (fn [pkg]
                    (let [pkg-aliases (:aliases pkg)
                          all-aliases (into aliases (or pkg-aliases []))]
                      (into ["clojure" (str flag (kmono.cp/serialize-aliases all-aliases))] [])))

        ;; Package a has :kmono/package {:aliases [:workspace/shared]}
        cmd-a (build-cmd (get packages 'com.kepler16/a))
        ;; Package b has no per-package aliases
        cmd-b (build-cmd (get packages 'com.kepler16/b))]

    (is (= ["clojure" "-M:test:workspace/shared"] cmd-a))
    (is (= ["clojure" "-M:test"] cmd-b))))

(deftest resolve-classpath-test
  (let [config (core.config/resolve-workspace-config *repo*)
        packages (core.packages/resolve-packages *repo* config)

        cp (kmono.cp/resolve-classpath *repo* config packages)]

    ;; This is pretty much just testing that the command doesn't fail.
    (is (string? cp))))
