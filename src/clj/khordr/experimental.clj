(ns khordr.experimental
  "A place to hack around and try things"
  (:require [khordr :as k]
            [khordr.platform :as p]
            [khordr.platform.common :as com]
            [khordr.logging :as log]))

(defn- thread-name
  "Returns the name of the current thread"
  []
  (.getName (Thread/currentThread)))

(defn -main []
  (log/info "Starting")
  (let [behaviors k/default-key-behaviors
        platform (p/platform)]
    (log/debug (str "platform is " platform))
    (.setName (Thread/currentThread) "main")
    (try
      (loop []
        (let [event (com/await-key-event platform)]
          (log/debug {:type :main/key-received :thread (thread-name) :data event})
          (com/send-key platform event)
          (when-not (= :esc (:key event))
            ;;(log/debug event)
            ;;(log/debug "recurring")
            (recur))))
      (finally
        (log/info "Cleaning up")
        (com/cleanup platform)))))