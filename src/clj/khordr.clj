(ns khordr
  (:refer-clojure :exclude [key send])
  (:require [khordr.platform.common :as com]
            [khordr.logging :as log]
            [khordr.handler :as h]
            [khordr.effect :as e]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Behaviors
;;
;; A behavior is a map with keys `:match` and `:handler`. The :match
;; dictates how the handler is selected. If it is a map, it can have
;; the keys `:key` and/or `:direction` which must match the keyevent
;; map that is passed in. If the value for :key or :direction is a
;; set, any value from the set matches.
;;
;; The :handler must be a symbol that specifies a namespace-qualified
;; Clojure record or a package-qualified Java class (support for Java
;; classes pending). If present, the optional `:args` entry of the
;; behavior map specifies a vector of arguments to the class or record
;; constructor.

(defn map-selector
  "Given a map m, return the whole map if the key k appears in it."
  [m k]
  (when (m k) m))

(def ^{:doc "Relates keys to behaviors. Absence from this data structure means it's a regular key."}
  default-key-behaviors
  '[{:match {:key #{:j :k :l}}
     :handler khordr.handler.modifier-alias/Handler
     :args [{:j :rshift :k :rcontrol :l :ralt}]}
    {:match {:key #{:f :d :s}}
     :handler khordr.handler.modifier-alias/Handler
     :args [{:f :lshift :d :lcontrol :s :lalt}]}
    {:match {:key :backtick}
     :handler khordr.handler.special-action/Handler}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Application engine

(defn base-state
  "An empty key-state value. Used for initializing the application
  engine."
  [behaviors]
  {:behaviors behaviors
   :handler nil
   :down-keys #{}})

;; Defines how a matcher like {:key #{:a :b} :direction :up} matches a
;; keyevent like {:key :a :direction :dn :device 2}
(defprotocol Matcher
  (match? [pattern keyevent]
    "Return true if this pattern matches this keyevent."))

;; Defines how an element of a matcher matches the corresponding
;; value. E.g. how #{:a b} matches :a
(defprotocol ElementMatcher
  (element-match? [pattern value]
    "Return true if this pattern matches this value."))

;; Defines how a handler specifier turns into an instance of
;; KeyHandler
(defprotocol HandlerFactory
  (make-handler [specifier args]))

(defn record-constructor
  "Given a namespaced symbol, return the constructor function for the
  record it names."
  [s]
  (let [ns-name   (namespace s)
        type-name (name s)
        ns-symbol (symbol ns-name)
        _         (require (symbol ns-name))
        ctor-name (symbol (str "->" type-name))]
    (ns-resolve (the-ns ns-symbol) ctor-name)))

(extend-protocol HandlerFactory
  ;; If namespaced, names a Clojure record type. If not, names a
  ;; package-qualified Java class (not currently supported)
  clojure.lang.Symbol
  (make-handler [specifier args]
    (apply (record-constructor specifier) args)))

(extend-protocol Matcher
  clojure.lang.PersistentArrayMap
  (match? [pattern keyevent]
    (and (element-match? (:key pattern) (:key keyevent))
         (element-match? (:direction pattern) (:direction keyevent)))))

(extend-protocol ElementMatcher
  nil
  (element-match? [pattern value] true)

  clojure.lang.Keyword
  (element-match? [pattern value] (= pattern value))

  clojure.lang.PersistentHashSet
  (element-match? [pattern value] (pattern value)))

;; TODO: Looking up the handler every time may be sort of slow. I
;; suspect that, if optimization is needed, some memoization in this
;; code path will be helpful.
(defn handler-match
  "Return an instance of the handler specified by `behavior` if it
  matches `keyevent`"
  [behavior keyevent]
  (when (match? (:match behavior) keyevent)
    (make-handler (:handler behavior) (:args behavior))))

(defn handler
  "Using `behaviors` return a handler that matches `keyevent` or nil if
  none match."
  [behaviors keyevent]
  (some #(handler-match % keyevent) behaviors))

(defn maybe-add-handler
  "Return a state value that has been updated to include any necessary
  new key handler."
  [state keyevent]
  ;; If there's already a handler, don't do anything.
  (if (:handler state)
    state
    (assoc state :handler (handler (:behaviors state) keyevent))))

(defn update-key-positions
  "Return a state that has been updated to reflect which keys are up
  and which are down."
  [state keyevent]
  (let [{:keys [key direction]} keyevent]
   (update-in state [:down-keys] (if (= direction :up) disj conj) key)))

(defn handle-keys
  "Given the current state and a key event, return an updated state."
  [state keyevent]
  ;; It turns out to be important to preserve other keys in the
  ;; keyevent, because they contain platform-specific stuff like the
  ;; keyboard the event arrived on.
  (let [{:keys [key direction]} keyevent
        _ (log/debug {:type :keyevent :data keyevent})
        state (maybe-add-handler state keyevent)
        _ (log/debug {:type :handler-updated :data (dissoc state :behaviors)})
        handler (:handler state)
        results (h/process handler state keyevent)
        _ (log/debug {:type :handler-results :data results})
        ;; Important! Don't update the positions until after we
        ;; process the key, since the new handler might care whether
        ;; any keys are down other than the one that triggered the new
        ;; handler.
        state (update-key-positions state keyevent)]
    (log/debug "----------")
    (-> state
        (assoc :handler (:handler results))
        (update-in [:effects] concat (:effects results)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Effects handling
;;

(defn enact-effects!
  "Given the state, enact any pending effects and return a new state."
  [state platform]
  (loop [state state
         [effect & more] (:effects state)]
    (if effect
      (let [state (e/enact effect state platform)]
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