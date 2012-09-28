;; IntPtr context;

;; int device;
;; Interception.Stroke stroke = new Interception.Stroke();

;; context = Interception.CreateContext();
(System/setProperty "jna.library.path" "ext")
(import 'interception.InterceptionLibrary)
(require '[khordr.platform.windows :as win])
(let [ctx (.interception_create_context InterceptionLibrary/INSTANCE)]
  (println "ctx" ctx)
  (try
    (.interception_set_filter
     InterceptionLibrary/INSTANCE
     ctx
     (reify interception.InterceptionLibrary$InterceptionPredicate
       (apply [_ device]
         (.interception_is_keyboard InterceptionLibrary/INSTANCE device)))
     (short -1))

    (println "filter set")

    (loop [n 20]
      (when (pos? n)
        (let [device (.interception_wait InterceptionLibrary/INSTANCE ctx)
              _ (println "device" device)
              stroke (interception.InterceptionKeyStroke$ByReference.)
              _ (println "stroke" stroke)
              received (.interception_receive
                        InterceptionLibrary/INSTANCE
                        ctx
                        device
                        stroke
                        1)
              _ (println "received" received)]

          (when (< 0 received)
            (println "received:" received (.code stroke) (.state stroke))
            ;; (when (= 0x15 (.code stroke))
            ;;   (set! (.code stroke) 0x2d))
            (println "sending" stroke "on device" device)
            (.interception_send InterceptionLibrary/INSTANCE
                                ctx
                                device
                                stroke
                                1)
            ;; If it's a y, send an additional x
            (when (= 0x15 (.code stroke))
              (let [direction (if (bit-test (.state stroke) 0) :up :dn)
                    new-stroke (win/event->stroke {:key :x :direction direction :device 2})]
                (println "sending extra x")
                (.interception_send InterceptionLibrary/INSTANCE ctx device new-stroke 1)))
            ;;  // Hitting escape terminates the program
            ;;  if (stroke.key.code == ScanCode.Escape)
            (if (= 0x01 (.code stroke))
              (println "quitting")
              (recur (dec n))
              )))))
    (finally
     (.interception_destroy_context InterceptionLibrary/INSTANCE ctx)
     (println "done"))))


;;  {
;;   break;
;;   }
;;  }

;; Interception.DestroyContext(context);

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(System/setProperty "jna.library.path" "ext")

(interception.TestLibrary/INSTANCE)
(import interception.TestLibrary)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require :reload 'kchordr.core)
(in-ns 'kchordr.core)

(process (state default-key-behaviors) (->event :q :dn))

(def jdn (process (state default-key-behaviors) (->event :j :dn)))

(handle-deciding-regular-press jdn :q)

(process jdn (->event :j :up))
(process jdn (->event :q :dn))
jdn

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(map #(apply ->event %) [[:b :dn] [:b :up]])

(->event :b :dn)

(update-in {:to-send []} [:to-send] append (->event :x :up))

(append [(->event :x :up)] (->event :x :dn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

evt: {:key :j :direction :dn}

state: 
{:to-send [evt evt evt]
 :keystate {:j :undecided, :k :right-control}}

process: state, evt -> state

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(undecided-modifier-downs jdn)

(get-in jdn [:behaviors :q])
(modifier-alias? nil)

(undecided-modifier? (:keystate jdn))

(regular-key? jdn :j)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(require :reload 'kchordr.core)
(decide-modifiers jdn)
(clojure.repl/doc decide-modifier)

(decide-modifier jdn [:j :undecided])

(reduce println jdn (:keystate state))

(concat (:to-send jdn) (undecided-modifier-downs jdn) [(->event :q :dn)])

(clojure.pprint/pprint  (process jdn (->event :q :dn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Working! The only weirdness is around printscreen and pause, which
;; both generate multiple keypresses. Since I don't care about those
;; at the moment, I'm not going to bother with them.
(defn handle-event [event]
  (let [{:keys [key direction]} event]
    (println "received" key direction)
    (not (or (= key 1) (= key :esc)))))

(System/setProperty "jna.library.path" "ext")
(require :reload-all 'kchordr.core)
(kchordr.core/intercept #'handle-event)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(handle-keys (base-state {}) {:key :a :direction :dn})

(def state (base-state {}))
(def state (update-key-positions state :a :dn))
(def state (maybe-add-handler (base-state {}) :a :dn))
(process (first (:handlers state)) :a :dn)
(def results (map #(process % :a :dn) (:handlers state)))
(def results (take-while :continue results))
(filter identity (map :handler results))
(mapcat :effects results)
state

handle-keys

(let [_ (println "--------------------")
      state (base-state {})
      event {:key :a :direction :dn}
      {:keys [key direction]} event
      _ (println "With update-key-positions: " state)
      state (maybe-add-handler state key direction)
      _ (println "Key direction: " key direction)
      state (update-key-positions state key direction)
      _ (println "With maybe-add-handler state:" state)
      ;; Walk the handler chain, dealing with the results at each step
      results (map #(process % key direction) (:handlers state))
      results (take-while :continue results)]
  (assoc state
    :handlers (filter identity (map :handler results))
    :effects (mapcat :effects results)))

(maybe-add-handler {:handlers [] :positions {:a :dn}} :a :dn)

(is-down? (base-state {}) :a)

(update-in {:handlers [] :positions {:a :dn}} [:handlers] concat [:foo])

(class (update-key-positions (base-state {}) :a :dn))
(class (base-state {}))

default-key-behaviors

(base-state default-key-behaviors)

(map (juxt :key :direction)) (:effects (handle-keys (base-state default-key-behaviors) {:key :b :direction :dn}))

;; [[:j :dn] [:x :dn] [:x :up] [:j :up]]

(-> (base-state default-key-behaviors)
    (handle-keys (->event :j :dn))
    (handle-keys (->event :x :dn))
    (handle-keys (->event :x :up))
    #_(handle-keys (->event :j :up)))

(def state (-> (base-state default-key-behaviors)
               (handle-keys (->event :j :dn))))

state
(def h (first (:handlers state)))
h
(def results (map #(process % :j :up) [h]))
results
(satisfies? IKeyHandler h)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(use 'khordr)
(use 'clojure.repl)
(use 'clojure.pprint)

(-> (base-state default-key-behaviors)
    (handle-keys {:key :backtick :direction :dn})
    :handlers
    first)

(-> (base-state default-key-behaviors)
    (handle-keys {:key :backtick :direction :dn})
    :handlers
    first
    (process {:key :q :direction :dn}))

(-> (base-state default-key-behaviors)
    (handle-keys {:key :j :direction :dn})
    :effects)

(-> (khordr.SpecialActionKeyHandler. :backtick)
    (process {:key :q :direction :dn}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(-> (base-state default-key-behaviors)
    (handle-keys {:key :j :direction :dn})
    :handlers
    first
    (process {:key :x :direction :dn}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require :reload '[khordr.platform.common :as com])
(require :reload '[khordr.platform :as p])
(require :reload '[khordr.platform.windows :as win])

(let [p (win/initialize)]
  (loop [limit 50]
    (when (pos? limit)
      (let [evt (com/await-key-event p)]
        (println "received" evt)
        (when (not= :esc (:key evt))
          (com/send-key p (assoc evt :key (if (= :x (:key evt)) :y (:key evt))))
          (recur (dec limit))))))
  (com/cleanup p)
  (println "done!"))

(require :reload '[khordr.platform.common :as com])
(require :reload '[khordr.platform :as p])
(require :reload '[khordr.platform.windows :as win])

(let [p (p/initialize)
      evt (com/await-key-event p)
      ]
  (println "operating on platform " p)
  (println "received" evt)
  (com/send-key p evt)
  (com/cleanup p)
  (println)
  (println "after cleanup, event is" evt)
  evt
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Print out what keys are up and what keys are down

(require :reload '[khordr.platform.common :as com])
(require :reload '[khordr.platform :as p])
(require '[clojure.string :as str])

(defn char+ [c n]
  (str (char (+ n (int c)))))

(def letters (map #(char+ \a %) (range 0 26)))
(def numbers (map #(char+ \0 %) (range 0 10)))
(def alphanumerics (concat letters numbers))

(def keymap
  (merge {:space "_"
          :lshift "#"
          :rshift "#"
          :lcontrol "^"
          :rcontrol "^"
          :capslock "$"                 ; Control on my system
          :lalt "@"
          :ralt "@"
          :quote "'"
          :semicolon ";"
          :enter "!"}
         (zipmap (map keyword alphanumerics) alphanumerics)))

;; This one shows a sort of moving graph of what keys are down
(let [platform (p/initialize)]
  (try
    (loop [key-str ""]
      (let [{:keys [key direction] :as event} (com/await-key-event platform)]
        ;;(println key direction)
        (when-not (= :esc key)
          (com/send-key platform event)
          (let [k (get keymap key)
                new-key-str (if-not k
                          key-str
                          (if (= direction :dn)
                            (if (.contains key-str k)
                              key-str
                              (.concat key-str k))
                            (.replace key-str k " ")))]
            (when-not (= key-str new-key-str)
              (println new-key-str)
              (flush))
            (recur (if (str/blank? new-key-str) "" new-key-str))))))
    (finally
     (println "Done!")
     (com/cleanup platform))))

;; This one collects keys for later analysis
(require '[clojure.pprint :as pp])


;;; Analysis
(use 'khordr.analysis)

(count @events)

;; Frequency of key pressed
(let [down-events (filter #(= :dn (:direction %)) @events)
      n (count down-events)]
  (->> down-events
       (reduce (fn [acc evt]
             (update-in acc [(:key evt)] (fnil inc 0)))
               {})
       (map (fn [[k c]] [k (float (/ c n))]))
       (sort-by second)
       reverse
       (take 10)
       pp/pprint))

;; Simultaneous key-downs

(->> (key-downs @events)
     (filter #(< 2 (count %)))
     (filter (contains-fn :space))
     (filter #(not= :space (last %)))
     distinct
     )

(->> (key-downs @events)
     (map (comp vec distinct))
     (filter #(< 2 (count %)))
     (filter (contains-fn :space))
     distinct
     (group-by (fn [e] (count (take-while (fn [x] (not= x :space)) e))))
     (#(get % 2))
     (filter #(< 2 (count %)))
     )

;; Show me all the events that have multiple keys down after space goes down
(->> (key-downs @events)
     (map (comp vec distinct))
     (filter multiple-after-space?)
     distinct
     )

(count (distinct (key-downs @events)))


;; OK, so sometimes there are multiple keys down after space goes
;; down. But do they come up in reverse order? Or in FIFO order?
(->> (key-downs @events)
     (map #(if (only-space? %) [:repeat-spaces] %))
     (drop-while (complement multiple-after-space?))
     (drop 1)
     (drop-while (complement multiple-after-space?))
     (drop 1)
     (drop-while (complement multiple-after-space?))
     (drop 1)
     (drop-while (complement multiple-after-space?))
     (drop 100)
     (drop-while (complement multiple-after-space?))     
     (drop 1)
     (drop-while (complement multiple-after-space?))
     (take 10))
([:space :t :h] [:t :h] [:h] [:h :i] [:i] [:i :s] [:s] [:s :space] [:repeat-spaces] [])`q
([:space :e :m] [:e :m] [:m] [:m :a] [:a] [:a :i] [:a] [] [:l] [:l :space])
([:space :t :h] [:t :h] [:h] [:h :a] [:a] [:a :t] [:t] [] [:period] [])
([:space :t :h] [:t :h] [:h] [:h :a] [:a] [:a :t] [:t] [:t :comma] [:comma] [:comma :space])
([:space :t :h] [:t :h] [:h] [:h :e] [:e] [] [:r] [:r :e] [:e] [:e :quote])


;; TODO: Figure out what query I need to do to tell me if
;; modifier-press, regular-press, regular-release, modifier-release
;; ever appears in normal usage. Also compare with usage of normal
;; modifier keys.


(count (chords (take 100000 (cycle [{:key :j :direction :dn}
                                    {:key :j :direction :up}
                                    {:key :k :direction :dn}
                                    {:key :x :direction :dn}
                                    {:key :x :direciton :up}
                                    {:key :k :direction :up}
                                    ]))))

(->> @events
     (take 10)
     chords
     (filter #(> 2 (count %)))
     count)
(take 10 @events)


(spit "C:/temp/keys.txt" [:a :bunch :of :data])

(spit "C:/temp/keys.txt" '({:key :x, :direction :up}
          {:key :rcontrol, :direction :up}
          {:key :ralt, :direction :up}
          {:key :rcontrol, :direction :dn} {:key :x, :direction :dn}
          {:key :rcontrol, :direction :up} {:key :x, :direction :up}
          {:key :o, :direction :dn} {:key :o, :direction :up}
          {:key :rshift, :direction :dn}))

;; Load them from disk
(require '[clojure.java.io :as io])


(count @events)

(take 10 events)
(chords '({:key :x, :direction :up}
          {:key :rcontrol, :direction :up}
          ;; {:key :ralt, :direction :up} ; WTF?
          ;; {:key :rcontrol, :direction :dn}
          ;; {:key :x, :direction :dn}
          ;; {:key :rcontrol, :direction :up}
          ;; {:key :x, :direction :up}
          ;; {:key :o, :direction :dn}
          ;; {:key :o, :direction :up}
          ;; {:key :rshift, :direction :dn}
          ))

(require '[clojure.pprint :as pp])

(reset! events [])
(load! "C:/temp/keys.clj")
(count @events)


;; How many true two-chords are there?
(->> @events
     chords
     ;; (filter true-3chord?)
     (filter true-2chord?)
     (filter (fn [chord] (not (modifier? (first chord)))))
     ;; (filter (fn [chord] (= :space (:key (first chord)))))
     ;; (filter #(homerow? (first %)))
     (filter (fn [[{:keys [key]}]] (#{:s :d :f :j :k :l} key)))
     ;; count 
     ;;(take 10)
      pp/pprint
     )


(homerow? {:key :a :direction :dn})

(use :reload 'khordr.analysis)

(count @events)
(count (key-downs @events))
(save! "C:/temp/keys3.clj")
(collect)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(khordr.test.khordr/valid-events #{:a :b} 1)

(reduce conj (mapcat (partial khordr.test.khordr/valid-events #{:j :k :x}) (range 1 5)))

(khordr.test.khordr/valid-events #{:a :b} 2)

(in-ns 'khordr.test.khordr)

(valid? [{:key :x :direction :dn}])

(all-events #{:x} 1)


(cross-prod [:a :b] [:c :d])

(use :reload-all 'khordr)

(-> default-key-behaviors
    base-state
    (handler :j))

(handler-specifier default-key-behaviors :x)

(->> default-key-behaviors
     (partition 2)
     first
     first
     )

(-> default-key-behaviors
    base-state
    (handle-keys {:key :backtick :direction :dn})
    (handle-keys {:key :q :direction :dn})
    :effects)

(-> default-key-behaviors
    base-state
    (handle-keys {:key :backtick :direction :dn})
    (handle-keys {:key :backtick :direction :up})
    (handle-keys {:key :q :direction :dn})
    (handle-keys {:key :backtick :direction :dn})
)

(-> default-key-behaviors
    base-state
    (handle-keys {:key :d :direction :dn})
    (handle-keys {:key :s :direction :dn})
    (handle-keys {:key :d :direction :up})
    ;;(handle-keys {:key :s :direction :up})
    (dissoc :behaviors)

)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(in-ns 'khordr.handler.modifier-alias)

(h/process (->MultiArmed [:d :s] {:s :lalt :d :lcontrol})
           {} 
           {:key :d :direction :up})