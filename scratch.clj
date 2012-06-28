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