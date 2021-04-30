(ns metrics-fetch-act-periodic.db.core
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]
            [java-time :as time]
            [taoensso.faraday :as far]
            [metrics-fetch-act-periodic.config :as config])
  (:import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
           com.amazonaws.client.builder.AwsClientBuilder$EndpointConfiguration
           [com.amazonaws.auth
            BasicAWSCredentials
            AWSStaticCredentialsProvider]))


(def DYNAMO_CLIENT_OPTS
  (let [{:keys [uri]} (:db-opts config/config)
        {:keys [aws-access-key-id
                aws-secret-access-key
                aws-region]} config/config
        res (if-not (config/is-running-local?)
              {:client (AmazonDynamoDBClientBuilder/defaultClient)}
              (let [creds (BasicAWSCredentials.
                           aws-access-key-id
                           aws-secret-access-key)
                    creds-provider (AWSStaticCredentialsProvider. creds)
                    client-builder
                    (cond-> (.. (AmazonDynamoDBClientBuilder/standard)
                                (withCredentials creds-provider))
                      aws-region
                      (.withRegion aws-region)
                      (and aws-region uri)
                      (.withEndpointConfiguration
                       (AwsClientBuilder$EndpointConfiguration.
                        uri
                        aws-region)))]
                {:client (.build client-builder)}))
        #_ (pp/pprint {:dynamo-conn-stuff
                      {:len-access-key (and aws-access-key-id
                                            (count aws-access-key-id))
                       :len-secret-key (and aws-secret-access-key
                                            (count aws-secret-access-key))
                       :aws-region aws-region
                       #_#_:config-keys (keys config/config)}})]
    res))

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

  (far/put-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   {:uuid (str (java.util.UUID/randomUUID))
    :datetime (str (time/instant))
    :name "hi"})


  )

(defn add-metrics! [metrics]
  (far/put-item
   DYNAMO_CLIENT_OPTS
   :well-gas
   metrics))


(defn persist! [{:keys [detector notifier] :as _world}]
  (let [to-persist (assoc detector :notification-result notifier)
        _ (add-metrics! to-persist)]
    {:success true}))
