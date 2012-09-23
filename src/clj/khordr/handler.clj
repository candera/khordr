(ns khordr.handler
  "Defines the interface for khordr key handlers")

(defprotocol KeyHandler
  (process [this keyevent]
    "Process a key event, returning a map. The map should contain the
  following keys:

  :handler - either an implementation of KeyHandler representing the
             updated state of this handler or nil, indicating that the
             handler should be deactivated;

  :effects - a seq of commands legal for consumption by `engine`."))

;; The default implementation: pass through key events unchanged.
;; yourself when self-key goes up.
(defrecord DefaultKeyHandler [self-key]
  KeyHandler
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
(extend-protocol KeyHandler
  nil
  (process [_ keyevent]
    {:handler nil
     :effects [{:effect :key :event keyevent}]}))

