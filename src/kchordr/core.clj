(ns kchordr.core)

(def ^{:private true
       :doc "Map of keys to classes. Absense from this list means it's a normal key."}
  key-classes
  {:j :modifier-alias})

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

(defmulti next-state
  "Given the current state, a Return a new key state ")

(def ^{:private true
       :doc "Maps the current state a key class and a direction to a
  new state"}
  state-transitions
  {[:ground :normal :up] :ground
   [:ground :normal :dn] :ground
   [:ground :modifier :dn] :mod-pending
   [:ground :modifier :up] :error})

(defn process
  "Given the current key state and a key event, return an updated key
  state."
  [state key direction]
  (let [cls (get key-classes key :normal)
        new-state (get state-transitions [cls direction])]
    (if (= cls :modifier)
      state
      (update-in state [:to-send] #(conj % [key direction])))))

(defn to-send
  "Given a key state, return any pending key events, in the form of
  [key direction] pairs."
  [state]
  (:to-send state))