# clj-java-decompiler

_You can read the motivation behind clj-java-decompiler and the usage example in
the
[blog post](http://clojure-goes-fast.com/blog/introspection-tools-java-decompilers/)._

This library is an integrated Clojure-to-Java decompiler usable from the REPL.
It is a wrapper
around [Procyon](https://bitbucket.org/mstrobel/procyon/overview) which is a
suite of Java metaprogramming tools focused on code generation and analysis.

Quick demo:

```java
user> (clj-java-decompiler.core/decompile
        (loop [i 100, sum 0]
          (if (< i 0)
            sum
            (recur (unchecked-dec i) (unchecked-add sum i)))))

// Decompiling class: user$fn__13332
import clojure.lang.*;

public final class user$fn__13332 extends AFunction
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

    public Object invoke() {
        return invokeStatic();
    }
}
```

## Why?

There are several usecases when you may want to use a Java decompiler:

- To get a general understanding how Clojure compiler works: how functions are
  compiled into classes, how functions are invoked, etc.
- To optimize performance bottlenecks when using low-level constructs like
  loops, primitive math, and type hints.
- To investigate how Java interop facilities are implemented (`reify`, `proxy`,
  `gen-class`).

## Usage

Add `com.clojure-goes-fast/clj-java-decompiler` to your dependencies:

[![](https://clojars.org/com.clojure-goes-fast/clj-java-decompiler/latest-version.svg)](https://clojars.org/com.clojure-goes-fast/clj-java-decompiler)

Then, at the REPL:

```clojure
user> (require '[clj-java-decompiler.core :refer [decompile]])
nil
user> (decompile (fn [] (println "Hello, decompiler!")))
```

```java
// Decompiling class: clj_java_decompiler/core$fn__13257
import clojure.lang.*;

public final class core$fn__13257 extends AFunction
{
    public static final Var const__0;

    public static Object invokeStatic() {
        return ((IFn)const__0.getRawRoot()).invoke((Object)"Hello, decompiler!");
    }

    public Object invoke() {
        return invokeStatic();
    }

    static {
        const__0 = RT.var("clojure.core", "println");
    }
}
```

You can also disassemble to bytecode, with the output being similar to the one
of `javap`.

```
user> (disassemble (fn [] (println "Hello, decompiler!")))

;;; Redacted

    public static java.lang.Object invokeStatic();
        Flags: PUBLIC, STATIC
        Code:
                  linenumber      1
               0: getstatic       clj_java_decompiler/core$fn__17004.const__0:Lclojure/lang/Var;
               3: invokevirtual   clojure/lang/Var.getRawRoot:()Ljava/lang/Object;
                  linenumber      1
               6: checkcast       Lclojure/lang/IFn;
               9: getstatic       clj_java_decompiler/core$fn__17004.const__1:
Lclojure/lang/Var;
              12: invokevirtual   clojure/lang/Var.getRawRoot:()Ljava/lang/Object;
                  linenumber      1
              15: checkcast       Lclojure/lang/IFn;
              18: ldc             "Hello, decompiler!"
                  linenumber      1
              20: invokeinterface clojure/lang/IFn.invoke:(Ljava/lang/Object;)Ljava/lang/Object;
                  linenumber      1
              25: invokeinterface clojure/lang/IFn.invoke:(Ljava/lang/Object;)Ljava/lang/Object;
              30: areturn
```

## Comparison with no.disassemble

[no.disassemble](https://github.com/gtrak/no.disassemble) (ND) is another tool
that lets you inspect what the Clojure code compiles to. However, it
substantially differs from clj-java-decompiler (CJD).

- ND can only disassemble the compiled code to bytecode representation. CJD
  decompiles the code into Java which is much easier to comprehend.
- ND requires the program to be loaded with its Java agent present. You either
  have to add the agent to JVM options manually or start the REPL with its
  Leiningen plugin. CJD can be loaded into any REPL dynamically.
- ND tracks every class that was loaded since the beginning of the program, so
  it has memory overhead. CJD bears no overhead.
- ND can disassemble any already defined Clojure function. CJD needs the Clojure
  form to be passed directly to it.

## License

clj-java-decompiler is distributed under the Eclipse Public License.
See [LICENSE](LICENSE).

Copyright 2018 Alexander Yakushev
