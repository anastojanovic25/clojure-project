(ns travelproject.api.geocoding
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def api-key "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6Ijc5ZjljMjQ5NjYwZjRlZDQ4MDI2ZThhNDJjMzg3N2QwIiwiaCI6Im11cm11cjY0In0=")

(defn city-coords [city]
  (let [resp (http/get "https://api.openrouteservice.org/geocode/search"
                       {:headers {"Authorization" api-key}
                        :query-params {:text city
                                       :size 1}})
        body (json/parse-string (:body resp) true)
        coords (-> body :features first :geometry :coordinates)]
    {:lon (first coords)
     :lat (second coords)}))
