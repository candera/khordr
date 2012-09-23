(ns khordr.handler
  "Defines the interface for khordr key handlers")

(defprotocol KeyHandler
  (process [this state keyevent]
    "Process given the application state and a key event, returning a
  map. The map should contain the following keys:

  :handler - either an implementation of KeyHandler representing the
             updated state of this handler or nil, indicating that the
             handler should be deactivated;

  :effects - a seq of commands legal for consumption by `engine`."))

;; In the absence of any other key handler, just pass events through
(extend-protocol KeyHandler
  nil
  (process [_ _ keyevent]
    {:handler nil
     :effects [{:effect :key :event keyevent}]}))

