(ns metrics-fetch-act-periodic.detector
  (:require [clojure.spec.alpha :as spec]
            [java-time :as time]
            [cheshire.core :as cheshire]
            [clj-http.lite.client :as client]
            [metrics-fetch-act-periodic.config :as config]
            [metrics-fetch-act-periodic.spec.core :as mfap-spec]))

(def DETECTOR_CONFIG (:detector config/config))
(def URI_BASE (str "https://api.particle.io/v1/devices/"
                   (:device-id DETECTOR_CONFIG)))
(def ACCESS_TOKEN (:access-token DETECTOR_CONFIG))


(defn call-particle-function [endpoint-name]
  (let [post-res
        (client/post
         (str URI_BASE "/" endpoint-name)
         {:form-params {:access_token ACCESS_TOKEN
                        :params "on"}
          :throw-exceptions false})]
    post-res))

(defn resp->resp-body-map [{:keys [body] :as resp}]
  (let [body-map (cheshire/parse-string body true)
        new-res (-> resp
                    (assoc :body-map body-map)
                    (dissoc :body))]
    new-res))


(comment

  (-> (call-particle-function "read-methane")
      resp->resp-body-map)
;; => {:headers
;;     {"date" "Tue, 27 Apr 2021 17:14:37 GMT",
;;      "content-type" "application/json; charset=utf-8",
;;      "content-length" "67",
;;      "connection" "keep-alive",
;;      "server" "nginx/1.19.1",
;;      "x-request-id" "9fe81fa024d1fa41de9da441584539ea",
;;      "access-control-allow-origin" "*"},
;;     :status 200,
;;     :body-map
;;     {:id "170030000747353138383138", :connected true, :return_value 0}}

  (-> (call-particle-function "read-heater")
      resp->resp-body-map)
;; => {:headers
;;     {"date" "Tue, 27 Apr 2021 17:14:54 GMT",
;;      "content-type" "application/json; charset=utf-8",
;;      "content-length" "67",
;;      "connection" "keep-alive",
;;      "server" "nginx/1.19.1",
;;      "x-request-id" "33f46b17814fcf6af400ed8fc53d21b7",
;;      "access-control-allow-origin" "*"},
;;     :status 200,
;;     :body-map
;;     {:id "170030000747353138383138", :connected true, :return_value 0}}
  

  (-> (call-particle-function "digital-danger")
      resp->resp-body-map)
;; => {:headers
;;     {"date" "Tue, 27 Apr 2021 17:15:06 GMT",
;;      "content-type" "application/json; charset=utf-8",
;;      "content-length" "67",
;;      "connection" "keep-alive",
;;      "server" "nginx/1.19.1",
;;      "x-request-id" "45eb76956645e6f92f1516dde9aed64d",
;;      "access-control-allow-origin" "*"},
;;     :status 200,
;;     :body-map
;;     {:id "170030000747353138383138", :connected true, :return_value 1}}
  
  )


(defn particle-get [endpoint-name]
  (let [get-res
        (client/get
         (str URI_BASE "/" endpoint-name)
         {:query-params {:access_token ACCESS_TOKEN}})]
    get-res))

(defn resp-body-map->resp-results-map [get-resp]
  (let [result (get-in get-resp [:body-map :result])]
    (-> get-resp
        (assoc-in [:body-map :result-map]
                  (cheshire/parse-string result true))
        (update-in [:body-map] dissoc :result))))

(comment

  (-> (particle-get "allmetricsjson")
      resp->resp-body-map
      resp-body-map->resp-results-map)
;; => {:headers
;;     {"content-encoding" "gzip",
;;      "server" "nginx/1.19.1",
;;      "content-type" "application/json; charset=utf-8",
;;      "access-control-allow-origin" "*",
;;      "connection" "keep-alive",
;;      "transfer-encoding" "chunked",
;;      "date" "Tue, 27 Apr 2021 21:27:15 GMT",
;;      "vary" "Accept-Encoding",
;;      "x-request-id" "4d50ae7348616846ffec1f1e97020461"},
;;     :status 200,
;;     :body-map
;;     {:cmd "VarReturn",
;;      :name "allmetricsjson",
;;      :coreInfo
;;      {:last_heard "2021-04-27T21:26:47.952Z",
;;       :connected true,
;;       :last_handshake_at "2021-04-27T20:35:43.820Z",
;;       :deviceID "170030000747353138383138",
;;       :product_id 6},
;;      :result-map
;;      {:heater {:reading-of-4096 149, :volts 0.120044},
;;       :methane {:reading-of-4096 402, :volts 0.323877},
;;       :danger {:reading-digital 1, :is-high? true}}}}  
  
  )

(spec/def ::result ::mfap-spec/non-blank-string)
(spec/def ::deviceID ::mfap-spec/non-blank-string)
(spec/def ::coreInfo (spec/keys :req-un [::deviceID]))
(spec/def ::BodyMap (spec/keys :req-un [::result
                                        ::coreInfo]))

(defn analog-metric->proportion [{:keys [reading-of-4096]}]
  (/ reading-of-4096 4096.0))

(defn fetch-metrics [_world]
  (let [{:keys [status body] :as resp} (particle-get "allmetricsjson")
        _ (when-not (= 200 status)
            (throw (ex-info "status not 200" {:response resp})))
        {:keys [result coreInfo] :as body-map}
        (cheshire/parse-string body true)
        _ (when-not (spec/valid? ::BodyMap body-map)
            (throw
             (ex-info "invalid body map"
                      {:explain-data (spec/explain-data ::BodyMap body-map)})))
        {:keys [heater methane] :as result-map}
        (cheshire/parse-string result true)
        _ (when-not (spec/valid? ::mfap-spec/Metrics result-map)
            (throw
             (ex-info
              "invalid metrics result"
              {:explain-data
               (spec/explain-data ::mfap-spec/Metrics result-map)})))
        result-map
        (-> result-map
            (assoc-in [:heater :proportion]
                      (analog-metric->proportion heater))
            (assoc-in [:methane :proportion]
                      (analog-metric->proportion methane)))]
    (merge
     result-map
     {:uuid (str (java.util.UUID/randomUUID))
      :datetime (str (time/instant))
      :device-id (:deviceID coreInfo)
      :request-meta (-> resp
                        (dissoc :body)
                        (assoc :body-map (dissoc body-map :result)))})))

(comment

  (fetch-metrics {})

  )
