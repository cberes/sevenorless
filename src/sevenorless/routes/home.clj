(ns sevenorless.routes.home
  (:require [compojure.core :refer :all]
            [sevenorless.views.layout :as layout]))

(defn home []
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
    [:div.c " "]))

(defroutes home-routes
  (GET "/" [] (home)))
