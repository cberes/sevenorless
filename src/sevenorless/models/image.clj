(ns sevenorless.models.image
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
            [noir.io :refer [upload-file]]
            [sevenorless.models.db :as db])
  (:import [java.io File]))

(def default-image-store-path
  (delay (System/getProperty "image.store.path")))

(defn image-store-path []
  @default-image-store-path)

(defn file-ext [content-type]
  (case content-type
    "image/jpeg" "jpg"
    "image/png" "png"
    "image/gif" "gif"
    nil))

(defn image-file-name [{user-id :_id} content-type]
  (str user-id "_" (System/currentTimeMillis)))

(defn save-image [{:keys [content-type filename] :as file} user]
  (when-not (empty? filename)
    (let [new-file-name (image-file-name user content-type) folder (image-store-path) ext (file-ext content-type)]
      (try
        (upload-file folder (assoc file :filename (str new-file-name "." ext)) :create-path? true)
        (:_id (first (db/add-image {:user_id (:_id user) :path new-file-name :ext ext})))
        (catch Exception ex
          (println (str "error uploading file: " (.getMessage ex))))))))

(defn delete-image [id]
  (when-not (nil? id)
    (when-let [img (db/get-image id)]
      (io/delete-file (str (image-store-path) File/separator (:path img)) true)
      (db/delete-image id))))
