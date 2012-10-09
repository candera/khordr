(ns khordr.effect
  (:require [khordr.logging :as log]
            [khordr.platform.common :as com]))

(defprotocol Effect
  (enact [effect state platform]
    "Enact the effect: do whatever weird side-effecting crap is
    necessary. Return a new state. The effect entry will be removed
    from the state automatically."))

(defrecord Key [keyevent]
  Effect
  (enact [effect state platform]
    (com/send-key platform keyevent)
    state))

(defrecord Quit []
  Effect
  (enact [_ state _]
    (assoc state :done true)))

(defrecord CycleLog []
  Effect
  (enact [_ state _]
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
    state))

;; Toggle all the behaviors that don't have `:notoggle true` marked
(defrecord ToggleBehaviors []
  Effect
  (enact [_ state _]
    (let [stashed-behaviors (:stashed-behaviors state)
          behaviors (:behaviors state)
          state (if stashed-behaviors
                  (-> state
                      (assoc :behaviors stashed-behaviors)
                      (dissoc :stashed-behaviors))
                  (-> state
                      (update-in [:behaviors] #(filterv :notoggle %))
                      (assoc :stashed-behaviors behaviors)))]
      (log/info (if stashed-behaviors
                  "Behaviors resumed"
                  "Behaviors suspended"))
      (log/debug {:type :toggle-behaviors
                  :old-behaviors behaviors
                  :old-stashed-behaviors stashed-behaviors
                  :new-behaviors (:behaviors state)
                  :new-stashed-behaviors (:stashed-behaviors state)
                  :state state})
      state)))

(defn enact-effects!
  "Given the state, enact any pending effects and return a new state."
  [state platform]
  (loop [state state
         [effect & more] (:effects state)]
    (if effect
      (let [state (enact effect state platform)]
        (recur state more))
      (dissoc state :effects))))
