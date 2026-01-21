(ns travelproject.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- read-dev-config []
  (let [f (io/file "config.edn")]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defonce cfg (or (read-dev-config) {}))

(defn get-in-cfg [ks]
  (get-in cfg ks))

(defn require! [ks msg]
  (let [v (get-in cfg ks)]
    (when (or (nil? v) (= "" v))
      (throw (ex-info msg {:missing ks})))
    v))
