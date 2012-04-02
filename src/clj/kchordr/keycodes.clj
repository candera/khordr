(ns kchordr.keycodes)

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
   [28 :e0] :kpenter})

