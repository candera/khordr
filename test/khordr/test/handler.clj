(ns khordr.test.handler
  "Utility functions for testing handlers"
  (:require [khordr.handler :as h]
            [khordr.effect :as e]
            [clojure.test :refer [is]]))

(defn next-state
  "Given a state and a keyevent, produce the next state."
  [state keyevent]
  (let [result (h/process (:handler state) state keyevent)]
    (-> state
        (assoc :handler (:handler result))
        (update-in [:effects] concat (:effects result)))))

(defn effects-of
  "Return the effects of the specified keypresses against the specified
  aliases given some initial handler."
  [initial-handler keyevents]
  (->> keyevents
       (map (fn [[k d]] {:key k :direction d})) 
       (reduce next-state 
               {:handler initial-handler :effects []})
       :effects
       ;; This is ugly, but all it does is turn a Key record into a
       ;; two-tuple of its key and direction
       (map (fn [{{:keys [key direction]} :keyevent}] [key direction]))))

(defn keytest
  "Given a set of input keypresses and some initial handler, ensure
  that the specified key effects are produced."
  [initial-handler keyevents effects]
  (is (= (effects-of initial-handler keyevents) effects)))

