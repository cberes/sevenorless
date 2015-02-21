(ns sevenorless.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [link-to]]))

(defn title [] "7 items or less")

(defn common [& body]
  (html5
    [:head
     [:title (title)]
     (include-css "/css/screen.css")]
    [:body
     [:div#menu
      (link-to "/register" "Sign up")
      (link-to "/login" "Log in")
      (link-to "/logout" "Log out")]
     [:div.w
      [:h1.main (link-to "/" (title))]
      body]]))

(defn simple [title & body]
  (common [:h2 title] [:div.c body]))
