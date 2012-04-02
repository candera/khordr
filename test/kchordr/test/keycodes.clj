(ns kchordr.test.keycodes
  (:use kchordr.keycodes
        clojure.test))

(deftest keycode-tests
  (are [original expected-translation]
       (= expected-translation (translate original))

       30 :a))