(ns sevenorless.routes.user
  (:require [compojure.core :refer :all]
            [noir.session :as session]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user]))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. "MMMM dd, yyyy") date))

(defn profile-details [logged-in-user user]
  (list
    [:h2 (:username user)]
	  [:div.c
	   [:table
	    [:tr [:th "User"] [:td (:username user)] [:th "Followers"] [:td 0]]
	    [:tr [:th "Joined"] [:td (format-date (:created user))] [:th "Follows"] [:td 0]]]]))

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

; TODO
(defn follows [user]
  (layout/common
    [:h2 "Today"]
	  [:div.c " "]
	  [:div.c " "]
	  [:div.c " "]))

; TODO
(defn settings [user]
  (layout/common
    [:h2 "Today"]
	  [:div.c " "]
	  [:div.c " "]
	  [:div.c " "]))

(defroutes user-routes
  (GET "/u/:username" [username] (profile (user/get-user) username))
  (GET "/feed" [] (feed (user/get-user)))
  (GET "/follows" [] (follows (user/get-user)))
  (GET "/settings" [] (settings (user/get-user))))
