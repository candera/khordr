(defproject khordr "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [net.java.dev.jna/jna "3.4.0"]
                 [com.nativelibs4java/jnaerator-runtime "0.9.10-SNAPSHOT"]
                 ;;[kchordr/interception "1.0.0"]
                 ]
  :plugins [[lein-swank "1.4.4"]]
  :source-paths ["src/clj"]
  ;; Use one of the two following, depending on whether you want
  ;; source or jar dependency
  :java-source-paths ["src/java"]
  :compile-path "target/classes"
  :repositories {;; "local" "file:repo"
                 "sonatype" "http://oss.sonatype.org/content/repositories/releases"
                 "nativelibs4java-repo" "http://nativelibs4java.sourceforge.net/maven"}
  :jvm-opts ["-Djna.library.path=ext"]
  ;; At the moment, the AOT that this requires is causing me all sorts
  ;; of problems, and I don't understand why. I will proceed without
  ;; it for now, but I'm going to need to get this working at some
  ;; point.
  ;; :main khordr.main
  )