(ns kchordr.core)

(def ^{:private true
       :doc "Map of keys to classes. Absence from this list means it's a normal key."}
  key-classes
  {:j [:modifier-alias :lshift]})

(defn key-state
  "Returns a new key-state object."
  []
  {:to-send []
   :state :ground})

;; (defprotocol KeyState
;;   (next-state [this key direction] "Compute the next state given the current state and a new key press"))

;; (defrecord GroundState []
;;     KeyState
;;     (next-state [this key direction]
;;       )
;;   )

;; (defmulti next-state
;;   "Given the current state, a Return a new key state ")

(def ^{:private true
       :doc "Maps the current state a key class and a direction to a
  new state"}
  state-transitions
  {[:ground :normal :up] :ground
   [:ground :normal :dn] :ground
   [:ground :modifier-alias :dn] :mod-pending
   [:ground :modifier-alias :up] :error})

;; State looks something like
;; {:to-send [] :keystate {:j :pending, :k :lcontrol}}

(defn pending-modifier?
  "Return true when the key state contains a pending modifier key."
  [state]
  (some #(= :pending %) (vals (:keystate state))))

(defn pending-modifier-downs
  "Return a seq of keypresses for the pending modifiers."
  [state]
  (throw (NotImplmentedException.)))

;; {:j :pending :k :lcontrol} => {:j :lshift :k :lcontrol}
(defn decide-pending-modifiers
  "Return a sequence of key events for pending modifiers, deciding
  them to be their aliased equivalents."
  [state]
  (into {} (map (fn [[k v]] (if (= :pending v)
                              [k (second (k (key-classes)))]
                              [k v])))))


(defn process
  "Given the current key state and a key event, return an updated key
  state."
  [state key direction]
  (let [cls (get key-classes key :normal)]
    (cond
     (and (= cls :modifier-alias) (= :dn direction))
     (update-in state [:keystate key] (constantly :pending))

     (and (pending-modifier? state)
          (regular-keydown? key direction))
     (assoc state
       :to-send (concat (:to-send state)
                        (pending-modifier-downs state)
                        [key :dn])
       :keystate (apply assoc (:keystate state)
                   (decide-pending-modifiers state)))

     :else
     (update-in state [:to-send] #(conj % [key direction])))))

(defn to-send
  "Given a key state, return any pending key events, in the form of
  [key direction] pairs."
  [state]
  (:to-send state))