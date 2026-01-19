(ns travelproject.db
    (:require
      [next.jdbc :as jdbc]
      [next.jdbc.result-set :as rs]))

(def db-spec
  {:dbtype "mysql"
   :dbname "travel_db"
   :host "localhost"
   :port 3307
   :user "root"
   :password ""
   :useSSL false
   :serverTimezone "UTC"})

(def ds
  (jdbc/get-datasource db-spec))

(defn test-connection []
      (jdbc/execute!
        ds
        ["SELECT 1"]
        {:builder-fn rs/as-unqualified-lower-maps}))

(defn city-scores
  "Returns a row from cities_raw with preference scores for a given city name."
  [city]
  (first
    (jdbc/execute!
      ds
      ["SELECT city, country, region,
              culture, adventure, nature, beaches, nightlife, cuisine, wellness
        FROM cities_raw
        WHERE LOWER(city) = LOWER(?)
        LIMIT 1"
       city]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn fetch-destinations []
  (jdbc/execute!
    ds
    ["SELECT
        city,
        avg_temp_monthly AS temp,
        CASE budget_level
          WHEN 'low' THEN 400
          WHEN 'medium' THEN 600
          WHEN 'high' THEN 900
        END AS price,
        'culture' AS type
      FROM cities_raw"]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-all-cities []
  (jdbc/execute!
    ds
    ["SELECT city, country, budget_level
      FROM cities_raw
      LIMIT 10"]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-cities-by-country [country]
  (jdbc/execute!
    ds
    ["SELECT city, country, region
      FROM cities_raw
      WHERE country = ?
      LIMIT 10"
     country]
    {:builder-fn rs/as-unqualified-lower-maps}))
