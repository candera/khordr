(defproject kchordr "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 ;[net.java.dev.jna/jna "3.4.0"]
                 ;[kchordr/interception "1.0.0"]
                 ]
  :source-paths ["src/clj"]
  ;; Use one of the two following, depending on whether you want
  ;; source or jar dependency
  :java-source-path "src/java"
  ;; :repositories {"local" "file:repo"}
  ;; :jvm-opts "-Djna.library.path=ext"
  )