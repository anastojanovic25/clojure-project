(ns travelproject.core-test
  (:require [midje.sweet :refer :all]
            [travelproject.core2 :as core]))


(facts "Average temperature of destinations"
       (avg-temp destinations) => 157/7)


(facts "Filter destinations by budget"
       (count (filter-by-budget destinations 700)) => 6)

(facts "Find hottest destination"
       (:city (hottest-destination destinations)) => "Bangkok")

(facts "Filter destinations by climate preference"
       (map :city (filter-by-climate destinations "cold")) => ["Stockholm"])

(facts "Find cheapest destination"
       (:city (cheapest-destination destinations)) => "Athens")


(facts "Average price for adventure trips"
       (avg-price-by-type destinations "adventure") => 700)
