(ns travelproject.recommender.budget-test
  (:require [midje.sweet :refer :all]
            [travelproject.recommender.budget :as rb]
            [travelproject.api.flight :as flight]
            [travelproject.api.accommodation :as acc]
            [travelproject.api.geocoding :as geo]
            [travelproject.api.distance :as dist]))

(def base-req
  {:budget 800
   :origin-iata "BEG"
   :origin-city "Belgrade"
   :check-in "2026-03-15"
   :check-out "2026-03-20"
   :nights 5})

(def cand-rome  {:city "Rome"  :city-iata "FCO"})
(def cand-paris {:city "Paris" :city-iata "CDG"})


(facts "round2"
       (rb/round2 12.345) => 12.35
       (rb/round2 12.344) => 12.34
       (rb/round2 nil)    => nil
       (rb/round2 "x")    => nil)

(fact "car-trip-cost returns nil if coords missing"
      (rb/car-trip-cost {:origin-city "X" :city "Rome"}) => nil
      (provided
        (geo/city-coords "X") => nil
        (geo/city-coords "Rome") => {:lat 1 :lon 2}))

(fact "car-trip-cost returns nil if distance is not numeric"
      (rb/car-trip-cost {:origin-city "Belgrade" :city "Rome"}) => nil
      (provided
        (geo/city-coords "Belgrade") => {:lat 1 :lon 1}
        (geo/city-coords "Rome")     => {:lat 2 :lon 2}
        (dist/distance {:lat 1 :lon 1} {:lat 2 :lon 2}) => "400km"))

(fact "transport-cost :plane uses best-flight-offer price if available"
      (rb/transport-cost (assoc base-req :transport :plane) cand-rome)
      => (contains {:mode :plane :transportE 220.0 :planeE 220.0})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => {:price 220.0}
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => 999.0
        (rb/car-trip-cost (contains {:city "Rome"})) => 500.0))

(fact "transport-cost :plane falls back to 2*one-way when no offer"
      (rb/transport-cost (assoc base-req :transport :plane) cand-rome)
      => (contains {:mode :plane :transportE 200.0 :planeE 200.0})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => 100.0
        (rb/car-trip-cost (contains {:city "Rome"})) => 500.0))

(fact "transport-cost :car returns car cost when available"
      (rb/transport-cost (assoc base-req :transport :car) cand-paris)
      => (contains {:mode :car :transportE 300.0})
      (provided
        (flight/best-flight-offer "BEG" "CDG" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "CDG" "2026-03-15") => 100.0
        (rb/car-trip-cost (contains {:city "Paris"})) => 300.0))

(fact "transport-cost :any chooses cheaper plane when p <= c"
      (rb/transport-cost (assoc base-req :transport :any) cand-rome)
      => (contains {:mode :plane :transportE 180.0 :reason :cheaper-plane})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => 90.0  ;; 2*90=180
        (rb/car-trip-cost (contains {:city "Rome"})) => 400.0))

(fact "transport-cost :any chooses cheaper car when c < p"
      (rb/transport-cost (assoc base-req :transport :any) cand-rome)
      => (contains {:mode :car :transportE 150.0 :reason :cheaper-car})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => 120.0 ;; 240
        (rb/car-trip-cost (contains {:city "Rome"})) => 150.0))

(fact "transport-cost :any with only plane option sets reason :only-option"
      (rb/transport-cost (assoc base-req :transport :any) cand-rome)
      => (contains {:mode :plane :reason :only-option})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => 100.0
        (rb/car-trip-cost (contains {:city "Rome"})) => nil))

(fact "transport-cost :any with only car option sets reason :only-option"
      (rb/transport-cost (assoc base-req :transport :any) cand-rome)
      => (contains {:mode :car :reason :only-option})
      (provided
        (flight/best-flight-offer "BEG" "FCO" "2026-03-15" "2026-03-20") => nil
        (flight/flight-cost "BEG" "FCO" "2026-03-15") => nil
        (rb/car-trip-cost (contains {:city "Rome"})) => 180.0))


(fact "estimate-trip-cost returns nil when transportE is not number"
      (rb/estimate-trip-cost (assoc base-req :transport :plane) cand-rome) => nil
      (provided
        (rb/transport-cost (contains {:transport :plane}) cand-rome)
        => {:mode :plane :transportE nil}))


