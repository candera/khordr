(ns kchordr.main)

(defn intercept
  "Start intercepting keys, calling the function held in the function or var f until it returns false."
  [f]
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
      (loop
        (let [device (.interception_wait InterceptionLibrary/INSTANCE ctx)
              stroke (interception.InterceptionKeyStroke$ByReference.)
              received (.interception_receive
                        InterceptionLibrary/INSTANCE
                        ctx
                        device
                        stroke
                        1)]

          (when (< 0 received)
            (.invoke v stroke)
            (.interception_send InterceptionLibrary/INSTANCE ctx device stroke 1)
            ;;  // Hitting escape terminates the program
            ;;  if (stroke.key.code == ScanCode.Escape)
            (when-not (= 0x01 (.code stroke))
              (recur)))))
      (finally
       (.interception_destroy_context InterceptionLibrary/INSTANCE ctx)))))
