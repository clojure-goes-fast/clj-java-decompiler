(task-options!
 pom {:project     'com.clojure-goes-fast/clj-java-decompiler
      :version     "0.3.0"
      :description "Integrated Clojure-to-Java decompiler"
      :url         "https://github.com/clojure-goes-fast/clj-java-decompiler"
      :scm         {:url "https://github.com/clojure-goes-fast/clj-java-decompiler"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 push {:repo "clojars"})

(set-env! :resource-paths #{"src"}
          :source-paths   #{"src"}
          :dependencies   '[[org.clojure/clojure "1.10.0" :scope "provided"]
                            [org.bitbucket.mstrobel/procyon-compilertools "0.5.34"]])

(deftask build
  "Build the project."
  []
  (comp (pom) (jar)))