(fact "estimate-trip-cost uses recommend-hotel when budget is number and hotel has total-price"
      (rb/estimate-trip-cost (assoc base-req :transport :plane :budget 800) cand-rome)
      => (contains {:city "Rome" :transport :plane :transportE 200.0 :hotelE 300.0 :totalE 500.0})
      (provided
        (rb/transport-cost (contains {:transport :plane}) cand-rome)
        => {:mode :plane :transportE 200.0 :planeE 200.0 :flight {:price 200.0}}

        (acc/recommend-hotel "Rome" "2026-03-15" "2026-03-20" 5 800)
        => {:name "Hotel A" :total-price 300.0}))


(fact "estimate-trip-cost falls back to accommodation-cost when recommend-hotel is nil"
      (rb/estimate-trip-cost (assoc base-req :transport :plane :budget 800) cand-rome)
      => (contains {:transportE 200.0 :hotelE 320.0 :totalE 520.0})
      (provided
        (rb/transport-cost (contains {:transport :plane}) cand-rome)
        => {:mode :plane :transportE 200.0 :planeE 200.0 :flight {:price 200.0}}

        (acc/recommend-hotel "Rome" "2026-03-15" "2026-03-20" 5 800) => nil
        (acc/accommodation-cost "Rome" "2026-03-15" "2026-03-20" 5) => 320.0))

(fact "estimate-trip-cost returns nil when hotelE is not positive"
      (rb/estimate-trip-cost (assoc base-req :transport :plane :budget 800) cand-rome) => nil
      (provided
        (rb/transport-cost (contains {:transport :plane}) cand-rome)
        => {:mode :plane :transportE 200.0 :planeE 200.0}

        (acc/recommend-hotel "Rome" "2026-03-15" "2026-03-20" 5 800) => nil
        (acc/accommodation-cost "Rome" "2026-03-15" "2026-03-20" 5) => 0.0))

(fact "estimate-trip-cost sets :reason only when requested transport is :any"
      (rb/estimate-trip-cost (assoc base-req :transport :any) cand-rome)
      => (contains {:requested-transport :any :reason :cheaper-car})
      (provided
        (rb/transport-cost (contains {:transport :any}) cand-rome)
        => {:mode :car :transportE 150.0 :carE 150.0 :reason :cheaper-car}

        (acc/recommend-hotel "Rome" "2026-03-15" "2026-03-20" 5 800)
        => {:name "Hotel A" :total-price 200.0}))

(fact "estimate-trip-cost does not set :reason when requested transport fixed"
      (rb/estimate-trip-cost (assoc base-req :transport :car) cand-rome)
      => (contains {:requested-transport :car :reason nil :transport :car})
      (provided
        (rb/transport-cost (contains {:transport :car}) cand-rome)
        => {:mode :car :transportE 150.0 :carE 150.0 :reason :cheaper-car}

        (acc/recommend-hotel "Rome" "2026-03-15" "2026-03-20" 5 800)
        => {:name "Hotel A" :total-price 200.0}))



(fact "recommend-by-budget filters trips above budget and sorts by totalE"
      (let [req (assoc base-req :budget 500 :transport :plane)]
        (rb/recommend-by-budget req [cand-rome cand-paris])
        => (just [(contains {:city "Rome" :totalE 400.0})])
        (provided
          (rb/estimate-trip-cost req cand-rome)  => {:city "Rome" :totalE 400.0}
          (rb/estimate-trip-cost req cand-paris) => {:city "Paris" :totalE 700.0})))


(fact "recommend-by-budget returns at most 5 results"
      (let [req   (assoc base-req :budget 9999 :transport :plane)
            cands (mapv (fn [i] {:city (str "C" i) :city-iata "XXX"}) (range 10))]
        (count (rb/recommend-by-budget req cands)) => 5
        (provided
          (rb/estimate-trip-cost req anything) => {:city "X" :totalE 100.0})))


(fact "recommend-by-budget returns empty"
      (let [req (assoc base-req :budget 50 :transport :plane)]
        (rb/recommend-by-budget req [cand-rome cand-paris]) => []
        (provided
          (rb/estimate-trip-cost req cand-rome)  => {:city "Rome"  :totalE 400.0}
          (rb/estimate-trip-cost req cand-paris) => {:city "Paris" :totalE 700.0})))

