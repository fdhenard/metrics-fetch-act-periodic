(ns metrics-fetch-act-periodic.core
  (:require [clojure.pprint :as pp]
            [taoensso.timbre :as log]
            [metrics-fetch-act-periodic.db.core :as db]
            [metrics-fetch-act-periodic.detector :as detector]
            [metrics-fetch-act-periodic.notifier :as notifier]
            [metrics-fetch-act-periodic.utils.railway :as railway]))


(defn do-the-deed! []
  (let [[world errors]
        (railway/==>
         {}
         [:detector detector/fetch-metrics]
         [:notifier notifier/process-notification!]
         [:persist db/persist!])
        error (first errors)
        _ (when error
            (when (get-in world [:notifier :success?])
              (notifier/send-error! world error))
            (throw error))
        #_ (log/info (with-out-str (pp/pprint {:insert-res insert-res})))]
    world))


(comment

  (do-the-deed!)


  )
