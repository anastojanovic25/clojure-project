(ns travelproject.api.groq-plan
  (:require [travelproject.config :as config]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(defn- api-key []
  (config/get-in-cfg [:groq :api-key]))

(defn- model []
  (or (config/get-in-cfg [:groq :model]) "llama-3.1-8b-instant"))

(defn generate-activity-plan
  [prompt]
  (when-let [k (api-key)]
    (let [resp (http/post "https://api.groq.com/openai/v1/chat/completions"
                          {:headers {"Authorization" (str "Bearer " k)
                                     "Content-Type" "application/json"}
                           :body (json/generate-string
                                   {:model (model)
                                    :messages [{:role "system" :content "You are a travel planner."}
                                               {:role "user" :content prompt}]
                                    :max_tokens 500
                                    :temperature 0.7})
                           :as :json})]
      (get-in resp [:body :choices 0 :message :content]))))
