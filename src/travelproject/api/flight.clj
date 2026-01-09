(ns travelproject.api.flight
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))


(def client-id "4XdEeeSY2eRG8DRJxbDg1Z7AVxfpqUT4")
(def client-secret "cvvFqXcNQ6RC00cK")


(defn access-token []
  (let [resp (http/post
               "https://test.api.amadeus.com/v1/security/oauth2/token"
               {:form-params {:grant_type "client_credentials"
                              :client_id client-id
                              :client_secret client-secret}
                :throw-exceptions false})
        body (json/parse-string (:body resp) true)]
    (:access_token body)))


;; Flight price- one-way- IMPORTANT

(defn flight-cost
  "Returns total price of the cheapest one-way flight"
  [origin destination date]
  (let [token (access-token)
        resp (http/get
               "https://test.api.amadeus.com/v2/shopping/flight-offers"
               {:headers {"Authorization" (str "Bearer " token)}
                :query-params {:originLocationCode origin
                               :destinationLocationCode destination
                               :departureDate date
                               :adults 1
                               :max 1}
                :throw-exceptions false})
        body (json/parse-string (:body resp) true)]
    (when-let [price (-> body :data first :price :total)]
      (Double/parseDouble price))))

(flight-cost "BEG" "FCO" "2026-03-15")


