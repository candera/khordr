(ns kchordr.core
  (:refer-clojure :exclude [key])
  (:use [clojure.core.match :only (match)]
        kchordr.keycodes)
  (:import interception.InterceptionLibrary))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-op [& args])
(def log no-op)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Map of keys to behaviors. Absence from this list means it's a regular key."}
  default-key-behaviors
  {:j [:modifier-alias :rshift]})

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
  (not (get-in state [:behaviors key])))

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

(defn handle-modifier-press
  "Return a new state that records the pressed modifier as undecided."
  [state key]
  (log "Modifier" key "being pressed")
  (update-in state [:keystate key] (constantly :undecided)))

(defn handle-deciding-regular-press
  "Return a new state that decides undecided modifiers, add down events
  for their aliases to the state and recording them as decided."
  [state key]
  (log "Regular key" key
       "being pressed while there are undecided modifiers")
  (assoc state
    :to-send (concat (:to-send state)
                     (undecided-modifier-downs state)
                     [(->event key :dn)])
    :keystate (decide-modifiers state)))

(defn handle-default
  "Handle a key event by simply appending it to the list of events to
  transmit."
  [state key direction]
  (log "Default processing for" key direction)
  (update-in state [:to-send] append (->event key direction)))

(defn process
  "Given the current state and a key event, return an updated state."
  [state event]
  (let [{:keys [key direction]} event
        [keyclass alias] (get-in state [:behaviors key] [:regular])
        keystate (:keystate state)]
    (log "Beginning state is" state)
    (log "Processing event" key direction)
    (log "Key behavior is" [keyclass alias])
    (log "Keystate is" keystate)
    (match [keyclass direction (undecided-modifier? keystate)]
           [:modifier-alias :dn _] (handle-modifier-press state key)
           [:regular :dn true] (handle-deciding-regular-press state key)
           [_ _ _] (handle-default state key direction))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn intercept
  "Start intercepting keys, calling the function (or var) with the
  received key event f until it returns false."
  [f]
  ;; TODO: This next statement needs to happen before this library loads
  ;; (System/setProperty "jna.library.path" "ext")

  ;; And this one should live somewhere else entirely
  ;;(import 'interception.InterceptionLibrary)
  
  (let [ctx (.interception_create_context InterceptionLibrary/INSTANCE)]
    (try
      (.interception_set_filter
       InterceptionLibrary/INSTANCE
       ctx
       (reify interception.InterceptionLibrary$InterceptionPredicate
         (apply [_ device]
           (.interception_is_keyboard InterceptionLibrary/INSTANCE device)))
       (short -1))
      (loop []
        (let [device (.interception_wait InterceptionLibrary/INSTANCE ctx)
              stroke (interception.InterceptionKeyStroke$ByReference.)
              received (.interception_receive
                        InterceptionLibrary/INSTANCE
                        ctx
                        device
                        stroke
                        1)]

          (when (< 0 received)
            ;; TODO: figure out how this should work - what function
            ;; should call what other function? Should there be a
            ;; trampoline involved?
            ;; (println "raw code:" (.code stroke) "raw state:" (.state stroke))
            (let [state (.state stroke)
                  direction (if (bit-test state 0) :up :dn)
                  e0 (when (bit-test state 1) :e0)
                  e1 (when (bit-test state 2) :e1)
                  key-index (filter identity [(.code stroke) e0 e1])
                  key (get kchordr.keycodes/keycodes key-index (.code stroke))
                  result (.invoke f (->event key direction))]
              ;; For now, just always send on the keystrokes
              (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1)
              (when result
                (recur))))))
      (finally
       (.interception_destroy_context InterceptionLibrary/INSTANCE ctx)))))