(ns travelproject.recommender.budget-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.recommender.budget :as rb]
    [travelproject.api.flight :as flight]
    [travelproject.api.accommodation :as acc]))

(def candidates
  [{:city "Paris" :city-iata "CDG"}
   {:city "Rome"  :city-iata "FCO"}])

(def base-req
  {:budget 800
   :origin-iata "BEG"
   :origin-city "Belgrade"
   :check-in "2026-03-15"
   :check-out "2026-03-20"
   :nights 5})

(facts "plane only"
       (rb/recommend-by-budget (assoc base-req :transport :plane :budget 1000) candidates)
       => (just [(contains {:city "Rome"  :transport :plane :transportE 200.0 :hotelE 200.0 :totalE 400.0})
                 (contains {:city "Paris" :transport :plane :transportE 200.0 :hotelE 300.0 :totalE 500.0})])
       (provided
         (flight/flight-cost "BEG" "CDG" "2026-03-15") => 100.0
         (flight/flight-cost "BEG" "FCO" "2026-03-15") => 100.0
         (acc/accommodation-cost "Paris" "2026-03-15" "2026-03-20" 5) => 300.0
         (acc/accommodation-cost "Rome"  "2026-03-15" "2026-03-20" 5) => 200.0))

(facts "car only"
       (rb/recommend-by-budget (assoc base-req :transport :car :budget 1000) candidates)
       => (just [(contains {:city "Rome" :transport :car :transportE 150.0 :hotelE 200.0 :totalE 350.0})
                 (contains {:city "Paris" :transport :car :transportE 250.0 :hotelE 300.0 :totalE 550.0})])
       (provided
         (flight/flight-cost anything anything anything) => (throws Exception)

         (rb/car-trip-cost {:origin-city "Belgrade" :city "Paris"}) => 250.0
         (rb/car-trip-cost {:origin-city "Belgrade" :city "Rome"})  => 150.0

         (acc/accommodation-cost "Paris" "2026-03-15" "2026-03-20" 5) => 300.0
         (acc/accommodation-cost "Rome"  "2026-03-15" "2026-03-20" 5) => 200.0))

(facts ":any travel"
       (rb/recommend-by-budget (assoc base-req :transport :any :budget 1000) candidates)
       => (contains [(contains {:city "Paris" :transport :car   :transportE 150.0})
                     (contains {:city "Rome"  :transport :plane :transportE 160.0})]
                    :in-any-order)
       (provided
         (flight/flight-cost "BEG" "CDG" "2026-03-15") => 120.0
         (flight/flight-cost "BEG" "FCO" "2026-03-15") => 80.0

         (rb/car-trip-cost {:origin-city "Belgrade" :city "Paris"}) => 150.0
         (rb/car-trip-cost {:origin-city "Belgrade" :city "Rome"})  => 500.0

         (acc/accommodation-cost "Paris" "2026-03-15" "2026-03-20" 5) => 300.0
         (acc/accommodation-cost "Rome"  "2026-03-15" "2026-03-20" 5) => 200.0))

(facts ":any travel with only one option"
       (rb/recommend-by-budget (assoc base-req :transport :any :budget 1000) candidates)
       => (contains [(contains {:city "Paris" :transport :car   :transportE 180.0})
                     (contains {:city "Rome"  :transport :plane :transportE 200.0})]
                    :in-any-order)
       (provided
         (flight/flight-cost "BEG" "CDG" "2026-03-15") => nil
         (flight/flight-cost "BEG" "FCO" "2026-03-15") => 100.0

         (rb/car-trip-cost {:origin-city "Belgrade" :city "Paris"}) => 180.0
         (rb/car-trip-cost {:origin-city "Belgrade" :city "Rome"})  => nil

         (acc/accommodation-cost "Paris" "2026-03-15" "2026-03-20" 5) => 300.0
         (acc/accommodation-cost "Rome"  "2026-03-15" "2026-03-20" 5) => 200.0))


(facts "filters by budget"
       (rb/recommend-by-budget (assoc base-req :transport :plane :budget 500) candidates)
       => (just [(contains {:city "Rome" :totalE 400.0})])
       (provided
         (flight/flight-cost "BEG" "CDG" "2026-03-15") => 400.0
         (acc/accommodation-cost "Paris" "2026-03-15" "2026-03-20" 5) => 500.0

         (flight/flight-cost "BEG" "FCO" "2026-03-15") => 100.0
         (acc/accommodation-cost "Rome" "2026-03-15" "2026-03-20" 5) => 200.0))
