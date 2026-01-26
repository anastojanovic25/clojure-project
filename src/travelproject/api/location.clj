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


(defonce airport-label-cache (atom {}))

(defn iata->airport-label
  [iata]
  (let [code (some-> iata str/trim str/upper-case)]
    (when (seq code)
      (or (get @airport-label-cache code)
          (let [token (flight/access-token)
                resp  (http/get "https://test.api.amadeus.com/v1/reference-data/locations"
                                {:headers {"Authorization" (str "Bearer " token)}
                                 ;; BITNO: page[limit] mora ovako (ne kao nested mapa)
                                 :query-params {"keyword" code
                                                "subType" "AIRPORT"
                                                "page[limit]" "10"}
                                 :throw-exceptions false})
                body  (json/parse-string (:body resp) true)
                data  (:data body)
                hit   (or (some #(when (= code (:iataCode %)) %) data)
                          (first data))
                name  (:name hit)
                city  (get-in hit [:address :cityName])
                label (cond
                        (and city name) (str code " (" city " - " name ")")
                        name            (str code " (" name ")")
                        :else           code)]
            (swap! airport-label-cache assoc code label)
            label)))))
