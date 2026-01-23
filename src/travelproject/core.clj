(ns travelproject.core
  (:gen-class)
  (:require
    [clojure.string :as str]
    [travelproject.recommender.budget :as rb]
    [travelproject.api.location :as loc]
    [travelproject.api.accommodation :as acc]))

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

(defn d0 [x] (double (or x 0.0)))

(defn yes? [s]
  (contains? #{"y" "yes"}
             (str/lower-case (str/trim (or s "")))))

(def dt-in  (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss"))
(def dt-out (java.time.format.DateTimeFormatter/ofPattern "dd.MM.yyyy HH:mm"))

(defn fmt-dt [s]
  (try
    (-> (java.time.LocalDateTime/parse s dt-in)
        (.format dt-out))
    (catch Exception _
      (or s ""))))

(def city->iata
  {"Belgrade" "BEG"
   "Beograd"  "BEG"
   "Novi Sad" "QND"
   "Niš"      "INI"
   "Nis"      "INI"})

(def airport-names
  {"AMS" "Amsterdam (Schiphol)"
   "FCO" "Rome (Fiumicino)"
   "CIA" "Rome (Ciampino)"
   "CDG" "Paris (Charles de Gaulle)"
   "ORY" "Paris (Orly)"
   "FRA" "Frankfurt"
   "MUC" "Munich"
   "VIE" "Vienna"
   "ZRH" "Zurich"
   "IST" "Istanbul"
   "SAW" "Istanbul (Sabiha Gokcen)"
   "DOH" "Doha (Hamad)"
   "DXB" "Dubai"
   "AUH" "Abu Dhabi"
   "MAD" "Madrid"
   "BCN" "Barcelona"
   "ATH" "Athens"
   "LHR" "London (Heathrow)"
   "LGW" "London (Gatwick)"})

(defn airport-label [code]
  (let [c (some-> code str/trim str/upper-case)]
    (if-let [nm (get airport-names c)]
      (str c " (" nm ")")
      (or c ""))))

(defn format-itinerary [{:keys [from to departure arrival flight-number stops via]}]
  (str from " -> " to
       " | " (fmt-dt departure) " - " (fmt-dt arrival)
       " | " (or flight-number "N/A")
       " | stops: " (or stops 0)
       (when (seq via)
         (str " | via: " (str/join ", " (map airport-label via))))))


(defn print-results [results]
  (println "\nTop recommendations:")
  (if (empty? results)
    (println "No destinations fit your budget for the selected dates.")
    (doseq [[idx r] (map-indexed vector results)]
      (let [reason (when (= (:requested-transport r) :any)
                     (some-> (:reason r) name))
            over-msg (when (number? (:overE r))
                       (format " | OVER BUDGET +%.2fE" (d0 (:overE r))))
            h      (:hotel r)
            f      (:flight r)]
        (println
          (format "%d) %s | total: %.2fE%s | transport: %s (%.2fE) | hotel: %.2fE "
                  (inc idx)
                  (:city r)
                  (d0 (:totalE r))
                  (or over-msg "")
                  (name (:transport r))
                  (d0 (:transportE r))
                  (d0 (:hotelE r))
                  (str (if reason (str " | reason: " reason) ""))))

        (when h
          (println
            (format "   Hotel: %s | rating: %s | %.2fE/night | total: %.2fE"
                    (:name h)
                    (or (:rating h) "n/a")
                    (d0 (or (:price-per-night h) 0.0))
                    (d0 (or (:total-price h) 0.0)))))

        (when f
          (let [total-stops (reduce + 0 (map #(or (:stops %) 0) (:itineraries f)))]
            (println
              (format "   Flight: %.2fE | total stops: %d"
                      (d0 (:price f))
                      total-stops)))
          (doseq [[leg it] (map-indexed vector (:itineraries f))]
            (println
              (format "     %s: %s"
                      (if (zero? leg) "Outbound" "Return")
                      (format-itinerary it)))))))))


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

(defn print-hotel-line [h]
  (println
    (format "- %s | rating: %s | %.2fE/night | total: %.2fE"
            (:name h)
            (or (:rating h) "n/a")
            (d0 (:price-per-night h))
            (d0 (:total-price h)))))

(defn extra-hotels-for-result
  [req r n]
  (let [city        (:city r)
        nights      (:nights req)
        remaining   (max 0.0 (- (d0 (:budget req)) (d0 (:transportE r))))
        chosen-name (get-in r [:hotel :name])
        opts        (acc/hotel-options city (:check-in req) (:check-out req) nights)]
    (->> opts
         (filter (fn [h]
                   (and (number? (:total-price h))
                        (<= (d0 (:total-price h)) remaining))))
         (remove (fn [h] (and chosen-name (= chosen-name (:name h)))))
         (sort-by (juxt (comp - (fn [h] (d0 (:rating h))))
                        (fn [h] (d0 (:total-price h)))))
         (take n)
         vec)))

(defn pick-result
  [results]
  (let [n (count results)]
    (cond
      (zero? n) nil
      (= n 1)   (first results)
      :else
      (loop []
        (let [raw (prompt (format "Choose destination number for more hotels (1-%d), or 'q' to cancel:" n))
              s   (str/trim (or raw ""))]
          (cond
            (or (str/blank? s) (= "q" (str/lower-case s)))
            nil
            :else
            (let [idx (try (Integer/parseInt s)
                           (catch Exception _ -1))]
              (if (<= 1 idx n)
                (nth results (dec idx))
                (do
                  (println (format "Invalid number. Please enter a value between 1 and %d." n))
                  (recur))))))))))



(defn run-once []
  (let [budget      (positive-int! "Budget" (prompt "Enter trip budget (EUR):"))

        origin-city (non-empty! "Origin city" (prompt "Enter origin city (e.g., Belgrade):"))
        origin-iata (or (loc/city->airport-iata origin-city)
                        (get city->iata origin-city)
                        (throw (ex-info "Could not find IATA code for that city."
                                        {:origin-city origin-city})))
        origin-city-iata (or (loc/city->iata origin-city) origin-iata)
        dest-city  (prompt "Destination city [leave empty for recommendations]:")
        dest-city  (when (seq dest-city) dest-city)

        check-in    (non-empty! "Check-in" (prompt "Enter check-in date (YYYY-MM-DD):"))
        _           (valid-date! "Check-in" check-in)

        check-out   (non-empty! "Check-out" (prompt "Enter check-out date (YYYY-MM-DD):"))
        _           (valid-date! "Check-out" check-out)

        transport   (parse-transport (prompt "Transport (plane/car/any):"))
        nights      (nights-between check-in check-out)
        req {:budget budget
             :origin-iata origin-iata
             :origin-city-iata origin-city-iata
             :origin-city origin-city
             :check-in check-in
             :check-out check-out
             :nights nights
             :transport transport}

        results (if dest-city
                  (rb/recommend-for-city req dest-city)
                  (rb/recommend-by-budget req (rb/dynamic-candidates req) ))]

    (println (str "Trip length: " nights " nights"))
    (print-results results)

    (when (seq results)
      (when (yes? (prompt "Do you want more hotel suggestions within your budget? (y/n):"))
        (loop []
          (if-let [r (pick-result results)]
            (let [remaining (max 0.0 (- (d0 (:budget req)) (d0 (:transportE r))))
                  extras    (extra-hotels-for-result req r 3)]
              (println (format "\nMore hotels in %s within remaining hotel budget %.2fE:"
                               (:city r) remaining))
              (if (empty? extras)
                (println "No additional hotels were found within your budget.")
                (doseq [h extras] (print-hotel-line h)))

              (when (yes? (prompt "\nCheck more hotels for another destination? (y/n):"))
                (recur)))
            (println "Cancelled.")))))
    ))


(defn -main [& _args]
  (println "=== Smart Travel Recommender ===")
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

