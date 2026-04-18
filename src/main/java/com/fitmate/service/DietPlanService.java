package com.fitmate.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for generating personalised 7-day diet plans via OpenAI GPT-4 Turbo.
 * Accepts the user's profile, computed health metrics, and dietary preferences,
 * builds a structured prompt, and parses the JSON plan returned by the API.
 * On first-attempt failure the request is automatically retried once.
 */
public class DietPlanService {

    private static DietPlanService instance;

    private final OkHttpClient httpClient;
    private final Gson         gson = new Gson();
    private final String       apiKey;

    private DietPlanService() {
        Dotenv dotenv   = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey     = dotenv.get("OPENAI_API_KEY", "");
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30,  TimeUnit.SECONDS)
                .readTimeout(120,    TimeUnit.SECONDS)
                .writeTimeout(30,    TimeUnit.SECONDS)
                .build();
    }

    public static synchronized DietPlanService getInstance() {
        if (instance == null) {
            instance = new DietPlanService();
        }
        return instance;
    }

    /**
     * Generates a 7-day diet plan (Monday–Sunday) tailored to the user.
     * Target calories are adjusted ±10% from TDEE based on the user's goal
     * (gain weight / lose weight / maintain).
     *
     * @param profile user data: age, gender, country, height, weight, goal, activityLevel
     * @param metrics computed values: bmi, bmr, tdee, bfp
     * @param prefs   dietary choices: dietType, allergies, dislikedFoods, medicalConditions
     * @return CompletableFuture resolving to the parsed plan as a Map
     */
    public CompletableFuture<Map<String, Object>> generateDietPlan(
            Map<String, Object> profile,
            Map<String, Object> metrics,
            Map<String, Object> prefs) {

        return CompletableFuture.supplyAsync(() -> {
            // Extract profile fields with safe defaults
            String country  = str(profile, "country",       "Unknown");
            String age      = str(profile, "age",           "Unknown");
            String gender   = str(profile, "gender",        "Unknown");
            String height   = str(profile, "height",        "Unknown");
            String weight   = str(profile, "weight",        "Unknown");
            String goal     = str(profile, "goal",          "maintain weight");
            String activity = str(profile, "activityLevel", "moderate");

            String bmi  = str(metrics, "bmi",  "N/A");
            String bmr  = str(metrics, "bmr",  "N/A");
            String tdee = str(metrics, "tdee", "N/A");
            String bfp  = str(metrics, "bfp",  "N/A");

            String dietType  = str(prefs, "dietType",          "balanced");
            String allergies = str(prefs, "allergies",         "none");
            String disliked  = str(prefs, "dislikedFoods",     "none");
            String medical   = str(prefs, "medicalConditions", "none");

            // Adjust target calories relative to TDEE based on the user's stated goal
            double tdeeVal = 2000;
            try { tdeeVal = Double.parseDouble(tdee); } catch (Exception ignored) {}
            double targetCal = tdeeVal;
            if (goal.toLowerCase().contains("gain"))      targetCal = tdeeVal * 1.1;
            else if (goal.toLowerCase().contains("lose")) targetCal = tdeeVal * 0.9;

            String weekStart = java.time.LocalDate.now().toString();

            // Build the full GPT prompt specifying user context and strict JSON output format
            String prompt = String.format("""
                You are a professional nutritionist. Create a complete weekly diet plan (7 days, Monday to Sunday)
                for the following user:
                
                Country: %s, Age: %s, Gender: %s, Height: %s cm, Weight: %s kg
                Goal: %s, Activity Level: %s
                BMI: %s, BMR: %s, TDEE: %s, Body Fat: %s
                
                Dietary Preferences: Type: %s, Allergies: %s, Disliked Foods: %s, Medical Conditions: %s
                
                Guidelines:
                - Generate 7 days (Monday to Sunday)
                - Each day includes 4 meals: breakfast, lunch, snacks, dinner
                - Each meal item includes: name, calories, carbs, protein, fat, fiber, vitamins
                - Include total daily nutrients in a 'totals' field
                - Use only simple, common, home-cooked foods typically eaten in the user's country
                - Keep total daily calories close to %d kcal
                - Reply ONLY with valid JSON, no markdown
                
                JSON format:
                {
                  "weekStart": "%s",
                  "monday": {
                    "breakfast": [{"name": "...", "calories": 300, "carbs": 45, "protein": 8, "fat": 8, "fiber": 5, "vitamins": "A, C"}],
                    "lunch": [...], "snacks": [...], "dinner": [...],
                    "totals": {"calories": ..., "protein": ..., "carbs": ..., "fat": ..., "fiber": ...}
                  },
                  "tuesday": {...}, ... "sunday": {...}
                }
                """,
                    country, age, gender, height, weight, goal, activity,
                    bmi, bmr, tdee, bfp, dietType, allergies, disliked, medical,
                    (int) targetCal, weekStart);

            String systemMsg = "You are a culturally aware nutrition expert. Output STRICT JSON only — no markdown, explanations, or commentary.";

            // Attempt generation; retry once on transient failure
            try {
                return callGpt(systemMsg, prompt);
            } catch (Exception firstError) {
                System.err.println("Diet plan first attempt failed, retrying: " + firstError.getMessage());
                try {
                    return callGpt(systemMsg, prompt);
                } catch (Exception secondError) {
                    throw new RuntimeException("Diet plan generation failed: " + secondError.getMessage(), secondError);
                }
            }
        });
    }

    /**
     * Sends the system + user messages to the OpenAI Chat Completions endpoint
     * and parses the JSON plan from the response content using a regex extractor
     * (guards against any accidental markdown wrapping in the output).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callGpt(String systemMsg, String userMsg) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4-turbo");
        requestBody.addProperty("max_completion_tokens", 2500);
        requestBody.addProperty("temperature", 0.8);

        // Build the messages array: [system, user]
        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemMsg);
        messages.add(sys);

        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMsg);
        messages.add(usr);

        requestBody.add("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String content = json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString().trim();

            // Extract the JSON object from the response, stripping any markdown fences
            Pattern pattern = Pattern.compile("\\{.*}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                throw new IOException("No JSON object found in GPT response");
            }

            return gson.fromJson(matcher.group(), Map.class);
        }
    }

    /** Safely reads a String value from a Map, returning a default if absent or null. */
    private String str(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : def;
    }
}
