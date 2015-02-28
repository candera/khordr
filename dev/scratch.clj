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

