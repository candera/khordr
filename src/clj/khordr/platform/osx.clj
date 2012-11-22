(ns khordr.platform.osx
  "Implement IPlatform for OS X"
  (:require [khordr.platform :as p]
            [khordr.platform.common :as c]
            [khordr.logging :as log])
  (:import khordr.KeyGrabber
           java.util.concurrent.LinkedBlockingQueue))

(def keycodes
  {53  :esc
   122 :f1
   120 :f2
   99  :f3
   118 :f4
   96  :f5
   97  :f6 
   98  :f7 
   100 :f8
   101 :f9
   109 :f10
   103 :f11
   111 :f12
   105 :prtscn
   107 :scrlk
   113 :pause
   50  :backtick
   18  :1
   19  :2
   20  :3
   21  :4
   23  :5
   22  :6
   26  :7
   28  :8
   25  :9
   29  :0
   27  :dash
   24  :equal
   51  :backspace
   114 :insert
   115 :home
   116 :page-up
   121 :page-down
   117 :delete
   119 :end
   48  :tab
   12  :q
   13  :w
   14  :e
   15  :r
   17  :t
   16  :y
   32  :u
   34  :i
   31  :o
   35  :p
   33  :lbracket
   30  :rbracket
   42  :backslash
   ;; ?? :capslock
   0   :a
   1   :s
   2   :d
   3   :f
   5   :g
   4   :h
   38  :j
   40  :k
   37  :l
   41  :semicolon
   39  :quote
   36  :enter
   -3  :lshift
   6   :z
   7   :x
   8   :c
   9   :v
   11  :b
   45  :n
   46  :m
   43  :comma
   47  :period
   44  :slash
   -4  :rshift
   -1  :lcontrol
   -5  [:lcommand :lwindows]
   -7  [:loption :lalt]
   49  :space
   -8  [:roption :ralt]
   -6  [:rcommand :rwindows]
   110 :menu
   -2  :rcontrol
   126 :up
   125 :down
   123 :left
   124 :right
   71  :numlock
   75  :kpslash
   67  :kpstar
   78  :kpdash
   82  :kp0
   83  :kp1
   84  :kp2
   85  :kp3
   86  :kp4
   87  :kp5
   88  :kp6
   89  :kp7
   91  :kp8
   92  :kp9
   65  :kpperiod
   69  :kpplus
   76  :kpenter})

(defn code-to-key
  [code]
  "Given a keycode, return the symbolic name for it, if there is one.
  If the keycode maps to more than one symbolic name (e.g. ralt =
  roption), return the first one."
  (let [key (get keycodes code code)]
    (if (coll? key)
      (first key)
      key)))

(defn is-match?
  "Return true if `key` matches `specifier`. A match is either an
  exact match, or if specifier is a sequence, if the sequence contains
  key."
  [key specifier]
  (or (= key specifier)
      (and (coll? specifier)
           (some #(= key %) specifier))))

(defn key-to-code
  [key]
  "Given the symbolic name of a key, return the keycode for it. Note
  that keys can have more than one name (e.g. ralt = roption), so we
  have to search."
  (->> keycodes
      (filter (fn [[code spec]] (is-match? key spec)))
      ffirst))

;; We need to keep track of which keys we've sent, because stupid
;; EventTaps send them right back to us, in which case we need to
;; ignore them.
(def sent-keys (atom []))

(defn- remove-first
  "Given a vector and an item x, return a vector of the items in
  the vector with the first instance of x removed."
  [v x]
  (loop [acc []
         c v]
    (if (seq c)
      (if (= x (first c))
        (reduce conj acc (rest c))
        (recur (conj acc (first c)) (rest c)))
      acc)))

(defrecord OSXPlatform [grabf queue]
  c/IPlatform
  (await-key-event [this]
    (let [received (.take queue)
          {:keys [key direction]} received]
      ;; Is this a key we sent? If so, mark it as seen and wait for
      ;; the next one. This probably needs to get updated so it deals
      ;; with the fact that a send of SHIFT might come in a either a
      ;; right or left shift.
      (if (some #(= [key direction] %) @sent-keys)
        (do 
          (swap! sent-keys remove-first [key direction])
          (recur))
        received)))
  (send-key [this {:keys [key direction]}]
    (log/debug [:sending key direction])
    (swap! sent-keys conj [key direction])
    (KeyGrabber/send (key-to-code key) (if (= direction :dn) 1 0)))
  (cleanup [this]
    ;; TODO: Fix this - do a proper shutdown
    (future-cancel grabf)))

(defmethod p/initialize :khordr.os/osx [_]
  (let [queue (LinkedBlockingQueue.)
        handler (reify khordr.KeyEventHandler
                  (onKeyEvent [_ key direction]
                    (.put queue {:key (code-to-key key)
                                 :direction (if (zero? direction) :up :dn)
                                 :context nil})))]
    (log/debug (str "Handler: " handler))
    (OSXPlatform. (future (KeyGrabber/grab handler))
                  queue)))