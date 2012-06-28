(ns khordr.main
  (:require [khordr :as k]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

(defn -main
  "Main entry point for the application."
  [& args]
  (let [platform (p/initialize)]
    (try
     (loop [state (k/base-state k/default-key-behaviors)]
       (let [event (com/await-key-event platform)
             state (k/handle-keys state event)
             state (k/enact-effects! state platform)]
         (when-not (:done state)
           (recur state))))
     (finally
      (com/cleanup platform)))))
