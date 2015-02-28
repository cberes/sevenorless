(ns sevenorless.routes.settings
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [hiccup.element :refer [image link-to]]
            [hiccup.form :refer [form-to label text-area text-field file-upload hidden-field check-box submit-button]]
            [hiccup.util :refer [url-encode]]
            [noir.io :refer [upload-file resource-path]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.route :refer [restricted]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user])
  (:import [java.io File]))

(defn settings [user]
  (layout/common
    [:h2 "Today"]
    [:div.c " "]
    [:div.c " "]
    [:div.c " "]))

(defroutes settings-routes
  (context "/settings" []
    (GET "/" [] (restricted (settings (user/get-user)))) ; email, privacy
    (GET "/password" [] (restricted (settings (user/get-user)))) ; change password
    (GET "/profile" [] (restricted (settings (user/get-user)))))) ; change portrait
