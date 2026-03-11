(ns k16.kmono.cli.common.context
  (:require
   [babashka.fs :as fs]
   [k16.kmono.cli.common.config :as common.config]
   [k16.kmono.core.config :as core.config]
   [k16.kmono.core.fs :as core.fs]
   [k16.kmono.core.packages :as core.packages]))

(defn load-context [props]
  (let [dir (str (fs/absolutize (or (:dir props) (fs/cwd))))
        project-root (core.fs/find-project-root! dir)
        workspace-config (-> (core.config/resolve-workspace-config project-root)
                             (common.config/merge-workspace-config props))
        packages (core.packages/resolve-packages project-root workspace-config)]
    {:root project-root
     :dir dir
     :config workspace-config
     :packages packages}))
