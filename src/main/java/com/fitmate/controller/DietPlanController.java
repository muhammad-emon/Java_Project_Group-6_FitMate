package com.fitmate.controller;

import com.fitmate.dao.*;
import com.fitmate.model.*;
import com.fitmate.service.*;
import com.fitmate.util.AppColors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.Instant;
import java.util.*;

/**
 * Controller for the Diet Plan sub-view.
 * On initialization it loads the user's profile and any existing saved plan.
 * When the user requests a new plan, it collects dietary preferences, calls
 * DietPlanService to generate a 7-day GPT-4 Turbo plan, saves it to Firestore,
 * and renders each day as a styled card with per-meal nutrient breakdowns.
 */
public class DietPlanController {

    @FXML private ComboBox<String> dietTypeCombo;
    @FXML private TextField allergiesField, dislikedField, medicalField;
    @FXML private Button    generateBtn;
    @FXML private Label     loadingLabel;
    @FXML private VBox      prefsForm, noPlanBox, planDisplay;

    private final UserDAO     userDAO     = new UserDAOImpl();
    private final DietPlanDAO dietPlanDAO = new DietPlanDAOImpl();

    // Cached user profile data used when building the GPT prompt
    private Map<String, Object> currentProfile = null;

    @FXML
    public void initialize() {
        dietTypeCombo.getItems().addAll("Balanced", "Vegetarian", "Vegan", "Keto", "Halal");
        dietTypeCombo.setValue("Balanced");

        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        // Fetch user profile asynchronously and cache for later use in plan generation
        userDAO.getUserProfile(uid).thenAccept(profile -> {
            if (profile != null) {
                currentProfile = new HashMap<>();
                currentProfile.put("age",           profile.getAge());
                currentProfile.put("gender",         profile.getGender());
                currentProfile.put("country",        profile.getCountry());
                currentProfile.put("height",         profile.getHeight());
                currentProfile.put("weight",         profile.getWeight());
                currentProfile.put("activityLevel",  profile.getActivityLevel());
                currentProfile.put("goal",           profile.getGoal());
            }
        });

        // Show any existing saved plan immediately without waiting for generation
        loadExistingPlan(uid);
    }

    /**
     * Fetches the most recently saved diet plan from Firestore and renders it.
     * If no plan exists, shows the "no plan" placeholder box instead.
     */
    @SuppressWarnings("unchecked")
    private void loadExistingPlan(String uid) {
        dietPlanDAO.getLatestDietPlan(uid).thenAccept(data -> Platform.runLater(() -> {
            if (data == null || !data.containsKey("plan")) {
                noPlanBox.setVisible(true);
                noPlanBox.setManaged(true);
                return;
            }

            try {
                Map<String, Object> plan = (Map<String, Object>) data.get("plan");
                renderPlan(plan);
            } catch (Exception e) {
                noPlanBox.setVisible(true);
                noPlanBox.setManaged(true);
            }
        }));
    }

    /**
     * Called when the user clicks "Generate Plan".
     * If any preference field is empty, shows the preferences form first.
     * Otherwise goes straight to plan generation.
     */
    @FXML
    private void handleGenerate() {
        if (currentProfile == null) return; // Profile hasn't loaded yet

        boolean allFilled = !allergiesField.getText().trim().isEmpty()
                && !dislikedField.getText().trim().isEmpty()
                && !medicalField.getText().trim().isEmpty();

        if (!allFilled) {
            // Show the preferences form so the user can fill in missing fields
            prefsForm.setVisible(true);
            prefsForm.setManaged(true);
            noPlanBox.setVisible(false);
            noPlanBox.setManaged(false);
            return;
        }

        generatePlan();
    }

    /** Called when the user submits the preferences form — hides it and generates. */
    @FXML
    private void submitPrefsAndGenerate() {
        prefsForm.setVisible(false);
        prefsForm.setManaged(false);
        generatePlan();
    }

