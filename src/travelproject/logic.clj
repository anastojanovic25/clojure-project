(ns travelproject.logic
  (:require [travelproject.entities :as ent]))

(def fuel-price 1.8)      ; €/l
(def consumption 7.0)     ; l / 100km

(defn car-cost [distance-km]
  (Math/round (* distance-km (/ consumption 100) fuel-price)))

(car-cost 1293)

(defn choose-transport [car-cost flight-cost]
  (if (< flight-cost car-cost)
    {:type :flight :cost flight-cost}
    {:type :car :cost car-cost}))













(defn recommend-destination [user destinations]
  (let [budget (:budget user)
        preferred-type (:trip-type user)
        preferred-climate (:preferred-climate user)
        filtered (filter #(and (<= (:price %) budget)
                               (= (:type %) preferred-type))
                         destinations)
        final (case preferred-climate
                "warm" (filter #(> (:temp %) 25) filtered)
                "mild" (filter #(and (>= (:temp %) 15) (<= (:temp %) 25)) filtered)
                "cold" (filter #(< (:temp %) 15) filtered)
                filtered)]
    (if (seq final)
      (first (sort-by :price final))
      nil)))

