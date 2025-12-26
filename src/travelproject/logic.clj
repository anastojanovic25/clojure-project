(ns projekat.logic
  (:require [projekat.entities :as ent]))

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

