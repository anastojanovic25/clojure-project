(ns travelproject.recommender.budget
  (:require
    [travelproject.api.flight :as flight]
    [travelproject.api.accommodation :as acc]
    [travelproject.api.distance :as dist]
    [travelproject.api.geocoding :as geo]
    [travelproject.api.location :as loc]
    [travelproject.api.inspiration :as insp]))


(def default-cost-per-km 0.15)        ;; fuel+wear approx
(def default-car-min-fee 60.0)        ;; fixed baseline (parking, misc, etc.)

(defn simulate-tolls-eur
  [km]
  (let [km (double (max 0.0 (or km 0.0)))]
    (cond
      (< km 200)  (+ 5.0  (rand 8.0))
      (< km 600)  (+ 7.0  (rand 25.0))
      (< km 1200) (+ 10.0 (rand 40.0))
      :else       (+ 20.0 (rand 80.0)))))


(defn round2 [x]
  (when (number? x)
    (/ (Math/round (* 100.0 (double x))) 100.0)))

(defn car-trip-cost
  [{:keys [origin-city city cost-per-km car-min-fee car-tolls]}]
  (let [from (geo/city-coords origin-city)
        to   (geo/city-coords city)]
    (when (and from to)
      (let [km      (dist/distance from to)
            cpk     (double (or cost-per-km default-cost-per-km))
            min-fee (double (or car-min-fee default-car-min-fee))]
        (when (number? km)
          (let [tolls (double (or (when (number? car-tolls) car-tolls)
                                  (simulate-tolls-eur km)))]
            (+ (* 2.0 (double km) cpk) min-fee tolls)))))))


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
                         :check-out   check-out
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
  [{:keys [budget] :as req} candidates]
  (->> candidates
       (keep (fn [cand] (estimate-trip-cost req cand)))
       (filter (fn [m]
                 (and (number? (:totalE m))
                      (<= (:totalE m) budget))))
       (sort-by :totalE)
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
   {:city "Paris" :city-iata "CDG"}
   {:city "London" :city-iata "LON"}
   {:city "Berlin" :city-iata "BER"}
   {:city "Prague" :city-iata "PRG"}])

(defn dynamic-candidates
  [{:keys [origin-city-iata origin-iata budget]}]
  (let [max-flight (int (* 0.4 (double budget)))
        origin (or origin-city-iata origin-iata)
        cands (insp/flight-destinations origin max-flight)]
    (if (seq cands) cands candidates)))

