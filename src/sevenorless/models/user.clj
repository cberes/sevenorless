(ns sevenorless.models.user
  (:require [noir.cookies :as cookies]
            [noir.session :as session]
            [sevenorless.models.db :as db]))

(def current-user (atom nil))

(defn get-user []
  (deref current-user))

(defn wrap-user-login [handler]
  (fn [request]
    (if-let [id (session/get :user)]
       (reset! current-user (db/get-user id))
       (if-let [user (db/get-remembered-user (cookies/get :remember))]
          (do
            (session/put! :user (:_id user))
            (reset! current-user user))
          (reset! current-user nil)))
    (handler request)))