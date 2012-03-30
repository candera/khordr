(ns kchordr.core
  (:refer-clojure :exclude [key]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-op [& args])
(def log no-op)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol KeyBehavior
  "Defines behavior of a mapped key."
  (modifier-alias? [this])
  (regular? [this]))

(extend-protocol KeyBehavior

  ;; Nil defines the behavior for normal keys
  nil
  (modifier-alias? [_] false)
  (regular? [_] true))

;; A key mapping to a modifier like shift or control
(defrecord ModifierAlias [alias]
  KeyBehavior
  (modifier-alias? [_] true)
  (regular? [_] false))

(def ^{:doc "Map of keys to behaviors. Absence from this list means it's a normal key."}
  default-key-behaviors
  {:j (ModifierAlias. :rshift)})

(defn state
  "Returns a new key-state object."
  [behaviors]
  {:keystate {}
   :to-send []
   :behaviors behaviors})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key events

(defn ->event
  "Given a key name and a direction, either as individual arguments or
  as a two-element sequence, return a new key event."
  ([[key direction]] (->event key direction))
  ([key direction] {:key key :direction direction}))

(defn append
  "Append bs to a, where bs and a are (potentially empty) sequences of
  events."
  [a & bs]
  (concat a bs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn undecided-modifier?
  "Return true when the keystate contains a undecided modifier key."
  [keystate]
  (some (fn [[k v]] (= :undecided v)) keystate))

(defn regular-key?
  "Return true if key is a regular key."
  [state key]
  (regular? (get-in state [:behaviors key])))

(defn regular-keydown?
  "Return true if the key event is a key down event where the key is
  not aliased."
  [state key direction]
  (and (regular-key? state key) (= direction :dn)))

(defn undecided-modifier-downs
  "Return a seq of events for the undecided modifiers."
  [state]
  {:keystate {:j :undecided}}
  (->> (:keystate state)
       (filter (fn [[k v]] (= v :undecided)))
       (map first)
       (map (fn [k] (->event (:alias (get (:behaviors state) k)) :dn)))))

(defn decide-modifier
  "Given the key state, and a vector containing a key and its status,
  assoc the appropriate new state for the key into the keystate. The
  appopriate state depends on whether the key is undecided. If it is,
  the state comes from the behaviors map. Otherwise, the key status
  remains unchanged."
  [state [key status]]
  (update-in state [:keystate key]
             #(if (= :undecided status)
                (:alias (get (:behaviors state) key))
                %)))

;; {:j :undecided :k :lcontrol} => {:j :lshift :k :lcontrol}
(defn decide-modifiers
  "Return a new keystate wherein undecided modifiers have been decided
  to be their aliased equivalents.

  {:j :undecided :k :undecided :l :lalt} =>
  [:j :rshift :k :rcontrol :l :lalt]"
  [state]
  (:keystate (reduce decide-modifier state (:keystate state))))

(defn process
  "Given the current state and a key event, return an updated state."
  [state event]
  (let [{:keys [key direction]} event
        behavior (get-in state [:behaviors key])
        keystate (:keystate state)]
    (log "Beginning state is" state)
    (log "Processing event" key direction)
    (log "Key behavior is" behavior)
    (log "Keystate is" keystate)
    ;; TODO: Change the cond statement out for core.match
    (cond
     (and (modifier-alias? behavior) (= :dn direction))
     (do
       (log "Modifier" key "being pressed")
       (update-in state [:keystate key] (constantly :undecided)))

     (and (undecided-modifier? keystate)
          (regular-keydown? state key direction))
     (do
       (log "Regular key" key
            "being pressed while there are undecided modifiers")
       (assoc state
         :to-send (concat (:to-send state)
                          (undecided-modifier-downs state)
                          [(->event key :dn)])
         :keystate (decide-modifiers state)))

     :else
     (do
       (log "Default processing for" key direction)
       (update-in state [:to-send] append (->event key direction))))))

