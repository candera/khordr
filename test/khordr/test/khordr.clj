(ns khordr.test.khordr
  (:refer-clojure :exclude (send))
  (:require [khordr :refer (handle-keys
                            make-modifier-alias
                            base-state)])
  (:use [clojure.test])
  (:import khordr.SpecialActionKeyHandler))

(def test-key-behaviors
  {:j [make-modifier-alias :rshift]
   :k [make-modifier-alias :rcontrol]
   :backtick [(fn [self-key] (SpecialActionKeyHandler. self-key))]})

(defn- sent
  "Given a sequence of key events, return the sequence of keys that
  will actually be sent."
  [events]
  (:effects (reduce #(handle-keys %1 %2)
                    (base-state test-key-behaviors)
                    events)))

(defn- ->event
  [[key direction]]
  {:key key :direction direction})

(defn- press
  "Given a seq of pairs of [key direction], return the sent keys as a
  similar seq."
  [pressed]
  (sent (map ->event pressed)))

(deftest key-tests
  (are [pressed anticipated]
       ;; We use vectors as the test format because they're easier to
       ;; read, but we still want to use maps as the underlying
       ;; construct for their flexibility. Yay juxt!
       (= anticipated (press pressed))
       ;; Single regular key press
       [[:b :dn]]
       [:key [:b :dn]]

       ;; Single regular key press and release
       [[:b :dn] [:b :up]]
       [:key [:b :dn] :key [:b :up]]

       ;; Modifier alias press only
       [[:j :dn]]
       []

       ;; Modifier alias press and release
       [[:j :dn] [:j :up]]
       [:key [:j :dn] :key [:j :up]]

       ;; Modifier alias repeat and release
       [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
       [:key [:j :dn] :key [:j :up]]

       ;; Modifier alias with regular key press
       [[:j :dn] [:x :dn]]
       [:key [:rshift :dn] :key [:x :dn]]

       ;; Modifier alias with regular key press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up]]
       [:key [:rshift :dn] :key [:x :dn] :key [:x :up] :key [:rshift :up]]

       ;; Modifier alias with regular key press and release followed
       ;; by modifier alias press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
       [:key [:rshift :dn]
        :key [:x :dn]
        :key [:x :up]
        :key [:rshift :up]
        :key [:j :dn]
        :key [:j :up]]

       ;; Multiple modifier aliases down sends the first modifier
       [[:j :dn] [:k :dn]]
       [:key [:rshift :dn]]

       ;; Multiple modifier aliases down followed by regular key down
       ;; adds both modifiers to regular key
       [[:j :dn] [:k :dn] [:x :dn]]
       [:key [:rshift :dn] :key [:rcontrol :dn] :key [:x :dn]]

       ;; Order of modifier aliases down is preserved? (TODO: Is this
       ;; what we want?)
       [[:k :dn] [:j :dn] [:x :dn]]
       [:key [:rcontrol :dn] :key [:rshift :dn] :key [:x :dn]]

       ;; A modifier alias going up when another modifier is undecided
       ;; means the second modifier was a regular keypress.
       [[:j :dn] [:k :dn] [:k :up]]
       [:key [:rshift :dn] :key [:k :dn] :key [:k :up]]

       ;; We have the ability to quit the application
       [[:backtick :dn] [:q :dn]]
       [:quit nil]

       ;; But other nearby key sequences don't quit
       [[:backtick :dn] [:backtick :up] [:q :dn] [:backtick :dn]]
       [:key [:backtick :dn]
        :key [:backtick :up]
        :key [:q :dn]]

       ;; TODO: Also move towards using key maps to describe keys even on output
       ))

