(ns travelproject.api.inspiration
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [travelproject.api.flight :as flight]))

(defonce loc-name-cache (atom {}))

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
            (Thread/sleep (long (* 400 attempt)))
            (recur (inc attempt)))

          :else
          (do
            (println "Amadeus flight-destinations failed" {:status status :body body})
            []))))))
