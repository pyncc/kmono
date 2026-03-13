(ns k16.kmono.cli.commands.repl
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [k16.kmono.cli.commands.clojure :as commands.clojure]
   [k16.kmono.cli.common.context :as common.context]
   [k16.kmono.cli.common.opts :as opts]
   [k16.kmono.core.packages :as core.packages]
   [k16.kmono.log :as log]))

(set! *warn-on-reflection* true)

(defn- render-aliases [aliases]
  (let [aliases (into []
                      (comp
                       (map (fn [alias]
                              [[:system-yellow alias]
                               [:bold.system-black ", "]]))
                       cat)
                      aliases)]
    (-> [[:bold.system-black "["]]
        (into (drop-last aliases))
        (into [[:bold.system-black "]"]]))))

(defn- repl-command [opts _]
  (let [{:keys [config packages]} (common.context/load-context opts)
        repl-aliases (:repl-aliases config)

        dir (:dir opts)
        targeted-packages (core.packages/find-package-at-dir
                           (or dir (str (fs/cwd))) packages)

        pkg-repl-aliases (when (seq targeted-packages)
                           (->> (vals targeted-packages)
                                (mapcat (fn [pkg]
                                          (->> (:repl-aliases pkg)
                                               (remove nil?)
                                               (map (fn [alias]
                                                      (if (namespace alias)
                                                        alias
                                                        (keyword (name (:fqn pkg)) (name alias))))))))
                                vec))

        effective-repl-aliases (into (vec repl-aliases) (or pkg-repl-aliases []))]

    (binding [log/*log-out* System/err]
      (apply log/info (into ["Aliases: "] (render-aliases (:aliases config))))
      (apply log/info (into ["Repl Aliases: "] (render-aliases effective-repl-aliases)))
      (apply log/info (into ["Package Aliases: "] (render-aliases (:package-aliases config)))))

    (commands.clojure/run-clojure (assoc opts
                                         :M (into (:aliases opts effective-repl-aliases)))
                                  [])))

(def command
  {:command "repl"
   :summary "Start a clojure repl"
   :desc (delay (io/resource "k16/kmono/docs/repl.md"))
   :options {:aliases opts/aliases-opt
             :package-aliases opts/package-aliases-opt}
   :run-fn repl-command})
