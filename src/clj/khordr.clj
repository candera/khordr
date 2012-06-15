(ns khordr
  (:refer-clojure :exclude [key send])
  (:use [clojure.core.match :only (match)]
        khordr.keycodes)
  ;; (:import interception.InterceptionLibrary)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-op [& args])
(def log no-op)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; IKeyHandler and implementations
;;

(defprotocol IKeyHandler
  (process [this key direction]
    "Process a key event, returning a map. The map should contain the
     following keys: :handler - either an implementation of
     IKeyHandler representing the updated state of this handler or
     nil, indicating that the handler should be removed from the
     handler list; :effects - a seq of commands legal for consumption
     by `engine`; :continue - true if the event should be passed to
     later handlers in the chain."))

(defrecord DefaultKeyHandler [self-key]
  IKeyHandler
  (process [this key direction]
    (if (= key (:self-key this))
      {:handler (when (= direction :dn) this)
       :continue true
       :effects [:key [key direction]]}
      {:handler this
       :effects []
       :continue true})))


(defrecord ModifierAliasKeyHandler [self-key alias state]
  IKeyHandler
  (process [this key direction]
    (match [state (if (= key self-key) :self :other) direction]

           ;; If we're undecided and we see another self-down,
           ;; continue waiting.
           [:undecided :self :dn]
           {:handler this
            :continue true}

           ;; If we're undecided and we see an other-down, then we
           ;; know that we're going to be aliasing, so we change state
           ;; and send the alias down.
           [:undecided :other :dn]
           {:handler (ModifierAliasKeyHandler. self-key alias :aliasing)
            :effects [:key [alias :dn]]
            :continue true}

           ;; If we're undecided and we see a self-up, then we're not
           ;; aliasing, so we can just send self-down and self-up.
           [:undecided :self :up]
           {:handler nil
            :effects [:key [self-key :dn] :key [self-key :up]]
            :continue true}

           ;; If we're aliasing and we see a self-down or a self-up,
           ;; then we can just send the alias, removing ourselves from
           ;; the chain if it's an up
           [:aliasing :self _]
           {:handler (when (= direction :dn) this)
            :effects [:key [alias direction]]
            :continue true}

           ;; Otherwise, we don't change anything
           [_ _ _]
           {:handler this
            :effects []
            :continue true}
           )))

(defn make-modifier-alias
  "Helper function to create a ModifierAliasKeyHandler in its initial
  state."
  [key alias]
  (->ModifierAliasKeyHandler key alias :undecided))

(defrecord SpecialActionKeyHandler [self-key]
  IKeyHandler
  (process [this key direction]
    (merge {:continue false}
           (match [key direction]

                  [self-key :up]
                  {:handler nil
                   :effects [:key [self-key :dn] :key [self-key :up]]}

                  [:q :dn]
                  {:handler this
                   :effects [:quit nil]}

                  [_ _]
                  {:handler this}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Map of keys to behaviors. Absence from this list means it's a regular key."}
  default-key-behaviors
  {:j [make-modifier-alias :rshift]
   :k [make-modifier-alias :rcontrol]
   :l [make-modifier-alias :ralt]
   :f [make-modifier-alias :lshift]
   :d [make-modifier-alias :lcontrol]
   :s [make-modifier-alias :lalt]
   :backtick [(fn [key] (SpecialActionKeyHandler. key))]})

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
;;
;; Application engine

(defn base-state
  "An empty key-state value. Used for initializing the application
  engine."
  [behaviors]
  {:behaviors behaviors
   :handlers []
   :positions {}})

(defn is-down?
  "Return true if the specified key is in the down position."
  [state key]
  (= :dn (get-in state [:positions key])))

(defn handler
  "Return a new handler for the specified key."
  [state key]
  (let [[make-handler & params]
        (get (:behaviors state) key [->DefaultKeyHandler])]
    (apply make-handler key params)))

(defn maybe-add-handler
  "Return a state value that has been updated to include any necessary
  new key handlers."
  [state key direction]
  ;; Is this a key down event? If not, return the state unchanged. If
  ;; so, is the key already down? If not, create a handler and add it
  ;; to the end of the chain. If so, return the state unchanged.
  (if (and (= direction :dn)
           (not (is-down? state key)))
    (update-in state [:handlers] concat [(handler state key)])
    state))

(defn update-key-positions
  "Return a state that has been updated to reflect which keys are up
  and which are down."
  [state key direction]
  (assoc-in state [:positions key] direction))

(defn handle-keys
  "Given the current state and a key event, return an updated state."
  [state event]
  (let [{:keys [key direction]} event
        ;; Important! Don't update the positions until after we add
        ;; the new handler, since whether or not we add one depends on
        ;; whether a key is already down.
        state (maybe-add-handler state key direction)
        state (update-key-positions state key direction)
        ;; Walk the handler chain, dealing with the results at each step
        handlers (:handlers state)
        results (map #(process % key direction) handlers)
        ;; If one of the handlers prevents processing from continuing,
        ;; we don't take any subsequent effects into consideration,
        ;; but we do let the handlers update themselves based on the
        ;; key event. We may need to revisit this in the future for
        ;; more complex scenarios, perhaps by making :continue provide
        ;; an enumerated value, rather than just a boolean.
        continue-count (count (take-while :continue results))
        effects (mapcat :effects (take (inc continue-count) results))]
    (-> state
        (assoc-in [:handlers] (filter identity (map :handler results)))
        (update-in [:effects] concat effects))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Main loop - commented out FTM

(comment
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
                   key (get khordr.keycodes/keycodes key-index (.code stroke))
                   result (.invoke f (->event key direction))]
               ;; For now, just always send on the keystrokes
               (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1)
               (when result
                 (recur))))))
       (finally
        (.interception_destroy_context InterceptionLibrary/INSTANCE ctx))))))