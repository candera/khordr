(ns kchordr.test.core
  (:use [kchordr.core])
  (:use [clojure.test]))

(defn- key-test
  "Given a sequence of key events, check that the result of processing
  them is correct."
  [pressed expected description]
  (is (= (partition 2 expected)
         (loop [state (key-state)
                presses pressed]
           (if (seq presses)
             (recur (process state (first presses) (second presses))
                    (rest (rest presses)))
             (to-send state))))
      description))

(defn- test-form [description pressed expected]
  (let [name (->> pressed
                  (map name)
                  (interpose "-")
                  (apply str)
                  symbol)]
   `(deftest ~name
      (key-test ~pressed ~expected ~description))))

(defmacro defkeytests [& body]
  (concat '(do)
          (map #(apply test-form %) (partition 3 body))))

(defkeytests
  "Single non-modified key down"
  [:b :dn]
  [:b :dn]

  "Single non-modified key press and release"
  [:b :dn :b :up]
  [:b :dn :b :up]

  "Modified key press only"
  [:j :dn]
  []

  "Modified shifted key"
  [:j :dn :x :dn]
  [:lshift :dn :x :dn])

