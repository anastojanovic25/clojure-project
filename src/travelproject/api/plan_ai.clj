(ns travelproject.api.plan-ai
  (:require [travelproject.api.groq-plan :as groq]))

(defn activity-prompt
  [{:keys [city nights budget transport]}]
  (str
    "Create an activity plan for a trip to " city " for " nights " nights.\n"
    "Format: Day 1/2/... and for each day: Morning, Afternoon, Evening.\n"
    "Include 1–2 realistic activities per time slot, plus a short tip.\n"
    "Prefer activities that are typical for the destination.\n"
    (when budget (str "Budget (EUR): " budget "\n"))
    (when transport (str "Transport: " (name transport) "\n"))
    "Write in English, short and clear."))

(defn generate-activity-plan
  [ctx]
  (groq/generate-activity-plan (activity-prompt ctx)))
