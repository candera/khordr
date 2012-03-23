(ns kchordr.test.core
  (:use [kchordr.core])
  (:use [clojure.test]))

(defn- sent
  "Given a sequence of key events, return the sequence of keys that
  will actually be sent."
  [presses]
  (loop [state (state)
         press presses]
    (if (seq presses)
      (recur (process state press)
             (rest (rest presses)))
      (to-send state))))

(deftest key-tests
  (are [pressed anticipated] (= anticipated (sent pressed))
       ;; Single non-modifier key press
       [[:b :dn]]
       [[:b :dn]]

       ;; Single non-modified key press and release
       [[:b :dn] [:b :up]]
       [[:b :dn] [:b :up]]

       ;; Modified key press only
       [[:j :dn]]
       []

       ;; Modified shifted key
       [[:j :dn] [:x :dn]]
       [[:rshift :dn] [:x :dn]]))

