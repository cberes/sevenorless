(ns sevenorless.routes.settings
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [hiccup.element :refer [image link-to]]
            [hiccup.form :refer [form-to label text-area text-field password-field file-upload hidden-field check-box radio-button submit-button]]
            [hiccup.util :refer [url-encode]]
            [noir.io :refer [upload-file resource-path]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [buddy.hashers :as crypt]
            [noir.util.route :refer [restricted]]
            [noir.validation :refer [rule errors? has-value? is-email? matches-regex? not-nil? on-error]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.image :as image]
            [sevenorless.models.item :as item]
            [sevenorless.models.user :as user])
  (:import [java.io File]))

(def title "Settings")

(defn control [field name text & [attrs]]
  (list [:tr
         [:th (label name text)]
         [:td (field (if attrs attrs {}) name)]
         [:td.error (on-error name first)]]))

(defn layout-settings-x [action attrs & body]
  (layout/common
    [:h2 title]
    [:div.c
     [:table {:id "settings-nav"}
      [:tr
       [:td (link-to "/settings" "Profile")]
       [:td (link-to "/settings/account" "Account")]
       [:td (link-to "/settings/privacy" "Privacy")]]]]
    (form-to attrs [:post action]
                   body
                   [:div.c (submit-button "Save settings")])))

(defn layout-settings [action & body]
  (layout-settings-x action {} body))

(defn account-settings [user]
  (layout-settings "/settings/account"
    [:div.c
     [:table
       [:tr [:td {:colspan 3} "Change your username. Your username is currently " [:strong (:username user)]]]
       (control text-field :username "New username" {:maxlength 20})
       [:tr [:td {:colspan 3} "Change your email address. Your email address currrently on file is " [:strong (:email user)]]]
       (control text-field :email "New email" {:maxlength 2048})
       [:tr [:td {:colspan 3} "Change your password. You must enter your current password as well."]]
       (control password-field :old-pass "Current password")
       (control password-field :pass "New password")
       (control password-field :pass1 "Confirm new password")]]))

(defn profile-settings [user]
  (layout-settings-x "/settings" {:enctype "multipart/form-data"}
    [:div.c
     [:p (label :portrait "Change your portrait, or remove your portrait to use a default one.")]
     [:p (file-upload {:accept "image/*"} :portrait)]
     [:p (label :default-portrait "Use a default portrait") (check-box :default-portrait)]
     [:p (on-error :portrait first)]]
    [:div.c
     [:p (label :bio "Enter a bio that will be displayed on your profile.")]
     [:p (text-area {:maxlength 512} :bio (:bio (db/get-user-bio (:_id user))))]
     [:p.right (label :raw "Raw HTML editor")
               (check-box {:onchange "toggleTinyMce(this, 'bio');"} :raw)]
     [:p (on-error :bio first)]
     [:div.clear ]]))

(defn privacy-settings [user]
  (let [privacy (db/get-user-privacy (:_id user))]
  (layout-settings "/settings/privacy"
    [:div.c
     [:table
       [:tr [:td {:colspan 3} "Choose what parts of your profile are public."]]
       [:tr [:th "Items"]
            [:td [:label (radio-button :items (or (nil? privacy) (:items privacy)) "public") "public"]
                 [:label (radio-button :items (not (or (nil? privacy) (:items privacy))) "private") "private"]]
            [:td.error (on-error :items first)]]]])))

(defn handle-account-settings [user username email pass pass1 old-pass]
  ; validation
  (when-not (string/blank? pass)
    (user/validate-password pass pass1)
    (rule (and (has-value? old-pass) (crypt/check old-pass (:password user)))
          [:old-pass "invalid password"]))
  (when-not (string/blank? username)
    (user/validate-username username (:_id user)))
  (when-not (string/blank? email)
    (user/validate-email email))
  ; update user
  (when-not (errors? :username :email :pass :pass1 :old-pass)
    (when-not (string/blank? email)
      (user/send-verify-email user (db/update-user-email (:_id user) email)))
    (when-not (string/blank? username)
      (db/update-user (:_id user) {:username username}))
    (when-not (string/blank? pass)
      (db/update-user (:_id user) {:password (crypt/encrypt pass)})))
  (account-settings user))

(defn handle-profile-settings [user bio-text {:keys [filename] :as portrait} default-portrait]
  (let [id (:_id user) bio (if-not (string/blank? bio-text) bio-text nil) bio-row {:bio bio}]
    ; delete current portrait
    (when (or (not (empty? filename)) default-portrait)
      (db/delete-user-portrait id)
      (image/delete-image (:image_id user)))
    ; save new portrait
    (if-let [image-id (image/save-image portrait user)]
      (db/add-user-portrait {:user_id id :image_id image-id}))
    ; save bio
    (if-not (nil? (db/get-user-bio id))
      (if-not (nil? bio)
        (db/update-user-bio id bio-row)
        (db/delete-user-bio id))
      (when-not (nil? bio) (db/add-user-bio (assoc bio-row :user_id id)))))
  (profile-settings user))

(defn handle-privacy-settings [user items]
  (let [id (:_id user) privacy {:items (= "public" items)}]
    (if-not (nil? (db/get-user-privacy id))
      (db/update-user-privacy id privacy)
      (db/add-user-privacy (assoc privacy :user_id id))))
  (privacy-settings user))

(defroutes settings-routes
  (context "/settings" []
    (GET "/" [] (restricted (profile-settings (user/get-user)))) ; portrait
    (POST "/" [bio portrait default-portrait] (restricted (handle-profile-settings (user/get-user) bio portrait default-portrait)))
    (GET "/privacy" [] (restricted (privacy-settings (user/get-user))))
    (POST "/privacy" [items] (restricted (handle-privacy-settings (user/get-user) items)))
    (GET "/account" [] (restricted (account-settings (user/get-user)))) ; password, email, privacy
    (POST "/account" [username email pass pass1 old-pass] (restricted (handle-account-settings (user/get-user) username email pass pass1 old-pass)))))
