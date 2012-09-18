(ns khordr.platform.windows
  "Implement IPlatform for Windows"
  (:require [khordr.platform.common :as c]
            [khordr.logging :as log]
            [clojure.set :as set])
  (:import interception.InterceptionLibrary))

(def keycodes
  {{:code 1}   :esc
   {:code 59}  :f1
   {:code 60}  :f2
   {:code 61}  :f3
   {:code 62}  :f4
   {:code 63}  :f5
   {:code 64}  :f6
   {:code 65}  :f7
   {:code 66}  :f8
   {:code 67}  :f9
   {:code 68}  :f10
   {:code 87}  :f11
   {:code 88}  :f12
   {:code 42 :flags #{:e0}} :prtscn
   {:code 70}  :scrlk
   {:code 29 :flags #{:e1}} :pause ; Something is weird. A press gives both 29 (extended) and 69 (not extended)
   {:code 41} :backtick
   {:code 2} :1
   {:code 3} :2
   {:code 4} :3
   {:code 5} :4
   {:code 6} :5
   {:code 7} :6
   {:code 8} :7
   {:code 9} :8
   {:code 10} :9
   {:code 11} :0
   {:code 12} :dash
   {:code 13} :equal
   {:code 14} :backspace
   {:code 82 :flags #{:e0}} :insert
   {:code 71 :flags #{:e0}} :home
   {:code 73 :flags #{:e0}} :page-up
   {:code 81 :flags #{:e0}} :page-down
   {:code 83 :flags #{:e0}} :delete
   {:code 79 :flags #{:e0}} :end
   {:code 15} :tab
   {:code 16} :q
   {:code 17} :w
   {:code 18} :e
   {:code 19} :r
   {:code 20} :t
   {:code 21} :y
   {:code 22} :u
   {:code 23} :i
   {:code 24} :o
   {:code 25} :p
   {:code 26} :lbracket
   {:code 27} :rbracket
   {:code 43} :backslash
   {:code 58} :capslock
   {:code 30} :a
   {:code 31} :s
   {:code 32} :d
   {:code 33} :f
   {:code 34} :g
   {:code 35} :h
   {:code 36} :j
   {:code 37} :k
   {:code 38} :l
   {:code 39} :semicolon
   {:code 40} :quote
   {:code 28} :enter
   {:code 42} :lshift
   {:code 44} :z
   {:code 45} :x
   {:code 46} :c
   {:code 47} :v
   {:code 48} :b
   {:code 49} :n
   {:code 50} :m
   {:code 51} :comma
   {:code 52} :period
   {:code 53} :slash
   {:code 54} :rshift
   {:code 29} :lcontrol
   {:code 91 :flags #{:e0}} :lwindows
   {:code 56} :lalt
   {:code 57} :space
   {:code 56 :flags #{:e0}} :ralt
   {:code 92 :flags #{:e0}} :rwindows
   {:code 93 :flags #{:e0}} :menu
   {:code 29 :flags #{:e0}} :rcontrol
   {:code 72 :flags #{:e0}} :up
   {:code 80 :flags #{:e0}} :down
   {:code 75 :flags #{:e0}} :left
   {:code 77 :flags #{:e0}} :right
   {:code 69} :numlock
   {:code 53 :flags #{:e0}} :kpslash
   {:code 55} :kpstar
   {:code 74} :kpdash
   {:code 82} :kp0
   {:code 79} :kp1
   {:code 80} :kp2
   {:code 81} :kp3
   {:code 75} :kp4
   {:code 76} :kp5
   {:code 77} :kp6
   {:code 71} :kp7
   {:code 72} :kp8
   {:code 73} :kp9
   {:code 83} :kpperiod
   {:code 78} :kpplus
   {:code 28 :flags #{:e0}} :kpenter})

(def ^{:doc "A reverse lookup map for keycodes"}
  keycode-index
  (into {} (map (comp vec reverse) keycodes)))

(defn receive-stroke
  "Return an Interception keystroke object and the device it came in
  on, or nil if none is available."
  [context]
  (let [device (.interception_wait InterceptionLibrary/INSTANCE context)
        stroke (interception.InterceptionKeyStroke$ByReference.)
        received (.interception_receive
                  InterceptionLibrary/INSTANCE
                  context
                  device
                  stroke
                  1)]
    (when (pos? received) [stroke device])))

(defn stroke->event
  "Convert an Interception keystroke into a khordr key event."
  [stroke device]
  (let [state (.state stroke)
        direction (if (bit-test state 0) :up :dn)
        code (.code stroke)
        key-index {:code code}
        ;; TODO: These next two statements will set :flags to nil,
        ;; which isn't the same thing as it being totally absent
        key-index (if (bit-test state 1)
                    (assoc key-index :flags #{:e0})
                    key-index)
        key-index (if (bit-test state 2)
                    (update-in key-index [:flags] set/union #{:e1})
                    key-index)
        key (get keycodes key-index code)]
    {:key key :direction direction :device device}))

(defn stroke-state
  "Convert a direction and flags into a value for the state field of
  an InterceptionKeyStroke object."
  [direction flags]
  (+ (if (= direction :up) 1 0)
     (if (:e0 flags) 2 0)
     (if (:e1 flags) 4 0)))

(defn event->stroke
  "Convert a khordr key event into an Interception keystroke."
  [event]
  (let [{:keys [key direction]} event
        {:keys [code flags]} (get keycode-index key)
        ;; Keys might be integers if we get one we've never heard of
        code (or code key)
        state (stroke-state direction flags) 
        stroke (interception.InterceptionKeyStroke$ByReference.)]
    (set! (.code stroke) code)
    (set! (.state stroke) state)
    stroke))

(defrecord WindowsPlatform [context]
  c/IPlatform
  (await-key-event [this]
    ;; (log/debug "awaiting key event for " context)
    (let [[stroke device] (receive-stroke context)]
      (when stroke
        (stroke->event stroke device))))
  (send-key [this keyevent]
    ;;(log/debug "sending key" keyevent "on" this)
    (if-let [device (:device keyevent)]
      (let [stroke (event->stroke keyevent)]
        ;; (log/debug "translated event:" stroke)
        ;; (log/debug "sending stroke" stroke "on device" device "in context" context)
        (.interception_send InterceptionLibrary/INSTANCE
                            context
                            device
                            stroke
                            1))
      (throw (ex-info "Device was absent from key event"
                      {:keyevent keyevent}))))
  (cleanup [this]
    (.interception_destroy_context InterceptionLibrary/INSTANCE context)))

(defn initialize
  "Return an implementation of IPlatform suitable for use on Windows
  operating systems."
  []
  (let [context (.interception_create_context InterceptionLibrary/INSTANCE)]
    (.interception_set_filter
     InterceptionLibrary/INSTANCE
     context
     (reify interception.InterceptionLibrary$InterceptionPredicate
       (apply [_ device]
         (.interception_is_keyboard InterceptionLibrary/INSTANCE device)))
     (short -1))
    (WindowsPlatform. context)))

(comment

  (println "received:" received (.code stroke) (.state stroke))
  ;; (when (= 0x15 (.code stroke))
  ;;   (set! (.code stroke) 0x2d))
  (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1)
  ;; If it's a y, send an additional x
  (when (= 0x15 (.code stroke))
    (set! (.code stroke) 0x2d)
    (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1))
  ;;  // Hitting escape terminates the program
  ;;  if (stroke.key.code == ScanCode.Escape)
  (if (= 0x01 (.code stroke))
    (println "quitting")
                                        ;(recur)
    ))