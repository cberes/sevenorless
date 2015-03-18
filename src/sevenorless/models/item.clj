(ns sevenorless.models.item
  (:require [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
            [noir.cookies :as cookies]
            [noir.session :as session]
            [sevenorless.models.db :as db]))

(defn format-time [time]
  (.format (java.text.SimpleDateFormat. "hh:mm aa, MMMM dd") time))

(defn format-date [time]
  (.format (java.text.SimpleDateFormat. "MMM dd") time))

(defn build-title [{:keys [link title]}]
  (if (nil? link)
    (if (nil? title) nil [:h3 title])
    [:h3 (link-to link (if (nil? title) link title)) " \u26A1"]))

(defn build-image [{:keys [image_id user_image_id]}]
  (when-not (nil? image_id)
    (image {:class "b"} (str "/img/" image_id ".jpg"))))

(defn build-body [title body]
  (when-not (and (nil? title) (string/blank? body))
    [:div.b title body]))

(defn build-item [item]
  [:div.i
   (build-image item)
   (build-body (build-title item) (:body item))
   [:div.u
    [:strong (link-to (str "/u/" (:username item)) (:username item))]
    (link-to (str "/u/" (:username item)) (image (if-not (nil? (:user_image_id item)) (str "/img/" (:user_image_id item) ".jpg") "/img/anon.png")))
    [:br]
    (format-time (:created item))
    [:br]
    [:br]
    "tags"
    [:div.clear]]])

(defn format-item [item]
  (let [formatted-item (build-item item)]
    (if (= (:rank item) 1)
      (list [:h2 (format-date (:created item))] formatted-item)
      formatted-item)))