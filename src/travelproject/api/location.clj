(ns travelproject.api.location
  (:require
    [clj-http.client :as http]
    [cheshire.core :as json]
    [clojure.string :as str]
    [travelproject.api.flight :as flight]))

(defonce iata-cache (atom {}))

(defn- norm [s]
  (-> (or s "") str/trim str/lower-case))

(defn city->iata
  [city]
  (let [k (norm city)]
    (or (get @iata-cache k)
        (let [token (flight/access-token)
              resp  (http/get "https://test.api.amadeus.com/v1/reference-data/locations"
                              {:headers {"Authorization" (str "Bearer " token)}
                               :query-params {:keyword city
                                              :subType "CITY,AIRPORT"
                                              :page {:limit 10}}
                               :throw-exceptions false})
              body  (json/parse-string (:body resp) true)
              data  (:data body)
              code  (or (some (fn [x]
                                (when (= "CITY" (:subType x))
                                  (:iataCode x)))
                              data)
                        (-> data first :iataCode))]
          (when code
            (swap! iata-cache assoc k code))
          code))))

(defonce airport-iata-cache (atom {}))

(defn city->airport-iata
   [city]
  (let [k (norm city)]
    (or (get @airport-iata-cache k)
        (let [token (flight/access-token)
              resp  (http/get "https://test.api.amadeus.com/v1/reference-data/locations"
                              {:headers {"Authorization" (str "Bearer " token)}
                               :query-params {:keyword city
                                              :subType "CITY,AIRPORT"
                                              :page {:limit 10}}
                               :throw-exceptions false})
              body  (json/parse-string (:body resp) true)
              data  (:data body)
              code  (or (some (fn [x]
                                (when (= "AIRPORT" (:subType x))
                                  (:iataCode x)))
                              data)
                        (city->iata city))]
          (when code
            (swap! airport-iata-cache assoc k code))
          code))))

(defn city->city-iata
  [city]
  (let [token (flight/access-token)
        resp  (http/get "https://test.api.amadeus.com/v1/reference-data/locations"
                        {:headers {"Authorization" (str "Bearer " token)}
                         :query-params {:keyword city
                                        :subType "CITY"
                                        :page {:limit 10}}
                         :throw-exceptions false})
        body  (json/parse-string (:body resp) true)
        data  (:data body)]
    (or (-> data first :iataCode)   ;; npr "PAR"
        (city->airport-iata city)))) ;; fallback na tvoju postojecu
