# Smart Travel Recommender (CLI) - Clojure / Leiningen

Smart Travel Recommender is a **console (CLI) application** implemented in **Clojure (Leiningen)**.
The program supports travel planning within a user-defined budget by combining:
- budget-based **destination recommendation**,
- **transport** and **accommodation** cost estimation,
- **hotel suggestion** for each destination,
- and an optional **AI-generated activity itinerary** for the selected destination.

> This is a terminal-based project.  
> The focus of the project is on **decision logic**, **robust API integration**, and **testability**.

---

## 1) Project Goal and Scope

### Goal
To build a CLI tool that recommends realistic travel options that fit the user’s budget and trip constraints, and provides an actionable plan (transport + hotel + optional itinerary).

### Scope (what the program covers)
- Collect user trip requirements (budget, dates, origin, optional destination, transport preference).
- Recommend destinations within budget OR process a user-provided destination.
- Estimate transport cost (flight or car).
- Recommend a hotel within remaining budget.
- Provide an interactive menu for additional hotel suggestions or AI itinerary generation.
- Ensure the project is portable (runs on another machine) and testable (tests do not require live APIs).

---

## 2) User Flows (What the Program Does)

The application supports two primary flows.

### Flow A - Recommendation mode (destination not provided)
If the user leaves the destination empty, the program:
1. Builds a set of candidate destinations (dynamic when possible; fallback to static when required).
2. For each destination:
    - estimates **transport cost** based on the chosen transport preference,
    - estimates **hotel cost** (based on number of nights),
    - produces a total cost estimate.
3. Filters destinations that exceed the total budget.
4. Sorts and prints **Top recommendations**.

For each recommendation, the program outputs (when available):
- **Total trip cost (EUR)**
- **Transport type** (plane/car) and **transport cost**
- **Hotel cost** (nights × price per night)
- Suggested **hotel details** (name, rating, price/night, total)
- Flight details (departure/return times, number of stops) when flight data exists

After displaying results, the user can choose:
1) Show more hotels
2) Generate an AI activity plan
3) Exit

### Flow B - Specific destination mode (destination provided)
If the user enters a destination:
1. The program focuses on that destination only.
2. Estimates transport cost according to the chosen preference (`plane`, `car`, `any`).
3. Recommends a hotel within the remaining budget.
4. Offers the same post-results menu.

---

## 3) Example Run 

Below is a short example of a typical terminal session (format may vary depending on API responses):

    Trip budget (EUR): 600
    Origin city: Belgrade
    Destination city (optional): [empty]
    Check-in date (YYYY-MM-DD): 2026-02-10
    Check-out date (YYYY-MM-DD): 2026-02-15
    Transport preference (plane/car/any): plane

    Top Recommendations:
    1) Athens | total: 550,29E | transport: plane (210,29E) | hotel: 340,00E 
    Hotel: Dinostratus House | rating: 4.7 | 68,00E/night | total: 340,00E
    Flight: 210,29E | total stops: 2
    Outbound: BEG -> ATH | 10.02.2026 12:35 - 11.02.2026 01:05 | KL1984 / KL1957 | stops: 1 | via: AMS (Amsterdam (Schiphol))
    Return: ATH -> BEG | 15.02.2026 06:05 - 15.02.2026 11:55 | KL1952 / KL1983 | stops: 1 | via: AMS (Amsterdam (Schiphol))

    What would you like to do next?
      1) Show more hotels
      2) Generate an AI activity plan
      3) Exit

---

## 4) Inputs Collected from the User

At runtime, the program asks for:
- Trip budget (EUR)
- Origin city
- Destination city (optional)
- Check-in date (YYYY-MM-DD)
- Check-out date (YYYY-MM-DD)
- Transport preference: `plane` / `car` / `any`

The number of nights is derived from the date range and is used for hotel pricing.

---

## 5) Core Decision Logic

### Budget evaluation
For each destination, the program estimates:
- `transportE` = transport cost estimate in EUR
- `hotelE`     = accommodation cost estimate in EUR
- `totalE`     = `transportE + hotelE`

Only destinations with `totalE <= budget` are kept.

### Transport logic
- If the user chooses `plane`, the program prioritizes flight offers when available.
- If the user chooses `car`, the program uses routing/distance-based estimation.
- If the user chooses `any`, the program evaluates both options and chooses the cheaper one (when both are available).

