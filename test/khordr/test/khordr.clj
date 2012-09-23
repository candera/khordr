(ns khordr.test.khordr
  (:refer-clojure :exclude (send))
  (:require [khordr :refer (handle-keys
                            base-state
                            match?
                            enact-effects!)])
  (:use [clojure.test]))

(def test-key-behaviors
  '[{:match {:key #{:j :k} :direction :dn}
     :handler khordr.handler.modifier-alias/Handler
     :args [{:j :rshift, :k :rcontrol}] }
    {:match {:key :backtick :direction :dn}
     :handler khordr.handler.special-action/Handler}])

(defn- sent
  "Given a sequence of key events, return the sequence of keys that
  will actually be sent."
  [events]
  (:effects (reduce #(handle-keys %1 %2)
                    (base-state test-key-behaviors)
                    events)))

(defn- event
  [[key direction]]
  {:key key :direction direction})

(defn- key-effect
  [[key direction]]
  {:effect :key :event {:key key :direction direction}})

(defn- press
  "Given a seq of pairs of [key direction], return a seq of
  corresponding key effects."
  [pressed]
  (sent (map event pressed)))

(defn cross-prod
  "Return the Cartesian cross-product of colls."
  [& colls]
  (if (= 1 (count colls))
    colls
    (->> colls
         (reduce #(for [x %1 y %2] [x y]))
         (map flatten)                  ; Not quite right, but close
                                        ; enough
         )))

(defn all-events
  "Returns a lazy seq of all the key events that can be made from the
  keys in keyset. If n is supplied, return a lazy seq of all the seqs
  of length n that can be made from those same keys."
  ([keyset]
     (->> (cross-prod keyset #{:up :dn})
          (map (fn [[key dir]] {:key key :direction dir}))))
  ([keyset n]
     (apply cross-prod (repeat n (all-events keyset)))))

(defn direction?
  "Return true if event is in direction. If specified, only return
  true if the event is also for key"
  ([direction event]
     (= direction (:direction event)))
  ([direction key event]
     (and (direction? direction event) (= key (:key event)))))

(defn down?
  "Return true if event is a down event. If specified, only return true
  if the event is also for key"
  ([event] (direction? :dn event))
  ([key event] (direction? :dn key event)))

(defn up?
  "Return true if event is a down event. If specified, only return true
  if the event is also for key"
  ([event] (direction? :up event))
  ([key event] (direction? :up key event)))

(defn up-does-not-preceed-down?
  "Returns true if the sequence of events does not contain a case where
  some key goes up before it goes down."
  [events]
  (loop [preceeding []
         [head & more] events]
    (if (seq head)
      (when-not (and (up? head)
                     (not (some (partial down? (:key head)) preceeding)))
        (recur (conj preceeding head) more))
      true)))

(defn unmatched-up?
  "Returns true if there are more up than down events for any given key"
  [events]
  (->> events
       (group-by :key)
       (map (fn [[k v]]
              (- (count (filter up? v))
                 (count (filter down? v)))))
       (some pos?)))

(defn valid?
  "Returns true if events 'makes sense'. E.g. an up event does not
  appear before the corresponding down event."
  [events]
  (and (up-does-not-preceed-down? events)
       (not (unmatched-up? events))))

(defn valid-events
  "Returns a lazy seq of all the valid seqs of key events of length n
  that can be made from the keys in keyset."
  [keyset n]
  (filter valid? (all-events keyset n)))

(deftest key-tests
  (are [pressed anticipated]
       ;; We use vectors as the test format because they're easier to
       ;; read, but we still want to use maps as the underlying
       ;; construct for their flexibility. Yay juxt!
       (= anticipated (press pressed))
       ;; Single regular key press
       [[:b :dn]]
       [(key-effect [:b :dn])]

       ;; Single regular key press and release
       [[:b :dn] [:b :up]]
       [(key-effect [:b :dn]) (key-effect [:b :up])]

       ;; Modifier alias press only
       [[:j :dn]]
       []

       ;; Modifier alias press and release
       [[:j :dn] [:j :up]]
       [(key-effect [:j :dn]) (key-effect [:j :up])]

       ;; Modifier alias repeat and release
       [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
       [(key-effect [:j :dn]) (key-effect [:j :up])]

       ;; Modifier alias with regular key press results in no events
       ;; because we're not sure yet whether the user is just
       ;; "rolling" through keys or trying to modify
       [[:j :dn] [:x :dn]]
       []

       ;; Modifier aliasing happens as soon as regular key goes up
       [[:j :dn] [:x :dn] [:x :up]]
       [(key-effect [:rshift :dn])
        (key-effect [:x :dn])
        (key-effect [:x :up])]

       ;; Modifier alias with regular key press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up]]
       [(key-effect [:rshift :dn])
        (key-effect [:x :dn])
        (key-effect [:x :up])
        (key-effect [:rshift :up])]

       ;; Modifier alias with regular key press and release followed
       ;; by modifier alias press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
       (map key-effect
            [[:rshift :dn]
             [:x :dn]
             [:x :up]
             [:rshift :up]
             [:j :dn]
             [:j :up]])

       ;; Multiple modifier aliases down followed by regular key down
       ;; adds both modifiers to regular key
       [[:j :dn] [:k :dn] [:x :dn] [:x :up]]
       (map key-effect
            [[:rshift :dn]
             [:rcontrol :dn]
             [:x :dn]
             [:x :up]])

       ;; Order of modifier aliases down is preserved? (TODO: Is this
       ;; what we want?)
       [[:k :dn] [:j :dn] [:x :dn] [:x :up]]
       (map key-effect
            [[:rcontrol :dn]
             [:rshift :dn]
             [:x :dn]
             [:x :up]])

       ;; A modifier alias going up when another modifier is undecided
       ;; means the user changed their mind. Do nothing.
       [[:j :dn] [:k :dn] [:k :up]]
       []

       ;; Adding and subtracting modifiers: you can change your mind
       ;; by releasing a modifier and pressing it (or a different one)
       ;; again.
       [[:j :dn] [:k :dn] [:k :up] [:k :dn] [:x :dn]]
       (map key-effect
            [[:rshift :dn]
             [:rcontrol :dn]
             [:x :dn]])

       ;; Rollover keys still work
       [[:r :dn] [:k :dn]]
       (map key-effect
            [[:r :dn]
             [:k :dn]])

       ;; We have the ability to quit the application
       [[:backtick :dn] [:q :dn]]
       [{:effect :quit}]

       ;; But other nearby key sequences don't quit
       [[:backtick :dn] [:backtick :up] [:q :dn] [:backtick :dn]]
       [(key-effect [:backtick :dn])
        (key-effect [:backtick :up])
        (key-effect [:q :dn])
        ;;(key-effect [:backtick :dn])
        ]

       ;; TODO: Also move towards using key maps to describe keys even on output
       ))

(deftest enact-test
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

(deftest behavior-match

  ;; If the key doesn't match, it doesn't match
  (is (not (match? {:key :a} {:key :b :direction :dn})))

  ;; {:key :a} matches {:key :a} in either direction and extra entries
  ;; make no difference
  (is (match? {:key :a} {:key :a :direction :dn :device 2}))
  (is (match? {:key :a} {:key :a :direction :up :device 2 :other "whatever"}))

  ;; Direction matters when specified
  (is (match? {:key :a :direction :dn} {:key :a :direction :dn :device 2}))
  (is (not (match? {:key :a :direction :dn} {:key :a :direction :up :device 2})))

  ;; We can use sets for keys
  (is (match? {:key #{:a :b}} {:key :a :direction :dn :device 2}))
  (is (match? {:key #{:a :b}} {:key :b :direction :up :device 2}))

  ;; We can match just on direction
  (is (match? {:direction :dn} {:key :x :direction :dn :device 2}))
  (is (not (match? {:direction :dn} {:key :x :direction :up :device 2})))

  ;; We can use sets for direction
  (is (match? {:direction #{:up :dn}} {:key :x :direction :dn :device 2}))
  (is (match? {:direction #{:up :dn}} {:key :x :direction :up :device 2}))

  ;; An empty set doesn't match anything
  (is (not (match? {:key #{}} {:key :x :direction :dn :device 2}))))
