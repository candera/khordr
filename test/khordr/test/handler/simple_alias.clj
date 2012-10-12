(ns khordr.test.handler.simple-alias
  (:require [clojure.test :refer (deftest)]
            [khordr.handler.simple-alias :refer :all]
            [khordr.test.handler :refer :all]
            [khordr.handler :as h]
            [khordr.effect :as e]))

(def kt (partial keytest (->Handler {:h :left :j :up :k :down :l :right})))

(deftest trigger-down
  (kt [[:a :dn]]
      []))

(deftest deciding
  (kt [[:a :dn] [:h :dn]]
      []))

(deftest aliasing
  (kt [[:a :dn] [:h :dn] [:h :up]]
      [[:left :dn] [:left :up]]))

(deftest aliasing-repeats
  (kt [[:a :dn] [:j :dn] [:j :dn]]
      [[:up :dn]]))

(deftest rollover-single
  (kt [[:a :dn] [:h :dn] [:a :up]]
      [[:a :dn] [:h :dn] [:a :up]]))

(deftest rollover-multiple
  (kt [[:a :dn] [:j :dn] [:k :dn] [:j :up]]
      [[:a :dn] [:j :dn] [:k :dn] [:j :up]]))

(deftest back-to-normal
  (kt [[:a :dn] [:l :dn] [:l :up] [:a :up] [:a :dn] [:a :up]]
      [[:right :dn] [:right :up] [:a :dn] [:a :up]]))

(deftest rollover-non-aliased
  (kt [[:a :dn] [:q :dn]]
      [[:a :dn] [:q :dn]]))

(deftest repeat-of-trigger-stays-armed
  (kt [[:a :dn] [:a :dn]]
      []))

(deftest repeat-trigger-still-aliases
  (kt [[:a :dn] [:a :dn] [:h :dn] [:h :up]]
      [[:left :dn] [:left :up]]))

