;; Example configuration file
{:version 2                             ; Ignored (for now)
 :log-level :info
 ;; TODO: Put better comments in here
 :filters {:before [
                    ;; Substitute capslock for left control
                    {:filter khordr.filter.substituter/Filter :args [{:capslock :lcontrol}]}]
           :after []}
 :behaviors [{:id :right-modifier-aliases
              :match {:key #{:j :k :l}}
              :handler khordr.handler.modifier-alias/Handler
              :args [{:j :rshift :k :rcontrol :l :ralt} {:typethrough-threshold 50}]}
             {:id :left-modifier-aliases
              :match {:key #{:f :d :s}}
              :handler khordr.handler.modifier-alias/Handler
              :args [{:f :lshift :d :lcontrol :s :lalt} {:typethrough-threshold 50}]}
             {:id :special-actions
              :notoggle true            ; Otherwise we'll toggle
                                        ; ourselves off with the other
                                        ; behaviors and never be able
                                        ; to toggle back on
              :match {:key :backtick}
              :handler khordr.handler.special-action/Handler}
             ;; {:match {:key :a}
             ;;  :handler khordr.handler.simple-alias/Handler
             ;;  :args [{:h :left :j :down :k :up :l :right}]}
             ;; {:match {:key :semicolon}
             ;;  :handler khordr.handler.simple-alias/Handler
             ;;  :args [{:a :home, :e :end}]}
             ;; {:id :modifier-suppressors
             ;;  :match {:key #{:rshift :lshift :rcontrol :lcontrol :lalt :ralt :capslock}}
             ;;  :handler khordr.handler.suppressor/Handler}
             ]}
