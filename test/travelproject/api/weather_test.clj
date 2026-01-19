(ns travelproject.api.weather-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.api.weather :as weather]))

(fact "Weather API returns valid weather data"
      (let [result (weather/current-weather "Rome")]
        result => map?
        (:temp result) => number?
        (:rain? result) => boolean?
        (:weather result) => string?))

