(ns khordr.logging)

(def ^:private l (Object.))

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

(defn debug
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :debug))
    (locking l
     (prn data))))

(defn info
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :info))
    (locking l
      (prn data))))

(defn error
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :error))
    (locking l
     (prn data))))

(defn set-log-level!
  "Sets logging level to level, which must be one of :debug, :info, or :error."
  [level]
  (alter-var-root #'*log-level* (constantly level))
  (info {:type :log-level-change :new-level level}))

