Start a Clojure REPL with workspace packages and aliases automatically configured.

Enhanced REPL with workspace awareness that automatically includes workspace packages in
the classpath and applies configured aliases from both the root project and workspace
packages.

Configuration example:

```clojure
;; deps.local.edn
{:kmono/workspace {;; Always include these aliases when running `kmono repl`
                   :repl-aliases [:nrepl :cider]
                   :aliases [:dev]
                   :package-aliases [:*/dev]}}
```

Packages can declare their own `:repl-aliases` in their `:kmono/package` config. When
`kmono repl` is run from within a package directory (or with `--dir` pointing to one),
those aliases are automatically added — no extra flags needed.

```clojure
;; packages/my-lib/deps.edn
{:kmono/package {:repl-aliases [:dev]}

 :aliases {:dev {:extra-paths ["dev"]
                 :extra-deps {org.clojure/tools.namespace {:mvn/version "1.4.4"}}}}}
```

With this config, running `kmono repl` from `packages/my-lib/` will include `:dev`
automatically, equivalent to `kmono repl -A :dev` from the workspace root.

Examples:

```bash
# Start basic REPL with workspace packages
kmono repl

# Start REPL with additional root aliases
kmono repl -A :dev,:test

# Start REPL with specific package aliases
kmono repl -P ':*/test,:a/dev'

# Start REPL with both root and package aliases
kmono repl -A :cider -P ':*/dev'
```

Use `deps.local.edn` for personal REPL preferences without affecting the shared project
configuration.
