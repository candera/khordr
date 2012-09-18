(ns khordr.main
  (:require [clojure.stacktrace :as trace]
            [khordr :as k]
            [khordr.logging :as log]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

(defn -main
  "Main entry point for the application."
  [& args]
  (let [platform (p/initialize)]
    (log/info "Initialized")
    (try
      (loop [state (k/base-state k/default-key-behaviors)]
        (let [event (com/await-key-event platform)
              state (k/handle-keys state event)
              state (k/enact-effects! state platform)]
          (when-not (:done state)
            (recur state))))
      (catch Throwable t
        (log/error {:type :exception
                    :data t
                    :message (.getMessage t)
                    :stacktrace (.getStackTrace t)
                    :stacktrace-str (with-out-str (trace/print-stack-trace t))}))
      (finally
       (com/cleanup platform)
       (log/info "Done")))))
