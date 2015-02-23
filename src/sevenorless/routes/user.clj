(ns sevenorless.routes.user
  (:require [compojure.core :refer :all]
            [hiccup.element :refer [link-to]]
            [noir.session :as session]
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

(defn profile-publish [logged-in-user user]
  (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
	  (list
     [:h2 "Publish"]
     [:div.c "TODO"])))

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
  [:tr [:td (:username f)]
       [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
       [:td (follow-link (:username f) true)]])

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
	  (POST "/follow" [] (follow (user/get-user) username true))
	  (POST "/unfollow" [] (follow (user/get-user) username false)))
  (GET "/feed" [] (feed (user/get-user)))
  (GET "/following" [] (following (user/get-user)))
  (GET "/settings" [] (settings (user/get-user))))
