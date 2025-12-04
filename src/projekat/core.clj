(ns projekat.core)

(def destinations
  [{:city "Athens"  :temp 28 :price 450}
   {:city "Oslo"    :temp 15 :price 700}
   {:city "Rome"    :temp 25 :price 500}
   {:city "Stockholm" :temp 10 :price 900}])

(defn avg-temp [destinations]
  (/ (reduce + (map :temp destinations))
     (count destinations)))

(defn filter-by-budget [destinations max-price]
  (filter #(<= (:price %) max-price) destinations))

(defn hottest-destination [destinations]
  (reduce (fn [acc e]
            (if (> (:temp e) (:temp acc)) e acc))
          destinations))

(println "Average temperature:" (avg-temp destinations))
(println "Destinationf of 600€:" (filter-by-budget destinations 600))
(println "The hottest destination:" (hottest-destination destinations))
