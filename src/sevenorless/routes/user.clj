(ns sevenorless.routes.user
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [hiccup.element :refer [image link-to]]
            [hiccup.form :refer [form-to label text-area text-field file-upload hidden-field check-box submit-button]]
            [hiccup.page :refer [html5]]
            [hiccup.util :refer [url-encode]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.route :refer [restricted]]
            [sevenorless.views.layout :as layout]
            [sevenorless.models.db :as db]
            [sevenorless.models.image :as image]
            [sevenorless.models.item :as item]
            [sevenorless.models.user :as user])
  (:import [java.io File]))

(defn format-date [date]
  (.format (java.text.SimpleDateFormat. "MMMM dd, yyyy") date))

(defn follow-link [username following?]
  (link-to {:id "follow" :onclick (str "follow(this,'" username "');")} "#" (if following? "Unfollow" "Follow")))

(defn details-follow-link [logged-in-user user]
  (if (and (not (nil? logged-in-user)) (not= (:_id user) (:_id logged-in-user)))
    (if (db/follow-pending? (:_id logged-in-user) (:_id user))
      "Pending"
      (follow-link (:username user) (db/following? (:_id logged-in-user) (:_id user))))
    (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
      (link-to "/settings" "Settings"))))

(defn following-link [logged-in-user user]
  (if (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
    (link-to "/following" "Following")
    "Following"))

(defn meta-link [logged-in-user user]
  (when (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user)))
    [:tr [:th "Drafts"] [:td 0] [:th (link-to "/followers/pending" "Pending Followers")] [:td (db/pending-followers-count (:_id user))]]))

