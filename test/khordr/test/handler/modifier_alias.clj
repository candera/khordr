(ns khordr.test.handler.modifier-alias
  (:use clojure.test
        khordr.handler.modifier-alias)
  (:require [khordr.handler :as h]
            [khordr.effect :as e]
            [khordr.test.handler :refer :all])
  (:import khordr.effect.Key))

(def kt
  (partial keytest (->Handler {:j :rshift :k :rcontrol})))

(deftest single-modifier-down
  (kt [[:j :dn]]
      []))

(deftest modifier-press-and-release
  (kt [[:j :dn] [:j :up]]
      [[:j :dn] [:j :up]]))

(deftest modifier-repeat-and-release
  (kt [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
      [[:j :dn] [:j :up]]))

(deftest await-decision
  ;; Modifier alias with regular key press results in no events
  ;; because we're not sure yet whether the user is just
  ;; "rolling" through keys or trying to modify
  (kt [[:j :dn] [:x :dn]]
      []))

(deftest modifier-aliasing-on-regular-up
  ;; Modifier aliasing happens as soon as regular key goes up
  (kt [[:j :dn] [:x :dn] [:x :up]]
      [[:rshift :dn]
       [:x :dn]
       [:x :up]]))

(deftest modifier-aliasing-with-release
  ;; Modifier alias with regular key press and release
  (kt [[:j :dn] [:x :dn] [:x :up] [:j :up]]
      [[:rshift :dn]
       [:x :dn]
       [:x :up]
       [:rshift :up]]))

(deftest modifier-aliasing-followed-by-modifier-press-and-release
  ;; Modifier alias with regular key press and release followed
  ;; by modifier alias press and release
  (kt [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
      [[:rshift :dn]
       [:x :dn]
       [:x :up]
       [:rshift :up]
       [:j :dn]
       [:j :up]]))

(deftest multiple-aliased-modifiers
  ;; Multiple modifier aliases down followed by regular key down
  ;; adds both modifiers to regular key
  (kt [[:j :dn] [:k :dn] [:x :dn] [:x :up]]
      [[:rshift :dn]
       [:rcontrol :dn]
       [:x :dn]
       [:x :up]]))

(deftest modifier-order-preserved
  ;; Order of modifier aliases down is preserved? (TODO: Is this
  ;; what we want?)
  (kt [[:k :dn] [:j :dn] [:x :dn] [:x :up]]
      [[:rcontrol :dn]
       [:rshift :dn]
       [:x :dn]
       [:x :up]]))

(deftest modifying-a-modifier
  ;; Someone used a modifier to modify a modifier
  (kt [[:j :dn] [:k :dn] [:k :up]]
      [[:rshift :dn]
       [:k :dn]
       [:k :up]]))

(deftest modifier-rollover
  ;; Someone was just typing two or more modifier alias keys in a row - normal
  ;; rollover.
  (kt [[:j :dn] [:k :dn] [:j :up] [:x :dn] [:k :up] [:x :up]]
      [[:j :dn] [:k :dn] [:j :up] [:x :dn] [:k :up] [:x :up]]))

(deftest rollover-including-modifier
  ;; If another key is already down, don't take over
  (is (= (h/process (->Handler {}) {:down-keys [:r]} {:key :j :direction :dn})
         {:handler nil
          :effects [(e/->Key {:key :j :direction :dn})]})))

(deftest only-alias-actual-alias-keys
  ;; Only the aliases that were down at the point where we start
  ;; aliasing should actually be aliased.
  (kt [[:j :dn] [:k :dn] [:k :up] [:k :dn]]
      [[:rshift :dn] [:k :dn] [:k :up] [:k :dn]]))

(deftest repeats-trigger-aliasing
  ;; If we see a regular key go down twice in a row without going up
  ;; first, then it's a repeat and we should trigger aliasing.
  (kt [[:j :dn] [:x :dn] [:x :dn]]
      [[:rshift :dn] [:x :dn] [:x :dn]]))