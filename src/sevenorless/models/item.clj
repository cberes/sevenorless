(ns sevenorless.models.item
  (:require [clojure.string :as string]
            [hiccup.element :refer [image link-to]]
            [hiccup.form :refer [form-to text-area hidden-field submit-button]]
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

(defn build-image [{:keys [image_id user_image_id link]}]
  (when-not (nil? image_id)
    (let [img-src (str "/img/" image_id ".jpg") img (image {:class "b"} img-src)]
      (link-to (if (nil? link) img-src link) img))))

(defn build-body [title body]
  (when-not (and (nil? title) (string/blank? body))
    [:div.b title body]))

(defn build-comment [comment]
  [:tr
   [:td.icon (link-to "#" (image "/img/anon.png"))]
   [:td.author [:div (link-to (str "/u/" "foo") "foo")]]
   [:td.body [:div "Whoa, a comment!"]]])

(defn build-comments [comments]
  (if (empty? comments)
    [:p "No comments yet"]
    [:table.comments (map build-comment comments)]))

(defn build-comment-form [id allow-comments?]
  (when allow-comments?
    (form-to {:onsubmit (str "return addComment(" id ", 'comments-" id "', this);")} [:post "/comment"]
             (hidden-field "comment-item-id" id)
             [:table.add-comment
              [:tr
               [:td (text-area {:maxlength 4096 :placeholder "comment"} :comment)]
               [:td.send (submit-button "Send")]]])))

(defn build-item [{:keys [_id user_image_id username created body comments] :as item}]
  [:div.i
   (build-image item)
   (build-body (build-title item) body)
   [:div.u
    [:div.m {:onclick (str "toggleComments(" _id ", 'comments-" _id "');")}
     [:span.tag-count "0 tags"] "0 comments, +0, -0"]
    [:div.f {:id (str "comments-" _id) :style "display: none;"}
     [:p.tags "#tag1 #tag2 #tag3"]
     (build-comment-form _id comments)
     [:div.comments-placeholder "Loading comments"]]
    [:div.a
     (link-to (str "/u/" username) (image (if-not (nil? user_image_id) (str "/img/" user_image_id ".jpg") "/img/anon.png")))
     [:strong (link-to (str "/u/" username) username)]
     [:span.item-time (format-time created)]]]])

(defn format-item [item]
  (let [formatted-item (build-item item)]
    (if (= (:rank item) 1)
      (list [:h2 (format-date (:created item))] formatted-item)
      formatted-item)))
