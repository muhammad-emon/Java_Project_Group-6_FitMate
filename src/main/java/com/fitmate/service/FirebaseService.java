package com.fitmate.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Singleton service that initialises the Firebase Admin SDK on first access.
 * Reads configuration from the .env file:
 *   FIREBASE_CREDENTIALS — path to the service account JSON key file
 *   FIREBASE_API_KEY     — Web API key used by the Auth REST endpoint
 *   FIREBASE_PROJECT_ID  — Firebase project identifier
 *
 * Provides shared access to the Firestore client used across all DAO classes.
 */
public final class FirebaseService {

    private static FirebaseService instance;

    private final Firestore firestore;
    private final String    apiKey;
    private final String    projectId;

    private FirebaseService() {
        try {
            // Load environment variables; fall back to defaults if .env is absent
            Dotenv dotenv  = Dotenv.configure().ignoreIfMissing().load();
            String credPath = dotenv.get("FIREBASE_CREDENTIALS", "serviceAccountKey.json");
            apiKey          = dotenv.get("FIREBASE_API_KEY",     "");
            projectId       = dotenv.get("FIREBASE_PROJECT_ID",  "");

            // Initialise the Firebase app only once (guards against re-initialisation in tests)
            FileInputStream serviceAccount = new FileInputStream(credPath);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            // Obtain the shared Firestore client bound to this Firebase app
            firestore = FirestoreClient.getFirestore();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise Firebase: " + e.getMessage(), e);
        }
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    /** Returns the shared Firestore database client. */
    public Firestore getFirestore() { return firestore; }

    /** Returns the Firebase Web API key (used by AuthService for REST calls). */
    public String getApiKey()       { return apiKey; }

    /** Returns the Firebase project ID. */
    public String getProjectId()    { return projectId; }
}
