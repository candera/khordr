(ns khordr.filter.substituter
  (:require [khordr.filter :as f]))

(defrecord Filter [aliases]
  f/Filter
  (filter-event [this {:keys [state event]}]
    ;; Swap the key for its alias, if it has one
    {:event (assoc event :key (get aliases (:key event) (:key event)))}))