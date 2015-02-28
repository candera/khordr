(ns khordr.main
  (:require [clojure.stacktrace :as trace]
            [khordr :as k]
            [khordr.logging :as log]
            [khordr.platform :as p]
            [khordr.platform.common :as com]))

;;; Adding a tray icon

;; TODO: Make this actually work
(defn create-image
  []
  (let [image (java.awt.image.BufferedImage. 32 32 java.awt.image.BufferedImage/TYPE_INT_RGB)
        graphics (.getGraphics image)]
    (doto graphics
      (.setBackground java.awt.Color/black)
      (.setColor java.awt.Color/red)
      (.clearRect 0 0 32 32)
      (.fillOval 0 0 32 32))
    image))

(defn add-tray-icon
  []
  (let [image (create-image)
        icon (java.awt.TrayIcon. image)]
    (-> (java.awt.SystemTray/getSystemTray)
        (.add icon))))

;;; Main

(defn read-config
  "Given something compatible with slurp, returns config. Return
  default behaviors if nil."
  [place]
  (if place
    (-> place slurp read-string)
    {:behaviors k/default-key-behaviors}))

(defn -main
  "Main entry point for the application."
  [& [config-place]]
  (let [config (read-config config-place)
        platform (p/platform)]
    (when-let [log-level (:log-level config)] (log/set-log-level! log-level))
    (log/info "Initialized")
    (try
      (loop [state (k/base-state config)]
        (let [event (com/await-key-event platform)
              _     (log/debug {:type :key-event-received
                                :data event})
              state (assoc state
                      :time-since-last-keyevent
                      (if-let [last-time (:last-keyevent-time state)]
                        (- (System/currentTimeMillis) last-time)
                        Long/MAX_VALUE))
              state (k/handle-keys state event)
              state (k/enact-effects! state platform)
              state (assoc state :last-keyevent-time (System/currentTimeMillis))]
          (when-not (:done state)
            (recur state))))
      (catch Throwable t
        (log/error {:type :exception
                    :data t
                    :message (.getMessage t)
                    :stacktrace (.getStackTrace t)
                    :stacktrace-str (with-out-str (trace/print-stack-trace t))}))
      (finally
        (shutdown-agents)
        (com/cleanup platform)
        (log/info "Done")))))
