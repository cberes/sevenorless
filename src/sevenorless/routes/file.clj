(ns sevenorless.routes.file
  (:require [compojure.core :refer :all]
            [ring.util.response :refer [file-response]]
            [sevenorless.models.db :as db]
            [sevenorless.models.item :as item])
  (:import [java.io File]))

(defn serve-image [id]
  (when-let [img (db/get-image (Integer/parseInt id))]
    (file-response (str (item/image-store-path) File/separator (:path img)))))

(defroutes file-routes
  (GET "/img/:id{[0-9]+}.jpg" [id] (serve-image id)))
