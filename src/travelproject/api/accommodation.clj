(ns travelproject.api.accommodation
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [travelproject.config :as config]))

(defn- serpapi-key []
  (config/require! [:serpapi :key] "Missing SerpApi key in config.edn"))

(defn hotels-response
  [city check-in check-out]
  (let [resp (http/get
               "https://serpapi.com/search.json"
               {:query-params {:engine "google_hotels"
                               :q (str "hotels in " city)
                               :location city
                               :check_in_date check-in
                               :check_out_date check-out
                               :api_key (serpapi-key)}
                :throw-exceptions false})]
    (json/parse-string (:body resp) true)))


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

(defn normalize-hotel
  [p nights]
  (let [ppn (or (get-in p [:rate_per_night :extracted_lowest])
                (get-in p [:rate_per_night :extracted])
                (get-in p [:rate_per_night :lowest]))
        rating (or (get p :overall_rating)
                   (get p :rating))
        name (or (get p :name) (get p :property_name))]
    {:name name
     :rating rating
     :price-per-night ppn
     :total-price (when (number? ppn) (* (double ppn) (double nights)))}))

(defn hotel-options
  [city check-in check-out nights]
  (let [resp (hotels-response city check-in check-out)
        props (:properties resp)]
    (->> props
         (map #(normalize-hotel % nights))
         (filter #(and (:name %) (number? (:price-per-night %))))
         vec)))

(defn best-hotel
  [options max-budget]
  (->> options
       (filter (fn [h]
                 (and (number? (:total-price h))
                      (<= (double (:total-price h)) (double max-budget)))))
       (sort-by (juxt (comp - (fn [h] (double (or (:rating h) 0.0))))
                      (fn [h] (double (:total-price h)))))
       first))

(defn recommend-hotel
  [city check-in check-out nights max-budget]
  (let [opts (hotel-options city check-in check-out nights)]
    (best-hotel opts max-budget)))