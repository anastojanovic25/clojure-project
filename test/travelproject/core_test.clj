(ns travelproject.core-test
  (:require
    [midje.sweet :refer :all]
    [travelproject.core :as core]))

(facts "parse-transport normalizes input"
       (core/parse-transport "plane") => :plane
       (core/parse-transport " CAR ") => :car
       (core/parse-transport "Any")   => :any
       (core/parse-transport "???")   => :any)

(facts "yes? works for common inputs"
       (core/yes? "y")   => true
       (core/yes? "YES") => true
       (core/yes? "no")  => false
       (core/yes? nil)   => false)

(facts "fmt-dt formats ISO datetime when possible"
       (core/fmt-dt "2026-03-15T10:30:00") => "15.03.2026 10:30"
       (core/fmt-dt "not-a-date")          => "not-a-date"
       (core/fmt-dt nil)                   => "")

(facts "nights-between calculates nights and rejects invalid ranges"
       (core/nights-between "2026-03-15" "2026-03-20") => 5
       (core/nights-between "2026-03-20" "2026-03-15") => (throws clojure.lang.ExceptionInfo))

(facts "airport-label uses known names and normalizes case"
       (core/airport-label "fco") => "FCO (Rome (Fiumicino))"
       (core/airport-label "xxx") => "XXX"
       (core/airport-label nil)   => "")
