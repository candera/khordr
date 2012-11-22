(ns khordr.platform
  "An abstraction over the underlying operating system for things like
  sending and receiving key events."
  (:require [khordr :as k]))

(defn windows?
  "Return true if the provided name identifies a Windows operating system."
  [os-name]
  (-> os-name
      (.indexOf "win")
      neg?
      not))

(defn osx?
  "Return true if the provided name identifies a Mac operating system."
  [os-name]
  (-> os-name
      (.indexOf "mac")
      zero?))

(defn os
  "Return one of :windows, :osx, or :linux, depending on what system
  we're running on."
  []
  (let [os (.toLowerCase (System/getProperty "os.name"))]
    (cond
     (windows? os) :khordr.os/windows
     (osx? os) :khordr.os/osx
     :else (throw (ex-info "Operating system not supported" {:os os})))))

(defmulti initialize
  "Multimethod which returns an instance of IPlatform given an operating system."
  identity)

(defn platform
  "Return an implemenation of IPlatform that provides the necessary
  services for whatever operating system we happen to be on."
  []
  ;; TODO: Deal with differences between 32 and 64-bit Windows/Java.
  ;; One thing that might help: (System/getProperty "sun.arch.data.model")
  (let [os (os)
        plat-ns-name (str "khordr.platform." (name os))
        plat-ns-sym (symbol plat-ns-name)]
    (println "Platform namespace: " plat-ns-name)
    (require plat-ns-sym)
    (initialize os)))