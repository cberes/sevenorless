(ns sevenorless.models.image
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
            [noir.io :refer [upload-file]]
            [sevenorless.models.db :as db])
  (:import [java.io File]))

(defn image-store-path []
  "/home/corey/siol/user-images")

(defn file-ext [content-type]
  (case content-type
    "image/jpeg" "jpg"
    "image/png" "png"
    "image/gif" "gif"
    nil))

(defn image-file-name [{user-id :_id} content-type]
  (str user-id "_" (System/currentTimeMillis) "." (file-ext content-type)))

(defn save-image [{:keys [content-type filename] :as file} user]
  (when-not (empty? filename)
    (let [new-file-name (image-file-name user content-type) folder (image-store-path)]
      (try
        (upload-file folder (assoc file :filename new-file-name) :create-path? true)
        (:_id (first (db/add-image {:user_id (:_id user) :path new-file-name})))
        (catch Exception ex
          (println (str "error uploading file: " (.getMessage ex))))))))

(defn delete-image [id]
  (when-not (nil? id)
    (when-let [img (db/get-image id)]
      (io/delete-file (str (image-store-path) File/separator (:path img)) true)
      (db/delete-image id))))
