(ns khordr.handler.modifier-alias
  "Implements the modifier alias key handler, which allows the typist
  to use alternate keys as modifiers (e.g. j for shift, k for control,
  l for alt, etc.)."
  (:require [khordr.handler :as h])
  (:import khordr.effect.Key))

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
  (Key. (assoc template :key key :direction direction)))

;; Handler: we start here. The very next thing to happen should be
;; that we get the key down that activated this handler, which pushes
;; us into Armed.
(defrecord Handler [aliases params])

;; Armed: the first state of the handler after the initial
;; keypress. Means that we're ready to alias if necessary, but
;; we can't tell yet if the user is just typing a regular key
;; or really does want to try to alias a modifier.
(defrecord Armed [down-modifiers aliases])

;; Deciding: The user might be trying to alias: we'll know for
;; sure if the next thing that happens is a regular key down.
;; Otherwise, it was probably just multiple accidental
;; simultaneous key presses (called "rollover").
(defrecord Deciding [down-modifiers pending-keys aliases])

;; Aliasing: we're translating normal keys into modifier keys
(defrecord Aliasing [down-modifiers aliases])

(extend-protocol h/KeyHandler

  Handler
  (process [{:keys [aliases params] :as this} state keyevent]
    (cond
      ;; If there are already keys down, we don't want to do any special
      ;; processing: It's a rollover situation
      (seq (:down-keys state))
      {:handler nil
       :effects [(Key. keyevent)]}

      ;; If the last keypress was more recent than the typethrough
      ;; threshold, it means that someone is trying to fly along, just
      ;; typing regular words, and we probably shouldn't try to
      ;; interpret this as an attempt to alias.
      (< (or (:time-since-last-keyevent state) Long/MAX_VALUE)
         (or (:typethrough-threshold params) -1))
      {:handler nil
       :effects [(Key. keyevent)]}
      :default
      (let [{:keys [key direction]} keyevent
            modifier?               (contains? aliases key)
            up?                     (= direction :up)
            down?                   (not up?)]

        (if (and modifier? down?)
          {:handler (Armed. [key] aliases)}
          (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was in the inital state: " keyevent)
                          {:keyevent keyevent
                           :reason :weird-state
                           :source this}))))))

  Armed
  (process [{:keys [down-modifiers aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)
          repeat?                 (= [(last down-modifiers) :dn]
                                     [key direction])]
      (cond
        repeat?
        {:handler (Aliasing. down-modifiers (select-keys aliases down-modifiers))
         :effects (mapv #(key-effect keyevent (aliases %) :dn) down-modifiers)}

        (and modifier? down?)
        {:handler (Armed. (conj down-modifiers key) aliases)}

        (and modifier? up?)
        (if (= key (last down-modifiers))
          ;; Someone used a modifier to modify a modifier
          (let [in-use-modifiers (butlast down-modifiers)]
            {:handler (Aliasing. in-use-modifiers (select-keys aliases in-use-modifiers))
             :effects (into (mapv #(key-effect keyevent (aliases %) :dn) in-use-modifiers)
                            (mapv #(key-effect keyevent key %) [:dn :up]))})
          ;; Not modifying: just rollover
          {:handler nil
           :effects (conj (mapv #(key-effect keyevent % :dn) down-modifiers)
                          (key-effect keyevent key :up))})

        (and (not modifier?) down?)
        {:handler (Deciding. down-modifiers [key] (select-keys aliases down-modifiers))}

        :else
        (throw (ex-info (str "Unexpected key event while ModifierAliasKeyHandler was armed: " keyevent)
                        {:keyevent keyevent
                         :reason :weird-state
                         :source this})))))

  Deciding
  (process [{:keys [down-modifiers pending-keys aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)
          repeat?                 (and down? (= (last pending-keys) key))]
      (cond
        repeat?
        {:handler (Aliasing. down-modifiers aliases)
         :effects (into (mapv #(key-effect keyevent % :dn) (map aliases down-modifiers))
                        (repeat 2 (key-effect keyevent (last pending-keys) :dn)))}

        (and (not modifier?) down?)
        {:handler (Deciding. down-modifiers (conj pending-keys key) aliases)}

        (and (not modifier?) up?)
        {:handler (Aliasing. down-modifiers (select-keys aliases down-modifiers))
         :effects (concat (map #(key-effect keyevent % :dn) (map aliases down-modifiers))
                          (map #(key-effect keyevent % :dn) pending-keys)
                          [(key-effect keyevent key :up)])}

        modifier?
        {:handler nil
         :effects (concat (map #(key-effect keyevent % :dn) down-modifiers)
                          (map #(key-effect keyevent % :dn) pending-keys)
                          [(key-effect keyevent key direction)])})))

  Aliasing
  (process [{:keys [down-modifiers aliases] :as this} state keyevent]
    (let [{:keys [key direction]} keyevent
          modifier?               (contains? aliases key)
          up?                     (= direction :up)
          down?                   (not up?)
          ;; TODO: Should these be modifiers, or interpreted as
          ;; regular keys that are themselves modified? Because we
          ;; interpret it more or less in the latter manner in the
          ;; MultiArmed state.
          new-down-modifiers      (cond
                                    (and modifier? down?)
                                    (conj-if-missing down-modifiers key)

                                    (and modifier? up?)
                                    (filterv #(not= key %) down-modifiers)

                                    :else
                                    down-modifiers)]
      {:handler (when (seq new-down-modifiers)
                  (Aliasing. new-down-modifiers
                             (select-keys aliases new-down-modifiers)))
       :effects [(key-effect keyevent (get aliases key key) direction)]})))
