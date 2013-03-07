(ns khordr.logging)

(def ^:private l (Object.))

(def ^:dynamic *log-level* :debug)

(def ^:private logger (agent nil))

(def log-level-values
  {:debug 3
   :info 2
   :error 1
   :critial 0
   :silent -1})

(defn log
  "Write to the log"
  [data]
  (send-off logger (fn [a d] (try (prn d) (catch Throwable t))) data))

(defn log-level
  "Returns the current log level"
  []
  *log-level*)

(defn debug
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :debug))
    (log data)))

(defn info
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :info))
    (log data)))

(defn error
  [data]
  (when (>= (log-level-values *log-level*) (log-level-values :error))
    (log data)))

(defn set-log-level!
  "Sets logging level to level, which must be one of :debug, :info, or :error."
  [level]
  (alter-var-root #'*log-level* (constantly level))
  (info {:type :log-level-change :new-level level}))

