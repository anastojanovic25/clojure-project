(ns travelproject.entities)

(defn make-user [name budget preferred-climate trip-type]
  {:name name
   :budget budget
   :preferred-climate preferred-climate
   :trip-type trip-type})

(defn make-destination [city temp price type]
  {:city city
   :temp temp
   :price price
   :type type})

(defn make-activity [city activity-type weather-condition indoor?]
  {:city city
   :activity-type activity-type
   :weather-condition weather-condition
   :indoor? indoor?})

(defn make-trip-plan [user destination activities status]
  {:user user
   :destination destination
   :activities activities
   :status status})

;;new
