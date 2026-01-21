(ns travelproject.api.geocoding
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [travelproject.config :as config]))

(def api-key
  (config/require! [:ors :api-key] "Missing ORS api-key in config.edn"))

(defn city-coords [city]
  (let [resp (http/get "https://api.openrouteservice.org/geocode/search"
                       {:headers {"Authorization" api-key}
                        :query-params {:text city
                                       :size 1}})
        body (json/parse-string (:body resp) true)
        coords (-> body :features first :geometry :coordinates)]
    {:lon (first coords)
     :lat (second coords)}))
