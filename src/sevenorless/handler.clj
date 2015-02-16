(ns sevenorless.handler
  (:require [compojure.core :refer [defroutes routes]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.session.memory :refer [memory-store]]
            [hiccup.middleware :refer [wrap-base-url]]
            [noir.session :as session]
            [noir.validation :refer [wrap-noir-validation]]
            [sevenorless.routes.auth :refer [auth-routes]]
            [sevenorless.routes.home :refer [home-routes]]))

(defn init []
  (println "sevenorless is starting"))

(defn destroy []
  (println "sevenorless is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes auth-routes app-routes)
      (handler/site)
      (wrap-base-url)
      (session/wrap-noir-session {:store (memory-store)})
      (wrap-noir-validation)))
