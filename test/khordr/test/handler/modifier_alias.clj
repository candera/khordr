(ns khordr.test.handler.modifier-alias
  (:use clojure.test
        khordr.handler.modifier-alias)
  (:require [khordr.handler :as h])
  (:import khordr.effect.Key))

(deftest stupid-multi-armed-bug
  (let [aliases{:f :lshift :d :lcontrol :s :lalt}
        handler (->MultiArmed [:d :s] aliases)]
    (is (= (->MultiArmed [:s] aliases)
           (:handler (h/process handler {} {:key :d :direction :up}))))))

(defn process
  "Send a key event to the current handler, returning an updated state
  and accumulating events."
 [state keyevent]
 (let [handler (:handler state)
       result (h/process handler state keyevent)]
   (-> state
       (assoc :handler (:handler result))
       (update-in [:effects] conj (:effects result)))))

(deftest eats-modifier-rollovers-bug
  (let [aliases{:f :lshift :d :lcontrol :s :lalt}
        handler (->MultiArmed [:d :s] aliases)
        state (process {} {:key :d :direction :up})
        state (process {} {:key :s :direction :up})]
    (is (= (:effects state)
           [(Key. {:key :lalt :direction :dn})
            (Key. {:key :d :direction :dn})
            (Key. {:key :d :direction :up})
            (Key. {:key :lalt :direction :up})]))))