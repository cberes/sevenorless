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
      (link-to {:class "title"} "/" (title))
      (link-to "/register" "Sign up")
      (link-to "/login" "Log in")]
     [:div.w
      [:span.h1-prefix "1 2 3 4 5 6"]
      [:h1.main (title)]
      body]]))
