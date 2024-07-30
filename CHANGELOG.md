# Changelog

### 0.3.5 (2024-07-30)

- Preserve function metadata when adding line numbers to lambdas.

### 0.3.4 (2023-03-09)

- Replace `const__` references with var names during post-processing.
- [#12](https://github.com/clojure-goes-fast/clj-java-decompiler/issues/12) Fix
  compatibility with JDK19.

### 0.3.3 (2022-12-20)

- Simplify member references using naive post-processing.

### 0.3.2 (2022-07-30)

- [#6](https://github.com/clojure-goes-fast/clj-java-decompiler/issues/6)
  Preserve type hints and other metadata after code walking.

### 0.3.1 (2021-11-01)

- Ensure consistent ordering of decompiled classes
- Try to embed line location of lambdas into their names

### 0.3.0 (2020-02-04)

- [#4](https://github.com/clojure-goes-fast/clj-java-decompiler/issues/4)
  Upgrade procyon to support JDK9+.

### 0.2.1 (2019-03-01)

- [#3](https://github.com/clojure-goes-fast/clj-java-decompiler/issues/3) Output
  doesn't show up in latest CIDER.

### 0.2.0 (2019-01-31)

- `:disable-locals-clearing` is now enabled by default when
  compiling/decompiling, unless set from the outside explicitly.

### 0.1.1

Initial release.
