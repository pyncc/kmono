# Example Workspace Project

This is a very simple project structure which serves as an example of how kmono is intended to be setup and used.

## Overview

This is the overall structure of the project:

```bash
.
├── packages
│   ├── a
│   │   ├── src
│   │   ├── test
│   │   └── deps.edn
│   ├── b
│   │   ├── src
│   │   ├── test
│   │   └── deps.edn
│   └── c
│       ├── dev
│       ├── src
│       ├── test
│       └── deps.edn
├── build.clj
└── deps.edn
```

In this structure we have three packages: `a`, `b` (which depends on `a`), and `c` (which depends on `b`).

Package `c` demonstrates the **per-package aliases** feature. Its `deps.edn` declares:

```clojure
{:kmono/package {:aliases [:c-dev]}
 ...}
```

The `:c-dev` alias is defined in the root `deps.edn`. When package `c` is targeted — either via
`-F` filter or by running `kmono` from the `packages/c` directory — the `:c-dev` alias is
automatically injected into the clojure invocation.

The `:c-dev` alias adds `packages/c/dev` to the classpath, making the `k16.c.dev` namespace
available only when working with package `c`.

```bash
# Start a REPL targeting c — :c-dev is injected automatically
# clojure runs with -A:kmono/packages:c-dev
kmono clojure -F :com.kepler16/c -A

# :c-dev is NOT injected when c is not targeted
kmono clojure -A
```

### Running from a package directory

kmono respects the working directory. Running from inside a package directory automatically
targets that package and its upstream dependency closure, and makes workspace root aliases
available via `-Sdeps` with paths rewritten relative to your location:

```bash
# From packages/c — targets c, b, and a; :c-dev injected and available
cd packages/c
kmono clojure -A

# Equivalent to running from the workspace root with an explicit filter:
kmono clojure -F :com.kepler16/c -A
```

You can also pass `--dir` explicitly from anywhere:

```bash
kmono --dir packages/c clojure -A
```

### Filtering includes upstream dependencies

When using `kmono clojure -F` to target a package, kmono automatically includes its full
upstream dependency closure. All packages the target depends on are added to `:kmono/packages`
and their scoped aliases (e.g. `:a/test`) are available in `-Sdeps`.

```bash
# Packages a, b, and c are all on the classpath — c depends on b which depends on a
kmono clojure -F :com.kepler16/c -A

# Only packages b and a are on the classpath — c is downstream of b, not upstream
kmono clojure -F :com.kepler16/b -A
```

> [!NOTE]
>
> Run `kmono query` to inspect the workspace package graph and see how packages relate

This project demonstrates a workflow where:

1. Packages are built and released when PR's are merged to master.
2. Only packages that have changed since their previous version are build and released.
3. The project uses conventional-commits and package versions are derived from commits.

The above requirements aren't needed to make use of kmono - this just serves to demonstrate a particular workflow and
how you might use kmono to achieve it.

## Building/Releasing

The build and release workflow described above is entirely encapsulated in the `build.clj` file using `tools.build` and
kmono-\* APIs.

Packages can be built by running:

```bash
clojure -T:build build
```

And the built packages can then be released by running

```bash
clojure -T:build release
```

For both building and releasing you can add the `:skip-unchanged true` argument to only build and release packages that
have changed since their last release. The idea being that you would pass this by default during CI.

```bash
clojure -T:build build :skip-unchanged true
```

This behaviour hasn't been hard-coded into the build process so that one might run `just build` locally during
development to build all packages.

---

Have a look at the [example GitHub workflow file](./.github/workflows/release.yaml) for how you might set up your CI
pipeline for release.

## Testing

To run the tests for each package you can run the command

```bash
# Run `clojure -M:test` in each package that has a `:test` alias.
kmono run -M :test

# Only run `clojure -M:test` in package a.
kmono run -M :test -F '*/a'
```

Each respective packages' `:test` alias will then be appended to the command when it is run, like so: `clojure -M:test`.
