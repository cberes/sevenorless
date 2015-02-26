(ns sevenorless.routes.auth
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer [form-to label text-field password-field hidden-field check-box submit-button]]
            [noir.cookies :as cookies]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.crypt :as crypt]
            [noir.util.route :refer [restricted]]
            [noir.validation :refer [rule errors? has-value? is-email? matches-regex? not-nil? on-error]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user]))

;; 2 weeks (in seconds)
(def cookie-max-age (* 14 86400))

(defn format-error [[error]]
  [:p.error error])

(defn control [field name text & [attrs]]
  (list [:tr
         [:th (label name text)]
         [:td (field (if attrs attrs {}) name)]
         [:td.error (on-error name first)]]))

(defn registration-page []
  (layout/simple "Sign up"
	  (form-to [:post "/register"]
	           [:table
	            (control text-field :username "Username" {:maxlength 20})
	            (control text-field :email "Email" {:maxlength 2048})
	            (control password-field :pass "Password")
	            (control password-field :pass1 "Confirm password")
              [:td {:colspan 3} "By creating an account, you acknowledge that you have read and agree to our "
                                (link-to "/policy" "terms of service")
                                ". They're short, take a minute to read them."]
	            [:tr [:th] [:td (submit-button "Create account")] [:td]]])))

(defn login-page []
  (layout/simple "Log in"
    (form-to [:post "/login"]
             [:table
              (control text-field :username "Username" {:maxlength 20})
              (control password-field :pass "Password")
              (control check-box :remember "Remember me")
              [:tr [:th] [:td (submit-button "Log in")] [:td]]])
    [:p {:style "text-align: center;"} (link-to "/forgot-password" "Forgot your password?")]))

(defn forgot-password-page []
  (layout/simple "Password reset"
    (form-to [:post "/forgot-password"]
             [:table
              (control text-field :id "Password or email" {:maxlength 2048})
              [:tr [:th] [:td (submit-button "Submit")] [:td]]])))

(defn password-reset-page [q]
  (layout/simple "Password reset"
    [:p {:style "text-align: center;"} "Enter a new password."]
    (form-to [:post "/password-reset"]
             (hidden-field "secret" q) 
             [:table
	            (control password-field :pass "Password")
	            (control password-field :pass1 "Confirm password")
              [:tr [:th] [:td (submit-button "Change password")] [:td]]])))

;; TODO
(defn send-password-reset-email [user token]
  (println user token))

;; TODO
(defn send-verify-email [user token]
  (println user token))

(defn validate-password [pass pass1]
  (rule (has-value? pass)
        [:pass "password is required"])
  (rule (= pass pass1)
        [:pass1 "password was not typed correctly"]))

(defn handle-password-reset [secret pass pass1]
  (let [user (db/get-password-reset-user secret)]
    (validate-password pass pass1)
    (rule (or (errors?) (not-nil? user))
          [:pass "password request is invalid"])
	  (if (errors? :pass :pass1)
	    (password-reset-page secret)
	    (do
	     (db/update-user (:_id user) {:password (crypt/encrypt pass)})
       (db/delete-password-reset-record user)
       ;; TODO tell user that password was reset?
	     (redirect "/login")))))

(defn send-password-reset-request [user]
  (rule (not-nil? user)
        [:id "user was not found"])
  (if (errors? :id)
    (forgot-password-page)
    (do
      (send-password-reset-email user (db/create-password-reset-record user))
      (layout/simple "Password reset" [:p "We've sent an email to you to let you reset your password."]))))

(defn handle-password-reset-request [id]
  (if-let [user (db/find-user id)]
    (send-password-reset-request user)
    (send-password-reset-request (db/find-user-by-email id))))

(defn handle-registration [username email pass pass1]
  (validate-password pass pass1)
  (rule (has-value? username)
        [:username "username is required"])
  (rule (or (not (has-value? username)) (matches-regex? username #".{4,25}"))
        [:username "username must be between 4 and 25 characters long"])
  (rule (or (not (has-value? username)) (matches-regex? username #"^[-a-zA-Z0-9]+$"))
        [:username "username can have only letters, digits, and dashes"])
;  (rule (or (not (has-value? username)) (matches-regex? username #"[a-zA-Z]"))
;        [:username "username must have at least one letter"])
  (rule (or (not (has-value? username)) (matches-regex? username #"^[^-].+[^-]$"))
        [:username "username cannot begin or end with a dash"])
  (rule (is-email? email)
        [:email "valid email address is required"])
  (rule (or (errors?) (not (has-value? username)) (not (db/find-user username)))
        [:username "this username is taken"])
  (rule (or (errors?) (not (is-email? email)) (not (db/find-user-by-email email)))
        [:email "this email address is already registered"])
  (if (errors? :username :email :pass :pass1)
    (registration-page)
    (do
      (db/add-user {:username username :email email :password (crypt/encrypt pass)})
      ; TODO kind of stupid that we have to query for the user right away
      (let [user (db/find-user username)]
        (send-verify-email user (db/create-email-verify-record user))
        (session/put! :user (:_id user))
        (redirect (str "/u/" username))))))

(defn handle-login [username pass remember]
  (let [user (db/find-user username)]
    (rule (has-value? username)
          [:username "username is required"])
    (rule (has-value? pass)
          [:pass "password is required"])
    (rule (and user (crypt/compare pass (:password user)))
          [:pass "invalid password"])
    (if (errors? :username :pass)
      (login-page)
      (do
       (when remember (cookies/put! :remember {:value (db/remember-user user) :max-age cookie-max-age}))
       (session/put! :user (:_id user))
       (redirect "/")))))

(defn handle-email-verify [q]
  (let [title "Verify email"]
	  (if-let [user (db/verify-email q)]
	    (layout/simple title [:p "Success! Your email address was verified."])
	    (layout/simple title [:p "Uh oh, we could not verify your email address. Is it already verified?"]))))

(defmacro enforce-logged-out [& body]
  `(if-not (nil? (user/get-user)) (redirect "/") (do ~@body)))

;; underscore indicates that an argument is ignored
(defroutes auth-routes
  (GET "/register" [_] (enforce-logged-out (registration-page)))
  (POST "/register" [username email pass pass1]
        (enforce-logged-out (handle-registration username email pass pass1)))
  (GET "/login" [] (enforce-logged-out (login-page)))
  (POST "/login" [username pass remember]
        (enforce-logged-out (handle-login username pass remember)))
  (GET "/logout" []
       (restricted (layout/simple "Log out"
                    [:p "Click the button to log out."]
                    (form-to [:post "/logout"]
                             (submit-button "Log out")))))
  (POST "/logout" []
        (restricted (cookies/put! :remember {:value "" :max-age 0})
                    (session/clear!)
                    (redirect "/")))
  (GET "/forgot-password" [] (enforce-logged-out (forgot-password-page)))
  (POST "/forgot-password" [id]
        (enforce-logged-out (handle-password-reset-request id)))
  (GET "/password-reset" [q] (enforce-logged-out (password-reset-page q)))
  (POST "/password-reset" [secret pass pass1]
        (enforce-logged-out (handle-password-reset secret pass pass1)))
  (GET "/verify-email" [q] (handle-email-verify q))
  )

