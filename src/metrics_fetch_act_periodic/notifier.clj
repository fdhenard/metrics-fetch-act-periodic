(ns metrics-fetch-act-periodic.notifier
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.stacktrace :as st]
            [postal.core :as postal]
            [metrics-fetch-act-periodic.config :as config]))


(def opts {:user (:aws-access-key-id config/config)
           :pass (:aws-secret-access-key config/config)
           :host (str "email-smtp." (:aws-region config/config)
                      ".amazonaws.com")
           :port 587})

(def NOTIFY_CONFIG (:notify config/config))
(def FROM (str "<" (:from NOTIFY_CONFIG) "> Well Gas Detection System"))
(def ADMINS (:admins NOTIFY_CONFIG))
(def USERS (:users NOTIFY_CONFIG))

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

(def METHANE_THRESHOLD 0.04)

(defn should-notify? [{:keys [heater methane danger] :as _detector}]
  true
  #_(let [heater-proportion (:proportion heater)]
   (or (> (:proportion methane) METHANE_THRESHOLD)
       #_(< heater-proportion ??some-value??)
       #_(> heater-proportion ??some-value??)
       (:high? danger))))


(defn process-notification! [{:keys [detector] :as _world}]
  (if-not (should-notify? detector)
    {:triggered false
     :result nil}
    (let [body
          (string/join
           " "
           ["It has been determined that you should be notified"
            "because the well gas detector has reached a threshold.\n\n"
            "One of the following are true:\n\n"
            "- explosive gas > " METHANE_THRESHOLD " proportion\n"
            "- ????? > heater-proportion > ?????\n"
            "- danger? is true\n\n"
            "Detector Data:\n\n" (with-out-str (pp/pprint _world))])]
      {:triggered true
       :result (send-message {:to USERS
                              :subject "Warning Triggered"
                              :body body})})))


(defn send-error! [world error]
  (let [body
        (string/join
         " "
         ["An error occurred\n\n"
          "error = \n\n" (with-out-str (st/print-stack-trace error))
          "world = \n\n" (with-out-str (pp/pprint world)) "\n\n"])]
    (send-message {:to ADMINS
                   :subject "Warning Triggered"
                   :body body})))
