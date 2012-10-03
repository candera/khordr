;; Example configuration file
{:version 1                             ; Ignored (for now)
 ;; TODO: Put better comments in here
 :behaviors [{:id :right-modifier-aliases
              :match {:key #{:j :k :l}}
              :handler khordr.handler.modifier-alias/Handler
              :args [{:j :rshift :k :rcontrol :l :ralt}]}
             {:id :left-modifier-aliases
              :match {:key #{:f :d :s}}
              :handler khordr.handler.modifier-alias/Handler
              :args [{:f :lshift :d :lcontrol :s :lalt}]}
             {:id :special-actions
              :match {:key :backtick}
              :handler khordr.handler.special-action/Handler}
             ;; {:id :modifier-suppressors 
             ;;  :match {:key #{:rshift :lshift :rcontrol :lcontrol :lalt :ralt :capslock}}
             ;;  :handler khordr.handler.suppressor/Handler}
             ]}