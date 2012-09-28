(ns khordr.test.khordr
  (:refer-clojure :exclude (send))
  (:require [khordr :refer (handle-keys
                            base-state
                            match?
                            enact-effects!)]
            [khordr.effect :as e])
  (:use [clojure.test])
  (:import [khordr.effect Key Quit]))


#_(defn- sent
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
  (Key. {:key key :direction direction}))

#_(defn- press
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


(deftest enact-test
  (let [sent-keys (atom [])
        platform (reify khordr.platform.common.IPlatform
                   (await-key-event [this])
                   (send-key [this keyevent]
                     (swap! sent-keys concat [keyevent])))]
    (is (= {} (enact-effects! {:effects []} platform)))
    (enact-effects! {:effects [(e/Key. {:key :a :direction :dn})
                               (e/Key. {:key :a :direction :up})]}
                    platform)
    (is (= @sent-keys [{:key :a :direction :dn}
                       {:key :a :direction :up}]))
    (is (:done (enact-effects! {:effects [(e/Quit.)]} platform)))))

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
