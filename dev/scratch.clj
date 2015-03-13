(h/process (->Handler {:j :rshift} {:typethrough-threshold 2})
                    {:time-since-last-keyevent 1}
                    {:key :j :direction :dn})

(require '[clojure.test :refer :all]
         '[khordr.handler :as h]
         '[khordr.handler.modifier-alias :refer :all]
         '[khordr.effect :as e]
         '[khordr.test.handler :refer :all])


(def kt
  (partial keytest (->Handler {:j :rshift :k :rcontrol} nil)))



(test/run-tests 'khordr.test.handler.modifier-alias)

(require 'khordr.handler.modifier-alias)
(pprint (keytest (khordr.handler.modifier-alias/->Handler {:j :rshift :k :rcontrol} nil)
                 [[:j :dn] [:j :dn] [:j :up] [:j :dn] [:j :up]]))

(pprint
 (khordr.test.handler/run (khordr.handler.modifier-alias/->Handler {:j :rshift :k :rcontrol} nil)
   [[:j :dn] [:x :dn] [:x :dn]]))

;;; Tray icons

(import [java.awt SystemTray TrayIcon PopupMenu MenuItem Menu
          Graphics Color]
        [java.awt.geom Rectangle2D$Double Ellipse2D$Double]
        [java.awt.image BufferedImage])

(SystemTray/isSupported)

(let [width 16
      height 16
      item (MenuItem. "Item")
      menu (Menu. "Menu")
      popup (PopupMenu.)
      image (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
      icon (TrayIcon. image)
      tray (SystemTray/getSystemTray)
      graphics (.createGraphics image)]
  (doto graphics
    (.setColor (Color/BLACK))
    ;;(.fill (Rectangle2D$Double. 0 0 width height))
    (.setColor (Color/RED))
    (.fill (Ellipse2D$Double. 0 0 width height)))
  (.add popup item)
  (.setPopupMenu icon popup)
  (.add tray icon))

;;; Bug smashing

(pprint (keytest (ma/->Handler {:j :rshift :k :rcontrol} nil)
                 [[:j :dn] [:k :dn] [:x :dn] [:x :up]]))

(pprint (keytest (ma/->Handler {:j :rshift :k :rcontrol} nil)
                 [[:k :dn] [:k :up]]))
