(ns metrics-fetch-act-periodic.core
  (:require [clojure.pprint :as pp]
            [taoensso.timbre :as log]
            [metrics-fetch-act-periodic.db.core :as db]
            [metrics-fetch-act-periodic.detector :as detector]))


(defn do-the-deed! []
  (let [detector-res (detector/get-all-metrics)
        _insert-res (db/add-metrics! detector-res)
        #_ (log/info (with-out-str (pp/pprint {:insert-res insert-res})))]))


(comment

  (do-the-deed!)


  )
