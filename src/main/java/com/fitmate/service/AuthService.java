package com.fitmate.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Handles Firebase Authentication via the Firebase Auth REST API.
 * Supports user sign-up, sign-in, sign-out, and session state management.
 * Stores the current user's UID, email, and ID token in memory for the session.
 */
public class AuthService {

    private static AuthService instance;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String apiKey;

    // In-memory session: holds current user's credentials after login
    private String currentUid;
    private String currentEmail;
    private String idToken;

    private AuthService() {
        this.apiKey = FirebaseService.getInstance().getApiKey();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Registers a new user with Firebase using email and password.
     * Does NOT create a session — user must log in after signup.
     *
     * @return CompletableFuture resolving to the new user's UID
     */
    public CompletableFuture<String> signUp(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;
            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);
            body.addProperty("returnSecureToken", true);

            try {
                String response = postJson(url, body.toString());
                JsonObject json = gson.fromJson(response, JsonObject.class);

                if (json.has("error")) {
                    String msg = json.getAsJsonObject("error").get("message").getAsString();
                    throw new RuntimeException(mapFirebaseError(msg));
                }

                // Return new UID without starting a session (user logs in after signup)
                return json.get("localId").getAsString();
            } catch (IOException e) {
                throw new RuntimeException("Network error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Signs in an existing user and saves session credentials in memory.
     *
     * @return CompletableFuture resolving to the authenticated user's UID
     */
    public CompletableFuture<String> signIn(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;
            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);
            body.addProperty("returnSecureToken", true);

            try {
                String response = postJson(url, body.toString());
                JsonObject json = gson.fromJson(response, JsonObject.class);

                if (json.has("error")) {
                    String msg = json.getAsJsonObject("error").get("message").getAsString();
                    throw new RuntimeException(mapFirebaseError(msg));
                }

                // Store session credentials for use across the app
                currentUid   = json.get("localId").getAsString();
                currentEmail = json.get("email").getAsString();
                idToken      = json.get("idToken").getAsString();

                return currentUid;
            } catch (IOException e) {
                throw new RuntimeException("Network error: " + e.getMessage(), e);
            }
        });
    }

    /** Clears the current session, effectively signing the user out. */
    public void signOut() {
        currentUid   = null;
        currentEmail = null;
        idToken      = null;
    }

    /** Returns true if a user is currently signed in. */
    public boolean isLoggedIn() {
        return currentUid != null;
    }

    public String getCurrentUid()   { return currentUid; }
    public String getCurrentEmail() { return currentEmail; }

    /**
     * Sends a JSON POST request and returns the raw response body as a String.
     */
    private String postJson(String url, String jsonBody) throws IOException {
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            throw new IOException("Empty response");
        }
    }

    /**
     * Maps raw Firebase error codes to human-readable messages shown in the UI.
     */
    private String mapFirebaseError(String code) {
        return switch (code) {
            case "EMAIL_EXISTS"               -> "An account with this email already exists.";
            case "EMAIL_NOT_FOUND"            -> "No account found with this email.";
            case "INVALID_PASSWORD"           -> "Incorrect password.";
            case "INVALID_LOGIN_CREDENTIALS"  -> "Invalid email or password.";
            case "USER_DISABLED"              -> "This account has been disabled.";
            case "WEAK_PASSWORD"              -> "Password should be at least 6 characters.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER"-> "Too many attempts. Please try again later.";
            default -> "Authentication error: " + code;
        };
    }
}
