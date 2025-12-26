(defproject projekat "0.1.0-SNAPSHOT"
  :description "Smart Travel Recommender"
  :dependencies
  [[org.clojure/clojure "1.11.1"]

   [uncomplicate/fluokitten "0.10.0"]

   [uncomplicate/neanderthal "0.60.0"]
   [org.bytedeco/mkl "2025.2-1.5.12" :classifier "windows-x86_64-redist"]

   [com.github.seancorfield/next.jdbc "1.3.939"]
   [com.mysql/mysql-connector-j "8.0.33"]]

  :profiles
  {:dev
   {:dependencies
    [[midje "1.10.10"]
     [criterium "0.4.6"]
     [com.clojure-goes-fast/clj-async-profiler "1.2.0"]]}})
