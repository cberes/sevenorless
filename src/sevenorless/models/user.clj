(ns sevenorless.models.user
  (:require [clojure.string :as string]
            [noir.cookies :as cookies]
            [noir.session :as session]
            [noir.validation :refer [rule errors? has-value? is-email? matches-regex? not-nil? on-error]]
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

(defn validate-password [pass pass1]
  (rule (has-value? pass)
        [:pass "password is required"])
  (rule (= pass pass1)
        [:pass1 "password was not typed correctly"]))

(defn validate-username [username id]
  (rule (or (not (has-value? username)) (matches-regex? username #".{4,25}"))
        [:username "username must be between 4 and 25 characters long"])
  (rule (or (not (has-value? username)) (matches-regex? username #"^[-a-zA-Z0-9]+$"))
        [:username "username can have only letters, digits, and dashes"])
  (rule (or (not (has-value? username)) (matches-regex? username #"^[^-].+[^-]$"))
        [:username "username cannot begin or end with a dash"])
  (rule (or (errors?) (not (has-value? username)) (not (db/check-for-user username id)))
        [:username "this username is taken"]))

(defn validate-email [email]
  (rule (is-email? email)
        [:email "valid email address is required"])
  (rule (or (errors?) (not (is-email? email)) (not (db/find-user-by-email email)))
        [:email "this email address is already registered"]))