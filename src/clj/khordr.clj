(ns khordr
  (:refer-clojure :exclude [key send])
  (:require [khordr.platform.common :as com]
            [khordr.logging :as log]
            [khordr.handler :as h]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Behaviors: a sequence of pairs, each of which is a selector and a
;; handler specifier. The selector is a function of one argument that
;; will be invoked with a key name (e.g. :a). If the selector returns
;; true, the first element of the specifier is invoked with the result
;; of calling the selector. As a special case, the selector may be a
;; keyword, which matches only itself.

;; TODO: the DSL here needs to be more fleshed-out once we use it as a
;; serialization format.

(defn map-selector
  "Given a map m, return the whole map if the key k appears in it."
  [m k]
  (when (m k) m))

(let [right-modifiers {:j :rshift
                       :k :rcontrol
                       :l :ralt}
      left-modifiers  {:f :lshift
                       :d :lcontrol
                       :s :lalt}]
  (def ^{:doc "Relates keys to behaviors. Absence from this data structure means it's a regular key."}
    default-key-behaviors
    [#(map-selector right-modifiers %) 'khordr.handler.modifier-alias/Initial
     #(map-selector left-modifiers %) 'khordr.handler.modifier-alias/Initial
     :backtick 'khordr.handler.special-action/SpecialActionKeyHandler]))

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
  (match [matcher key]
    "Return true if this handler matches this key."))

(extend-protocol HandlerMatch
  clojure.lang.Keyword
  (match [k key] (when (= k key) k))

  clojure.lang.IFn
  (match [f key] (f key)))

(defn handler-match
  "Given a key and a handler matcher-specifier pair, return the pair if
  the matcher matches the key, and nil otherwise."
  [key [matcher specifier]]
  (when-let [result (match matcher key)]
    [result specifier]))

(defn handler-specifier
  "Given behaviors and a key, return a two-element vector, [result
  specifier], where result is the result of calling the selector
  function and specifier is the corresponding handler specifier.
  Return nil if no matches are found."
  [behaviors key]
  (->> behaviors
       (partition 2)
       (filter (partial handler-match key))
       first))

(defn make-handler
  "Given the result of a calling a selector function and a handler
  specifier, return an instance of IKeyHandler."
  [specifier selector-result]
  (let [ns (ns-name specifier)]))

(defn handler
  "Return a new handler for the specified key."
  [state key]
  (let [[result specifier] (handler-specifier (:behaviors state) key)]
    (make-handler specifier result)))

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
        results (h/process handler keyevent)]
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