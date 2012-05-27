(defproject kchordr "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.match "0.2.0-alpha9"]
                 [net.java.dev.jna/jna "3.4.0"]
                 [com.nativelibs4java/jnaerator-runtime "0.9.10-SNAPSHOT"]
                 ;;[kchordr/interception "1.0.0"]
                 ]
  :dev-dependencies [[lein-swank "1.4.4"]]
  :source-path "src/clj"
  ;; Use one of the two following, depending on whether you want
  ;; source or jar dependency
  :java-source-path "src/java"
  :repositories {"local" "file:repo"
                 "sonatype" "http://oss.sonatype.org/content/repositories/releases"
                 "nativelibs4java-repo" "http://nativelibs4java.sourceforge.net/maven"}
  :jvm-opts ["-Djna.library.path=ext"])