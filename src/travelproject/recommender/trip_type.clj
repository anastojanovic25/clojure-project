(ns travelproject.recommender.trip-type
  (:require [travelproject.db :as db]))

(def trip-type->weights
  {:culture  {:culture 1.0  :cuisine 0.3 :nightlife 0.2}
   :adventure {:adventure 1.0 :nature 0.6}
   :relax    {:beaches 1.0  :wellness 0.8 :nature 0.2}})

(defn score-city
  [trip-type city]
  (when-let [row (db/city-scores city)]
    (let [weights (get trip-type->weights trip-type {})]
      (reduce
        +
        (for [[k w] weights]
          (* (double w) (double (or (get row k) 0))))))))
