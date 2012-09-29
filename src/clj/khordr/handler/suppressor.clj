(ns khordr.handler.suppressor
  "Suppresses one or more keys completely, turning them off like they
  didn't exist."
  (:require [khordr.handler :as h]))

(defrecord Handler []
  h/KeyHandler
  (process [this state keyevent]
    {:handler nil}))