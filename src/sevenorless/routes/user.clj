(ns sevenorless.routes.user
  (:require [compojure.core :refer :all]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer [form-to label text-area text-field file-upload hidden-field check-box submit-button]]
            [noir.session :as session]
            [noir.util.route :refer [restricted]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user]))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. "MMMM dd, yyyy") date))

(defn follow-link [username following?]
  (link-to {:id "follow" :onclick (str "follow(this,'" username "');")} "#" (if following? "Unfollow" "Follow")))

(defn details-follow-link [logged-in-user user]
  (when (and (not (nil? logged-in-user)) (not= (:_id user) (:_id logged-in-user)))
    (follow-link (:username user) (db/following? (:_id logged-in-user) (:_id user)))))

(defn following-link [logged-in-user user]
  (if (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
    (link-to "/following" "Following")
    "Following"))

(defn profile-details [logged-in-user user]
  (list
    [:h2 (:username user)]
	  [:div.c
	   [:table
	    [:tr [:td {:colspan 2} "Portrait"] [:td {:colspan 2 :style "text-align: right;"} (details-follow-link logged-in-user user)]]
	    [:tr [:th "User"] [:td (:username user)] [:th "Followers"] [:td (:count (db/followers-count (:_id user)))]]
	    [:tr [:th "Joined"] [:td (format-date (:created user))] [:th (following-link logged-in-user user)] [:td (:count (db/following-count (:_id user)))]]]]))

(defn post-count-message [id]
  (let [count (:count (db/daily-items-count id)) remaining (- 7 count)]
    (if (= remaining 0)
      "You used all your items today! If you create another item today, it will be saved as a draft."
      (str "You can post " remaining " more item" (when (not= remaining 1) "s") " today."))))

(defn profile-publish [logged-in-user user]
  (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
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
                [:p
                 (label :image "Image")
                 (file-upload :image)
                 (submit-button "Post")])])))

(defn publish [user]
  (println user))

; TODO 
(defn profile-feed [logged-in-user user]
  (list
    [:h2 "Today"]
	  [:div.c " "]
	  [:div.c " "]
	  [:div.c " "]))

(defn profile [logged-in-user username]
  (let [user (db/find-user username)]
    (layout/common
      (profile-details logged-in-user user)
      (profile-publish logged-in-user user)
      (profile-feed logged-in-user user))))

; TODO
(defn feed [user]
  (layout/common
    [:h2 "Today"]
	  [:div.c " "]
	  [:div.c " "]
	  [:div.c " "]))

(defn format-follow [f]
  (let [username (:username f)]
    [:tr [:td (link-to (str "/u/" username) username)]
         [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
         [:td (follow-link username true)]]))

(defn following [user]
  (let [title "Following" records (db/get-follows (:_id user))]
    (if (empty? records)
      (layout/simple title [:p "You aren't following anyone! Find some people to follow."])
      (layout/simple title [:table (map format-follow records)]))))

; TODO
(defn settings [user]
  (layout/common
    [:h2 "Today"]
    [:div.c " "]
    [:div.c " "]
    [:div.c " "]))

(defn follow [logged-in-user username follow?]
  (let [user (db/find-user username)]
    (if follow?
      (db/follow (:_id logged-in-user) (:_id user))
      (db/unfollow (:_id logged-in-user) (:_id user)))))

(defroutes user-routes
  (context "/u/:username" [username]
    (GET "/" [] (profile (user/get-user) username))
    (POST "/follow" [] (restricted (follow (user/get-user) username true)))
    (POST "/unfollow" [] (restricted (follow (user/get-user) username false))))
  (POST "/publish" [] (restricted (publish (user/get-user))))
  (GET "/feed" [] (restricted (feed (user/get-user))))
  (GET "/following" [] (restricted (following (user/get-user))))
  (GET "/settings" [] (restricted (settings (user/get-user)))))
