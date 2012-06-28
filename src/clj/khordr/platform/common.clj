(ns khordr.platform.common
  "The common abstractions for khordr platform services.")

(defprotocol IPlatform
  (await-key-event [platform]
    "Block until a key event happens, and then return it.")
  (send-key [platform keyevent]
    "Transmit a key press or release.")
  (cleanup [platform]
    "Release any resources associated with the instance."))



