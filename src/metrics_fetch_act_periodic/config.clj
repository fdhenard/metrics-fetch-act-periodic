(ns metrics-fetch-act-periodic.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cprop.source]
            [cprop.core :as cprop]))


(def config-dir-path
  (string/join "/" [(System/getProperty "user.home")
                    "Dropbox"
                    "dev"
                    "dev-local-config"]))


(def ^java.io.File dev-local-config-file
  (io/as-file (string/join "/" [config-dir-path
                                "metrics-fetch-act-periodic.edn"])))

(def ^java.io.File user-config-file
  (io/as-file (string/join "/" [config-dir-path
                                "user-config.edn"])))

(defn is-running-local? []
  (.exists dev-local-config-file))

(defn file->hashmap [file]
  (if-not (.exists file)
    {}
    (or (-> file
            slurp
            edn/read-string)
        {})))

(def config (cprop/load-config
             :resource "config.edn"
             :merge [(file->hashmap dev-local-config-file)
                     (file->hashmap user-config-file)
                     (cprop.source/from-system-props)
                     (cprop.source/from-env)]))

(comment

  config

  )
