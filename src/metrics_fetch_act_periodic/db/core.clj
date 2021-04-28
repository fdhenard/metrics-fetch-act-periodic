(ns metrics-fetch-act-periodic.db.core
  (:require [java-time :as time]
            [taoensso.faraday :as far]
            [metrics-fetch-act-periodic.config :as config]))


(def DYNAMO_CLIENT_OPTS
  (let [{:keys [access-key
                secret-key
                uri]} (:db-opts config/config)]
    {:access-key access-key
     :secret-key secret-key
     :endpoint uri}))

(comment

  DYNAMO_CLIENT_OPTS
  
  (far/list-tables DYNAMO_CLIENT_OPTS)

  (far/create-table
   DYNAMO_CLIENT_OPTS
   :well-gas
   [:uuid :s]
   {:range-keydef [:datetime :s]
    :throughput {:read 1 :write 1} ; Read & write capacity (units/sec)
    :block? true ; Block thread during table creation
    })

  #_(far/put-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   {:id 0
    :name "frank"})

  #_(far/get-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   {:id 0})

  (far/scan DYNAMO_CLIENT_OPTS
            :well-gas)

  (java.util.UUID/randomUUID)

  (str (time/instant))

  (far/put-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   {:uuid (str (java.util.UUID/randomUUID))
    :datetime (str (time/instant))
    :name "hi"})


  )

(defn list-tables []
  (far/list-tables DYNAMO_CLIENT_OPTS))


(defn add-metrics! [metrics]
  (far/put-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   metrics))

#_(holy-lambda/deflambda MetricFetchHandle
  [request]
  (println request)
  (hl-resp/json {:message "Hello"
            "it's" "me"
            :you "looking"
            :for true}))

#_(holy-lambda/gen-main
 [#'MetricFetchHandle])
