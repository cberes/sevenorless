(ns sevenorless.models.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as sql]
            [jdbc.pool.c3p0 :as pool]
            [buddy.hashers :as crypt]
            [clojure.string :as str])
  (:import java.sql.DriverManager))

(def db (delay (pool/make-datasource-spec
                 {:subprotocol "postgresql"
                  :subname (System/getProperty "db.name")
                  :user (System/getProperty "db.user")
                  :password (System/getProperty "db.pass")})))

;; TODO function in clojure to generate random string?
(def alphanumeric "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")
(defn get-random-id [length]
  (loop [acc []]
    (if (= (count acc) length) (apply str acc)
      (recur (conj acc (rand-nth alphanumeric))))))

(defn add-user [user]
  (sql/insert! @db :web_user user))

(defn update-user [id updates]
  (sql/update! @db :web_user updates ["_id = ?" id]))

(defn get-user [id]
  (sql/query @db
    ["select u.*, p.image_id, pi.ext as image_ext, COALESCE(y.items, TRUE) as items_public
      from web_user u
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      left outer join image pi on p.image_id = pi._id
      where u._id = ?" id]
    :result-set-fn first))

(defn find-user [username & [my-id]]
  (sql/query @db
    ["select u.*, p.image_id, pi.ext as image_ext, COALESCE(y.items, TRUE) as items_public,
      u.created AT TIME ZONE 'UTC' AT TIME ZONE COALESCE(my.tz, 'America/New_York') AS created_local
      from web_user u
      left outer join web_user my on my._id = ?
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      left outer join image pi on p.image_id = pi._id
      where u.username ilike ?" (or my-id 0) username]
    :result-set-fn first))

(defn find-user-by-email [email]
  (sql/query @db
    ["select u.*, p.image_id, pi.ext as image_ext, COALESCE(y.items, TRUE) as items_public
      from web_user u
      left outer join user_portrait p on u._id = p.user_id
      left outer join user_privacy y on u._id = y.user_id
      left outer join image pi on p.image_id = pi._id
      where u.email ilike ?" email]
    :result-set-fn first))

(defn check-for-user [username id]
  (sql/query @db
    ["select _id from web_user where username ilike ? and _id <> ?" username id]
    :result-set-fn first))

(defn compare-user-token [id token]
  (crypt/check token (:token
    (sql/query @db
      ["select token from user_token where user_id = ? and created >= (NOW() - INTERVAL '2 weeks') limit 1" id]
      :result-set-fn first))))

(defn compare-user-password-reset [id token]
  (crypt/check token (:token
    (sql/query @db
      ["select token from user_password_reset where user_id = ? and created >= (NOW() - INTERVAL '2 weeks') limit 1" id]
      :result-set-fn first))))

(defn compare-user-email-verify [id token]
  (crypt/check token (:token
    (sql/query @db
      ["select token from user_email_verify where user_id = ? limit 1" id]
      :result-set-fn first))))

(defn create-email-verify-record [user]
  (let [id (:_id user) token (get-random-id 32)]
    (sql/delete! @db :user_email_verify ["user_id = ?" id])
    (sql/insert! @db :user_email_verify {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

(defn verify-email [secret]
  (let [{id 0, token 1} (str/split secret #":" 2) id (Integer/parseInt id)]
    (when (compare-user-email-verify id token)
      (sql/delete! @db :user_email_verify ["user_id = ?" id])
      (update-user id {:activated (quot (System/currentTimeMillis) 1000)})
      (get-user id))))

(defn update-user-email [id email]
  (sql/update! @db :web_user {:email email :activated nil} ["_id = ?" id])
  (create-email-verify-record (get-user id)))

(defn delete-password-reset-record [user]
  (sql/delete! @db :user_password_reset ["user_id = ?" (:_id user)]))

(defn create-password-reset-record [user]
  (delete-password-reset-record user)
  (let [id (:_id user) token (get-random-id 32)]
    (sql/insert! @db :user_password_reset {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-password-reset-user [secret]
  (let [{id 0, token 1} (str/split secret #":" 2) id (Integer/parseInt id)]
    (when (compare-user-password-reset id token) (get-user id))))

;; returns cookie value
(defn remember-user [user]
  (let [id (:_id user) token (get-random-id 32)]
    (sql/delete! @db :user_token ["user_id = ?" id])
    (sql/insert! @db :user_token {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-remembered-user [cookie-value]
  (when (not (nil? cookie-value))
    (let [{id 0, token 1} (str/split cookie-value #":" 2) id (Integer/parseInt id)]
      (when (compare-user-token id token) (get-user id)))))

(defn get-follows [id]
  (sql/query @db
    ["select u._id, u.username,
      f.created AT TIME ZONE 'UTC' AT TIME ZONE my.tz AS created
      from web_user u
      join follow f on f.followed_id = u._id
      join web_user my on my._id = ?
      where f.user_id = my._id
      order by u.username" id]
    :result-set-fn doall))

(defn get-pending-followers [id]
  (sql/query @db
    ["select u._id, u.username,
      f.created AT TIME ZONE 'UTC' AT TIME ZONE my.tz AS created
      from web_user u
      join pending_follow f on f.user_id = u._id
      join web_user my on my._id = ?
      where f.followed_id = my._id and f.approved is null
      order by date_trunc('day', f.created) desc, u.username" id]
    :result-set-fn doall))

(defn users-count []
  (:count (sql/query @db
            ["select COALESCE(count(*), 0) as count from web_user where deactivated is null"]
            :result-set-fn first)))

(defn followers-count [id]
  (:count (sql/query @db
            ["select COALESCE(count(*), 0) as count from follow where followed_id = ?" id]
            :result-set-fn first)))

(defn pending-followers-count [id]
  (:count (sql/query @db
            ["select COALESCE(count(*), 0) as count from pending_follow where followed_id = ? and approved is null" id]
            :result-set-fn first)))

(defn following-count [id]
  (:count (sql/query @db
            ["select COALESCE(count(*), 0) as count from follow where user_id = ?" id]
            :result-set-fn first)))

(defn following? [id other-id]
  (sql/query @db
    ["select u._id, u.username, f.created from web_user u join follow f on f.followed_id = u._id where f.user_id = ? and f.followed_id = ?" id other-id]
    :result-set-fn first))

(defn follow [id other-id]
  (when (and (not= id other-id) (not (following? id other-id)))
    (sql/delete! @db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/insert! @db :follow {:user_id id :followed_id other-id})))

(defn unfollow [id other-id]
  (when (not= id other-id)
    (sql/delete! @db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/delete! @db :follow ["user_id = ? and followed_id = ?" id other-id])))

; leave in 'pending' state for a month before a user can request to follow again
; called as user who wants to follow another user
(defn follow-pending? [id other-id]
  (sql/query @db
    ["select u._id, u.username, f.created
      from pending_follow f
      join web_user u on f.followed_id = u._id
      where f.user_id = ? and f.followed_id = ?
      and (f.approved is null or f.created >= (NOW() - INTERVAL '1 month'))" id other-id]
    :result-set-fn first))

; called as user who wants to follow another user
(defn request-follow [id other-id]
  (when (and (not= id other-id) (not (following? id other-id)) (not (follow-pending? id other-id)))
    (sql/delete! @db :pending_follow ["user_id = ? and followed_id = ?" id other-id])
    (sql/insert! @db :pending_follow {:user_id id :followed_id other-id})))

; called as user who is being followed
(defn approve-follow [id other-id]
  (when (and (not= id other-id) (not (following? other-id id)))
    (sql/delete! @db :pending_follow ["user_id = ? and followed_id = ?" other-id id])
    (sql/insert! @db :follow {:user_id other-id :followed_id id})))

; called as user who is being followed
(defn deny-follow [id other-id]
  (when (not= id other-id)
    (sql/update! @db :pending_follow {:approved false} ["user_id = ? and followed_id = ?" other-id id])))

(defn daily-items-count [id]
  (:count (sql/query @db
            ["select COALESCE(count(*), 0) as count from item where user_id = ? and created >= CURRENT_DATE" id]
            :result-set-fn first)))

(defn add-image [image]
  (sql/insert! @db :image image))

(defn get-image [id]
  (sql/query @db
    ["select * from image where _id = ?" id]
    :result-set-fn first))

(defn get-user-image [id user-id]
  (sql/query @db
    ["select * from image where _id = ? and user_id = ?" id user-id]
    :result-set-fn first))

(defn delete-image [id user-id]
  (sql/delete! @db :image ["_id = ? and user_id = ?" id user-id]))

(defn delete-item-extras [id]
  (sql/delete! @db :item_bias ["item_id = ?" id])
  (sql/delete! @db :item_comment ["item_id = ?" id]))

(defn delete-item [id user-id]
  (sql/delete! @db :item ["_id = ? and user_id = ?" id user-id]))

(defn add-item [item]
  (sql/insert! @db :item item))

(defn update-item [id updates]
  (sql/update! @db :item updates ["_id = ?" id]))

(defn get-item [id user-id]
  (sql/query @db
    ["select * from item where _id = ? and user_id = ?" id user-id]
    :result-set-fn first))

(def page-filter-clause
  " and (i.created < ? or (i.created = ? and i._id > ?))")

(defn get-items [user-id offset limit]
  (sql/query @db
    ["select i._id, i.user_id, i.image_id, i.title, i.body, i.link, i.public, i.comments,
      i.created AT TIME ZONE 'UTC' AT TIME ZONE COALESCE(my.tz, 'America/New_York') AS created,
      u.username, u.email,
      a.image_id AS user_image_id, ai.ext AS user_image_ext, ii.ext AS image_ext,
      (select coalesce(count(*), 0) from item_comment c where c.item_id = i._id) AS comments_count,
      rank() OVER (PARTITION BY
      date_trunc('day', i.created AT TIME ZONE 'UTC' AT TIME ZONE COALESCE(my.tz, 'America/New_York'))
      ORDER BY i.created DESC, i._id ASC)
      from item i
      left outer join web_user my on my._id = ?
      left outer join image ii on i.image_id = ii._id
      join web_user u on i.user_id = u._id
      left outer join user_privacy p on p.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      left outer join image ai on a.image_id = ai._id
      left outer join follow f on f.followed_id = u._id and f.user_id = my._id
      where (i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = my._id
      order by i.created desc, i._id asc
      limit ?" user-id limit]
    :result-set-fn doall))

; posts by those who user follows
; privacy doesn't matter
(defn get-follows-items [user-id offset limit]
  (sql/query @db
    ["select i._id, i.user_id, i.image_id, i.title, i.body, i.link, i.public, i.comments,
      i.created AT TIME ZONE 'UTC' AT TIME ZONE my.tz AS created,
      u.username, u.email,
      a.image_id AS user_image_id, ai.ext AS user_image_ext, ii.ext AS image_ext,
      (select coalesce(count(*), 0) from item_comment c where c.item_id = i._id) AS comments_count,
      rank() OVER (PARTITION BY
      date_trunc('day', i.created AT TIME ZONE 'UTC' AT TIME ZONE my.tz)
      ORDER BY i.created DESC, i._id ASC)
      from item i
      left outer join web_user my on my._id = ?
      left outer join image ii on i.image_id = ii._id
      join web_user u on i.user_id = u._id
      join follow f on f.followed_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      left outer join image ai on a.image_id = ai._id
      where f.user_id = my._id
      order by i.created desc, i._id asc
      limit ?" user-id limit]
    :result-set-fn doall))

; if user's privacy setting for posts is private, query will not be run if user is not privileged
; so consider only per-post privacy
(defn get-users-items [user-id current-user-id offset limit]
  (sql/query @db
    ["select i._id, i.user_id, i.image_id, i.title, i.body, i.link, i.public, i.comments,
      i.created AT TIME ZONE 'UTC' AT TIME ZONE COALESCE(my.tz, 'America/New_York') AS created,
      u.username, u.email,
      a.image_id AS user_image_id, ai.ext AS user_image_ext, ii.ext AS image_ext,
      (select coalesce(count(*), 0) from item_comment c where c.item_id = i._id) AS comments_count,
      rank() OVER (PARTITION BY
      date_trunc('day', i.created AT TIME ZONE 'UTC' AT TIME ZONE COALESCE(my.tz, 'America/New_York'))
      ORDER BY i.created DESC, i._id ASC)
      from item i
      left outer join web_user my on my._id = ?
      left outer join image ii on i.image_id = ii._id
      join web_user u on i.user_id = u._id
      left outer join user_privacy p on p.user_id = u._id
      left outer join user_portrait a on u._id = a.user_id
      left outer join image ai on a.image_id = ai._id
      left outer join follow f on f.followed_id = i.user_id and f.user_id = my._id
      where u._id = ? and ((i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = my._id)
      order by i.created desc, i._id asc
      limit ?" current-user-id user-id limit]
    :result-set-fn doall))

(defn get-user-privacy [id]
  (sql/query @db
    ["select * from user_privacy where user_id = ?" id]
    :result-set-fn first))

(defn add-user-privacy [privacy]
  (sql/insert! @db :user_privacy privacy))

(defn update-user-privacy [id updates]
  (sql/update! @db :user_privacy updates ["user_id = ?" id]))

(defn get-user-bio [id]
  (sql/query @db
    ["select bio from user_bio where user_id = ?" id]
    :result-set-fn first))

(defn add-user-bio [bio]
  (sql/insert! @db :user_bio bio))

(defn update-user-bio [id updates]
  (sql/update! @db :user_bio updates ["user_id = ?" id]))

(defn delete-user-bio [id]
  (sql/delete! @db :user_bio ["user_id = ?" id]))

(defn add-user-portrait [portrait]
  (sql/insert! @db :user_portrait portrait))

(defn delete-user-portrait [id]
  (sql/delete! @db :user_portrait ["user_id = ?" id]))

(defn can-comment? [user-id item-id]
  (not (nil?(sql/query @db
              ["select i._id
                from item i
                left outer join web_user my on my._id = ?
                left outer join user_privacy p on p.user_id = i.user_id
                left outer join follow f on f.followed_id = i.user_id and f.user_id = my._id
                where i._id = ? and i.comments
                and ((i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = my._id)" user-id item-id]
              :result-set-fn first))))

(defn add-comment [{:keys [item_id user_id] :as comment}]
  (when (can-comment? user_id item_id)
    (sql/insert! @db :item_comment comment)))

(defn get-comments [user-id item-id]
  (sql/query @db
    ["select c.*, u.username, a.image_id as user_image_id, ai.ext as user_image_ext
      from item_comment c
      left outer join web_user my on my._id = ?
      join web_user u on c.user_id = u._id
      join item i on i._id = c.item_id
      left outer join user_portrait a on c.user_id = a.user_id
      left outer join image ai on a.image_id = ai._id
      left outer join user_privacy p on p.user_id = i.user_id
      left outer join follow f on f.followed_id = i.user_id and f.user_id = my._id
      where c.item_id = ? and ((i.public and (p.items is null or p.items)) or f.created is not null or i.user_id = my._id)
      order by c.created desc" user-id item-id]
    :result-set-fn doall))

(defn get-time-zones []
  (sql/query @db ["select name from pg_timezone_names order by name"] :row-fn :name))

(defn is-time-zone? [name]
  (first (sql/query @db ["select abbrev from pg_timezone_names where name = ? limit 1" name] :row-fn :abbrev)))
