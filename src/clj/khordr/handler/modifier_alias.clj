(ns khordr.handler.modifier-alias
  "Implements the modifier alias key handler, which allows the typist
  to use alternate keys as modifiers (e.g. j for shift, k for control,
  l for alt, etc.)."
  (:require [khordr.handler :as h]))

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

;; Handler: we start here. The very next thing to happen should be
;; that we get the key down that activated this handler, which pushes
;; us into Armed.
(defrecord Handler [aliases])

;; Armed: the first state of the handler after the initial
;; keypress. Means that we're ready to alias if necessary, but
;; we can't tell yet if the user is just typing a regular key
;; or really does want to try to alias a modifier.
(defrecord Armed [trigger aliases])

;; Multi-armed: The user has simultaneously pressed more than
;; one modifier alias. We're not sure what to do yet, since
;; they might release them all or go on to press a regular key
;; they want to modify.
(defrecord MultiArmed [down-modifiers pending-keys aliases])

;; Deciding: The user might be trying to alias: we'll know for
;; sure if the next thing that happens is a regular key down.
;; Otherwise, it was probably just multiple accidental
;; simultaneous key presses (called "rollover").
(defrecord Deciding [down-modifiers pending-keys aliases])

;; Aliasing: we're translating normal keys into modifier keys
(defrecord Aliasing [down-modifiers pending-keys aliases])

(extend-protocol h/KeyHandler

  Handler
  (process [{:keys [aliases] :as this} state keyevent]
    ;; If there are already keys down, we don't want to do any special
    ;; processing: It's a rollover situation
    (if-not (empty? (:down-keys state))
      {:handler nil
       :effect :key :event keyevent}
      (let [{:keys [key direction]} keyevent
            modifier?               (contains? aliases key)
            up?                     (= direction :up)
            down?                   (not up?)]

        (if (and modifier? down?)
          {:handler (Armed. key aliases)}
          (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was in the inital state: " keyevent)
                          {:keyevent keyevent
                           :reason :weird-state
                           :source this}))))))

  Armed
  (process [{:keys [trigger aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)]
      (cond
       (and modifier? down?)
       (if (= key trigger)
         {:handler this}                ; It's a repeat
         {:handler (MultiArmed. [trigger key] [] aliases)})

       (and modifier? up?)
       {:handler nil
        :effects [(key-effect keyevent key :dn)
                  (key-effect keyevent key :up)]}

       (and (not modifier?) down?)
       {:handler (Deciding. [trigger] [key] aliases)}

       :else
       (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was armed: " keyevent)
                       {:keyevent keyevent
                        :reason :weird-state
                        :source this})))))

  MultiArmed
  (process [{:keys [down-modifiers pending-keys aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)]
      (cond
       (and modifier? down?)
       {:handler (MultiArmed.
                  (conj-if-missing down-modifiers key)
                  pending-keys
                  aliases)}

       (and modifier? up?)
       (let [new-down-modifiers (filterv #(not= modifier? %)
                                         down-modifiers)]
         (if (seq new-down-modifiers)
           {:handler (MultiArmed.
                      new-down-modifiers
                      pending-keys
                      aliases)}
           {:handler nil}))

       (and (not modifier?) down?)
       {:handler (Aliasing. down-modifiers [] aliases)
        :effects (conj
                  (mapv #(key-effect keyevent % :dn)
                        (map aliases down-modifiers))
                  (key-effect keyevent key :dn))}

       :else
       (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was multi-armed: " keyevent)
                       {:keyevent keyevent
                        :reason :weird-state
                        :source this})))))

  Deciding
  (process [{:keys [down-modifiers pending-keys aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)]
      (cond
       (and (not modifier?) down?)
       {:handler (Deciding. down-modifiers (conj pending-keys key) aliases)}

       (and (not modifier?) up?)
       {:handler (Aliasing. down-modifiers [] aliases)
        :effects (concat (map #(key-effect keyevent % :dn) (map aliases down-modifiers))
                         (map #(key-effect keyevent % :dn) pending-keys)
                         [(key-effect keyevent key :up)])}

       modifier?
       {:handler nil
        :effects (concat (map #(key-effect keyevent % :dn) down-modifiers)
                         (map #(key-effect keyevent % :dn) pending-keys)
                         [(key-effect keyevent key direction)])})))

  Aliasing
  (process [{:keys [down-modifiers pending-keys aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)
          new-down-modifiers      (cond
                                   (and modifier? down?)
                                   (conj-if-missing down-modifiers key)

                                   (and modifier? up?)
                                   (filterv #(not= key %) down-modifiers)

                                   :else
                                   down-modifiers)]
      {:handler (when (seq new-down-modifiers)
                  (Aliasing. new-down-modifiers nil aliases))
       :effects [(key-effect keyevent (get aliases key key) direction)]})))