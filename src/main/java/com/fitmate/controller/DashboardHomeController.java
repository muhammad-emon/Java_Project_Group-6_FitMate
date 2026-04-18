package com.fitmate.controller;

import com.fitmate.dao.*;
import com.fitmate.model.*;
import com.fitmate.service.*;
import com.fitmate.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.*;

/**
 * Controller for the Dashboard Home sub-view.
 * On load it fetches four data sources concurrently:
 *   1. User profile  — greeting, weight, goal, computed metrics (BMI, TDEE, BMR)
 *   2. Diet plan     — today's meal names displayed as a quick summary
 *   3. AI Coach      — last question and reply previewed on the card
 *   4. Weight log    — progress delta since the first recorded weight entry
 */
public class DashboardHomeController {

    @FXML private Label greetingLabel, goalLabel, currentWeightLabel, targetWeightLabel;
    @FXML private Label bmiLabel, bmiCatLabel, tdeeLabel, bmrLabel;
    @FXML private Label dietOverviewLabel, progressLabel, aiCoachPreviewLabel;

    private final UserDAO       userDAO       = new UserDAOImpl();
    private final DietPlanDAO   dietPlanDAO   = new DietPlanDAOImpl();
    private final ChatDAO       chatDAO       = new ChatDAOImpl();
    private final WeightLogDAO  weightLogDAO  = new WeightLogDAOImpl();

