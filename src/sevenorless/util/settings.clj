(ns sevenorless.util.settings
  (:require [clojure.java.io :refer [file reader resource]]))

(def properties-file-name "config.properties")

(defn properties-file []
  (let [file (file properties-file-name)]
    (if (.exists file) file (resource properties-file-name))))

(defn load-properties []
  (let [props (java.util.Properties. (System/getProperties))]
    (.load props (reader (properties-file)))
    (System/setProperties props)))