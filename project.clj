(defproject metrics-fetch-act-periodic "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.taoensso/timbre "5.1.2"]
                 [com.taoensso/faraday "1.11.1"
                  :exclusions [#_commons-logging
                               #_com.taoensso/encore]]
                 [cprop "0.1.17"]

                 [org.martinklepsch/clj-http-lite "0.4.3"]
                 [org.clojure/data.json "2.2.2"]

                 [clojure.java-time "0.3.2"]
                 ]
  :repl-options {:init-ns metrics-fetch-act-periodic.core})

