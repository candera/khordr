;; IntPtr context;

;; int device;
;; Interception.Stroke stroke = new Interception.Stroke();

;; context = Interception.CreateContext();
(System/setProperty "jna.library.path" "ext")
(import 'interception.InterceptionLibrary)
(let [ctx (.interception_create_context InterceptionLibrary/INSTANCE)]
  (try
;; Interception.SetFilter(context, Interception.IsKeyboard, Interception.Filter.All);
    (.interception_set_filter InterceptionLibrary/INSTANCE ctx (reify interception.InterceptionLibrary$InterceptionPredicate
                                    (apply [_ device] (.interception_is_keyboard InterceptionLibrary/INSTANCE device)))
                              (short -1))
;; while (Interception.Receive(context, device = Interception.Wait(context), ref stroke, 1) > 0)
;; {
;;  Console.WriteLine("SCAN CODE: {0}/{1}", stroke.key.code, stroke.key.state);
    (let [device (.interception_wait InterceptionLibrary/INSTANCE ctx)
          stroke (interception.InterceptionKeyStroke$ByReference.)]
      (if (< 0 (.interception_receive InterceptionLibrary/INSTANCE ctx device stroke 1))
        (println "SCAN CODE: " (.code stroke) (.state stroke))))
;;  if (stroke.key.code == ScanCode.X) 
;;  {
;;   stroke.key.code = ScanCode.Y;
;;   }
;;  Interception.Send(context, device, ref stroke, 1);

    (finally
     (.interception_destroy_context InterceptionLibrary/INSTANCE ctx))))

;;  // Hitting escape terminates the program
;;  if (stroke.key.code == ScanCode.Escape)
;;  {
;;   break;
;;   }
;;  }

;; Interception.DestroyContext(context);

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(System/setProperty "jna.library.path" "ext")
