(ns travelproject.api.weather
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [travelproject.config :as config]))

(def api-key
  (config/require! [:weather :api-key] "Missing Weather api-key in config.edn"))

(defn current-weather [city]
  (let [resp (http/get "https://api.weatherapi.com/v1/current.json"
                       {:query-params {:key api-key
                                       :q city
                                       :aqi "no"}})
        body (json/parse-string (:body resp) true)]
    {:temp (-> body :current :temp_c)
     :rain? (> (-> body :current :precip_mm) 0)
     :weather (-> body :current :condition :text)}))


