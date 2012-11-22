(ns khordr.experimental
  "A place to hack around and try things"
  (:require [khordr :as k]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

(defn -main []
  (println "Starting")
  (khordr.KeyGrabber/grab nil)
  (Thread/sleep 100000))

(comment
 (defn -main []
   (println "Starting")
   (let [behaviors k/default-key-behaviors
         platform (p/platform)]
     (try 
       (loop []
         (let [event (com/await-key-event platform)]
           (com/send-key event)
           (when-not (= :esc (:key event))
             (println event)
             (recur))))
       (finally
         (com/cleanup platform))))))