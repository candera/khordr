(defproject khordr "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.nativelibs4java/jnaerator-runtime "0.11"]]
  :plugins [[lein-swank "1.4.4"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :repositories {;; "local" "file:repo"
                 "sonatype" "http://oss.sonatype.org/content/repositories/releases"
                 "nativelibs4java-repo" "http://nativelibs4java.sourceforge.net/maven"}
  :jvm-opts ["-Djna.library.path=ext"
             "-Djava.library.path=ext"]
  :main khordr.main
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[fipp "0.5.2"]]
                   :repl-options {:init-ns user}}
             :uberjar {:aot :all}})
