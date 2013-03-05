(ns khordr.filter
  "Defines the interface for khordr filters")

(defprotocol Filter
  (filter-event [this data]
    "Filter `data` a map containing keys :event and :state. Returns
    another map containing the same keys. If present, values of those
    keys replace the current value in the processing pipeline."))