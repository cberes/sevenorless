(ns sevenorless.models.item
  (:require [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
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
    [:h3 (link-to link (if (nil? title) link title))]))

(defn build-image [{:keys [image_id user_image_id]}]
  (when-not (nil? image_id)
    (image {:class "b"} (str "/img/" image_id ".jpg"))))

(defn build-body [title body]
  (when-not (and (nil? title) (string/blank? body))
    [:div.b title body]))

(defn format-item [item]
  [:div.i
   (build-image item)
   (build-body (build-title item) (:body item))
   [:div.u
    (link-to (str "/u/" (:username item)) (:username item))
    (link-to (str "/u/" (:username item)) (image (if-not (nil? (:user_image_id item)) (str "/img/" (:user_image_id item) ".jpg") "/img/anon.png")))
    [:div.clear]]])