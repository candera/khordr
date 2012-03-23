(ns kchordr.core)

(def ^{:private true
       :doc "Map of keys to classes. Absence from this list means it's a normal key."}
  key-classes
  {:j [:modifier-alias :rshift]})

(defn key-alias
  "Given a key, give the key it aliases to."
  [key]
  (if-let [alias (get key-classes key)]
    (second alias)
    key))

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
   [:ground :modifier-alias :dn] :mod-undecided
   [:ground :modifier-alias :up] :error})

(defn modifier-alias?
  "Given a key class, return true if it is a modifier alias"
  [cls]
  (and (coll? cls) (= (first cls) :modifier-alias)))

(defn undecided?
  "Given a key and its state, return true if it is undecided."
  [[_ s]]
  (= :undecided s))


;; State looks something like
;; {:to-send [] :keystate {:j :undecided, :k :lcontrol}}

(defn undecided-modifier?
  "Return true when the keystate contains a undecided modifier key."
  [keystate]
  (some undecided? keystate))

(defn regular-key?
  "Return true if key is not an aliased key."
  [key]
  (not (get key-classes key nil)))

(defn regular-keydown?
  "Return true if the key event is a key down event where the key is
  not aliased."
  [key direction]
  (and (regular-key? key) (= direction :dn)))

(defn undecided-modifier-downs
  "Return a seq of keypresses for the undecided modifiers."
  [keystate]
  (->> keystate
       (filter undecided?)
       (map first)
       (mapcat (fn [k] [(key-alias k) :dn]))))

(defn decide-modifier
  "Given a keystate map, and a vector containing a key and its state,
  assoc the appropriate new state for the key into the keytstate. The
  appopriate state depends on whether the key is undecided. If it is,
  the state comes from the key-classes map. Otherwise, the key state
  remains unchanged."
  [keystate ks]
  (assoc keystate (first ks) (if (undecided? ks)
                               (second (get key-classes (second ks)))
                               (second ks))))

;; {:j :undecided :k :lcontrol} => {:j :lshift :k :lcontrol}
(defn decide-modifiers
  "Return a new keystate wherein undecided modifiers have been decided
  to be their aliased equivalents.

  {:j :undecided :k :undecided :l :lalt} =>
  [:j :rshift :k :rcontrol :l :lalt]"
  [keystate]
  (reduce decide-modifier {} keystate))

(defn process
  "Given the current key state and a key event, return an updated key
  state."
  [state key direction]
  (let [cls (get key-classes key :normal)
        keystate (:keystate state)]
    (cond
     (and (modifier-alias? cls) (= :dn direction))
     (update-in state [:keystate key] (constantly :undecided))

     (and (undecided-modifier? keystate)
          (regular-keydown? key direction))
     (assoc state
       :to-send (concat (:to-send state)
                        (undecided-modifier-downs keystate)
                        [key :dn])
       :keystate (decide-modifiers keystate))

     :else
     (update-in state [:to-send] #(concat % [[key direction]])))))

(defn to-send
  "Given a key state, return any pending key events, in the form of
  [key direction] pairs."
  [state]
  (:to-send state))