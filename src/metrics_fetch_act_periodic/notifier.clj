(ns metrics-fetch-act-periodic.notifier
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.stacktrace :as st]
            [postal.core :as postal]
            [metrics-fetch-act-periodic.config :as config]))


(def opts {:user (:aws-ses-smpt-username config/config)
           :pass (:aws-ses-smpt-password config/config)
           :host (str "email-smtp." (:aws-region config/config)
                      ".amazonaws.com")
           ;; :port 587
           :port 25})

(defn parse-emails [addrs-str]
  (->> (string/split addrs-str #",")
       (map string/trim)))

(def NOTIFY_CONFIG (:notify config/config))
(def FROM (str "<" (:from NOTIFY_CONFIG) "> Well Gas Detection System"))
(def ADMINS (parse-emails (:admins NOTIFY_CONFIG)))
(def USERS (parse-emails (:users NOTIFY_CONFIG)))

(defn send-message [{:keys [subject] :as msg-in}]
  (postal/send-message
   opts
   (merge msg-in
          {:from FROM
           :subject (str "[Well Gas Detection System] " subject)})))

(comment


  config/config
  
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

(defn sent-success? [{:keys [result] :as _notification-result}]
  (= :SUCCESS (:error result)))

(defn code-string->html [code-str]
  (-> code-str
      (string/replace #"\n" "<br>")
      (string/replace #" " "&nbsp;")))


(defn process-notification! [{:keys [detector] :as _world}]
  (if-not (should-notify? detector)
    {:triggered false
     :result nil}
    (let [world-as-html (-> (with-out-str (pp/pprint _world))
                            code-string->html)
          body
          (string/join
           " "
           ["It has been determined that you should be notified"
            "because the well gas detector has reached a threshold<br><br>"
            "One or more of the following are true:<br><br>"
            "- explosive-gas-proportion > " METHANE_THRESHOLD "<br>"
            "- ????? > heater-proportion > ?????<br>"
            "- danger? is true<br><br>"
            "Detector Data:<br><br><code>" world-as-html
            "</code>"])]
      {:triggered true
       :result (send-message {:to USERS
                              :subject "Warning Triggered"
                              :body
                              [{:type "text/html"
                                :content body}]})})))


(defn send-error! [world error]
  (let [stack-trace-html (-> (with-out-str (st/print-stack-trace error))
                             code-string->html)
        world-html (-> (with-out-str (pp/pprint world))
                       code-string->html)
        body
        (string/join
         " "
         ["An error occurred<br><br>"
          "error = <br><br><code>" stack-trace-html "</code>"
          "world = <br><br><code>"  world-html "</code>"])]
    (send-message {:to ADMINS
                   :subject "Warning Triggered"
                   :body [{:type "text/html"
                           :content body}]})))
