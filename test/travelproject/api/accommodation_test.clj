(ns travelproject.api.accommodation-test
  (:require [midje.sweet :refer :all]
            [travelproject.api.accommodation :as acc]))

(fact "normalize-hotel computes total price from nights"
      (acc/normalize-hotel {:name "Hotel A"
                            :overall_rating 8.9
                            :rate_per_night {:extracted_lowest 50}} 3)
      => (contains {:name "Hotel A"
                    :rating 8.9
                    :price-per-night 50
                    :total-price 150.0}))

(facts "best-hotel chooses highest rating within budget, tie -> cheaper"
       (let [opts [{:name "X" :rating 9.2 :total-price 400}
                   {:name "Y" :rating 9.5 :total-price 600}
                   {:name "Z" :rating 9.5 :total-price 500}]]
         (acc/best-hotel opts 520)
         => (contains {:name "Z" :rating 9.5 :total-price 500})))

