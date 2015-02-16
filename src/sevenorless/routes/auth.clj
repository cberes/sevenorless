(ns sevenorless.routes.auth
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.form :refer [form-to label text-field password-field submit-button]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.crypt :as crypt]
            [noir.validation :refer [rule errors? has-value? is-email? on-error]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]))

(defn format-error [[error]]
  [:p.error error])

(defn control [field name text & [attrs]]
  (list [:tr
         [:th (label name text)]
         [:td (field (if attrs attrs {}) name)]
         [:td.error (on-error name first)]]))

(defn registration-page []
  (layout/common
    [:h2 "Sign up"]
    [:div.c
     (form-to [:post "/register"]
              [:table
               (control text-field :username "username" {:maxlength 20})
               (control text-field :email "email" {:maxlength 2048})
               (control password-field :pass "password")
               (control password-field :pass1 "confirm password")
               [:tr [:th] [:td (submit-button "create account")] [:td]]])]))

(defn login-page []
  (layout/common
    [:h2 "Login"]
    [:div.c
     (form-to [:post "/login"]
              [:table
               (control text-field :username "username" {:maxlength 20})
               (control password-field :pass "password")
               [:tr [:th] [:td (submit-button "login")] [:td]]])]))

(defn handle-registration [username email pass pass1]
  (rule (has-value? username)
        [:username "username is required"])
  (rule (is-email? email)
        [:email "valid email address is required"])
  (rule (has-value? pass)
        [:pass "password is required"])
  (rule (= pass pass1)
        [:pass1 "password was not typed correctly"])
  (rule (or (errors?) (not (has-value? username)) (not (db/find-user username)))
        [:username "this username is taken"])
  (rule (or (errors?) (not (is-email? email)) (not (db/find-user-by-email email)))
        [:email "this email address is already registered"])
  (if (errors? :username :email :pass :pass1)
    (registration-page)
    (do
      (db/add-user {:username username :email email :pass (crypt/encrypt pass)})
      (redirect "/login"))))

(defn handle-login [username pass]
  (let [user (db/find-user username)]
    (rule (has-value? username)
          [:username "username is required"])
    (rule (has-value? pass)
          [:pass "password is required"])
    (rule (and user (crypt/compare pass (:pass user)))
          [:pass "invalid password"])
    (if (errors? :username :pass)
      (login-page)
      (do
       (session/put! :user (:_id user))
       (redirect "/")))))

;; underscore indicates that an argument is ignored
(defroutes auth-routes
  (GET "/register" [_] (registration-page))
  (POST "/register" [username email pass pass1]
        (handle-registration username email pass pass1))
  (GET "/login" [] (login-page))
  (POST "/login" [username pass]
        (handle-login username pass))
  (GET "/logout" []
       (layout/common
        (form-to [:post "/logout"]
                 (submit-button "logout"))))
  (POST "/logout" [id pass]
        (session/clear!)
        (redirect "/")))
