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

(defn get-user-bio [id]
  (sql/query db
    ["select bio from user_bio where user_id = ?" id]
    :result-set-fn first))

(defn get-user [id]
  (sql/query db
    ["select u.*, p.image_id from web_user u
      left outer join user_portrait p on u._id = p.user_id
      where u._id = ?" id]
    :result-set-fn first))

(defn get-follows [id]
  (sql/query db
    ["select u._id, u.username, f.created from web_user u join follow f on f.followed_id = u._id where f.user_id = ? order by u.username" id]
    :result-set-fn doall))

(defn find-user [username]
  (sql/query db
    ["select u.*, p.image_id from web_user u
      left outer join user_portrait p on u._id = p.user_id
      where u.username = ?" username]
    :result-set-fn first))

(defn find-user-by-email [email]
  (sql/query db
    ["select u.*, p.image_id from web_user u
      left outer join user_portrait p on u._id = p.user_id
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

(defn followers-count [id]
  (sql/query db
    ["select COALESCE(count(*), 0) as count from follow where followed_id = ?" id]
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
    (sql/insert! db :follow {:user_id id :followed_id other-id})))

(defn unfollow [id other-id]
  (when (not= id other-id)
    (sql/delete! db :follow ["user_id = ? and followed_id = ?" id other-id])))

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

(defn add-item [item]
  (sql/insert! db :item item))

(defn get-items [offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id from item i
      join web_user u on i.user_id = u._id
      left outer join user_privacy p on p.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      where i.public and (p.items is null or p.items)
      order by i.created desc, i._id asc"]
    :result-set-fn doall))

(defn get-follows-items [user-id offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id from item i
      join web_user u on i.user_id = u._id
      join follow f on f.followed_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      where f.user_id = ?
      order by i.created desc, i._id asc" user-id]
    :result-set-fn doall))

(defn get-users-items [user-id offset limit]
  (sql/query db
    ["select i.*, u.*, a.image_id as user_image_id from item i
      join web_user u on i.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      where u._id = ?
      order by i.created desc, i._id asc" user-id]
    :result-set-fn doall))
