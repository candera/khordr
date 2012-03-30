;; IntPtr context;

;; int device;
;; Interception.Stroke stroke = new Interception.Stroke();

;; context = Interception.CreateContext();
(System/setProperty "jna.library.path" "ext")
(import 'interception.InterceptionLibrary)
(let [ctx (.interception_create_context InterceptionLibrary/INSTANCE)]
  (try
    (.interception_set_filter
     InterceptionLibrary/INSTANCE
     ctx
     (reify interception.InterceptionLibrary$InterceptionPredicate
       (apply [_ device]
         (.interception_is_keyboard InterceptionLibrary/INSTANCE device)))
     (short -1))

    (dotimes [n 5000o]
      (let [device (.interception_wait InterceptionLibrary/INSTANCE ctx)
            stroke (interception.InterceptionKeyStroke$ByReference.)
            received (.interception_receive
                      InterceptionLibrary/INSTANCE
                      ctx
                      device
                      stroke
                      1)]

        (when (< 0 received)
          (println "received:" received (.code stroke) (.state stroke))
          ;; (when (= 0x15 (.code stroke))
          ;;   (set! (.code stroke) 0x2d))
          (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1)
          ;;  // Hitting escape terminates the program
          ;;  if (stroke.key.code == ScanCode.Escape)
          (if (= 0x01 (.code stroke))
            (println "quitting")
            (recur)))))
    (finally
     (.interception_destroy_context InterceptionLibrary/INSTANCE ctx))))


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