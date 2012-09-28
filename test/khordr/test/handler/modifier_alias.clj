(ns khordr.test.handler.modifier-alias
  (:use clojure.test
        khordr.handler.modifier-alias)
  (:require [khordr.handler :as h]
            [khordr.effect :as e])
  (:import khordr.effect.Key))

(defn next-state
  "Given a state and a keyevent, produce the next state."
  [state keyevent]
  (let [result (h/process (:handler state) state keyevent)]
    (-> state
        (assoc :handler (:handler result))
        (update-in [:effects] concat (:effects result)))))

(defn effects-of
  "Return the effects of the specified keypresses against the specified
  aliases."
  [aliases keyevents]
  (->> keyevents
       (map (fn [[k d]] {:key k :direction d})) 
       (reduce next-state 
               {:handler (->Handler aliases) :effects []})
       :effects
       ;; This is ugly, but all it does is turn a Key record into a
       ;; two-tuple of its key and direction
       (map (fn [{{:keys [key direction]} :keyevent}] [key direction]))))

(defn keytest
  "Given a set of input keypresses and some default behaviors, ensure
  that the specified key effects are produced."
  [keyevents effects]
  (is (= (effects-of {:j :rshift :k :rcontrol} keyevents) effects)))

(deftest single-modifier-down
  (keytest [[:j :dn]]
           []))

(deftest modifier-press-and-release
  (keytest [[:j :dn] [:j :up]]
           [[:j :dn] [:j :up]]))

(deftest modifier-repeat-and-release
  (keytest [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
           [[:j :dn] [:j :up]]))

(deftest await-decision
  ;; Modifier alias with regular key press results in no events
  ;; because we're not sure yet whether the user is just
  ;; "rolling" through keys or trying to modify
  (keytest [[:j :dn] [:x :dn]]
           []))

(deftest modifier-aliasing-on-regular-up
  ;; Modifier aliasing happens as soon as regular key goes up
  (keytest [[:j :dn] [:x :dn] [:x :up]]
           [[:rshift :dn]
            [:x :dn]
            [:x :up]]))

(deftest modifier-aliasing-with-release
  ;; Modifier alias with regular key press and release
  (keytest [[:j :dn] [:x :dn] [:x :up] [:j :up]]
           [[:rshift :dn]
            [:x :dn]
            [:x :up]
            [:rshift :up]]))

(deftest modifier-aliasing-followed-by-modifier-press-and-release
  ;; Modifier alias with regular key press and release followed
  ;; by modifier alias press and release
  (keytest [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
           [[:rshift :dn]
            [:x :dn]
            [:x :up]
            [:rshift :up]
            [:j :dn]
            [:j :up]]))

(deftest multiple-aliased-modifiers
  ;; Multiple modifier aliases down followed by regular key down
  ;; adds both modifiers to regular key
  (keytest [[:j :dn] [:k :dn] [:x :dn] [:x :up]]
           [[:rshift :dn]
            [:rcontrol :dn]
            [:x :dn]
            [:x :up]]))

(deftest modifier-order-preserved
  ;; Order of modifier aliases down is preserved? (TODO: Is this
  ;; what we want?)
  (keytest [[:k :dn] [:j :dn] [:x :dn] [:x :up]]
           [[:rcontrol :dn]
            [:rshift :dn]
            [:x :dn]
            [:x :up]]))

(deftest modifying-a-modifier
  ;; Someone used a modifier to modify a modifier
  (keytest [[:j :dn] [:k :dn] [:k :up]]
           [[:rshift :dn]
            [:k :dn]
            [:k :up]]))

(deftest modifier-rollover
  ;; Someone was just typing two or more modifier alias keys in a row - normal
  ;; rollover.
  (keytest [[:j :dn] [:k :dn] [:j :up] [:x :dn] [:k :up] [:x :up]]
           [[:j :dn] [:k :dn] [:j :up] [:x :dn] [:k :up] [:x :up]]))

(deftest rollover-including-modifier
  ;; If another key is already down, don't take over
  (is (= (h/process (->Handler {}) {:down-keys [:r]} {:key :j :direction :dn})
         {:handler nil
          :effects [(e/->Key {:key :j :direction :dn})]})))

