(ns khordr.handler.simple-alias
  "Implements a simple alias handler, which maps a chorded key to
  another key. For example, it can be used to map the combination
  of a and j to down-arrow."
  (:require [khordr.handler :as h])
  (:import khordr.effect.Key))

;; Handler: we start here. The very next thing to happen should be
;; that we get the key down that activated this handler, which tells
;; us the trigger and pushes us into Armed.
(defrecord Handler [aliases])

;; Armed: the first state of the handler after the initial keypress.
;; Means that we're ready to alias if necessary, but we can't tell yet
;; if the user is just typing a regular key or really does want to try
;; to alias a modifier.
(defrecord Armed [trigger aliases])

;; Deciding: The user might be trying to alias: if the next thing that
;; happens is either a repeat of the pending-key or the pending key
;; going up, then we're aliasing. Otherwise it was just a rollover.
(defrecord Deciding [trigger pending-key aliases])

;; Aliasing: The user is using the chord as an alias
(defrecord Aliasing [trigger aliases])

(extend-protocol h/KeyHandler
  
  Handler
  (process [{:keys [aliases] :as this}
            state
            {:keys [key] :as keyevent}]
    ;; If there are already keys down, we don't want to do any special
    ;; processing: It's a rollover situation
    (if-not (empty? (:down-keys state))
      {:handler nil
       :effects [(Key. keyevent)]}
      {:handler (Armed. key aliases)}))

  Armed
  (process [{:keys [trigger aliases] :as this}
            state
            {:keys [key direction] :as keyevent}]
    (cond

     (= [trigger :dn] [key direction])
     {:handler this}

     (= [trigger :up] [key direction])
     {:handler nil
      :effects [(Key. (assoc keyevent :direction :dn))
                (Key. keyevent)]}

     (and (= direction :dn) (contains? aliases key))
     {:handler (Deciding. trigger key aliases)}

     :else
     {:handler nil
      :effects [(Key. (assoc keyevent :key trigger :direction :dn))
                (Key. keyevent)]}))


  Deciding
  (process [{:keys [trigger pending-key aliases]}
            state
            {:keys [key direction] :as keyevent}]
    (cond 

     ;; Repeat of aliased key - we're aliasing
     (= [pending-key :dn] [key direction])
     {:handler (Aliasing. trigger aliases)
      :effects [(Key. (assoc keyevent :key (aliases key)))]}

     ;; Aliased key goes up - we're aliasing
     (= [pending-key :up] [key direction])
     {:handler (Aliasing. trigger aliases)
      :effects [(Key. (assoc keyevent :key (aliases key) :direction :dn))
                (Key. (assoc keyevent :key (aliases key) :direction :up))]}

     ;; Rollover
     :else
     {:handler nil
      :effects [(Key. (assoc keyevent :key trigger :direction :dn))
                (Key. (assoc keyevent :key pending-key :direction :dn))
                (Key. keyevent)]}))

  Aliasing
  (process [{:keys [trigger aliases] :as this}
            state
            {:keys [key direction] :as keyevent}]
    (let [aliased-key (aliases key)]
      (cond

       (= [trigger :up] [key direction])
       {:handler nil}

       aliased-key
       {:handler this
        :effects [(Key. (assoc keyevent :key aliased-key))]}
       
       :else
       {:handler this}))))