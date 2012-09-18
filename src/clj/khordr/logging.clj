(ns khordr.logging)

(def ^:dynamic *log-level* :debug)

(def log-level-values
  {:debug 3
   :info 2
   :error 1
   :critial 0
   :silent -1})

(defn log-level
  "Returns the current log level"
  []
  *log-level*)

(defn set-log-level!
  "Sets logging level to level, which must be one of :debug, :info, or :error."
  [level]
  (alter-var-root #'*log-level* (constantly level)))

(defn debug
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :debug))
    (prn data)))

(defn info
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :info))
    (prn data)))

(defn error
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :error))
    (prn data)))