(ns sevenorless.routes.user
  (:require [compojure.core :refer :all]
            [noir.session :as session]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user]))

(defn profile-details [user logged-in-user]
  [:h2 (:username user)]
  [:div.c "TODO"])

(defn profile-publish [user logged-in-user]
  (when (= (:id user) (:id logged-in-user))
	  [:h2 "Publish"]
	  [:div.c "TODO"]))

; TODO 
(defn profile-feed [user logged-in-user]
  [:h2 "Today"]
  [:div.c " "]
  [:div.c " "]
  [:div.c " "])

(defn profile [username]
  (let [user (db/find-user username) logged-in (user/get-user)]
	  (layout/common
	    (profile-details user logged-in)
	    (profile-publish user logged-in)
	    (profile-feed user logged-in))))

; TODO
(defn feed [username]
  (let [user (db/find-user username) logged-in (user/get-user)]
	  (layout/common
	    [:h2 "Today"]
		  [:div.c " "]
		  [:div.c " "]
		  [:div.c " "])))

; TODO
(defn follows [username]
  (let [user (db/find-user username) logged-in (user/get-user)]
	  (layout/common
	    [:h2 "Today"]
		  [:div.c " "]
		  [:div.c " "]
		  [:div.c " "])))

; TODO
(defn settings [username]
  (let [user (db/find-user username) logged-in (user/get-user)]
	  (layout/common
	    [:h2 "Today"]
		  [:div.c " "]
		  [:div.c " "]
		  [:div.c " "])))

(defroutes user-routes
  (context "/u/:username" [username]
	  (GET "/" [] (profile username))
	  (GET "/feed" [] (feed username))
	  (GET "/follows" [] (follows username))
	  (GET "/settings" [] (settings username))))
