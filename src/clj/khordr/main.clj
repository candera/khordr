(ns khordr.main
  (:require [khordr :as k]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

(defn noop [& args])

;; (def log noop)
(def log println)

(defn -main
  "Main entry point for the application."
  [& args]
  (let [platform (p/initialize)]
    (log "Initialized")
    (try
     (loop [state (k/base-state k/default-key-behaviors)]
       (let [event (com/await-key-event platform)
             _ (log "RECEIVED" (:direction event) (:key event))
             state (k/handle-keys state event)
             ;;_ (log "ENACTING" (:effects state))
             state (k/enact-effects! state platform)]
         (when-not (:done state)
           (recur state))))
     (finally
      (com/cleanup platform)
      (log "Done")))))
