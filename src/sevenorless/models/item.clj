(ns sevenorless.models.item
  (:require [hiccup.element :refer [image link-to]]
            [noir.cookies :as cookies]
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

(defn build-title [{:keys [link title]}]
  (if (nil? link)
    (if (nil? title) nil [:h3 title])
    (link-to link [:h3 (if (nil? title) link title)])))

(defn build-image [{:keys [image_id]}]
  (when-not (nil? image_id)
    (image {:style "width: 100%;"} (str "/img/" image_id ".jpg"))))

(defn format-item [item]
  [:div.c
   (build-image item)
   (build-title item)
   (:body item)
   [:p (:username item)]])