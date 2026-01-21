(ns travelproject.api.inspiration
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [travelproject.api.flight :as flight]))

(defonce loc-name-cache (atom {}))

(defn- safe-str [s]
  (let [v (some-> s str/trim)]
    (when (and v (not (str/blank? v))) v)))

(defn- iata->city-name
   [iata]
  (let [k (safe-str iata)]
    (when k
      (or (get @loc-name-cache k)
          (let [token (flight/access-token)
                resp  (http/get "https://test.api.amadeus.com/v1/reference-data/locations"
                                {:headers {"Authorization" (str "Bearer " token)}
                                 :query-params {:keyword k
                                                :subType "CITY,AIRPORT"
                                                :page {:limit 10}}
                                 :throw-exceptions false})
                body  (json/parse-string (:body resp) true)
                first-hit (-> body :data first)
                city (or (get-in first-hit [:address :cityName])
                         (:name first-hit))
                city (safe-str city)
                city (or city k)]
            (swap! loc-name-cache assoc k city)
            city)))))

(defn flight-destinations
  [origin-iata max-price]
  (let [token (flight/access-token)]
    (loop [attempt 1]
      (let [resp   (http/get "https://test.api.amadeus.com/v1/shopping/flight-destinations"
                             {:headers {"Authorization" (str "Bearer " token)}
                              :query-params {:origin origin-iata
                                             :maxPrice (int max-price)}
                              :throw-exceptions false})
            status (:status resp)
            body   (json/parse-string (:body resp) true)]
        (cond
          (= 200 status)
          (->> (:data body)
               (map (fn [x] {:city-iata (:destination x)}))
               (remove #(nil? (:city-iata %)))
               vec)

          (and (= 500 status) (< attempt 3))
          (do
            (println "Amadeus 500 on flight-destinations, retry" attempt)
            (Thread/sleep (long (* 400 attempt)))
            (recur (inc attempt)))

          :else
          (do
            (println "Amadeus flight-destinations failed" {:status status :body body})
            []))))))
