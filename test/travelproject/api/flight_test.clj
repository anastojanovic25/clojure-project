(ns travelproject.api.flight-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.api.flight :as flight]))

;; (fact "Flight price between BEG and FCO is returned"
;; (let [price (flight/flight-cost "BEG" "FCO" "2026-03-15")]
;;  price => number?
;;  price => pos?) )

; (fact "Returned flight price is within a realistic range"
; (let [price (flight/flight-cost "BEG" "FCO" "2026-03-15")]
;; price => #(> % 20)
;;  price => #(< % 2000)) )

(def sample-offer-cheap
  {:price                  {:total "120.00"}
   :validatingAirlineCodes ["JU"]
   :itineraries
   [{:segments [{:departure   {:iataCode "BEG" :at "2026-03-15T10:00:00"}
                 :arrival     {:iataCode "VIE" :at "2026-03-15T11:00:00"}
                 :carrierCode "JU" :number "100"}
                {:departure   {:iataCode "VIE" :at "2026-03-15T12:00:00"}
                 :arrival     {:iataCode "FCO" :at "2026-03-15T13:10:00"}
                 :carrierCode "JU" :number "200"}]}]})

(def sample-offer-expensive
  (assoc-in sample-offer-cheap [:price :total] "180.00"))

(facts "normalize-flight-offer extracts a clean structure"
       (let [n (flight/normalize-flight-offer sample-offer-cheap)]
         (:price n) => 120.0
         (:airline n) => "JU"
         (:operating-airlines n) => ["JU"]
         (count (:itineraries n)) => 1
         (-> n :itineraries first :from) => "BEG"
         (-> n :itineraries first :to) => "FCO"
         (-> n :itineraries first :stops) => 1
         (-> n :itineraries first :via) => ["VIE"]))

(fact "best-flight-offer picks the cheapest offer (mocked)"
      (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20")
      => (contains {:price 120.0 :airline "JU"})
      (provided
        (flight/flight-offers "BEG" "FCO" "2026-03-15" "2026-03-20" 50)
        => [sample-offer-expensive sample-offer-cheap]))