### Hotel logic
- The hotel is selected based on availability and budget constraints (remaining budget after transport).
- The user can request additional hotel suggestions for a selected destination from the menu.

---

## 6) Key Features

- Budget-based destination recommendation
- Transport cost estimation (flight/car/cheapest)
- Hotel recommendation based on remaining budget
- Extra hotel suggestions for the selected destination (menu option 1)
- AI activity plan generation (menu option 2)

---

## 7) External APIs Used (with documentation links)

This project integrates real-world data via external services:

- **SerpApi (Google Hotels engine)** - hotel search and pricing results  
  Docs: https://serpapi.com/google-hotels-api


- **Amadeus for Developers** - flight data (offers / inspiration endpoints)  
  Docs: https://developers.amadeus.com/


- **OpenRouteService** - distance/routing and geocoding  
  Docs: https://openrouteservice.org/dev/


- **Groq API (OpenAI-compatible endpoint)** - AI-generated activity plan  
  Docs: https://console.groq.com/docs

---

## 8) Setup and Running the Program

- Leiningen installed

### Install dependencies
    lein deps

### Run
    lein run

The application runs in the terminal and guides the user through prompts.

---

## 9) Configuration (API Keys) - `config.edn`

To keep API keys secure, configuration is split into:

- `config.example.edn` - safe template (committed)
- `config.edn` - local file with real keys (not committed)

### Create local config
    cp config.example.edn config.edn

Then open `config.edn` and paste your API keys.

> `config.edn` is ignored by Git (see `.gitignore`) to prevent leaking secrets.

### Running without keys
The program can start without keys, but API-dependent features (hotels / flights / AI plan) will be unavailable and the program will print a clear message.

---

## 10) AI Activity Plan (Optional Feature)

From the post-results menu, choose:
- **2) Generate an AI activity plan**

The program asks the user to select a destination (from the previously computed results) and generates a short itinerary:
- Day 1 / Day 2 / ...
- Morning / Afternoon / Evening
- realistic activities + short tips

If the AI API key is missing or invalid, the program continues normally and prints:
`AI plan is not available (missing or invalid API key).`

---

## 11) Testing Strategy (Midje)

The project includes **Midje** tests designed to validate the recommendation logic in a deterministic way.

### What is tested
- budget calculations
- destination filtering and ranking/sorting behavior
- helper/formatting functions
- logic that selects transport option and computes totals

### How external dependencies are handled
External API calls are **stubbed** (mocked) to ensure tests:
- do not require internet access,
- do not require API keys,
- do not fail due to rate limits or changing API responses.

Run tests:
> lein midje

---

## 12) Challenges Faced 

### 1) Toll calculation for car travel
A practical challenge was calculating **tolls** for car travel.
A reliable API-based approach was not available within the project constraints (coverage and/or free tier limitations), so tolls could not be calculated directly via an API.

**Solution:** toll cost was implemented as a **manual estimation** based on realistic assumptions, ensuring that the application can still compute a complete car travel cost.

### 2) Unstable destination suggestion endpoint in Amadeus sandbox
The initial plan was to use Amadeus to dynamically generate destination suggestions.
However, the destination suggestion functionality was **unstable in the test/sandbox environment** (inconsistent results and/or errors), reducing reliability.

**Solution (Fallback):**
- Implemented a fallback to a **static set of candidate destinations** so the application remains fully functional.

### 3) Building reliable tests without network dependencies
External APIs are not reliable for automated tests due to keys, network availability, rate limits, and changing responses.

**Solution:** tests use Midje stubs/mocks for external calls and validate only deterministic local logic.

---

## 14) Literature / References

- Clojure Documentation- https://clojure.org/reference/documentation
- Leiningen- https://leiningen.org/
- Midje framework- https://github.com/marick/Midje
- SerpApi - Google Hotels API- https://serpapi.com/google-hotels-api
- Amadeus for Developers- https://developers.amadeus.com/
- OpenRouteService API- https://openrouteservice.org/dev/
- Groq API Documentation- https://console.groq.com/docs

---

## 15) Project Structure

- `src/travelproject/core.clj` — CLI flow, prompts, menu, main entry point
- `src/travelproject/recommender/` — budget evaluation and recommendation logic
- `src/travelproject/api/` — integrations (hotels, flights, distance, AI plan)
- `test/` — Midje tests

---

## Author

Ana Stojanovic
