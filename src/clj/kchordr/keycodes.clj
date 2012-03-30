(ns kchordr.keycodes)

(def keycodes
  {1   :esc
   59  :f1
   60  :f2
   61  :f3
   62  :f4
   63  :f5
   64  :f6
   65  :f7
   66  :f8
   67  :f9
   68  :f10
   87  :f11
   88  :f12
   [42 1] :prtscn
   70  :scrlk
   [29 2] :pause                        ; Something is weird. A press gives both 29 (extended) and 69 (not extended)
   41 :backtick
   2 :1
   3 :2
   4 :3
   5 :4
   6 :5
   7 :6
   8 :7
   9 :8
   10 :9
   11 :0
   12 :dash
   13 :equal
   14 :backspace
   [82 1] :insert
   [71 1] :home
   [73 1] :page-up
   [81 1] :page-down
   [83 1] :delete
   [79 1] :end
   15 :tab
   16 :q
   17 :w
   18 :e
   19 :r
   20 :t
   21 :y
   22 :u 
   23 :i
   24 :o
   25 :p
   26 :lbracket
   27 :rbracket
   43 :backslash
   58 :capslock
   30 :a
   31 :s
   32 :d
   33 :f
   34 :g
   35 :h
   36 :j
   37 :k
   38 :l
   39 :semicolon
   40 :quote
   28 :enter
   42 :lshift
   44 :z
   45 :x
   46 :c
   47 :v
   48 :b
   49 :n
   50 :m
   51 :comma
   52 :period
   53 :slash
   54 :rshift
   29 :lcontrol
   [91 1] :lwindows
   56 :lalt
   57 :space
   [56 1] :ralt
   [92 1] :rwindows
   [93 1] :menu
   [29 1] :rcontrol
   [72 1] :up
   [80 1]:down
   [75 1] :left
   [77 1] :right
   69 :numlock
   [53 1] :kpslash
   55 :kpstar
   74 :kpdash
   82 :kp0
   79 :kp1
   80 :kp2
   81 :kp3
   75 :kp4
   76 :kp5
   77 :kp6
   71 :kp7
   72 :kp8
   73 :kp9
   83 :kpperiod
   [28 1] :kpenter})