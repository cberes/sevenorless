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
            [sevenorless.models.user :as user]
            [sevenorless.routes.auth :refer [auth-routes]]
            [sevenorless.routes.file :refer [file-routes]]
            [sevenorless.routes.home :refer [home-routes]]
            [sevenorless.routes.policy :refer [policy-routes]]
            [sevenorless.routes.user :refer [user-routes]]))

(defn init []
  (println "sevenorless is starting"))

(defn destroy []
  (println "sevenorless is shutting down"))

(defn user-access [_]
  (user/get-user))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes policy-routes user-routes auth-routes file-routes app-routes)
      (handler/site)
      (wrap-base-url)
      (wrap-access-rules [user-access])
      (wrap-noir-validation)
      (user/wrap-user-login)
      (session/wrap-noir-session {:store (memory-store)})
      (cookies/wrap-noir-cookies)))
