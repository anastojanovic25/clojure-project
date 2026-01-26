(ns travelproject.api.distance
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [travelproject.config :as config]))

(defn- api-key []
  (config/require! [:ors :api-key] "Missing ORS api-key in config.edn"))

(defn distance [from to]
  (let [resp (http/post
               "https://api.openrouteservice.org/v2/matrix/driving-car"
               {:headers {"Authorization" (api-key)
                          "Content-Type" "application/json"
                          "Accept" "application/json"}
                :throw-exceptions false
                :body (json/generate-string
                        {:locations [[(:lon from) (:lat from)]
                                     [(:lon to)   (:lat to)]]
                         :metrics ["distance"]})})
        body (json/parse-string (:body resp) true)]
    (/ (-> body :distances first second) 1000)))



