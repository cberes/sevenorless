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
  (sql/insert! :web_user user))

(defn update-user [id updates]
  (sql/update! :web_user updates ["_id = ?" id]))

(defn get-user [id]
  (sql/query db
    ["select * from web_user where _id = ?" id]
    :result-set-fn first))

(defn find-user [username]
  (sql/query db
    ["select * from web_user where username = ?" username]
    :result-set-fn first))

(defn find-user-by-email [email]
  (sql/query db
    ["select * from web_user where email = ?" email]
    :result-set-fn first))

(defn days-delta [op x]
  (op (quot (System/currentTimeMillis) 1000) (* x 86400)))

(defn compare-user-token [id token]
  (crypt/compare token
    (sql/query db
	    ["select token from user_token where user_id = ? and created >= ? limit 1" id (days-delta - 14)]
	    :result-set-fn first)))

(defn compare-user-password-reset [id token]
  (crypt/compare token
    (sql/query db
	    ["select token from user_password_reset where user_id = ? and created >= ? limit 1" id (days-delta - 14)]
	    :result-set-fn first)))

(defn compare-user-email-verify [id token]
  (crypt/compare token
    (sql/query db
	    ["select token from user_email_verify where user_id = ? limit 1" id]
	    :result-set-fn first)))

(defn create-email-verify-record [user]
  (let [id (:id user) token (get-random-id 32)]
    (sql/delete! db :user_email_verify ["user_id = ?" id])
    (sql/insert! :user_email_verify {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

(defn verify-email [secret]
  (let [{id 0, token 1} (str/split secret #":" 2)]
    (when (compare-user-email-verify id token)
      (sql/delete! db :user_email_verify ["user_id = ?" id])
      (update-user id {:activated (quot (System/currentTimeMillis) 1000)})
      (get-user id))))

(defn update-user-email [id email]
  (sql/update! :web_user {:email email :activated nil} ["_id = ?" id])
  (create-email-verify-record (get-user id)))

(defn create-password-reset-record [user]
  (let [id (:id user) token (get-random-id 32)]
    (sql/delete! db :user_password_reset ["user_id = ?" id])
    (sql/insert! :user_password_reset {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-password-reset-user [secret]
  (let [{id 0, token 1} (str/split secret #":" 2)]
    (when (compare-user-password-reset id token) (get-user id))))

;; returns cookie value
(defn remember-user [user]
  (let [id (:id user) token (get-random-id 32)]
    (sql/delete! db :user_token ["user_id = ?" id])
    (sql/insert! :user_token {:user_id id :token (crypt/encrypt token)})
    (str id ":" token)))

;; returns the remembered user (or nil)
;; do not call if user is in session
(defn get-remembered-user [cookie-value]
  (let [{id 0, token 1} (str/split cookie-value #":" 2)]
    (when (compare-user-token id token) (get-user id))))