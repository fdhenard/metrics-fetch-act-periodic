(ns metrics-fetch-act-periodic.spec.core
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]))


(s/def ::non-blank-string (s/and string? (complement string/blank?)))

(s/def ::Volts double?)
(s/def ::ReadingOf4096 int?)

(s/def ::reading-of-4096 ::ReadingOf4096)
(s/def ::volts ::Volts)

(s/def ::AnalogReading (s/keys :req-un [::reading-of-4096
                                        ::volts]))

(s/def ::reading-digital (s/and int? #{0 1}))
(s/def ::is-high? boolean?)
(s/def ::DigitalReading (s/keys :req-un [::reading-digital
                                         ::is-high?]))


(s/def ::heater ::AnalogReading)
(s/def ::methane ::AnalogReading)
(s/def ::danger ::DigitalReading)

(s/def ::Metrics (s/keys :req-un [::heater
                                  ::methane
                                  ::danger]))
