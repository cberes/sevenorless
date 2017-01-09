(ns sevenorless.handler
  (:require [compojure.core :refer [defroutes routes]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.session.memory :refer [memory-store]]
            [hiccup.middleware :refer [wrap-base-url]]
            [noir.cookies :refer [wrap-noir-cookies]]
            [noir.session :refer [wrap-noir-session]]
            [noir.util.middleware :refer [wrap-access-rules]]
            [noir.validation :refer [wrap-noir-validation]]
            [sevenorless.models.user :refer [wrap-user-login get-user]]
            [sevenorless.routes.auth :refer [auth-routes]]
            [sevenorless.routes.file :refer [file-routes]]
            [sevenorless.routes.home :refer [home-routes]]
            [sevenorless.routes.policy :refer [policy-routes]]
            [sevenorless.routes.settings :refer [settings-routes]]
            [sevenorless.routes.user :refer [user-routes]]))

(defn user-access [_]
  (get-user))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes
              policy-routes
              user-routes
              settings-routes
              auth-routes
              file-routes
              app-routes)
      (handler/site)
      (wrap-base-url)
      (wrap-access-rules [user-access])
      (wrap-user-login)
      (wrap-noir-session {:store (memory-store)})
      (wrap-noir-cookies)
      (wrap-noir-validation)))
