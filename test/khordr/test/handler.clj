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
      (assoc :event keyevent)
      (update-in [:effects] concat (:effects result)))))

(defn run
  "Return a map of the effects and final state of the specified
  keypresses against the specified aliases given some initial
  handler."
  ([initial-handler keyevents]
   (run initial-handler keyevents {}))
  ([initial-handler keyevents {:keys [reductions?] :as options}]
   (let [r (if reductions? reductions reduce)]
     (->> keyevents
       (map (fn [[k d]] {:key k :direction d}))
       (r next-state
          {:handler initial-handler :effects []})))))

(defn effects-of
  [state]
  (->> state
    :effects
    ;; This is ugly, but all it does is turn a Key record into a
    ;; two-tuple of its key and direction
    (map (fn [{{:keys [key direction]} :keyevent}] [key direction]))))

(defn keytest
  "Given a set of input keypresses and some initial handler, ensure
  that the specified key effects are produced."
  ([initial-handler keyevents effects]
   (is (= (effects-of (run initial-handler keyevents)) effects))))
