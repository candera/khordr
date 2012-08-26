(ns khordr.logging)

(defn debug
  [data]
  (println (pr-str data)))

(defn info
  [data]
  (println (pr-str data)))

(defn error
  [data]
  (println (pr-str data)))