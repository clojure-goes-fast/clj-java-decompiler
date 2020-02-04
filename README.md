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

To make the output clearer, clj-java-decompiler by default disables [locals
clearing](https://clojuredocs.org/clojure.core/*compiler-options*) for the code
it compiles. You can re-enable it by setting this compiler option to false
explicitly, like this:

```clj
(binding [*compiler-options* {:disable-locals-clearing false}]
  (decompile ...))
```

You can also change other compiler options (static linking, metadata elision) in
the same way.

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

The last limitation comes from the fact that Java and Clojure don't keep the
bytecode for classes it loaded anywhere. When the Clojure compiler compiles a
piece of Clojure code, it transforms it into bytecode in memory, then loads with
a classloader, and discards the bytecode.

no.disassemble works around this by being a Java agent which instruments the
classloader to save all classes it ever loaded into an accessible hashmap, so
that they can be retrieved later. This however means you must start the Clojure
program with ND's agent on the classpath.

So, you can't decompile an existing function definition with CJD. But if you are
using CIDER, you can jump to the definition of the function you want to
decompile, disable read-only mode (<kbd>C-x C-q</kbd>), wrap the `defn` form
with `clj-java-decompiler.core/decompile` and recompile the form (<kbd>C-c
C-c</kbd>).

## License

clj-java-decompiler is distributed under the Eclipse Public License.
See [LICENSE](LICENSE).

Copyright 2018-2020 Alexander Yakushev
