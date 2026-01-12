(ns travelproject.recommender.budget
  (:require
    [travelproject.api.flight :as flight]
    [travelproject.api.accommodation :as acc]
    [travelproject.api.distance :as dist]
    [travelproject.api.geocoding :as geo]))

(def default-cost-per-km 0.15)

(def default-cost-per-km 0.15)

(defn car-trip-cost
 [{:keys [origin-city city cost-per-km]}]
  (let [from (geo/city-coords origin-city)
        to   (geo/city-coords city)]
    (when (and from to)
      (let [km  (dist/distance from to)
            cpk (double (or cost-per-km default-cost-per-km))]
        (when (number? km)
          (* (double km) cpk))))))

(defn transport-cost
  [{:keys [transport origin-iata origin-city check-in]} {:keys [city city-iata] :as cand}]
  (let [one-way (flight/flight-cost origin-iata city-iata check-in)
        planeE  (when (number? one-way) (* 2 (double one-way)))
        carE    (car-trip-cost {:origin-city origin-city :city city})]
    (case transport
      :plane {:mode :plane :transportE planeE :planeE planeE :carE carE}

      :car   {:mode :car   :transportE carE   :planeE planeE :carE carE}

      :any   (let [choices (->> [{:mode :plane :transportE planeE}
                                 {:mode :car   :transportE carE}]
                                (filter #(number? (:transportE %))))]
               (when (seq choices)
                 (let [{:keys [mode transportE]} (apply min-key :transportE choices)]
                   {:mode mode
                    :transportE transportE
                    :planeE planeE
                    :carE carE
                    :reason :cheaper})))

      {:mode :plane :transportE planeE :planeE planeE :carE carE})))


(defn estimate-trip-cost
  [{:keys [transport] :as req} {:keys [city] :as cand}]
  (let [{:keys [mode transportE planeE carE reason]}
        (transport-cost {:transport (or transport :any)
                         :origin-iata (:origin-iata req)
                         :origin-city (:origin-city req)
                         :check-in (:check-in req)}
                        cand)]
    (when (number? transportE)
      (let [hotelE (acc/accommodation-cost city (:check-in req) (:check-out req) (:nights req))
            totalE (+ (double transportE) (double hotelE))]
        {:city city
         :transport mode
         :reason reason          ;; :cheaper when is :any
         :planeE (when (number? planeE) (double planeE))
         :carE (when (number? carE) (double carE))
         :transportE (double transportE)
         :hotelE (double hotelE)
         :totalE totalE}))))




(defn recommend-by-budget
  [{:keys [budget] :as req} candidates]
  (->> candidates
       (keep (fn [cand]
               (estimate-trip-cost req cand)))
       (filter (fn [m]
                 (and (number? (:totalE m))
                      (<= (:totalE m) budget))))
       (sort-by :totalE)
       (take 5)))

(def candidates
  [{:city "Rome" :city-iata "FCO"}
   {:city "Athens" :city-iata "ATH"}
   {:city "Barcelona" :city-iata "BCN"}
   {:city "Paris" :city-iata "CDG"}])
