(ns khordr.handler.special-action
  "Implements the special action handler, which takes care of built-in
  actions, such as qutting and suspending the application."
  (:require [khordr.handler :as h])
  (:import [khordr.effect Key Quit CycleLog]))

(defrecord InitializedHandler [trigger]
  h/KeyHandler
  (process [this state keyevent]
    (let [{:keys [key direction]} keyevent]
      (cond
       (and (= key trigger) (= direction :up))
       {:handler nil
        :effects [(Key. (assoc keyevent :key trigger :direction :dn))
                  (Key. (assoc keyevent :key trigger :direction :up))]}

       (and (= key :q) (= direction :dn))
       {:handler this
        :effects [(Quit.)]}

       (and (= key :l) (= direction :dn))
       {:handler this
        :effects [(CycleLog.)]}

       :else
       {:handler this}))))

;; The first key event we get is the thing that triggered us
(defrecord Handler []
  h/KeyHandler
  (process [this state keyevent]
    {:handler (InitializedHandler. (:key keyevent))}))

