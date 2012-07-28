(ns khordr.analysis
  "A namespace for analyzing key events."
  (:require [khordr.platform.common :as com]
            [khordr.platform :as p]
            [clojure.java.io :as io]))

(defonce events (atom []))

(defn save!
  "Save the contents of the events atom to path."
  [path]
  (spit path @events))

(defn load!
  "Append the data stored in path to the contents of the events vector."
  [path]
  (swap! events
         into
         (read (java.io.PushbackReader. (io/reader path))))
  ;; Don't return anything, so the REPL doesn't try to print a
  ;; gigantic vector
  nil 
  )

(defn collect
  "Start a loop that will collect all the key events into the @events
  atom. Quit the collection by holding down backtick and typing q.
  Temporarily pause and resume it by holding down backtick and typing
  p."
  []
  (let [platform (p/initialize)]
   (try
     (loop [down-keys #{}
            suspended false]
       (let [{:keys [key direction] :as event} (com/await-key-event platform)]
         (com/send-key platform event)
         (when (not suspended)
           (swap! events conj {:key key :direction direction}))
         (let [dk (if (= direction :dn)
                    (conj down-keys key)
                    (disj down-keys key))]
           (when (= #{:backtick :s} dk)
             (println "Still working!"))
           (if (= #{:backtick :q} dk)
             ;; Since we're not gathering more data, we need to add the
             ;; up events that we won't see.
             (do (swap! events conj {:key :q :direction :up})
                 (swap! events conj {:key :backtick :direction :up})
                 ;; We don't want to return the value of events, since
                 ;; that causes SLIME to get very slow
                 nil)
             (recur dk (if (= #{:backtick :p} dk)
                         (do
                           (println "Suspended: " (not suspended))
                           (not suspended))
                         suspended))))))
     (finally
      (println "Done!")
      (com/cleanup platform)))))


(defn contains-fn
  "Return a predicate that returns true if called on a collection containing the specified element"
  [e]
  (fn [coll]
    (some #(= e %) coll)))

(defn key-downs
  "Given a seq of key events, return the sequence representing what
  keys are down following each event."
  [events]
  (reductions (fn [acc {:keys [key direction]}]
                (if (= :dn direction)
                  (conj acc key)
                  (filterv #(not= key %) acc)))
              []
              events))

(defn something-after-space?
  "Returns true if the provided seq contains :space and something after it."
  [s]
  (let [[before after] (split-with #(not= :space %) s)]
    (< 1 (count after))))

(defn multiple-after-space?
  "Returns true if more than one key in the seq appears after :space"
  [s]
  (let [[before after] (split-with #(not= :space %) s)]
    (< 2 (count after))))

(defn only-space?
  "Return true if the seq consists solely of spaces"
  [s]
  (and (seq s)
       (every? #(= :space %) s)))

(defn take-until
  "Returns a lazy sequence of successive items from coll until (pred
  item) returns true."
  [pred coll]
  (let [[head tail] (split-with (complement pred) coll)]
    (concat head (take 1 tail))))

(defn chords
  "Partition the provided sequence into subsequences, where each
  subsequence starts with a down key event and ends with the
  corresponding up event."
  [events]
  (loop [evts events
         chrds []]
    (if (seq evts)
      (let [[{:keys [key direction]}] evts]
        (if (= direction :dn)
          (let [chord (take-until #(and (= key (:key %))
                                        (= :up (:direction %)))
                                  evts)]
            (recur (drop (max 1 (count chord)) evts)
                   (conj chrds chord)))
          (recur (rest evts) chrds)))
      chrds)))

(defn true-2chord?
  "Return true if the provided sequence matches the pattern, a-down,
  b-down, b-up, a-up"
  [[a b c d & rest]]
  (and a b c d (empty? rest)
       (= (:key a) (:key d))
       (= (:key b) (:key c))
       (= :dn (:direction a) (:direction b))
       (= :up (:direction c) (:direction d))))

(comment (true-2chord? [{:key :a :direction :dn}
                        {:key :b :direction :dn}
                        {:key :b :direction :up}
                        {:key :a :direction :up}]))

(defn true-3chord?
  "Return true if the provided sequence matches the pattern a-down,
  b-down, c-down, c-up, b-up, a-up, or the pattern a-down, b-down,
  c-down, b-up, c-up, a-up."
  [[a b c d e f & rest]]
  (and a b c d e f (empty? rest)
       (= (:key a) (:key f))
       (or (and (= (:key b) (:key e))
                (= (:key c) (:key d)))
           (and (= (:key b) (:key d))
                (= (:key c) (:key e))))
       (= [:dn :dn :dn :up :up :up] (map :direction [a b c d e f]))))

(comment (true-3chord? [{:key :a :direction :dn}
                        {:key :b :direction :dn}
                        {:key :c :direction :dn}
                        {:key :c :direction :up}
                        {:key :b :direction :up}
                        {:key :a :direction :up}
                                        ;{:key :d :direction :dn}
                        ]))

(defn modifier?
  "Return true if the key event is for a modifier key like shift."
  [{:keys [key]}]
  (#{:lshift :rshift :lalt :ralt :lcontrol :rcontrol :backtick :capslock
     :lwindows :rwindows} key))

(defn homerow?
  "Return true if the key event is for a home-row key."
  [{:keys [key]}]
  (#{:a :s :d :f :g :h :j :k :l :semicolon} key))

