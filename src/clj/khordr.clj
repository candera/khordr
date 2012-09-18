(ns khordr
  (:refer-clojure :exclude [key send])
  (:require [khordr.platform.common :as com]
            [khordr.logging :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; IKeyHandler and implementations
;;

(defprotocol IKeyHandler
  (process [this keyevent]
    "Process a key event, returning a map. The map should contain the
  following keys:

  :handler - either an implementation of IKeyHandler representing the
  updated state of this handler or nil, indicating that the handler
  should be deactivated;

  :effects - a seq of commands legal for consumption by `engine`."))

;; The default implementation: pass through key events unchanged.
;; yourself when self-key goes up.
(defrecord DefaultKeyHandler [self-key]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent]
      {:handler (when-not (and (= direction :up)
                               (= key (:self-key this)))
                  this)
       :effects [{:effect :key :event keyevent}]})))

;; And if we're not tracking any keys (as can happen if the first
;; event is a key up), just pass through the events unchanged. The
;; reason we want both this and DefaultKeyHandler is that
;; DefaultKeyHandler will prevent any other key down event from
;; activating a handler.
(extend-protocol IKeyHandler
  nil
  (process [_ keyevent]
    {:handler nil
     :effects [{:effect :key :event keyevent}]}))

(defn conj-if-missing
  "Returns coll with elem conj'd on, but only if coll does not already
  contain elem."
  [coll elem]
  (if (some #(= elem %) coll)
    coll
    (conj coll elem)))

(defn key-effect
  "Creates a key effect given a keyevent template, a key, and a direction."
  [template key direction]
  {:effect :key :event (assoc template :key key :direction direction)})

(defrecord ModifierAliasKeyHandler [down-modifiers pending-keys aliases state]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent
          modifier? (contains? aliases key)
          up? (= direction :up)
          down? (not up?)]

      ;; It's a bummer, but we can't use core.match because of AOT.
      ;; The code was somewhat prettier with it. Maybe we can switch
      ;; back when core.match matures.
      (case state

        ;; Initial: we start here. The very next thing to happen
        ;; should be that we get the key down that activated this
        ;; handler.
        :initial
        (if (and modifier? down?)
          {:handler
           (ModifierAliasKeyHandler.
            (conj down-modifiers key)
            pending-keys
            aliases
            :armed)}
          (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was in the inital state: " keyevent)
                          {:keyevent keyevent
                           :reason :weird-state
                           :source this})))

        ;; Armed: the first state of the handler after the initial
        ;; keypress. Means that we're ready to alias if necessary, but
        ;; we can't tell yet if the user is just typing a regular key
        ;; or really does want to try to alias a modifier
        :armed
        (cond
         (and modifier? down?)
         (if (= [key] down-modifiers)
           {:handler this}              ; It's a repeat
           {:handler (ModifierAliasKeyHandler.
                     (conj-if-missing down-modifiers key)
                     pending-keys
                     aliases
                     :multi-armed)})

         (and modifier? up?)
         {:handler nil
          :effects [(key-effect keyevent key :dn)
                    (key-effect keyevent key :up)]}

         (and (not modifier?) down?)
         {:handler (ModifierAliasKeyHandler.
                    down-modifiers
                    (conj pending-keys key)
                    aliases
                    :deciding)}

         :else
         (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was armed: " keyevent)
                         {:keyevent keyevent
                          :reason :weird-state
                          :source this})))

        ;; Multi-armed: The user has simultaneously pressed more than
        ;; one modifier alias. We're not sure what to do yet, since
        ;; they might release them all or go on to press a regular key
        ;; they want to modify.
        :multi-armed
        (cond
         (and modifier? down?)
         {:handler (ModifierAliasKeyHandler.
                    (conj-if-missing down-modifiers key)
                    pending-keys
                    aliases
                    :multi-armed)}

         (and modifier? up?)
         (let [new-down-modifiers (filterv #(not= modifier? %)
                                           down-modifiers)]
           (if (seq new-down-modifiers)
             {:handler (ModifierAliasKeyHandler.
                        new-down-modifiers
                        pending-keys
                        aliases
                        :multi-armed)}
             {:handler nil}))

         (and (not modifier?) down?)
         {:handler (ModifierAliasKeyHandler.
                    down-modifiers
                    []
                    aliases
                    :aliasing)
          :effects (conj
                    (mapv #(key-effect keyevent % :dn)
                          (map aliases down-modifiers))
                    (key-effect keyevent key :dn))}

         :else
         (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was multi-armed: " keyevent)
                         {:keyevent keyevent
                          :reason :weird-state
                          :source this})))

        ;; Deciding: The user might be trying to alias: we'll know for
        ;; sure if the next thing that happens is a regular key down.
        ;; Otherwise, it was probably just multiple accidental
        ;; simultaneous key presses.
        :deciding
        (cond
         (and (not modifier?) down?)
         {:handler (ModifierAliasKeyHandler.
                    down-modifiers
                    (conj pending-keys key)
                    aliases
                    :deciding)}

         (and (not modifier?) up?)
         {:handler (ModifierAliasKeyHandler.
                    down-modifiers
                    []
                    aliases
                    :aliasing)
          :effects (concat (map #(key-effect keyevent % :dn) (map aliases down-modifiers))
                           (map #(key-effect keyevent % :dn) pending-keys)
                           [(key-effect keyevent key :up)])}

         modifier?
         {:handler nil
          :effects (concat (map #(key-effect keyevent % :dn) down-modifiers)
                           (map #(key-effect keyevent % :dn) pending-keys)
                           [(key-effect keyevent key direction)])})

        ;; Aliasing: we're treating modifiers as their aliases, until
        ;; the last one goes up
        :aliasing
        (let [new-down-modifiers
              (cond
               (and modifier? down?)
               (conj-if-missing down-modifiers key)

               (and modifier? up?)
               (filterv #(not= key %) down-modifiers)

               :else
               down-modifiers)]
          {:handler (when (seq new-down-modifiers)
                      (ModifierAliasKeyHandler.
                       new-down-modifiers
                       nil
                       aliases
                       :aliasing))
           :effects [(key-effect keyevent (get aliases key key) direction)]})))))

(defn make-modifier-alias
  "Helper function to create a ModifierAliasKeyHandler in its initial
  state."
  [key aliases]
  (->ModifierAliasKeyHandler [] [] aliases :initial))

(defrecord SpecialActionKeyHandler [self-key]
  IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent]
      (cond
       (and (= key self-key) (= direction :up))
       {:handler nil
        :effects [{:effect :key
                   :event (assoc keyevent :key self-key :direction :dn)}
                  {:effect :key
                   :event (assoc keyevent :key self-key :direction :up)}]}

       (and (= key :q) (= direction :dn))
       {:handler this
        :effects [{:effect :quit}]}

       (and (= key :l) (= direction :dn))
       {:handler this
        :effects [{:effect :cycle-log}]}

       :else
       {:handler this}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; A sequence of pairs: a selector and a handler specifier. The
;; selector is a function of one argument that will be invoked with a
;; key name (e.g. :a). If the selector returns true, the first element
;; of the specifier is invoked with the matched key and any remaining
;; elements of the selector. As a special case, the selector may be a
;; keyword, which matches only itself.

;; TODO: Consider special-casing keywords as a selectors, and consider
;; passing the selector to the handler specifier. In general, the DSL
;; here needs to be more fleshed-out once we use it as a serialization
;; format.
(let [right-modifiers {:j :rshift
                       :k :rcontrol
                       :l :ralt}
      left-modifiers  {:f :lshift
                       :d :lcontrol
                       :s :lalt}]
  (def ^{:doc "Relates keys to behaviors. Absence from this data structure means it's a regular key."}
    default-key-behaviors
    [right-modifiers [make-modifier-alias right-modifiers]
     left-modifiers [make-modifier-alias left-modifiers]
     :backtick [->SpecialActionKeyHandler]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Application engine

(defn base-state
  "An empty key-state value. Used for initializing the application
  engine."
  [behaviors]
  {:behaviors behaviors
   :handler nil
   :positions {}})

(defn is-down?
  "Return true if the specified key is in the down position."
  [state key]
  (= :dn (get-in state [:positions key])))

(defprotocol HandlerMatch
  (match? [matcher key]
    "Return true if this handler matches this key."))

(extend-protocol HandlerMatch
  clojure.lang.Keyword
  (match? [k key] (= k key))

  clojure.lang.IFn
  (match? [f key] (f key)))

(defn handler-match
  "Given a key and a handler matcher-specifier pair, return the pair if
  the matcher matches the key, and nil otherwise."
  [key [matcher specifier]]
  (when (match? matcher key)
    [matcher specifier]))

(defn handler-specifier
  "Given behaviors and a key, return the first handler specifier that
  matches, or nil if no matches are found."
  [behaviors key]
  (->> behaviors
       (partition 2)
       (filter (partial handler-match key))
       first
       second))

(defn handler
  "Return a new handler for the specified key."
  [state key]
  (let [[make-handler & params]
        (or (handler-specifier (:behaviors state) key) [->DefaultKeyHandler])]
    (apply make-handler key params)))

(defn all-up?
  "Return true if no keys are currently in the down state."
  [state]
  (not (some (fn [[k v]] (= v :dn)) (:positions state))))

(defn maybe-add-handler
  "Return a state value that has been updated to include any necessary
  new key handler."
  [state keyevent]
  ;; If there's already a handler, don't do anything.
  (if (:handler state)
    state
    ;; Is this a key down event? If not, return the state unchanged. If
    ;; so, is the key already down? If not, create a handler and add it
    ;; to the end of the chain. If so, return the state unchanged.
    ;; Also, don't add a handler unless all the other keys are up.
    (let [{:keys [key direction]} keyevent]
      (if (and (= direction :dn) (all-up? state))
        (assoc state :handler (handler state key))
        state))))

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
        _ (log/debug (:handler state))
        _ (log/debug keyevent)
        ;; Important! Don't update the positions until after we add
        ;; the new handler, since whether or not we add one might
        ;; depend on whether a key is already down.
        state (maybe-add-handler state keyevent)
        state (update-key-positions state keyevent)
        ;; Walk the handler chain, dealing with the results at each step
        handler (:handler state)
        results (process handler keyevent)]
    (log/debug results)
    (log/debug "----------")
    (-> state
        (assoc :handler (:handler results))
        (update-in [:effects] concat (:effects results)))))

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

(defmethod enact :cycle-log
  [_ state _]
  (case (log/log-level)
    :debug
    (do
      (log/set-log-level! :info)
      (println "Log level set to INFO"))

    :info
    (do
      (log/set-log-level! :error)
      (println "Log level set to ERROR"))

    :error
    (do
      (log/set-log-level! :debug)
      (println "Log level set to DEBUG")))
  state)

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