(ns sevenorless.routes.auth
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer [form-to label text-field password-field hidden-field check-box submit-button]]
            [noir.cookies :as cookies]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [buddy.hashers :as crypt]
            [noir.util.route :refer [restricted]]
            [noir.validation :refer [rule errors? has-value? is-email? matches-regex? not-nil? on-error]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.user :as user]))

;; 2 weeks (in seconds)
(def cookie-max-age (* 14 86400))

;; Default timezone
(def default-tz "America/New_York")

(def use-captcha?
  (delay (Boolean/parseBoolean (System/getenv "SIOL_USE_CAPTCHA"))))

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
              [:tr
               [:th " "]
               [:td
                (when @use-captcha? [:div.g-recaptcha {:data-sitekey "6LeHigMTAAAAAGQUrmT1Yj-VYzXwyyHVKlM0r8NQ"}])]
               [:td.error {:colspan 2} (on-error :captcha first)]]
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
              (control text-field :id "Username or email" {:maxlength 2048})
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

(defn handle-password-reset [secret pass pass1]
  (let [user (db/get-password-reset-user secret)]
    (user/validate-password pass pass1)
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
      (db/create-password-reset-record user)
      (layout/simple "Password reset" [:p "We've sent an email to you to let you reset your password."]))))

(defn handle-password-reset-request [id]
  (if-let [user (db/find-user id)]
    (send-password-reset-request user)
    (send-password-reset-request (db/find-user-by-email id))))

(defn send-captcha-request [response remoteip]
  (client/post "https://www.google.com/recaptcha/api/siteverify"
    {:form-params {:secret "6LeHigMTAAAAAFU-v1rPbDNuHQZe9an711uwYL_v"
                   :response response
                   :remoteip remoteip}
     :accept :json}))

(defn human? [response remoteip]
  (or (not @use-captcha?)
      (try
        (:success (json/read-str (:body (send-captcha-request response remoteip)) :key-fn keyword))
        (catch Exception e (prn "caught" e)))))

(defn handle-registration [username email pass pass1 g-recaptcha-response remoteip]
  (rule (has-value? username) [:username "username is required"])
  (rule (human? g-recaptcha-response remoteip) [:captcha "verification failed"])
  (user/validate-password pass pass1)
  (user/validate-username username 0)
  (user/validate-email email)
  (rule (< (db/users-count) 100) [:username "registration is closed,  please check back soon"])
  (if (errors? :username :email :pass :pass1 :captcha)
    (registration-page)
    (do
      (db/add-user {:username username :email email :password (crypt/encrypt pass) :tz default-tz})
      ; TODO kind of stupid that we have to query for the user right away
      (let [user (db/find-user username)]
        (db/create-email-verify-record user)
        (session/put! :user (:_id user))
        (redirect (str "/u/" username))))))

(defn handle-login [username pass remember]
  (let [user (db/find-user username)]
    (rule (has-value? username)
          [:username "username is required"])
    (rule (has-value? pass)
          [:pass "password is required"])
    (rule (and user (crypt/check pass (:password user)))
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
  (POST "/register" {:keys [remote-addr params] :as request} ;[username email pass pass1]
        (enforce-logged-out (handle-registration (:username params) (:email params) (:pass params) (:pass1 params) (:g-recaptcha-response params) remote-addr)))
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

