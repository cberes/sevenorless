(ns sevenorless.routes.user
  (:require [compojure.core :refer :all]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]))

(defn profile [username]
  (let [user (db/find-user username)]
	  (layout/common
	    [:h2 "Today"]
	    [:div.c " "]
	    [:div.c " "]
	    [:h2 "Yesterday"]
	    [:div.c " "]
	    [:div.c " "]
	    [:div.c " "]
	    [:h2 "Last week"]
	    [:div.c " "]
	    [:div.c " "]
	    [:div.c " "])))

(defroutes user-routes
  (GET "/u/:username" [username] (profile username)))
