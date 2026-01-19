(ns travelproject.api.distance-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.core2 :as core]))

(fact "Distance between Belgrade and Rome is a positive integer"
      (let [d (core/city-distance "Belgrade" "Rome")]
      d => number?
      d => pos?
      d => integer?))

(fact "Distance between Belgrade and Budapest is smaller than 1000 km"
      (let [d (core/city-distance "Belgrade" "Budapest")]
        d => number?
        d => pos?
        d => integer?
        d => #(< % 1000)))

