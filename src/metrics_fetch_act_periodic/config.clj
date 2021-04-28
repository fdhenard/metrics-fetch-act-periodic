(ns metrics-fetch-act-periodic.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cprop.source]
            [cprop.core :as cprop]))


(def ^java.io.File dev-local-config-file
  (let [path (str (System/getProperty "user.home")
                  "/dev/dev-local-config-files/metrics-fetch-act-periodic.edn")]
    (io/as-file path)))

(defn is-running-local? []
  (.exists dev-local-config-file))

(def dev-local-config
  (if-not (.exists dev-local-config-file)
    {}
    (or (-> dev-local-config-file
            slurp
            edn/read-string)
        {})))

(def config (cprop/load-config
             :resource "config.edn"
             :merge [dev-local-config
                     (cprop.source/from-system-props)
                     (cprop.source/from-env)]))

(comment

  config

  )
