package com.fitmate.service;

import com.fitmate.dao.*;
import com.fitmate.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for the Zenith AI Coach chatbot, powered by OpenAI GPT-4 Turbo.
 * Each message is enriched with the user's saved profile and health metrics
 * before being sent to the API, so advice is always personalised.
 * The question and reply are persisted to Firestore via ChatDAO after each exchange.
 */
public class ChatbotService {

    private static ChatbotService instance;

    private final OkHttpClient  httpClient;
    private final Gson          gson = new Gson();
    private final String        apiKey;
    private final ChatDAO       chatDAO;
    private final DietPlanDAO   dietPlanDAO;

    /**
     * System-level instructions that define Zenith's persona, tone rules,
     * response length limits, and safety boundaries.
     * These are prepended to every API call as the "system" role message.
     */
    private static final String SYSTEM_PROMPT = """
            You are Zenith, a friendly, motivating AI fitness and nutrition coach.
            
            HARD RULES (must always follow):
            - Keep replies VERY SHORT. No long paragraphs.
            - MAX length:
              • Simple greetings: 1–2 short sentences.
              • Normal fitness/diet questions: 2–5 short bullet points OR 2–3 tight sentences.
            - Never exceed 80 words.
            - Never repeat the user's question.
            - Never tell stories, give intros, or conclusions.
            
            USE USER METRICS STRICTLY:
            - Always use the user's real profile metrics (calories, TDEE, BMI, goal, etc.)
            - NEVER invent calorie targets or ranges.
            - ALL calorie guidance must be based on the actual TDEE provided.
            - If metrics are missing, acknowledge briefly instead of guessing.
            
            Tone: Warm, supportive, direct. Practical and simple phrasing.
            
            Safety: You are NOT a doctor. Only provide general diet/exercise guidance.
            If the user mentions injury, illness, diabetes, or medical concerns,
            advise consulting a medical professional.
            
            Remember: SHORT, CLEAR, METRIC-BASED advice.
            """;

    private ChatbotService() {
        Dotenv dotenv  = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey    = dotenv.get("OPENAI_API_KEY", "");
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.chatDAO    = new ChatDAOImpl();
        this.dietPlanDAO = new DietPlanDAOImpl();
    }

    public static synchronized ChatbotService getInstance() {
        if (instance == null) {
            instance = new ChatbotService();
        }
        return instance;
    }

    /**
     * Sends the user's message to GPT-4 Turbo with injected profile context,
     * saves the resulting ChatMessage (question + reply) to Firestore,
     * and returns the AI reply text via a CompletableFuture.
     *
     * @param uid     the logged-in user's Firestore UID
     * @param message the raw question typed by the user
     * @return CompletableFuture that resolves to the AI reply string
     */
    public CompletableFuture<String> askAI(String uid, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Inject the user's saved profile and metrics into the prompt for personalised advice
                String context = buildUserContext(uid);

                String composedMessage = String.format("""
                        User question:
                        %s
                        
                        Background information about this user (may be partial or missing):
                        %s
                        
                        Guidelines:
                        - Use the profile/metrics info only to tailor general guidance.
                        - Do NOT give medical diagnoses or treatment plans.
                        - If the question clearly requires a doctor or specialist,
                          say that the user should consult a professional.
                        """, message, context);

                // Send to GPT and get the reply text
                String reply = callGpt(composedMessage);

                // Persist both sides of the conversation to Firestore
                ChatMessage chatMsg = new ChatMessage(uid, message);
                chatMsg.setReply(reply);
                chatMsg.setCreatedAt(Instant.now().toString());
                chatDAO.saveChat(uid, chatMsg).get();

                return reply;
            } catch (Exception e) {
                throw new RuntimeException("AI Coach error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Reads the user's most recent diet plan document from Firestore and
     * formats the profile, metrics, and dietary preferences into a readable
     * context string that is appended to every GPT prompt.
     * Returns a fallback message when no saved data is available.
     */
    @SuppressWarnings("unchecked")
    private String buildUserContext(String uid) {
        try {
            Map<String, Object> planData = dietPlanDAO.getLatestDietPlan(uid).get();
            if (planData == null) {
                return "There is no saved profile or metrics for this user yet.";
            }

            Map<String, Object> profile = (Map<String, Object>) planData.getOrDefault("profile",     Map.of());
            Map<String, Object> metrics = (Map<String, Object>) planData.getOrDefault("metrics",     Map.of());
            Map<String, Object> prefs   = (Map<String, Object>) planData.getOrDefault("preferences", Map.of());

            return String.format("""
                    User profile info (if available):
                    - Age: %s, Gender: %s, Country: %s
                    - Height: %s cm, Weight: %s kg
                    - Goal: %s, Activity level: %s
                    
                    Latest health metrics:
                    - BMI: %s, BMR: %s, TDEE: %s, Body Fat: %s
                    
                    Diet preferences:
                    - Diet type: %s, Allergies: %s
                    - Disliked foods: %s, Medical conditions: %s
                    """,
                    profile.get("age"),    profile.get("gender"),  profile.get("country"),
                    profile.get("height"), profile.get("weight"),
                    profile.get("goal"),   profile.get("activityLevel"),
                    metrics.get("bmi"),    metrics.get("bmr"), metrics.get("tdee"), metrics.get("bfp"),
                    prefs.get("dietType"), prefs.get("allergies"),
                    prefs.get("dislikedFoods"), prefs.get("medicalConditions"));
        } catch (Exception e) {
            return "Error loading user context. Answer based only on the question.";
        }
    }

    /**
     * Builds and sends an OpenAI Chat Completions request with the system
     * prompt and the composed user message, then extracts and returns the
     * content of the first choice from the JSON response.
     */
    private String callGpt(String userMessage) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4-turbo");
        requestBody.addProperty("max_completion_tokens", 500);
        requestBody.addProperty("temperature", 0.7);

        // Build the messages array: [system, user]
        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", SYSTEM_PROMPT);
        messages.add(sys);

        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMessage);
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

            // Extract the content string from choices[0].message.content
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString().trim();
        }
    }
}
