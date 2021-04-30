(ns metrics-fetch-act-periodic.lambda-interface
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [cheshire.core :as cheshire]
            [metrics-fetch-act-periodic.core :as core])
  (:gen-class
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))


(defn -handleRequest [_ _input-stream output-stream _context]
  (let [world (core/do-the-deed!)]
    (with-open [writer (io/writer output-stream)]
      (cheshire/generate-stream {:success true
                                 :world world} writer))))
