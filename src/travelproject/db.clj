(ns projekat.db
    (:require
      [next.jdbc :as jdbc]
      [next.jdbc.result-set :as rs]))

(def db-spec
  {:dbtype "mysql"
   :dbname "travel_db"
   :host "localhost"
   :user "root"
   :password
   :useSSL false
   :serverTimezone "UTC"})

(def ds (jdbc/get-datasource db-spec))

(defn test-connection []
      (jdbc/execute!
        ds
        ["SELECT 1"]
        {:builder-fn rs/as-unqualified-lower-maps}))