    /**
     * Builds the user profile, computed metrics, and preference maps,
     * then calls DietPlanService to generate a 7-day plan via GPT-4 Turbo.
     * On success the plan is saved to Firestore and rendered in the UI.
     */
    @SuppressWarnings("unchecked")
    private void generatePlan() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null || currentProfile == null) return;

        // Show loading state while the API call is in progress
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        generateBtn.setDisable(true);
        generateBtn.setText("Generating...");
        planDisplay.setVisible(false);
        planDisplay.setManaged(false);
        noPlanBox.setVisible(false);
        noPlanBox.setManaged(false);

        // Compute health metrics to include in the GPT prompt
        HealthMetrics m = MetricsCalculator.calculate(
                (String) currentProfile.get("weight"),
                (String) currentProfile.get("height"),
                (String) currentProfile.get("age"),
                (String) currentProfile.get("gender"),
                (String) currentProfile.get("activityLevel"));

        Map<String, Object> metrics = new HashMap<>();
        if (m != null) {
            metrics.put("bmi",  m.getBmi());
            metrics.put("bmr",  m.getBmr());
            metrics.put("tdee", m.getTdee());
            metrics.put("bfp",  m.getBfp());
        }

        // Collect dietary preferences; use "none" when a field is left blank
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("dietType",          dietTypeCombo.getValue());
        prefs.put("allergies",         allergiesField.getText().trim().isEmpty() ? "none" : allergiesField.getText().trim());
        prefs.put("dislikedFoods",     dislikedField.getText().trim().isEmpty()  ? "none" : dislikedField.getText().trim());
        prefs.put("medicalConditions", medicalField.getText().trim().isEmpty()   ? "none" : medicalField.getText().trim());

        DietPlanService.getInstance().generateDietPlan(currentProfile, metrics, prefs)
                .thenAccept(planMap -> {
                    // Bundle the full plan with metadata and persist it to Firestore
                    Map<String, Object> saveData = new HashMap<>();
                    saveData.put("createdAt", Instant.now().toString());
                    saveData.put("weekStart", planMap.getOrDefault("weekStart", ""));
                    saveData.put("profile",     currentProfile);
                    saveData.put("metrics",     metrics);
                    saveData.put("preferences", prefs);
                    saveData.put("plan",        planMap);

                    dietPlanDAO.saveDietPlan(uid, saveData)
                            .thenRun(() -> Platform.runLater(() -> {
                                loadingLabel.setVisible(false);
                                loadingLabel.setManaged(false);
                                generateBtn.setDisable(false);
                                generateBtn.setText("Generate New Plan");
                                renderPlan(planMap);
                            }));
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loadingLabel.setText("Error: " + ex.getMessage());
                        generateBtn.setDisable(false);
                        generateBtn.setText("Generate New Plan");
                    });
                    return null;
                });
    }

    /**
     * Renders the 7-day plan map as a vertical list of day cards.
     * Each day card contains labelled meal sections (Breakfast, Lunch, Snacks, Dinner)
     * with per-item nutrient info, followed by a daily totals summary line.
     */
    @SuppressWarnings("unchecked")
    private void renderPlan(Map<String, Object> plan) {
        planDisplay.getChildren().clear();
        planDisplay.setVisible(true);
        planDisplay.setManaged(true);

        String[] dayOrder = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (String day : dayOrder) {
            Object dayData = plan.get(day);
            if (!(dayData instanceof Map)) continue;

            Map<String, Object> dayMap = (Map<String, Object>) dayData;

            VBox dayCard = new VBox(12);
            dayCard.getStyleClass().add("day-card");
            dayCard.setPadding(new Insets(20));

            // Day heading (e.g. "Monday")
            Label dayTitle = new Label(Character.toUpperCase(day.charAt(0)) + day.substring(1));
            dayTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00C897;");
            dayCard.getChildren().add(dayTitle);

            // Render each meal section in order
            for (String mealType : List.of("breakfast", "lunch", "snacks", "dinner")) {
                Object mealData = dayMap.get(mealType);
                if (!(mealData instanceof List)) continue;

                Label mealLabel = new Label(Character.toUpperCase(mealType.charAt(0)) + mealType.substring(1));
                mealLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00C897;");
                dayCard.getChildren().add(mealLabel);

                // Render each food item as a compact card with name + macro breakdown
                for (Object item : (List<?>) mealData) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> mi = (Map<String, Object>) item;

                    VBox mealCard = new VBox(4);
                    mealCard.getStyleClass().add("meal-card");
                    mealCard.setPadding(new Insets(10));

                    Label nameLabel = new Label(String.valueOf(mi.getOrDefault("name", "?")));
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E0E0E0;");

                    Label infoLabel = new Label(String.format(
                            "🔥 %s kcal  🍗 %sg protein  🍚 %sg carbs  🧀 %sg fat  🌾 %sg fiber",
                            mi.getOrDefault("calories", "?"),
                            mi.getOrDefault("protein",  "?"),
                            mi.getOrDefault("carbs",    "?"),
                            mi.getOrDefault("fat",      "?"),
                            mi.getOrDefault("fiber",    "?")));
                    infoLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 12px;");
                    infoLabel.setWrapText(true);

                    mealCard.getChildren().addAll(nameLabel, infoLabel);
                    dayCard.getChildren().add(mealCard);
                }
            }

            // Daily totals row summarising calories, protein, carbs, and fat
            Object totalsObj = dayMap.get("totals");
            if (totalsObj instanceof Map) {
                Map<String, Object> t = (Map<String, Object>) totalsObj;
                Label totalsLabel = new Label(String.format(
                        "Daily Totals: %s kcal — %sg protein, %sg carbs, %sg fat",
                        t.getOrDefault("calories", "?"),
                        t.getOrDefault("protein",  "?"),
                        t.getOrDefault("carbs",    "?"),
                        t.getOrDefault("fat",      "?")));
                totalsLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 12px; -fx-font-weight: bold;");
                totalsLabel.setWrapText(true);
                VBox.setMargin(totalsLabel, new Insets(8, 0, 0, 0));
                dayCard.getChildren().add(totalsLabel);
            }

            planDisplay.getChildren().add(dayCard);
        }
    }
}
