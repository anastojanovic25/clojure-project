(ns projekat.core
  (:require [projekat.entities :as ent]
            [projekabrt.logic :as logic]))

(def destinations
  [{:city "Athens" :temp 28 :price 450 :type "culture"}
   {:city "Oslo" :temp 15 :price 700 :type "adventure"}
   {:city "Rome" :temp 25 :price 500 :type "culture"}
   {:city "Stockholm" :temp 10 :price 900 :type "relax"}
   {:city "Bangkok" :temp 30 :price 700 :type "adventure"}
   {:city "Prague" :temp 22 :price 480 :type "culture"}
   {:city "Barcelona" :temp 27 :price 520 :type "relax"}
   ])


(defn avg-temp [destinations]
  (/ (reduce + (map :temp destinations))
     (count destinations)))

(defn filter-by-budget [destinations max-price]
  (filter #(<= (:price %) max-price) destinations))

(defn hottest-destination [destinations]
  (reduce (fn [acc e]
            (if (> (:temp e) (:temp acc)) e acc))
          destinations))

(defn count-within-budget [destinations max-price]
  (count (filter #(<= (:price %) max-price) destinations)))


(println "Average temperature:" (avg-temp destinations))
(println "Destination in budget:" (filter-by-budget destinations 900))
(println "Number of destinations within budget:" (count-within-budget destinations 900))
(println "The hottest destination:" (hottest-destination destinations))


(defn filter-by-climate [destinations preference]
  (filter
    (fn [d]
      (case preference
        "warm" (> (:temp d) 25)
        "mild" (and (>= (:temp d) 15) (<= (:temp d) 25))
        "cold" (< (:temp d) 15)
        false))
    destinations))
(println "Cold destinations:" (filter-by-climate destinations "cold"))

(defn cheapest-destination [destinations]
  (reduce (fn [acc d]
            (if (< (:price d) (:price acc)) d acc))
          destinations))

(println "Cheapest destination:" (cheapest-destination destinations))

(defn filter-by-type [destinations trip-type]
  (filter #(= (:type %) trip-type) destinations))

(println "Culture trips:" (filter-by-type destinations "culture"))

(defn avg-price-by-type [destinations trip-type]
  (let [filtered (filter #(= (:type %) trip-type) destinations)
        prices   (map :price filtered)]
    (if (seq prices)
      (/ (reduce + prices) (count prices))
      0)))
(println "Average price for adventure trips:" (avg-price-by-type destinations "adventure"))



(def user1 (ent/make-user "Ana" 600 "warm" "culture"))
(println "Recommended destination:"
         (logic/recommend-destination user1 destinations))



