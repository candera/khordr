(ns khordr.test.handler.modifier-alias
  (:use clojure.test
        khordr.handler.modifier-alias)
  (:require [khordr.handler :as h]))

(deftest stupid-multi-armed-bug
  (let [aliases{:f :lshift :d :lcontrol :s :lalt}
        handler (->MultiArmed [:d :s] [] aliases)]
    (is (= (->MultiArmed [:s] [] aliases)
           (:handler (h/process handler {} {:key :d :direction :up}))))))