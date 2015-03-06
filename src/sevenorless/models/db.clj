(ns sevenorless.models.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as sql]
            [jdbc.pool.c3p0 :as pool]
            [noir.util.crypt :as crypt]
            [clojure.string :as str])
  (:import java.sql.DriverManager))

(def db (pool/make-datasource-spec
          {:subprotocol "postgresql"
           :subname "//localhost/sevenorless"
           :user "sevenorless"
           :password "winnie"}))

;; TODO function in clojure to generate random string?
(def alphanumeric "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")
(defn get-random-id [length]
  (loop [acc []]
    (if (= (count acc) length) (apply str acc)
      (recur (conj acc (rand-nth alphanumeric))))))

(defn add-user [user]
  (sql/insert! db :web_user user))

(defn update-user [id updates]
  (sql/update! db :web_user updates ["_id = ?" id]))

(defn get-user [id]
  (sql/query db
    ["select u.*, p.image_id, COALESCE(y.items, TRUE) as items_public from web_user u
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      where u._id = ?" id]
    :result-set-fn first))

(defn find-user [username]
  (sql/query db
    ["select u.*, p.image_id, COALESCE(y.items, TRUE) as items_public from web_user u
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      where u.username = ?" username]
    :result-set-fn first))

(defn find-user-by-email [email]
  (sql/query db
    ["select u.*, p.image_id, COALESCE(y.items, TRUE) as items_public from web_user u
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      where u.email = ?" email]
    :result-set-fn first))

(defn compare-user-token [id token]
  (crypt/compare token (:token
    (sql/query db
      ["select token from user_token where user_id = ? and created >= (NOW() - INTERVAL '2 weeks') limit 1" id]
      :result-set-fn first))))

(defn compare-user-password-reset [id token]
  (crypt/compare token (:token
    (sql/query db
      ["select token from user_password_reset where user_id = ? and created >= (NOW() - INTERVAL '2 weeks') limit 1" id]
      :result-set-fn first))))

(defn compare-user-email-verify [id token]
  (crypt/compare token (:token
    (sql/query db
      ["select token from user_email_verify where user_id = ? limit 1" id]
      :result-set-fn first))))

(defn create-email-verify-record [user]
  (let [id (:_id user) token (get-random-id 32)]
    (sql/delete! db :user_email_verify ["user_id = ?" id])
    (sql/insert! db :user_email_verify {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

(defn verify-email [secret]
  (let [{id 0, token 1} (str/split secret #":" 2) id (Integer/parseInt id)]
    (when (compare-user-email-verify id token)
      (sql/delete! db :user_email_verify ["user_id = ?" id])
      (update-user id {:activated (quot (System/currentTimeMillis) 1000)})
      (get-user id))))

(defn update-user-email [id email]
  (sql/update! db :web_user {:email email :activated nil} ["_id = ?" id])
  (create-email-verify-record (get-user id)))

(defn delete-password-reset-record [user]
  (sql/delete! db :user_password_reset ["user_id = ?" (:_id user)]))

(defn create-password-reset-record [user]
  (delete-password-reset-record user)
  (let [id (:_id user) token (get-random-id 32)]
    (sql/insert! db :user_password_reset {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-password-reset-user [secret]
  (let [{id 0, token 1} (str/split secret #":" 2) id (Integer/parseInt id)]
    (when (compare-user-password-reset id token) (get-user id))))

;; returns cookie value
(defn remember-user [user]
  (let [id (:_id user) token (get-random-id 32)]
    (sql/delete! db :user_token ["user_id = ?" id])
    (sql/insert! db :user_token {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-remembered-user [cookie-value]
  (when (not (nil? cookie-value))
    (let [{id 0, token 1} (str/split cookie-value #":" 2) id (Integer/parseInt id)]
      (when (compare-user-token id token) (get-user id)))))

(defn get-follows [id]
  (sql/query db
    ["select u._id, u.username, f.created from web_user u join follow f on f.followed_id = u._id where f.user_id = ? order by u.username" id]
    :result-set-fn doall))

(defn get-pending-followers [id]
  (sql/query db
    ["select u._id, u.username, f.created
      from web_user u
      join pending_follow f on f.user_id = u._id
      where f.followed_id = ? and f.approved is null
      order by date_trunc('day', f.created) desc, u.username" id]
    :result-set-fn doall))

(defn followers-count [id]
  (sql/query db
    ["select COALESCE(count(*), 0) as count from follow where followed_id = ?" id]
    :result-set-fn first))

(defn pending-followers-count [id]
  (sql/query db
    ["select COALESCE(count(*), 0) as count from pending_follow where followed_id = ? and approved is null" id]
    :result-set-fn first))

(defn following-count [id]
  (sql/query db
    ["select COALESCE(count(*), 0) as count from follow where user_id = ?" id]
    :result-set-fn first))

(defn following? [id other-id]
  (sql/query db
    ["select u._id, u.username, f.created from web_user u join follow f on f.followed_id = u._id where f.user_id = ? and f.followed_id = ?" id other-id]
    :result-set-fn first))

(defn follow [id other-id]
  (when (and (not= id other-id) (not (following? id other-id)))
    (sql/delete! db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/insert! db :follow {:user_id id :followed_id other-id})))

(defn unfollow [id other-id]
  (when (not= id other-id)
    (sql/delete! db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/delete! db :follow ["user_id = ? and followed_id = ?" id other-id])))

; leave in 'pending' state for a month before a user can request to follow again
; called as user who wants to follow another user
(defn follow-pending? [id other-id]
  (sql/query db
    ["select u._id, u.username, f.created from pending_follow where user_id = ? and followed_id = ? and (approved is null or created >= (NOW() - INTERVAL '1 month'))" id other-id]
    :result-set-fn first))

; called as user who wants to follow another user
(defn request-follow [id other-id]
  (when (and (not= id other-id) (not (following? id other-id)) (not (follow-pending? id other-id)))
    (sql/delete! db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/insert! db :pending_follow {:user_id id :followed_id other-id})))

; called as user who is being followed
(defn approve-follow [id other-id]
  (when (and (not= id other-id) (not (following? other-id id)))
    (sql/delete! db :pending_follow ["user_id = ? and followed_id = ?" other-id id])
    (sql/insert! db :follow {:user_id other-id :followed_id id})))

; called as user who is being followed
(defn deny-follow [id other-id]
  (when (and (not= id other-id))
    (sql/update! db :pending_follow ["user_id = ? and followed_id = ?" other-id id] {:approved false})))

(defn daily-items-count [id]
  (sql/query db
    ["select COALESCE(count(*), 0) as count from item where user_id = ? and created >= CURRENT_DATE" id]
    :result-set-fn first))

(defn add-image [image]
  (sql/insert! db :image image))

(defn get-image [id]
  (sql/query db
    ["select * from image where _id = ?" id]
    :result-set-fn first))

(defn delete-image [id]
  (sql/delete! db :image ["_id = ?" id]))

(defn add-item [item]
  (sql/insert! db :item item))

(defn get-items [user-id offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id,
      rank() OVER (PARTITION BY date_trunc('day', i.created) ORDER BY i.created DESC, i._id ASC)
      from item i
      join web_user u on i.user_id = u._id
      left outer join user_privacy p on p.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      left outer join follow f on f.followed_id = u._id and f.user_id = ?
      where (i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = ?
      order by i.created desc, i._id asc" user-id user-id]
    :result-set-fn doall))

; posts by those who user follows
; privacy doesn't matter
(defn get-follows-items [user-id offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id,
      rank() OVER (PARTITION BY date_trunc('day', i.created) ORDER BY i.created DESC, i._id ASC)
      from item i
      join web_user u on i.user_id = u._id
      join follow f on f.followed_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      where f.user_id = ?
      order by i.created desc, i._id asc" user-id]
    :result-set-fn doall))

; if user's privacy setting for posts is private, query will not be run if user is not privileged
; so consider only per-post privacy
(defn get-users-items [user-id current-user-id offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id,
      rank() OVER (PARTITION BY date_trunc('day', i.created) ORDER BY i.created DESC, i._id ASC)
      from item i
      join web_user u on i.user_id = u._id
      left outer join user_privacy p on p.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      left outer join follow f on f.followed_id = i.user_id and f.user_id = ?
      where u._id = ? and ((i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = ?)
      order by i.created desc, i._id asc" current-user-id user-id current-user-id]
    :result-set-fn doall))

(defn get-user-privacy [id]
  (sql/query db
    ["select * from user_privacy where user_id = ?" id]
    :result-set-fn first))

(defn add-user-privacy [privacy]
  (sql/insert! db :user_privacy privacy))

(defn update-user-privacy [id updates]
  (sql/update! db :user_privacy updates ["user_id = ?" id]))

(defn get-user-bio [id]
  (sql/query db
    ["select bio from user_bio where user_id = ?" id]
    :result-set-fn first))

(defn add-user-bio [bio]
  (sql/insert! db :user_bio bio))

(defn update-user-bio [id updates]
  (sql/update! db :user_bio updates ["user_id = ?" id]))

(defn delete-user-bio [id]
  (sql/delete! db :user_bio ["user_id = ?" id]))

(defn add-user-portrait [portrait]
  (sql/insert! db :user_portrait portrait))

(defn delete-user-portrait [id]
  (sql/delete! db :user_portrait ["user_id = ?" id]))
