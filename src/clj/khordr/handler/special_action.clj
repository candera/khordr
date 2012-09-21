(ns khordr.handler.special-action
  "Implements the special action handler, which takes care of built-in
  actions, such as qutting and suspending the application."
  (:require [khordr.handler :as h]))

;; The first key event we get is the thing that triggered us
(defrecord Handler []
  h/IKeyHandler
  (process [this keyevent]
    {:handler (Initialized. (:key keyevent))}))

(defrecord InitializedHandler [trigger]
  h/IKeyHandler
  (process [this keyevent]
    (let [{:keys [key direction]} keyevent]
      (cond
       (and (= key trigger) (= direction :up))
       {:handler nil
        :effects [{:effect :key
                   :event (assoc keyevent :key trigger :direction :dn)}
                  {:effect :key
                   :event (assoc keyevent :key trigger :direction :up)}]}

       (and (= key :q) (= direction :dn))
       {:handler this
        :effects [{:effect :quit}]}

       :else
       {:handler this}))))
