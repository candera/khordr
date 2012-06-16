(ns khordr.platform.windows
  "Implement IPlatform for Windows"
  (:require [khordr.platform.common :as c])
  (:import interception.InterceptionLibrary))

(def keycodes
  {[1]   :esc
   [59]  :f1
   [60]  :f2
   [61]  :f3
   [62]  :f4
   [63]  :f5
   [64]  :f6
   [65]  :f7
   [66]  :f8
   [67]  :f9
   [68]  :f10
   [87]  :f11
   [88]  :f12
   [42 :e0] :prtscn
   [70]  :scrlk
   [29 :e1] :pause                        ; Something is weird. A press gives both 29 (extended) and 69 (not extended)
   [41] :backtick
   [2] :1
   [3] :2
   [4] :3
   [5] :4
   [6] :5
   [7] :6
   [8] :7
   [9] :8
   [10] :9
   [11] :0
   [12] :dash
   [13] :equal
   [14] :backspace
   [82 :e0] :insert
   [71 :e0] :home
   [73 :e0] :page-up
   [81 :e0] :page-down
   [83 :e0] :delete
   [79 :e0] :end
   [15] :tab
   [16] :q
   [17] :w
   [18] :e
   [19] :r
   [20] :t
   [21] :y
   [22] :u
   [23] :i
   [24] :o
   [25] :p
   [26] :lbracket
   [27] :rbracket
   [43] :backslash
   [58] :capslock
   [30] :a
   [31] :s
   [32] :d
   [33] :f
   [34] :g
   [35] :h
   [36] :j
   [37] :k
   [38] :l
   [39] :semicolon
   [40] :quote
   [28] :enter
   [42] :lshift
   [44] :z
   [45] :x
   [46] :c
   [47] :v
   [48] :b
   [49] :n
   [50] :m
   [51] :comma
   [52] :period
   [53] :slash
   [54] :rshift
   [29] :lcontrol
   [91 :e0] :lwindows
   [56] :lalt
   [57] :space
   [56 :e0] :ralt
   [92 :e0] :rwindows
   [93 :e0] :menu
   [29 :e0] :rcontrol
   [72 :e0] :up
   [80 :e0]:down
   [75 :e0] :left
   [77 :e0] :right
   [69] :numlock
   [53 :e0] :kpslash
   [55] :kpstar
   [74] :kpdash
   [82] :kp0
   [79] :kp1
   [80] :kp2
   [81] :kp3
   [75] :kp4
   [76] :kp5
   [77] :kp6
   [71] :kp7
   [72] :kp8
   [73] :kp9
   [83] :kpperiod
   [78] :kpplus
   [28 :e0] :kpenter})

(defn receive-stroke
  "Return an Interception keystroke object, or nil if none is available."
  [context]
  (let [device (.interception_wait InterceptionLibrary/INSTANCE context)
        stroke (interception.InterceptionKeyStroke$ByReference.)
        received (.interception_receive
                  InterceptionLibrary/INSTANCE
                  context
                  device
                  stroke
                  1)]
    (when (pos? received) stroke)))

(defn stroke->event
  "Convert an Interception keystroke into a khordr key event."
  [stroke device]
  (let [state (.state stroke)
        direction (if (bit-test state 0) :up :dn)
        e0 (when (bit-test state 1) :e0)
        e1 (when (bit-test state 2) :e1)
        key-index (filter identity [(.code stroke) e0 e1])
        key (get keycodes key-index (.code stroke))]
    {:key key :direction direction :device device}))

(defrecord WindowsPlatform [context]
  c/IPlatform
  (await-key-event [this]
    (let [stroke (receive-stroke context)]
      (when stroke (stroke->event stroke))))
  (send-key [this keyevent]
    (when-not (:device keyevent)
      (throw (ex-info "Device was absent from key event" {:keyevent keyevent})))
    (send-stroke (event->stroke keyevent) (:device keyevent))))

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