(ns travelproject.recommender.budget
  (:require
    [travelproject.api.flight :as flight]
    [travelproject.api.accommodation :as acc]
    [travelproject.api.distance :as dist]
    [travelproject.api.geocoding :as geo]
    [travelproject.recommender.trip-type :as tt]
    [travelproject.api.location :as loc]))


(def default-cost-per-km 0.15)        ;; fuel+wear approx
(def default-car-min-fee 60.0)        ;; fixed baseline (parking, misc, etc.)
(def default-car-tolls   50.0)        ;; rough tolls/vignettes baseline


(defn round2 [x]
  (when (number? x)
    (/ (Math/round (* 100.0 (double x))) 100.0)))

(defn car-trip-cost
  [{:keys [origin-city city cost-per-km car-min-fee car-tolls]}]
  (let [from (geo/city-coords origin-city)
        to   (geo/city-coords city)]
    (when (and from to)
      (let [km  (dist/distance from to)
            cpk (double (or cost-per-km default-cost-per-km))
            min-fee (double (or car-min-fee default-car-min-fee))
            tolls   (double (or car-tolls   default-car-tolls))]
        (when (number? km)
          (+ (* 2.0 (double km) cpk) min-fee tolls))))))

(defn transport-cost
  [{:keys [transport origin-iata origin-city check-in check-out
           cost-per-km car-min-fee car-tolls]}
   {:keys [city city-iata] :as _cand}]
  (let [one-way (flight/flight-cost origin-iata city-iata check-in)
        flight-offer (when (and origin-iata city-iata check-in check-out)
                       (flight/best-flight-offer origin-iata city-iata check-in check-out))

        planeE (cond
                 (and flight-offer (number? (:price flight-offer)))
                 (double (:price flight-offer))
                 (number? one-way)
                 (* 2.0 (double one-way))
                 :else nil)
        carE    (car-trip-cost {:origin-city origin-city
                                :city city
                                :cost-per-km cost-per-km
                                :car-min-fee car-min-fee
                                :car-tolls car-tolls})

        p       (when (number? planeE) (double planeE))
        c       (when (number? carE)   (double carE))]

    (case (or transport :any)
      :plane (when p
               {:mode :plane :transportE p :planeE p :carE c :flight flight-offer})

      :car   (when c
               {:mode :car :transportE c :planeE p :carE c})

      :any   (cond
               (and p c)
               (if (<= p c)
                 {:mode :plane :transportE p :planeE p :carE c :reason :cheaper-plane :flight flight-offer}
                 {:mode :car   :transportE c :planeE p :carE c :reason :cheaper-car})

               p {:mode :plane :transportE p :planeE p :carE c :reason :only-option :flight flight-offer}
               c {:mode :car   :transportE c :planeE p :carE c :reason :only-option}
               :else nil)
      (cond
        (and p c) (if (<= p c)
                    {:mode :plane :transportE p :planeE p :carE c :flight flight-offer}
                    {:mode :car   :transportE c :planeE p :carE c})
        p {:mode :plane :transportE p :planeE p :carE c :flight flight-offer}
        c {:mode :car   :transportE c :planeE p :carE c}
        :else nil))))

(defn estimate-trip-cost
  [{:keys [transport check-in check-out nights budget origin-iata origin-city
           cost-per-km car-min-fee car-tolls] :as req}
   {:keys [city] :as cand}]
  (let [requested (or transport :any)
        {:keys [mode transportE planeE carE reason flight]}
        (transport-cost {:transport   requested
                         :origin-iata origin-iata
                         :origin-city origin-city
                         :check-in    check-in
                         :check-out   check-out   ;; <-- DODAJ OVO
                         :cost-per-km cost-per-km
                         :car-min-fee car-min-fee
                         :car-tolls   car-tolls}
                        cand)]
    (when (number? transportE)
      (let [
            hotel  (when (number? budget)
                     (acc/recommend-hotel city check-in check-out nights budget))
            hotelE (cond
                     (and hotel (number? (:total-price hotel)))
                     (double (:total-price hotel))

                     :else
                     (acc/accommodation-cost city check-in check-out nights))]

        (when (and (number? hotelE) (pos? (double hotelE)))
          (let [t (double transportE)
                h (double hotelE)]
            {:city city
             :transport mode
             :requested-transport requested
             :reason (when (= requested :any) reason)

             :planeE (when (number? planeE) (double planeE))
             :carE   (when (number? carE) (round2 (double carE)))

             :flight (when (= mode :plane) flight)
             :hotel  hotel

             :transportE (round2 t)
             :hotelE     (round2 h)
             :totalE     (round2 (+ t h))}))))))


(defn recommend-by-budget
  [{:keys [budget trip-type] :as req} candidates]
  (->> candidates
       (keep (fn [cand] (estimate-trip-cost req cand)))
       (filter (fn [m]
                 (and (number? (:totalE m))
                      (<= (:totalE m) budget))))
       (map (fn [m]
              (let [s (or (tt/score-city trip-type (:city m)) 0.0)]
                (assoc m :typeScore (double s) :tripType trip-type))))
       (sort-by (juxt (comp - :typeScore) :totalE))
       (take 5)))

(defn recommend-for-city
  [req city]
  (let [cand {:city city
              :city-iata (loc/city->airport-iata city)}
        r    (estimate-trip-cost req cand)
        b    (:budget req)]
    (cond
      (nil? r) []

      (and (number? b) (number? (:totalE r))
           (> (double (:totalE r)) (double b)))
      [(assoc r :overE (round2 (- (double (:totalE r)) (double b))))]

      :else
      [r])))


(def candidates
  [{:city "Rome" :city-iata "FCO"}
   {:city "Athens" :city-iata "ATH"}
   {:city "Barcelona" :city-iata "BCN"}
   {:city "Paris" :city-iata "CDG"}])
