(ns sevenorless.handler
  (:require [compojure.core :refer [defroutes routes]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.session.memory :refer [memory-store]]
            [hiccup.middleware :refer [wrap-base-url]]
            [noir.cookies :as cookies]
            [noir.session :as session]
            [noir.util.middleware :refer [wrap-access-rules]]
            [noir.validation :refer [wrap-noir-validation]]
            [sevenorless.models.db :as db]
            [sevenorless.routes.auth :refer [auth-routes]]
            [sevenorless.routes.home :refer [home-routes]]))

(defn init []
  (println "sevenorless is starting"))

(defn destroy []
  (println "sevenorless is shutting down"))

(defn auto-login []
  (when (nil? (session/get :user))
    (let [user (db/get-remembered-user (cookies/get :remember))]
      (when (not (nil? user)) (session/put! :user (:id user))))))

(defn user-access [_]
  (auto-login) ;; TODO why can't I add this as middleware?
  (session/get :user))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes auth-routes app-routes)
      (handler/site)
      (wrap-base-url)
      (session/wrap-noir-session {:store (memory-store)})
      (cookies/wrap-noir-cookies)
      (wrap-noir-validation)
      (wrap-access-rules [user-access])))
