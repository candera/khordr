(ns khordr.platform.common
  "The common abstractions for khordr platform services.")

(defprotocol IPlatform
  (await-key-event [this]
    "Block until a key event happens, and then return it.")
  (send-key [this keyevent]
    "Transmit a key press or release."))



