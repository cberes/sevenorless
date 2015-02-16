(ns sevenorless.models.db
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:require [clojure.java.jdbc :as sql]
            [jdbc.pool.c3p0 :as pool])
  (:import java.sql.DriverManager))

(def db (pool/make-datasource-spec
          {:subprotocol "postgresql"
           :subname "//localhost/sevenorless"
           :user "sevenorless"
           :password "winnie"}))

(defn add-user [user]
  (sql/insert! :web_user user))

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