(defn profile-details [logged-in-user user]
  (list
    [:h2 (:username user)]
    [:div.c
      (image {:id "portrait"} (if-not (nil? (:image_id user)) (str "/img/" (:image_id user) "." (:image_ext user)) "/img/anon.png"))
      [:table#profile
        [:tr [:td {:colspan 4 :style "text-align: right;"} (details-follow-link logged-in-user user)]]
        [:tr [:th "User"] [:td (:username user)] [:th "Followers"] [:td (db/followers-count (:_id user))]]
        [:tr [:th "Joined"] [:td (format-date (:created_local user))] [:th (following-link logged-in-user user)] [:td (db/following-count (:_id user))]]
        (meta-link logged-in-user user)]
      (when-let [bio (:bio (db/get-user-bio (:_id user)))]
        [:div#profile-bio bio])
      [:div.clear]]))

(defn post-count-message [id]
  (let [count (db/daily-items-count id) remaining (- 7 count)]
    (if (= remaining 0)
      "You used all your items today! If you create another item today, it will be saved as a draft."
      (str "You can post " remaining " more item" (when (not= remaining 1) "s") " today."))))

(defn own-profile? [logged-in-user user]
  (and (not (nil? logged-in-user)) (= (:_id user) (:_id logged-in-user))))

(defn profile-publish [logged-in-user user item-id]
  (when (own-profile? logged-in-user user)
    (let [item (when item-id (db/get-item (Integer/parseInt item-id) (:_id user)))]
      (list
        [:h2#publish (if item-id "Edit" "Publish")]
        [:div.c
         [:p (if item "You are editing an item." (post-count-message (:_id user)))]
         [:p "You can enter a title, body, link, and/or image. Each field is optional, but you need to enter at least one."]
         (form-to {:id "publish" :enctype "multipart/form-data"} [:post (if item-id (str "/edit/" item-id) "/publish")]
                  [:p
                   (label :title "Title")
                   [:br]
                   (text-field {:maxlength 256} :title (:title item))]
                  [:p.right
                   (label :raw "Raw HTML editor")
                   (check-box {:onchange "toggleTinyMce(this, 'body');"} :raw)]
                  [:p
                   (label :body "Body")
                   [:br]
                   (text-area {:maxlength 4096 :class "rich"} :body (:body item))]
                  [:p
                   (label :link "Link")
                   [:br]
                   (text-field {:maxlength 2048} :link (:link item))]
                  (when (:image_id item)
                    [:p.right
                     (check-box :no-image false)
                     (label {:class "after-input"} :no-image "Remove image")])
                  [:p
                   (label :img "Image")
                   [:br]
                   (when (:image_id item)
                     (image {:id "item-preview"} (str "/img/" (:image_id item) "." (:image_ext item))))
                   (file-upload {:accept "image/*"} :img)
                   [:div.clear]]
                  [:p.left
                   ; default checkbox to true
                   (check-box :public (if item (:public item) true))
                   (label {:class "after-input"} :public "Public")
                   ; default checkbox to true
                   (check-box :comments (if item (:comments item) true))
                   (label {:class "after-input"} :comments "Allow comments")]
                  [:p.right
                   (submit-button (if item "Update" "Post"))
                   (when item (link-to {:style "margin-left: 1em;"} (str "/u/" (:username user)) "Cancel"))]
                  [:div.clear])]))))

(defn publish [user id title body link file public comments no-image]
  (when-not (and (string/blank? title)
                 (string/blank? body)
                 (string/blank? link)
                 (empty? (:filename file)))
    (try
      (let [item-id (if id (Integer/parseInt id) nil)
            user-id (:_id user)
            item {:user_id user-id
                  :title (if (string/blank? title) nil title)
                  :body (if (string/blank? body) nil body)
                  :link (if (string/blank? link) nil link)
                  :comments (boolean comments)
                  :public (boolean public)}]
        (if item-id
          ; edit
          (when-let [current-item (db/get-item item-id user-id)]
            (do
              (db/update-item item-id (conj
                                        item
                                        {:image_id (if (seq (:filename file))
                                                     (image/save-image file user)
                                                     (when-not no-image (:image_id current-item)))}))
              (when (or no-image (seq (:filename file)))
                (db/delete-image (:image_id current-item) user-id))))
          ; publish
          (db/add-item (conj item {:image_id (image/save-image file user)}))))
      (catch Exception e
        (println (str "error " (if id "editing" "creating") " item: " (.getMessage e)))
        (.printStackTrace e))))
  (redirect (str "/u/" (:username user))))

; not sure if better to query for privacy and maybe not run items query
; or include privacy in the items query
(defn is-feed-public? [logged-in-user user]
  (let [items (:items (db/get-user-privacy (:_id user)))]
    (or (nil? items) items)))

(defn profile-feed [logged-in-user user]\
  (let [items (db/get-users-items (:_id user) (:_id logged-in-user) 0 100)]
    (if (empty? items)
      (list [:h2 "Feed"]
            [:div.c "There's nothing here!"])
      (map item/format-item items))))

(defn profile [logged-in-user username item-id]
  (if-let [user (db/find-user username (:_id logged-in-user))]
    (layout/common
      (profile-details logged-in-user user)
      (profile-publish logged-in-user user item-id)
      (profile-feed logged-in-user user))
    (redirect "/")))

(defn feed [user]
  (layout/common
    (let [items (db/get-follows-items (:_id user) 0 100)]
      (if (empty? items)
        (list [:h2 "My Feed"]
              [:div.c "Follow more people to see their posts here! Or see "
               (link-to "/" "everyone's posts")
               "."])
        (map item/format-item items)))))

(defn format-follow [f]
  (let [username (:username f)]
    [:tr [:td (link-to (str "/u/" username) username)]
         [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
         [:td (follow-link username true)]]))

(defn format-pending-follow [f]
  (let [username (:username f)]
    [:tr [:td (link-to (str "/u/" username) username)]
         [:td {:style "text-align: right;"} (str "Since " (format-date (:created f)))]
         [:td (link-to {:id "approve" :onclick (str "approve(this,'" username "');")} "#" "Approve")]
         [:td (link-to {:id "deny" :onclick (str "deny(this,'" username "');")} "#" "Deny")]]))

(defn following [user]
  (let [title "Following" records (db/get-follows (:_id user))]
    (if (empty? records)
      (layout/simple title [:p "You aren't following anyone! Find some people to follow."])
      (layout/simple title [:table (map format-follow records)]))))

(defn follow [logged-in-user username follow?]
  (when-let [user (db/find-user username)]
    (if follow?
      (if (:items_public user)
        (db/follow (:_id logged-in-user) (:_id user))
        (db/request-follow (:_id logged-in-user) (:_id user)))
      (db/unfollow (:_id logged-in-user) (:_id user)))))

(defn approve [logged-in-user username approve?]
  (when-let [user (db/find-user username)]
    (if approve?
      (db/approve-follow (:_id logged-in-user) (:_id user))
      (db/deny-follow (:_id logged-in-user) (:_id user)))))

(defn pending-followers [user]
  (let [title "Following" records (db/get-pending-followers (:_id user))]
    (if (empty? records)
      (layout/simple title [:p "You have no followers pending your approval."])
      (layout/simple title [:table (map format-pending-follow records)]))))

(defn comments [user id]
  (html5 (item/build-comments (db/get-comments (if (nil? user) 0 (:_id user)) (Integer/parseInt id)))))

(defn publish-comment [user id comment]
  (when-not (string/blank? comment)
    (let [item-id (Integer/parseInt id)]
      (db/add-comment {:user_id (:_id user) :item_id item-id :body comment})
      (html5 (item/build-comments (db/get-comments (:_id user) item-id))))))

(defroutes user-routes
  (context "/u/:username" [username]
    (GET "/" [item] (profile (user/get-user) username item))
    (POST "/follow" [] (restricted (follow (user/get-user) username true)))
    (POST "/unfollow" [] (restricted (follow (user/get-user) username false)))
    (POST "/approve" [] (restricted (approve (user/get-user) username true)))
    (POST "/deny" [] (restricted (approve (user/get-user) username false))))
  (POST "/publish" [title body link img public comments] (restricted (publish (user/get-user) nil title body link img public comments false)))
  (POST "/edit/:id" [id title body link img public comments no-image] (restricted (publish (user/get-user) id title body link img public comments no-image)))
  (GET "/feed" [] (restricted (feed (user/get-user))))
  (GET "/following" [] (restricted (following (user/get-user))))
  (GET "/followers/pending" [] (restricted (pending-followers (user/get-user))))
  (GET "/comments/:id" [id] (comments (user/get-user) id))
  (POST "/comments/:id" [id comment] (restricted (publish-comment (user/get-user) id comment)))
  (context "/i/:id" [id]
    (GET "/" [] (redirect "/"))
    (DELETE "/" [] (restricted (item/delete-item (Integer/parseInt id) (user/get-user))))))
