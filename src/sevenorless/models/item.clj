(ns sevenorless.models.item
  (:require [noir.cookies :as cookies]
            [noir.session :as session]
            [sevenorless.models.db :as db]))

(defn image-store-path []
  "/home/corey/siol-images")

(defn file-ext [content-type]
  (case content-type
    "image/jpeg" "jpg"
    "image/png" "png"
    "image/gif" "gif"
    nil))

(defn image-file-name [{user-id :_id} content-type]
  (str user-id "_" (System/currentTimeMillis) "." (file-ext content-type)))