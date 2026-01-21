(ns travelproject.api.flight
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [travelproject.config :as config]))

(def client-id
  (config/require! [:amadeus :client-id] "Missing Amadeus client-id in config.edn"))

(def client-secret
  (config/require! [:amadeus :client-secret] "Missing Amadeus client-secret in config.edn"))

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

(defn flight-offers
  [origin destination depart-date return-date max-results]
  (let [token (access-token)
        resp (http/get
               "https://test.api.amadeus.com/v2/shopping/flight-offers"
               {:headers {"Authorization" (str "Bearer " token)}
                :query-params (cond-> {:originLocationCode origin
                                       :destinationLocationCode destination
                                       :departureDate depart-date
                                       :adults 1
                                       :max (or max-results 10)}
                                      return-date (assoc :returnDate return-date))
                :throw-exceptions false})
        body (json/parse-string (:body resp) true)]
    (:data body)))

(defn normalize-flight-offer [offer]
  (let [total (some-> (get-in offer [:price :total]) Double/parseDouble)
        carrier (get-in offer [:validatingAirlineCodes 0])
        itineraries (:itineraries offer)

        ;; NEW: avio-kompanije koje stvarno operišu segmente (npr. KL, LH...)
        operating-airlines
        (->> itineraries
             (mapcat :segments)
             (map :carrierCode)
             distinct
             vec)]
    {:airline carrier
     :operating-airlines operating-airlines
     :price total
     :itineraries
     (->> itineraries
          (map (fn [it]
                 (let [segs (get it :segments)
                       first-seg (first segs)
                       last-seg  (last segs)
                       dep (get first-seg :departure)
                       arr (get last-seg  :arrival)
                       flight-nos (map (fn [s] (str (:carrierCode s) (:number s))) segs)
                       flight-no-str (apply str (interpose " / " flight-nos))]
                   {:from (get dep :iataCode)
                    :to   (get arr :iataCode)
                    :departure (get dep :at)
                    :arrival   (get arr :at)
                    :flight-number flight-no-str})))
          vec)}))


(defn best-flight-offer
  [origin destination depart-date return-date]
  (let [offers (flight-offers origin destination depart-date return-date 10)]
    (->> offers
         (map normalize-flight-offer)
         (filter #(number? (:price %)))
         (sort-by :price)
         first)))