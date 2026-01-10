(ns travelproject.api.accommodation
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def serpapi-key "686c671ff5eea556fd317db193e38b71b3f957dc8595d6f73ab3aaafc94f539f")

(defn hotels-response
  [city check-in check-out]
  (let [resp (http/get
               "https://serpapi.com/search.json"
               {:query-params {:engine "google_hotels"
                               :q (str "hotels in " city)
                               :location city
                               :check_in_date check-in
                               :check_out_date check-out
                               :api_key serpapi-key}
                :throw-exceptions false})]
    (json/parse-string (:body resp) true)))



(hotels-response "Rome" "2026-03-15" "2026-03-20")

(defn extract-prices
  [response]
  (->> (:properties response)
       (map #(get-in % [:rate_per_night :extracted_lowest]))
       (filter number?)))


(defn avg-hotel-price-per-night
  [city check-in check-out]
  (let [prices (extract-prices
                 (hotels-response city check-in check-out))]
    (if (seq prices)
      (double (/ (reduce + prices)
                 (count prices)))
      0.0)))

(defn accommodation-cost
  [city check-in check-out nights]
  (* (avg-hotel-price-per-night city check-in check-out)
     nights))

(accommodation-cost "Rome" "2026-03-15" "2026-03-20" 5)
(extract-prices
  (hotels-response "Rome" "2026-03-15" "2026-03-20"))
;; => (95 110 130 89 ...)

(avg-hotel-price-per-night "Rome" "2026-03-15" "2026-03-20")
;; => npr. 104.6

(accommodation-cost "Rome" "2026-03-15" "2026-03-20" 5)
;; => npr. 523.0
