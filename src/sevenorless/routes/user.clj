(ns sevenorless.routes.user
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [hiccup.element :refer [image link-to]]
            [hiccup.form :refer [form-to label text-area text-field file-upload hidden-field check-box submit-button]]
            [hiccup.util :refer [url-encode]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.route :refer [restricted]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.image :as image]
            [sevenorless.models.item :as item]
            [sevenorless.models.user :as user])
  (:import [java.io File]))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. "MMMM dd, yyyy") date))

(defn follow-link [username following?]
  (link-to {:id "follow" :onclick (str "follow(this,'" username "');")} "#" (if following? "Unfollow" "Follow")))

(defn details-follow-link [logged-in-user user]
  (if (and (not (nil? logged-in-user)) (not= (:_id user) (:_id logged-in-user)))
    (if (db/follow-pending? (:_id logged-in-user) (:_id user))
      "Pending"
      (follow-link (:username user) (db/following? (:_id logged-in-user) (:_id user))))
    (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
      (link-to "/settings" "Settings"))))

(defn following-link [logged-in-user user]
  (if (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
    (link-to "/following" "Following")
    "Following"))

(defn meta-link [logged-in-user user]
  (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
    [:tr [:th "Drafts"] [:td 0] [:th (link-to "/followers/pending" "Pending Followers")] [:td (db/pending-followers-count (:_id user))]]))

(defn profile-details [logged-in-user user]
  (list
    [:h2 (:username user)]
    [:div.c
      (image {:id "portrait"} (if-not (nil? (:image_id user)) (str "/img/" (:image_id user) ".jpg") "/img/anon.png"))
      [:table#profile
        [:tr [:td {:colspan 4 :style "text-align: right;"} (details-follow-link logged-in-user user)]]
        [:tr [:th "User"] [:td (:username user)] [:th "Followers"] [:td (db/followers-count (:_id user))]]
        [:tr [:th "Joined"] [:td (format-date (:created user))] [:th (following-link logged-in-user user)] [:td (db/following-count (:_id user))]]
        (meta-link logged-in-user user)]
      (when-let [bio (:bio (db/get-user-bio (:_id user)))]
        [:div#profile-bio bio])
      [:div.clear]]))

(defn post-count-message [id]
  (let [count (db/daily-items-count id) remaining (- 7 count)]
    (if (= remaining 0)
      "You used all your items today! If you create another item today, it will be saved as a draft."
      (str "You can post " remaining " more item" (when (not= remaining 1) "s") " today."))))

(defn own-profile? [logged-in-user user]
  (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user))))

(defn profile-publish [logged-in-user user]
  (when (own-profile? logged-in-user user)
    (list
      [:h2 "Publish"]
      [:div.c
       [:p (post-count-message (:_id user))]
       [:p "You can enter a title, body, link, and/or image. Each field is optional, but you need to enter at least one."]
       (form-to {:id "publish" :enctype "multipart/form-data"} [:post "/publish"]
                [:p
                 (label :title "Title")
                 [:br]
                 (text-field {:maxlength 256} :title)]
                [:p.right
                 (label :raw "Raw HTML editor")
                 (check-box {:onchange "toggleTinyMce(this, 'body');"} :raw)]
                [:p
                 (label :body "Body")
                 [:br]
                 (text-area {:maxlength 4096} :body)]
                [:p
                 (label :link "Link")
                 [:br]
                 (text-field {:maxlength 2048} :link)]
                [:p.right
                 (check-box :public true)
                 (label {:class "after-input"} :public "Public")
                 (check-box :comments true)
                 (label {:class "after-input"} :comments "Allow comments")]
                [:p
                 (label :img "Image")
                 (file-upload :img)]
                [:p.right (submit-button "Post")]
                [:div.clear])])))

(defn publish [user title body link file public comments]
  (when-not (and (string/blank? title) (string/blank? body)(string/blank? link) (empty? (:filename file)))
    (db/add-item {:user_id (:_id user)
                   :image_id (image/save-image file user)
                   :title (if (string/blank? title) nil title)
                   :body (if (string/blank? body) nil body)
                   :link (if (string/blank? link) nil link)
                   :comments (boolean comments)
                   :public (boolean public)}))
  (redirect (str "/u/" (:username user))))

; not sure if better to query for privacy and maybe not run items query
; or include privacy in the items query
(defn is-feed-public? [logged-in-user user]
  (let [items (:items (db/get-user-privacy (:_id user)))]
    (or (nil? items) items)))

(defn profile-feed [logged-in-user user]
  ;(when (or (is-feed-public? logged-in-user user) (db/following? (:_id logged-in-user) (:_id user)))
  (map item/format-item (db/get-users-items (:_id user) (:_id logged-in-user) 0 100)))

(defn profile [logged-in-user username]
  (let [user (db/find-user username)]
    (layout/common
      (profile-details logged-in-user user)
      (profile-publish logged-in-user user)
      (profile-feed logged-in-user user))))

(defn feed [user]
  (layout/common
    (map item/format-item (db/get-follows-items (:_id user) 0 100))))

(defn format-follow [f]
  (let [username (:username f)]
    [:tr [:td (link-to (str "/u/" username) username)]
         [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
         [:td (follow-link username true)]]))

(defn format-pending-follow [f]
  (let [username (:username f)]
    [:tr [:td (link-to (str "/u/" username) username)]
         [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
         [:td (link-to {:id "approve" :onclick (str "approve(this,'" username "');")} "#" "Approve")]
         [:td (link-to {:id "deny" :onclick (str "deny(this,'" username "');")} "#" "Deny")]]))

(defn following [user]
  (let [title "Following" records (db/get-follows (:_id user))]
    (if (empty? records)
      (layout/simple title [:p "You aren't following anyone! Find some people to follow."])
      (layout/simple title [:table (map format-follow records)]))))

(defn follow [logged-in-user username follow?]
  (when-let [user (db/find-user username)]
    (if follow?
      (if (:items_public user)
        (db/follow (:_id logged-in-user) (:_id user))
        (db/request-follow (:_id logged-in-user) (:_id user)))
      (db/unfollow (:_id logged-in-user) (:_id user)))))

(defn approve [logged-in-user username approve?]
  (when-let [user (db/find-user username)]
    (if approve?
      (db/approve-follow (:_id logged-in-user) (:_id user))
      (db/deny-follow (:_id logged-in-user) (:_id user)))))

(defn pending-followers [user]
  (let [title "Following" records (db/get-pending-followers (:_id user))]
    (if (empty? records)
      (layout/simple title [:p "You have no followers pending your approval."])
      (layout/simple title [:table (map format-pending-follow records)]))))

(defroutes user-routes
  (context "/u/:username" [username]
    (GET "/" [] (profile (user/get-user) username))
    (POST "/follow" [] (restricted (follow (user/get-user) username true)))
    (POST "/unfollow" [] (restricted (follow (user/get-user) username false)))
    (POST "/approve" [] (restricted (approve (user/get-user) username true)))
    (POST "/deny" [] (restricted (approve (user/get-user) username false))))
  (POST "/publish" [title body link img public comments] (restricted (publish (user/get-user) title body link img public comments)))
  (GET "/feed" [] (restricted (feed (user/get-user))))
  (GET "/following" [] (restricted (following (user/get-user))))
  (GET "/followers/pending" [] (restricted (pending-followers (user/get-user)))))
