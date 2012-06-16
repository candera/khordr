(ns khordr.test.khordr
  (:refer-clojure :exclude (send))
  (:require [khordr :refer (handle-keys
                            make-modifier-alias
                            base-state
                            enact-effects!)])
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

(defn- ->key-effect
  [[key direction]]
  {:effect :key :event {:key key :direction direction}})

(defn- press
  "Given a seq of pairs of [key direction], return a seq of
  corresponding key effects."
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
       [(->key-effect [:b :dn])]

       ;; Single regular key press and release
       [[:b :dn] [:b :up]]
       [(->key-effect [:b :dn]) (->key-effect [:b :up])]

       ;; Modifier alias press only
       [[:j :dn]]
       []

       ;; Modifier alias press and release
       [[:j :dn] [:j :up]]
       [(->key-effect [:j :dn]) (->key-effect [:j :up])]

       ;; Modifier alias repeat and release
       [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
       [(->key-effect [:j :dn]) (->key-effect [:j :up])]

       ;; Modifier alias with regular key press
       [[:j :dn] [:x :dn]]
       [(->key-effect [:rshift :dn]) (->key-effect [:x :dn])]

       ;; Modifier alias with regular key press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up]]
       [(->key-effect [:rshift :dn])
        (->key-effect [:x :dn])
        (->key-effect [:x :up])
        (->key-effect [:rshift :up])]

       ;; Modifier alias with regular key press and release followed
       ;; by modifier alias press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
       [(->key-effect [:rshift :dn])
        (->key-effect [:x :dn])
        (->key-effect [:x :up])
        (->key-effect [:rshift :up])
        (->key-effect [:j :dn])
        (->key-effect [:j :up])]

       ;; Multiple modifier aliases down sends the first modifier
       [[:j :dn] [:k :dn]]
       [(->key-effect [:rshift :dn])]

       ;; Multiple modifier aliases down followed by regular key down
       ;; adds both modifiers to regular key
       [[:j :dn] [:k :dn] [:x :dn]]
       [(->key-effect [:rshift :dn])
        (->key-effect [:rcontrol :dn])
        (->key-effect [:x :dn])]

       ;; Order of modifier aliases down is preserved? (TODO: Is this
       ;; what we want?)
       [[:k :dn] [:j :dn] [:x :dn]]
       [(->key-effect [:rcontrol :dn])
        (->key-effect [:rshift :dn])
        (->key-effect [:x :dn])]

       ;; A modifier alias going up when another modifier is undecided
       ;; means the second modifier was a regular keypress.
       [[:j :dn] [:k :dn] [:k :up]]
       [(->key-effect [:rshift :dn])
        (->key-effect [:k :dn])
        (->key-effect [:k :up])]

       ;; We have the ability to quit the application
       [[:backtick :dn] [:q :dn]]
       [{:effect :quit}]

       ;; But other nearby key sequences don't quit
       [[:backtick :dn] [:backtick :up] [:q :dn] [:backtick :dn]]
       [(->key-effect [:backtick :dn])
        (->key-effect [:backtick :up])
        (->key-effect [:q :dn])]

       ;; TODO: Also move towards using key maps to describe keys even on output
       ))

(deftest enact
  (let [sent-keys (atom [])
        platform (reify khordr.platform.common.IPlatform
                   (await-key-event [this])
                   (send-key [this keyevent]
                     (swap! sent-keys concat [keyevent])))]
    (is (= {} (enact-effects! {:effects []} platform)))
    (enact-effects! {:effects [{:effect :key
                                :event {:key :a :direction :dn}}
                               {:effect :key
                                :event {:key :a :direction :up}}]}
                    platform)
    (is (= @sent-keys [{:key :a :direction :dn}
                       {:key :a :direction :up}]))
    (is (:done (enact-effects! {:effects [{:effect :quit}]} platform)))))