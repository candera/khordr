(ns kchordr.test.core
  (:use [kchordr.core])
  (:use [clojure.test]))

(defn- sent
  "Given a sequence of key events, return the sequence of keys that
  will actually be sent."
  [events]
  (:to-send (reduce #(process %1 %2) (state default-key-behaviors) events)))

(deftest key-tests
  (are [pressed anticipated]
       ;; We use vectors as the test format because they're easier to
       ;; read, but we still want to use maps as the underlying
       ;; construct for their flexibility. Yay juxt!
       (= anticipated (map (juxt :key :direction) (sent (map ->event pressed))))
       ;; Single regular key press
       [[:b :dn]]
       [[:b :dn]]

       ;; Single regular key press and release
       [[:b :dn] [:b :up]]
       [[:b :dn] [:b :up]]

       ;; Modifier alias press only
       [[:j :dn]]
       []

       ;; Modifier alias press and release
       [[:j :dn] [:j :up]]
       [[:j :dn] [:j :up]]

       ;; Modifier alias repeat and release
       [[:j :dn] [:j :dn] [:j :dn] [:j :dn] [:j :up]]
       [[:j :dn] [:j :up]]

       ;; Modifier alias with regular key press
       [[:j :dn] [:x :dn]]
       [[:rshift :dn] [:x :dn]]

       ;; Modifier alias with regular key press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up]]
       [[:rshift :dn] [:x :dn] [:x :up] [:rshift :up]]

       ;; Modifier alias with regular key press and release followed
       ;; by modifier alias press and release
       [[:j :dn] [:x :dn] [:x :up] [:j :up] [:j :dn] [:j :up]]
       [[:rshift :dn] [:x :dn] [:x :up] [:rshift :up] [:j :dn] [:j :up]]
       ))

