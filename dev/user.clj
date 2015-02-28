(ns user
  (:require [clojure.test :as test]
            [fipp.clojure :refer [pprint]]
            [khordr.handler :as h]))

(defn next-state
  "Given a state and a keyevent, produce the next state."
  [state keyevent]
  (let [result (h/process (:handler state) state keyevent)]
    (-> state
      (assoc :handler (:handler result))
      (assoc :event keyevent)
      (update-in [:effects] concat (:effects result)))))

(defn keytest
  [handler events]
  (->> ;;[[:j :dn] [:x :dn] [:x :up]]
      events
    (map (fn [[k d]] {:key k :direction d}))
    (reductions next-state {:handler handler :effects []})))

