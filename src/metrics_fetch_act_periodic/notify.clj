(ns metrics-fetch-act-periodic.notify
  (:require [postal.core :as postal]
            [metrics-fetch-act-periodic.config :as config]))


(def opts {:user (:aws-access-key-id config/config)
           :pass (:aws-secret-access-key config/config)
           :host (str "email-smtp." (:aws-region config/config)
                      ".amazonaws.com")
           :port 587})

(def NOTIFY_CONFIG (:notify config/config))
(def FROM (str "<" (:from NOTIFY_CONFIG) "> Well Gas Detection System"))
(def ADMINS (:admins NOTIFY_CONFIG))

(defn send-message [{:keys [subject] :as msg-in}]
  (postal/send-message
   (merge msg-in
          {:from FROM
           :subject (str "[Well Gas Detection System] " subject)})))

(comment

  (send-message {:to ADMINS
                 :subject "testing 1 2"
                 :body "testing"})


  )
