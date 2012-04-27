(ns kchordr.test.core
  (:refer-clojure :exclude (send))
  (:require [kchordr.core :refer (state process default-key-behaviors ->event)])
  (:use [clojure.test]))

(defn- sent
  "Given a sequence of key events, return the sequence of keys that
  will actually be sent."
  [events]
  (:to-send (reduce #(process %1 %2) (state default-key-behaviors) events)))

(defn- press
  "Given a seq of pairs of [key direction], return the sent keys as a
  similar seq."
  [pressed]
  (map (juxt :key :direction) (sent (map ->event pressed))))

(deftest key-tests
  (are [pressed anticipated]
       ;; We use vectors as the test format because they're easier to
       ;; read, but we still want to use maps as the underlying
       ;; construct for their flexibility. Yay juxt!
       (= anticipated (press pressed))
       ;; Single regular key press
       [[:b :dn]]
       [[:b :dn]]

       ;; Single regular key press and release
       [[:b :dn] [:b :up]]
       [[:b :dn] [:b :up]]

       ;; Modifier alias press only
       [[:j :dn]]
       []

       ;; Modifier alias press and release
       [[:j :dn] [:j :up]]
       [[:j :dn] [:j :up]]

       ;; Modifier alias repeat and release
       [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
       [[:j :dn] [:j :up]]

       ;; Modifier alias with regular key press
       [[:j :dn] [:x :dn]]
       [[:rshift :dn] [:x :dn]]

       ;; Modifier alias with regular key press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up]]
       [[:rshift :dn] [:x :dn] [:x :up] [:rshift :up]]

       ;; Modifier alias with regular key press and release followed
       ;; by modifier alias press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
       [[:rshift :dn] [:x :dn] [:x :up] [:rshift :up] [:j :dn] [:j :up]]

       ;; Multiple modifier aliases down sends nothing
       [[:j :dn] [:k :dn]]
       []

       ;; Multiple modifier aliases down followed by regular key down
       ;; adds both modifiers to regular key
       [[:j :dn] [:k :dn] [:x :dn]]
       [[:rshift :dn] [:rcontrol :dn] [:x :dn]]

       ;; Order of modifier aliases down is preserved? (TODO: Is this
       ;; what we want?)
       [[:k :dn] [:j :dn] [:x :dn]]
       [[:rcontrol :dn] [:rshift :dn] [:x :dn]]

       ;; A modifier alias going up when another modifier is undecided
       ;; means the second modifier was a regular keypress.
       [[:j :dn] [:k :dn] [:k :up]]
       [[:rshift :dn] [:k :dn] [:k :up]]
       ))

