(ns khordr
  (:refer-clojure :exclude [key send])
  (:require [khordr.platform.common :as com]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn no-op [& args])
;; (def log println)
(def log no-op)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; IKeyHandler and implementations
;;

(defprotocol IKeyHandler
  (process [this keyevent]
    "Process a key event, returning a map. The map should contain the
     following keys: :handler - either an implementation of
     IKeyHandler representing the updated state of this handler or
     nil, indicating that the handler should be removed from the
     handler list; :effects - a seq of commands legal for consumption
     by `engine`; :continue - true if the event should be passed to
     later handlers in the chain."))

(defrecord DefaultKeyHandler [self-key]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent]
     (if (= key (:self-key this))
       {:handler (when (= direction :dn) this)
        :continue true
        :effects [{:effect :key :event keyevent}]}
       {:handler this
        :effects []
        :continue true}))))


(defrecord ModifierAliasKeyHandler [self-key alias state]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent
          which (if (= key self-key) :self :other)]
      (log "key=" key "direction=" direction "which=" which "state=" state)
      ;; It's a bummer, but we can't use core.match because of AOT.
      ;; The code was somewhat prettier with it. Maybe we can switch
      ;; back when core.match matures.
      (cond
       ;; If we're undecided and we see another self-down,
       ;; continue waiting.
       (and (= state :undecided) (= which :self) (= direction :dn))
       {:handler this
        :continue true}

       ;; If we're undecided and we see an other-down, then we
       ;; know that we're going to be aliasing, so we change state
       ;; and send the alias down.
       (and (= state :undecided) (= which :other) (= direction :dn))
       {:handler (ModifierAliasKeyHandler. self-key alias :aliasing)
        :effects [{:effect :key
                   :event (assoc keyevent :key alias :direction :dn)}]
        :continue true}

       ;; If we're undecided and we see a self-up, then we're not
       ;; aliasing, so we can just send self-down and self-up.
       (and (= state :undecided) (= which :self) (= direction :up))
       (do
         (log "self-up")
         {:handler nil
          :effects [{:effect :key
                     :event (assoc keyevent :key self-key :direction :dn)}
                    {:effect :key
                     :event (assoc keyevent :key self-key :direction :up)}]
          :continue true})

       ;; If we're aliasing and we see a self-down or a self-up,
       ;; then we can just send the alias, removing ourselves from
       ;; the chain if it's an up
       (and (= state :aliasing) (= which :self))
       {:handler (when (= direction :dn) this)
        :effects [{:effect :key :event (assoc keyevent :key alias)}]
        :continue true}

       ;; Otherwise, we don't change anything
       :else
       {:handler this
        :effects []
        :continue true})
      )))

(defn make-modifier-alias
  "Helper function to create a ModifierAliasKeyHandler in its initial
  state."
  [key alias]
  (->ModifierAliasKeyHandler key alias :undecided))

(defrecord SpecialActionKeyHandler [self-key]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent]
      (println key direction)
      (merge {:continue false}
             (cond
              ;;[key direction]

              (and (= key self-key) (= direction :up))
              {:handler nil
               :effects [{:effect :key
                          :event (assoc keyevent :key self-key :direction :dn)}
                         {:effect :key
                          :event (assoc keyevent :key self-key :direction :up)}]}

              (and (= key :q) (= direction :dn))
              {:handler this
               :effects [{:effect :quit}]}

              :else
              {:handler this})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Map of keys to behaviors. Absence from this list means it's a regular key."}
  default-key-behaviors
  {:j [make-modifier-alias :rshift]
   :k [make-modifier-alias :rcontrol]
   :l [make-modifier-alias :ralt]
   :f [make-modifier-alias :lshift]
   :d [make-modifier-alias :lcontrol]
   :s [make-modifier-alias :lalt]
   :backtick [->SpecialActionKeyHandler]})

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
  [state keyevent]
  ;; Is this a key down event? If not, return the state unchanged. If
  ;; so, is the key already down? If not, create a handler and add it
  ;; to the end of the chain. If so, return the state unchanged.
  (let [{:keys [key direction]} keyevent]
    (if (and (= direction :dn)
             (not (is-down? state key)))
      (update-in state [:handlers] concat [(handler state key)])
      state)))

(defn update-key-positions
  "Return a state that has been updated to reflect which keys are up
  and which are down."
  [state keyevent]
  (let [{:keys [key direction]} keyevent]
   (assoc-in state [:positions key] direction)))

(defn handle-keys
  "Given the current state and a key event, return an updated state."
  [state keyevent]
  ;; It turns out to be important to preserve other keys in the
  ;; keyevent, because they contain platform-specific stuff like the
  ;; keyboard the event arrived on.
  (let [{:keys [key direction]} keyevent
        ;; Important! Don't update the positions until after we add
        ;; the new handler, since whether or not we add one depends on
        ;; whether a key is already down.
        state (maybe-add-handler state keyevent)
        state (update-key-positions state keyevent)
        ;; Walk the handler chain, dealing with the results at each step
        handlers (:handlers state)
        results (map #(process % keyevent) handlers)
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
;; Effects handling
;;

(defmulti enact
  "Extension point for effects produced by handlers."
  (fn [effect state platform] (:effect effect)))

(defmethod enact :key
  [effect state platform]
  (com/send-key platform (:event effect))
  state)

(defmethod enact :quit
  [_ state _]
  (assoc state :done true))

(defn enact-effects!
  "Given the state, enact any pending effects and return a new state."
  [state platform]
  (loop [state state
         [effect & more] (:effects state)]
    (if effect
      (let [state (enact effect state platform)]
        (recur state more))
      (dissoc state :effects))))

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