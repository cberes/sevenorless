(ns sevenorless.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [link-to]]
            [sevenorless.models.user :as user]))

(defn title [] "7 items or less")

(defn menu []
  (if-let [user (user/get-user)]
    (list
      (link-to "/feed" "My feed")
      (link-to (str "/u/" (:username user)) "Profile")
      (link-to "/logout" "Log out"))
    (list
      (link-to "/register" "Sign up")
      (link-to "/login" "Log in"))))

(defn common [& body]
  (html5
    [:head
     [:title (title)]
     (include-css "/css/screen.css")
     (include-js "/js/jquery-2.1.3.min.js")
     (include-js "/js/follow.js")]
    [:body
     [:div#menu (menu)]
     [:div.w
      [:h1.main (link-to "/" (title))]
      body]]))

(defn simple [title & body]
  (common [:h2 title] [:div.c body]))
