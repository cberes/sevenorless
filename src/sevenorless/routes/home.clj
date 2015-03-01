(ns sevenorless.routes.home
  (:require [compojure.core :refer :all]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.item :as item]
            [sevenorless.models.user :as user]))

(defn home []
  (layout/common
    (map item/format-item (db/get-items (:_id (user/get-user)) 0 100))))

(defroutes home-routes
  (GET "/" [] (home)))
