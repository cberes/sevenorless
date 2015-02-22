(ns sevenorless.routes.policy
  (:require [compojure.core :refer :all]
            [sevenorless.views.layout :as layout]))

(defn policy []
  (layout/simple "Policy"
    [:p "Anythiing you create on this website belongs to you. We will not use it for promotional
         purposes without your permission. However, we may permanently remove anything at any time.
         You cannot receive compensation from us for anything you create here or anything we remove
         from here."]
    [:p "We will not sell your account information or behavior to third parties."]
    [:p "We reserve the right to change this policy at any time without notice. However, we will try
         to contact you to notify you of any changes."]))

(defroutes policy-routes
  (GET "/policy" [] (policy)))

