(ns sevenorless.routes.file
  (:require [compojure.core :refer :all]
            [ring.util.response :refer [file-response]]
            [sevenorless.models.db :as db]
            [sevenorless.models.image :as image])
  (:import [java.io File]))

(defn serve-image [id]
  (when-let [img (db/get-image (Integer/parseInt id))]
    (file-response (str (image/image-store-path) File/separator (:path img) "." (:ext img)))))

(defroutes file-routes
  (GET "/img/:id{[0-9]+}.:ext{gif|jpg|png}" [id] (serve-image id)))
