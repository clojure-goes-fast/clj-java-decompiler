(ns clj-java-decompiler.core
  (:require [clojure.java.io :as io])
  (:import clojure.lang.Compiler
           com.strobel.assembler.InputTypeLoader
           (com.strobel.assembler.metadata DeobfuscationUtilities MetadataSystem
                                           IMetadataResolver MetadataParser
                                           TypeReference)
           (com.strobel.decompiler DecompilationOptions DecompilerSettings
                                   PlainTextOutput)
           com.strobel.decompiler.languages.Languages
           java.io.File))

(defonce ^:private tmp-dir
  (doto (io/file (System/getProperty "java.io.tmpdir") "clj-java-decompiler")
    (.mkdir)))

;;;; Compilation

clojure.walk/prewalk
(defn- walk-meta-preserving
  "Like `clojure.walk/walk`, but preserves meta. Redundant after
  https://clojure.atlassian.net/browse/CLJ-2568 is merged."
  [inner outer form]
  (let [restore-meta #(if-let [fm (meta form)]
                        (with-meta %
                          (merge fm (meta %)))
                        %)]
    (cond
      (list? form) (outer (restore-meta (apply list (map inner form))))
      (instance? clojure.lang.IMapEntry form)
      (outer (clojure.lang.MapEntry/create (inner (key form)) (inner (val form))))
      (seq? form) (outer (restore-meta (doall (map inner form))))
      (instance? clojure.lang.IRecord form)
      (outer (restore-meta (reduce (fn [r x] (conj r (inner x))) form form)))
      (coll? form) (outer (restore-meta (into (empty form) (map inner form))))
      :else (outer form))))

(defn- prewalk [f form]
  (walk-meta-preserving (partial prewalk f) identity (f form)))

(defn- enrich-lambdas-with-line-numbers
  "Walk the `form` and attach line number-derived names to nameless fns so that
  they are easier to match to source code."
  [form]
  (prewalk
   #(if (and (sequential? %) (not (vector? %))
             ('#{fn fn*} (first %)) (not (symbol? (second %))))
      (if-let [line (:line (meta %))]
        (list* 'fn (symbol (str "fn_line_" line)) (rest %))
        %)
      %)
   form))

(defn- aot-compile
  "Compile the form to classfiles in the temporary directory."
  [form]
  (let [form (enrich-lambdas-with-line-numbers form)
        tmp-source (File/createTempFile "tmp-src" "" tmp-dir)]
    (spit tmp-source (binding [*print-meta* true]
                       (pr-str form)))
    (binding [*compile-files* true
              *compile-path* (str tmp-dir)
              *compiler-options* (cond-> *compiler-options*
                                   (not (contains? *compiler-options* :disable-locals-clearing))
                                   (assoc :disable-locals-clearing true))]
      (Compiler/compile (io/reader tmp-source) "cjd.clj" "cjd"))
    (.delete tmp-source)))

(defn- list-compiled-classes
  "Return the list of class files produced after AOT compilation."
  []
  (let [all-files (filter (fn [^File f]
                            (and (.isFile f)
                                 (.endsWith ^String (.getName f) ".class")))
                          (file-seq tmp-dir))]
    (if (= (count all-files) 1)
      all-files ;; 1 file means only wrapping ns was compiled - return it
      (->> all-files
           (remove #(= (.getName ^File %) "cjd__init.class"))
           (sort-by #(.getName ^File %) #(.compareTo ^String %2 %1))))))

(defn- cleanup-tmp-dir
  "Remove all files and directories from `tmp-dir`."
  []
  (run! (fn [^File f] (when (.isFile f) (.delete f))) (file-seq tmp-dir))
  (run! #(when-not (= % tmp-dir) (.delete ^File %)) (file-seq tmp-dir)))

;;;; Decompilation

(defn- resolve-class-from-file
  "Read and process the given classfile with Procyon."
  [file]
  (doto (.resolve (.lookupType (MetadataSystem. (InputTypeLoader.)) (str file)))
    (DeobfuscationUtilities/processType)))

(def ^:private java-decompiler (Languages/java))
(def ^:private bytecode-decompiler (Languages/bytecode))

(defn- decompile-classfile
  "Decompile the given classfile and print the result to stdout."
  [file options]
  (let [type (resolve-class-from-file file)
        decompiler (if (= (:decompiler options) :bytecode)
                     bytecode-decompiler
                     java-decompiler)
        decomp-options (doto (DecompilationOptions.)
                         (.setSettings (doto (DecompilerSettings.)
                                         (.setSimplifyMemberReferences true))))
        output (PlainTextOutput.)]
    (println "\n// Decompiling class:" (.getInternalName type))
    (.decompileType decompiler type output decomp-options)
    (println (str output))))

(defn decompile-form
  "Decompile the given form and print the result to stdout. `:decompiler` in
  `options` controls which decompiler to use - `:java` or `:bytecode`."
  [options form]
  (try
    (aot-compile form)
    (run! #(decompile-classfile % options) (list-compiled-classes))
    (catch Exception ex (throw ex))
    (finally (cleanup-tmp-dir))))

(defmacro decompile
  "Decompile the form into Java and print it to stdout. Form shouldn't be quoted."
  [form]
  `(decompile-form {:decompiler :java} '~form))

(defmacro disassemble
  "Disassemble the form into Java bytecode (analogous to `javap`) and print it to
  stdout. Form shouldn't be quoted."
  [form]
  `(decompile-form {:decompiler :bytecode} '~form))

(comment

  (binding [*compiler-options* {:direct-linking true}]
    (decompile
      (defn turtles "mydocs" []
        ^{:doc "hello!"} (map #(+ % 1) (range 10)))))

  (binding [*compiler-options* {:direct-linking true}]
    (decompile (fn [] (println (str "Hello, decompiler!")))))

  (disassemble (fn [] (println "Hello, decompiler!")))

  (decompile
    (loop [i 100, sum 0]
      (if (< i 0)
        sum
        (recur (unchecked-dec i) (unchecked-add sum i)))))

  (decompile
   (let [a "foo", b (str a "bar")]
     (println a b)))

  (decompile (definterface ITest (method1 [x y])))

  (decompile (fn [] (cond (= 1 2) 10 (= 1 3) 20 :else 40)))

  (decompile (let [x "baz"] (case x "foo" 2 "bar" 4 "baz" 20)))

  (definterface ITest (^long method1 [^long x ^long y]))
  (decompile
    (reify ITest
      (method1 [this x y] (+ x y))))

  (decompile
    (proxy [ITest] []
      (method1 [x y] (+ x y)))))
