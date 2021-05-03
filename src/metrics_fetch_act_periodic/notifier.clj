(ns metrics-fetch-act-periodic.notifier
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.stacktrace :as st]
            [postal.core :as postal]
            [metrics-fetch-act-periodic.config :as config])
  (:import [com.amazonaws.services.simpleemail
            AmazonSimpleEmailServiceClientBuilder]
           [com.amazonaws.services.simpleemail.model Body Content
            Destination Message SendEmailRequest]))


(def postal-opts {:user (:aws-ses-smpt-username config/config)
                  :pass (:aws-ses-smpt-password config/config)
                  :host (str "email-smtp." (:aws-region config/config)
                             ".amazonaws.com")
                  ;; :port 587
                  :port 25})

(def ses-client
  (when-not (config/is-running-local?)
    (AmazonSimpleEmailServiceClientBuilder/defaultClient)))

(defn parse-email-addrs [addrs-str]
  (->> (string/split addrs-str #",")
       (map string/trim)))

(def NOTIFY_CONFIG (:notify config/config))
(def FROM (str "<" (:from NOTIFY_CONFIG) "> Well Gas Detection System"))
(def ADMINS (parse-email-addrs (:admins NOTIFY_CONFIG)))
(def USERS (parse-email-addrs (:users NOTIFY_CONFIG)))

(defn send-via-postal [{:keys [subject body] :as msg-in}]
  (let [{:keys [error] :as res}
        (postal/send-message
               postal-opts
               (merge msg-in
                      {:from FROM
                       :subject subject
                       :body [{:type "text/html"
                               :content body}]}))]
    {:success? (= :SUCCESS error)
     :library :postal
     :library-result res}))

(defn send-via-ses-sdk [{:keys [subject to body] :as _msg-in}]
  (let [mail-req
        (.. (SendEmailRequest.)
            (withSource FROM)
            (withDestination
             (.. (Destination.)
                 (withToAddresses to)))
            (withMessage
             (.. (Message.)
                 (withSubject
                  (.. (Content.)
                      (withCharset "UTF-8")
                      (withData subject)))
                 (withBody
                  (.. (Body.)
                      (withHtml
                       (.. (Content.)
                           (withCharset "UTF-8")
                           (withData body))))))))
        _ (.sendEmail ses-client mail-req)]
    {:success? true
     :library :aws-ses-sdk
     :library-result nil}))

(defn send-message [{:keys [subject] :as msg-in}]
  (let [subject (str "[Well Gas Detection System] " subject)
        msg (assoc msg-in :subject subject)]
    (if (config/is-running-local?)
      (send-via-postal msg)
      (send-via-ses-sdk msg))))

(comment


  config/config
  
  (send-message {:to ADMINS
                 :subject "testing 1 2"
                 :body "testing"})


  )

(def METHANE_THRESHOLD 0.04)

(defn should-notify? [{:keys [heater methane danger] :as _detector}]
  (let [heater-proportion (:proportion heater)]
    (or (> (:proportion methane) METHANE_THRESHOLD)
        (= heater-proportion 0.0)
        (:high? danger))))

(defn triggered-and-failed? [{:keys [notifier] :as _world}]
  (let [{:keys [result triggered?]} notifier]
   (and triggered?
        (false? (:success? result)))))

(defn code-string->html [code-str]
  (-> code-str
      (string/replace #"\n" "<br>")
      (string/replace #" " "&nbsp;")))


(defn process-notification! [{:keys [detector] :as _world}]
  (if-not (should-notify? detector)
    {:triggered? false
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
            "- heater-proportion == 0<br>"
            "- danger? is true<br><br>"
            "Detector Data:<br><br><code>" world-as-html
            "</code>"])
          send-res (try
                     (send-message {:to USERS
                                    :subject "Warning Triggered"
                                    :body body})
                     (catch Throwable exc
                       {:success? false
                        :error-message (.getMessage exc)
                        :stack-trace-str (with-out-str (st/print-stack-trace exc))}))]
      {:triggered? true
       :result send-res})))


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
                   :body body})))