    @FXML
    public void initialize() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        // Load all dashboard sections independently (each has its own async chain)
        loadProfile(uid);
        loadDietSummary(uid);
        loadAICoachPreview(uid);
        loadWeightProgress(uid);
    }

    /**
     * Fetches the user profile and populates the greeting, goal, weight labels,
     * and computed health metrics (BMI category, TDEE, BMR).
     */
    private void loadProfile(String uid) {
        userDAO.getUserProfile(uid).thenAccept(profile -> Platform.runLater(() -> {
            if (profile == null) {
                greetingLabel.setText("Welcome! 👋");
                return;
            }

            greetingLabel.setText("Welcome back, " + profile.getDisplayName() + " 👋");
            goalLabel.setText(profile.getGoal() != null ? profile.getGoal() : "Set your goal");

            if (profile.getWeight() != null)       currentWeightLabel.setText(profile.getWeight() + " kg");
            if (profile.getTargetWeight() != null) targetWeightLabel.setText(profile.getTargetWeight() + " kg");

            // Compute and display BMI, BMR, TDEE from profile data
            HealthMetrics m = MetricsCalculator.calculate(
                    profile.getWeight(), profile.getHeight(),
                    profile.getAge(), profile.getGender(),
                    profile.getActivityLevel());

            if (m != null) {
                bmiLabel.setText(String.format("%.1f", m.getBmi()));
                bmiCatLabel.setText(m.getBmiCategory());
                tdeeLabel.setText(String.format("%.0f", m.getTdee()));
                bmrLabel.setText(String.format("%.0f", m.getBmr()));
            } else {
                bmiLabel.setText("—");
                bmiCatLabel.setText("Complete your profile");
                tdeeLabel.setText("—");
                bmrLabel.setText("—");
            }
        })).exceptionally(ex -> {
            System.err.println("Error loading profile: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Loads the most recent diet plan and displays today's meals as a compact summary.
     * Determines today's day key by querying the system calendar.
     */
    @SuppressWarnings("unchecked")
    private void loadDietSummary(String uid) {
        dietPlanDAO.getLatestDietPlan(uid).thenAccept(data -> Platform.runLater(() -> {
            if (data == null || !data.containsKey("plan")) {
                dietOverviewLabel.setText("No active diet plan yet. Generate a plan from the Diet page.");
                return;
            }

            try {
                Map<String, Object> plan = (Map<String, Object>) data.get("plan");

                // Map Calendar day-of-week index to the day key used in the plan map
                String[] days = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
                String todayKey = days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];

                Map<String, Object> todayPlan = (Map<String, Object>) plan.get(todayKey);
                if (todayPlan == null) {
                    dietOverviewLabel.setText("No plan for today (" + todayKey + ").");
                    return;
                }

                // Build a readable summary of meal names for each meal type
                StringBuilder sb = new StringBuilder();
                sb.append("Showing plan for ")
                  .append(Character.toUpperCase(todayKey.charAt(0)))
                  .append(todayKey.substring(1))
                  .append("\n\n");

                for (String meal : List.of("breakfast", "lunch", "snacks", "dinner")) {
                    Object mealData = todayPlan.get(meal);
                    if (mealData instanceof List) {
                        List<Object> items = (List<Object>) mealData;
                        sb.append(Character.toUpperCase(meal.charAt(0))).append(meal.substring(1)).append(": ");
                        List<String> names = new ArrayList<>();
                        for (Object item : items) {
                            if (item instanceof Map<?, ?> itemMap) {
                                names.add(String.valueOf(itemMap.getOrDefault("name", "?")));
                            }
                        }
                        sb.append(String.join(", ", names)).append("\n");
                    }
                }

                dietOverviewLabel.setText(sb.toString().trim());
            } catch (Exception e) {
                dietOverviewLabel.setText("Error loading diet summary.");
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> dietOverviewLabel.setText("Error loading diet plan."));
            return null;
        });
    }

    /**
     * Loads the most recent AI Coach conversation entry and shows a preview
     * of the last user question and the coach's reply.
     */
    private void loadAICoachPreview(String uid) {
        chatDAO.getChatHistory(uid).thenAccept(chats -> Platform.runLater(() -> {
            if (chats == null || chats.isEmpty()) {
                aiCoachPreviewLabel.setText("You haven't chatted with your AI Coach yet. Ask a question to get started.");
                return;
            }

            ChatMessage last = chats.get(chats.size() - 1);
            aiCoachPreviewLabel.setText(
                    "Last question: " + last.getMessage() + "\n\nCoach reply: " + last.getReply());
        })).exceptionally(ex -> {
            Platform.runLater(() -> aiCoachPreviewLabel.setText("Error loading chat history."));
            return null;
        });
    }

    /**
     * Compares the user's current weight against their first logged weight
     * and displays the total change (positive or negative) as a progress summary.
     */
    private void loadWeightProgress(String uid) {
        weightLogDAO.getWeightLog(uid).thenAccept(entries -> Platform.runLater(() -> {
            userDAO.getUserProfile(uid).thenAccept(profile -> Platform.runLater(() -> {
                if (profile == null || profile.getWeight() == null) {
                    progressLabel.setText("Log your weight and set a target to track progress.");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Current: ").append(profile.getWeight()).append(" kg");

                if (profile.getTargetWeight() != null) {
                    sb.append("\nTarget: ").append(profile.getTargetWeight()).append(" kg");
                }

                // Show weight delta since the very first log entry
                if (entries != null && !entries.isEmpty()) {
                    double first   = entries.get(0).getWeight();
                    double current = Double.parseDouble(profile.getWeight());
                    double diff    = current - first;
                    sb.append(String.format("\nSince first log: %s%.1f kg", diff > 0 ? "+" : "", diff));
                }

                progressLabel.setText(sb.toString());
            }));
        })).exceptionally(ex -> null);
    }

    // Quick-action links from the dashboard home cards
    @FXML private void goToProfile()  { navigateDashboard("profile"); }
    @FXML private void goToMetrics()  { navigateDashboard("metrics"); }
    @FXML private void goToDiet()     { navigateDashboard("diet"); }
    @FXML private void goToAICoach()  { navigateDashboard("aiChat"); }

    /** Delegates to DashboardController to swap the currently displayed sub-view. */
    private void navigateDashboard(String view) {
        DashboardController dc = SceneManager.getInstance().getController("DashboardView");
        if (dc != null) dc.loadSubView(view);
    }
}
