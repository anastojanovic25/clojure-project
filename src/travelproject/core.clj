(ns travelproject.core
  (:gen-class)
  (:require
    [clojure.string :as str]
    [travelproject.recommender.budget :as rb]
    [travelproject.api.location :as loc]))

(defn prompt [label]
  (print (str label " "))
  (flush)
  (str/trim (read-line)))

(defn parse-int [s]
  (Integer/parseInt (str/trim s)))

(defn parse-transport [s]
  (case (str/lower-case (str/trim s))
    "plane" :plane
    "car"   :car
    "any"   :any
    :any))

(defn parse-trip-type [s]
  (case (clojure.string/lower-case (clojure.string/trim (or s "")))
    "culture"  :culture
    "adventure" :adventure
    "relax"    :relax
    nil))

(defn print-results [results]
  (println "\nTop recommendations:")
  (if (empty? results)
    (println "No destinations fit your budget for the selected dates.")
    (doseq [[idx r] (map-indexed vector results)]
      (let [score  (double (or (:typescore r) 0.0))
            reason (when (= (:requested-transport r) :any)
                     (some-> (:reason r) name))
            h      (:hotel r)
            f      (:flight r)]
        (println
          (format "%d) %s | total: %.2fE | transport: %s (%.2fE) | hotel: %.2fE | type: %s | score: %.2f%s"
                  (inc idx)
                  (:city r)
                  (double (:totalE r))
                  (name (:transport r))
                  (double (:transportE r))
                  (double (:hotelE r))
                  (or (:tripType r)(:trip-type r) "-")
                  score
                  (if reason (str " | reason: " reason) "")))

        (when h
          (println
            (format "   Hotel: %s | rating: %s | %.2fE/night | total: %.2fE"
                    (:name h)
                    (or (:rating h) "n/a")
                    (double (or (:price-per-night h) 0.0))
                    (double (or (:total-price h) 0.0)))))

        (when f
          (println
            (format "   Flight: validating=%s | operating=%s | price: %.2fE"
                    (:airline f)
                    (pr-str (:operating-airlines f))
                    (double (or (:price f) 0.0))))
          (doseq [[leg it] (map-indexed vector (:itineraries f))]
            (println
              (format "     Leg %d: %s -> %s | %s -> %s | %s"
                      (inc leg)
                      (:from it) (:to it)
                      (:departure it) (:arrival it)
                      (:flight-number it)))))))))


(defn non-empty! [label s]
  (let [v (str/trim (or s ""))]
    (when (str/blank? v)
      (throw (ex-info (str label " cannot be empty.") {:label label})))
    v))

(defn positive-int! [label s]
  (let [n (parse-int s)]
    (when (<= n 0)
      (throw (ex-info (str label " must be > 0.") {:label label :value n})))
    n))

(defn valid-date! [label s]
  (try
    (java.time.LocalDate/parse (str/trim s))
    (catch Exception _
      (throw (ex-info (str label " must be in YYYY-MM-DD format.") {:label label :value s})))))

(defn nights-between [check-in check-out]
  (let [in  (java.time.LocalDate/parse check-in)
        out (java.time.LocalDate/parse check-out)
        n   (.between java.time.temporal.ChronoUnit/DAYS in out)]
    (when (<= n 0)
      (throw (ex-info "Check-out must be after check-in."
                      {:check-in check-in :check-out check-out})))
    (int n)))

(def city->iata
  {"Belgrade" "BEG"
   "Beograd"  "BEG"
   "Novi Sad" "QND"
   "Niš"      "INI"
   "Nis"      "INI"})

(defn run-once []
  (let [budget      (positive-int! "Budget" (prompt "Enter trip budget (EUR):"))

        origin-city (non-empty! "Origin city" (prompt "Enter origin city (e.g., Belgrade):"))
        origin-iata (or (loc/city->iata origin-city)
                        (get city->iata origin-city)
                        (throw (ex-info "Could not find IATA code for that city."
                                        {:origin-city origin-city})))

        check-in    (non-empty! "Check-in" (prompt "Enter check-in date (YYYY-MM-DD):"))
        _           (valid-date! "Check-in" check-in)

        check-out   (non-empty! "Check-out" (prompt "Enter check-out date (YYYY-MM-DD):"))
        _           (valid-date! "Check-out" check-out)

        transport   (parse-transport (prompt "Transport (plane/car/any):"))
        nights      (nights-between check-in check-out)
        trip-type (parse-trip-type (prompt "Trip type (culture/adventure/relax) [optional]:"))
        req {:budget budget
             :origin-iata origin-iata
             :origin-city origin-city
             :check-in check-in
             :check-out check-out
             :nights nights
             :transport transport
             :trip-type trip-type}

        results (rb/recommend-by-budget req rb/candidates)]

    (println (format "\nOrigin: %s (%s)" origin-city origin-iata))
    (println (str "Trip length: " nights " nights"))
    (print-results results)))


(defn -main [& _args]
  (println "=== Smart Travel Recommender (MVP) ===")
  (loop []
    (let [ok?
          (try
            (run-once)
            true
            (catch Exception e
              (println "\nInput error:" (.getMessage e))
              (println "Let's try again.\n")
              false))]
      (when (not ok?)
        (recur)))))

