(ns khordr.main
  (:require [clojure.stacktrace :as trace]
            [khordr :as k]
            [khordr.logging :as log]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

(defn read-behaviors
  "Given something compatible with slurp, return behaviors. Return
  default behaviors if nil."
  [place]
  (if place
    (-> place slurp read-string :behaviors)
    k/default-key-behaviors))

(defn -main
  "Main entry point for the application."
  [& [config-place]]
  (let [behaviors (read-behaviors config-place)
        platform (p/platform)]
    (log/info "Initialized")
    (try
      (loop [state (k/base-state behaviors)]
        (let [event (com/await-key-event platform)
              _     (log/debug {:type :key-event-received
                                :data event})
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
        (shutdown-agents)
        (log/info "Done")))))
