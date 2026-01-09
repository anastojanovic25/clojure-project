(ns travelproject.api.distance
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def api-key "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6Ijc5ZjljMjQ5NjYwZjRlZDQ4MDI2ZThhNDJjMzg3N2QwIiwiaCI6Im11cm11cjY0In0=")


(defn distance [from to]
  (let [resp (http/post
               "https://api.openrouteservice.org/v2/matrix/driving-car"
               {:headers {"Authorization" api-key
                          "Content-Type" "application/json"
                          "Accept" "application/json"}
                :throw-exceptions false
                :body (json/generate-string
                        {:locations [[(:lon from) (:lat from)]
                                     [(:lon to)   (:lat to)]]
                         :metrics ["distance"]})})
        body (json/parse-string (:body resp) true)]
    (/ (-> body :distances first second) 1000)))



