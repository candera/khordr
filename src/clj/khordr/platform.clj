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

(defn os-name
  "Return one of `windows`, `osx`, or `linux` as a string, depending on
  what system we're running on."
  []
  (let [os (.toLowerCase (System/getProperty "os.name"))]
    (if (windows? os)
      "windows"
      (throw (ex-info "Operating system not supported" {:os os})))))

(defn initialize
  "Return an implemenation of IPlatform that provides the necessary
  services for whatever operating system we happen to be on."
  []
  ;; TODO: Deal with differences between 32 and 64-bit Windows/Java.
  ;; One thing that might help: (System/getProperty "sun.arch.data.model")
  (let [plat-ns-name (str "khordr.platform." (os-name))
        plat-ns-sym (symbol plat-ns-name)
        _ (require plat-ns-sym)
        plat-ns (find-ns plat-ns-sym)
        initializer (ns-resolve plat-ns 'initialize)]
    (initializer)))