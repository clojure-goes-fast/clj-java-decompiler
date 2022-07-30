(ns clj-java-decompiler.core-test
  (:require [clj-java-decompiler.core :as sut]
            [clojure.test :refer :all]
            clojure.pprint
            [clojure.string :as str]))

(defn ns-fixture
  [f]
  (in-ns 'clj-java-decompiler.core-test)
  (f))

;; Force tests to be run within this namespaces
;; (see https://github.com/cognitect-labs/test-runner/issues/38)
(use-fixtures :once ns-fixture)

(defmacro with-out-trimmed-str [& body]
  `(let [res# (with-out-str ~@body)]
     (->> res#
          str/split-lines
          (remove str/blank?)
          (str/join "\n"))))

(defn =str [s1 s2]
  (let [s1s (str/split-lines s1)
        s2s (str/split-lines s2)]
    (or (every? true? (map (fn [l1 l2]
                             (or (= l1 "<<<anything>>>")
                                 (= l2 "<<<anything>>>")
                                 (= l1 l2)
                                 (when-let [[_ before after] (re-matches #"(.*)<<<ignore>>>(.*)" l2)]
                                   (and (str/starts-with? l1 before)
                                        (str/ends-with? l1 after)))))
                           (str/split-lines s1)
                           (str/split-lines s2)))
        (do (println "FAIL\ns1:")
            (clojure.pprint/pprint s1s)
            (println  "\n-------\ns2:\n")
            (clojure.pprint/pprint s2s)
            false))))

(deftest basic-test
  (is (=str (with-out-trimmed-str
              (sut/decompile
               (loop [i 100, sum 0]
                 (if (< i 0)
                   sum
                   (recur (unchecked-dec i) (unchecked-add sum i))))))
            "<<<anything>>>
package clj_java_decompiler;
import clojure.lang.*;
<<<anything>>>
{
    public static Object invokeStatic() {
        long i = 100L;
        long sum = 0L;
        while (i >= 0L) {
            final long n = i - 1L;
            sum += i;
            i = n;
        }
        return Numbers.num(sum);
    }
    @Override
    public Object invoke() {
        return invokeStatic();
    }
}"))

  (=str (with-out-trimmed-str
          (sut/decompile (defn hello [] (println "Hello, decompiler!"))))
        "// Decompiling class: clj_java_decompiler/core_test$hello
package clj_java_decompiler;
import clojure.lang.*;
public final class core_test$hello extends AFunction
{
    public static final Var const__0;
    public static Object invokeStatic() {
        return ((IFn)core_test$hello.const__0.getRawRoot()).invoke(\"Hello, decompiler!\");
    }
    @Override
    public Object invoke() {
        return invokeStatic();
    }
    static {
        const__0 = RT.var(\"clojure.core\", \"println\");
    }
}"))

(deftest lambda-linenumber-test
  (is (=str (with-out-trimmed-str
              (sut/decompile (fn [] (+ 1 2))))
            "// Decompiling class: clj_java_decompiler/core_test$fn_line_91__<<<ignore>>>
package clj_java_decompiler;
import clojure.lang.*;
public final class core_test$fn_line_91__<<<ignore>>> extends AFunction
{
    public static Object invokeStatic() {
        return Numbers.num(Numbers.add(1L, 2L));
    }
    @Override
    public Object invoke() {
        return invokeStatic();
    }
}")))

(deftest metadata-preserved-test
  (is (=str (with-out-trimmed-str
              (sut/decompile (defn hint-callsite [m] (.intValue ^Long (m 1)))))
            "// Decompiling class: clj_java_decompiler/core_test$hint_callsite
package clj_java_decompiler;
import clojure.lang.*;
public final class core_test$hint_callsite extends AFunction
{
    public static final Object const__0;
    public static Object invokeStatic(final Object m) {
        return ((Long)((IFn)m).invoke(core_test$hint_callsite.const__0)).intValue();
    }
    @Override
    public Object invoke(final Object m) {
        return invokeStatic(m);
    }
    static {
        const__0 = 1L;
    }
}"))

  (is (=str (with-out-trimmed-str
              (sut/decompile (defn hint-let-symbol [m]
                               (let [^Long l (m 1)]
                                 (.intValue l)))))
            "// Decompiling class: clj_java_decompiler/core_test$hint_let_symbol
package clj_java_decompiler;
import clojure.lang.*;
public final class core_test$hint_let_symbol extends AFunction
{
    public static final Object const__0;
    public static Object invokeStatic(final Object m) {
        final Object l = ((IFn)m).invoke(core_test$hint_let_symbol.const__0);
        return ((Long)l).intValue();
    }
    @Override
    public Object invoke(final Object m) {
        return invokeStatic(m);
    }
    static {
        const__0 = 1L;
    }
}"))

  (is (=str (with-out-trimmed-str
              (sut/decompile (defn hint-let-value [m]
                               (let [l ^Long (m 1)]
                                 (.intValue l)))))
            "// Decompiling class: clj_java_decompiler/core_test$hint_let_value
package clj_java_decompiler;
import clojure.lang.*;
public final class core_test$hint_let_value extends AFunction
{
    public static final Object const__0;
    public static Object invokeStatic(final Object m) {
        final Object l = ((IFn)m).invoke(core_test$hint_let_value.const__0);
        return ((Long)l).intValue();
    }
    @Override
    public Object invoke(final Object m) {
        return invokeStatic(m);
    }
    static {
        const__0 = 1L;
    }
}")))
