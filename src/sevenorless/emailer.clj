(ns sevenorless.emailer
  (:import [com.amazonaws.services.simpleemail AmazonSimpleEmailServiceClient]
           [com.amazonaws.services.simpleemail.model Body
                                                     Content
                                                     Message
                                                     SendEmailRequest]
           [com.amazonaws.regions Region
                                  Regions])
  (:require [sevenorless.models.db :as db]))

(def title "7 Items or Less")

(def base-url
  (delay (System/getenv "SIOL_BASE_URL")))

(def email-from
  (delay (System/getenv "SIOL_EMAIL_FROM")))

(defn #^Content build-data [data]
  (.withData (Content.) (str data)))

(defn #^Body build-body [body]
  (.withText (Body.) body))

(defn #^Message build-message [subject body]
  (doto (Message.)
    (.withSubject (build-data subject))
    (.withBody (build-body (build-data body)))))

(defn #^SendEmailRequest build-email [from to subject body]
  (doto (SendEmailRequest.)
    (.withSource from)
    (.withDestination to)
    (.withMessage (build-message subject body))))

(defn #^AmazonSimpleEmailServiceClient send-email [from to subject body]
  (doto (AmazonSimpleEmailServiceClient.)
    (.setRegion (Region/getRegion Regions/US_EAST_1))
    (.sendEmail (build-email from to subject body))))

(defn build-verification-email-body [record]
  (str "Thank you for joining " title "!" \newline \newline
       "Please verify your email by clicking the link below:" \newline
       base-url "/verify-email?q=" (:user_id record) ":" (:token record) \newline \newline
       "Enjoy " title "!"))

(defn send-verification-email [record]
  (send-email @email-from
              (:email record)
              (str "Welcome to " title "!")
              (build-verification-email-body record)))

(defn send-verification-emails []
  (doseq [{id :user_id :as record} (db/get-verify-emails-to-send)]
    (try
      (send-verification-email record)
      (db/update-verify-email id "P")
    (catch Exception e (db/update-verify-email id "F")))))

(defn build-password-reset-body [record]
  (str "We received your request to change your password for " title "." \newline
       "If you did not request this, then please ignore this message." \newline \newline
       "To change your password, please click the link below:" \newline
       base-url "/password-reset?q=" (:user_id record) ":" (:token record) \newline \newline
       "Enjoy " title "!"))

(defn send-password-reset [record]
  (send-email @email-from
              (:email record)
              (str title " Password Reset")
              (build-password-reset-body record)))

(defn send-password-reset-emails []
  (doseq [{id :user_id :as record} (db/get-password-resets-to-send)]
    (try
      (send-password-reset record)
      (db/update-password-reset id "P")
    (catch Exception e (db/update-password-reset id "F")))))

(defn send-emails []
  (send-verification-emails)
  (send-password-reset-emails))
