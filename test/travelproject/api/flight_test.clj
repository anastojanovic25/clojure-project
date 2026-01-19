(ns travelproject.api.flight-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.api.flight :as flight]))


(fact "Flight price between BEG and FCO is returned"
      (let [price (flight/flight-cost "BEG" "FCO" "2026-03-15")]
        price => number?
        price => pos?))

(fact "Returned flight price is within a realistic range"
      (let [price (flight/flight-cost "BEG" "FCO" "2026-03-15")]
        price => #(> % 20)
        price => #(< % 2000)))