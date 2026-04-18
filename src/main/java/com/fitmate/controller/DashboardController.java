package com.fitmate.controller;

import com.fitmate.service.AuthService;
import com.fitmate.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for the Dashboard shell layout.
 * Owns the top navigation bar and a StackPane content area into which
 * all module sub-views (Home, Profile, Metrics, Diet, AI Coach) are loaded.
 */
public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button    btnHome, btnProfile, btnMetrics, btnDiet, btnAI;

    private String currentView = "";

    @FXML
    public void initialize() {
        // Load the Dashboard Home sub-view as the default landing page
        loadSubView("home");
    }

    /**
     * Loads the specified module sub-view into the dashboard content area.
     * Clears any previously loaded view before adding the new one.
     *
     * @param viewName logical name: "home", "profile", "metrics", "diet", "aiChat"
     */
    public void loadSubView(String viewName) {
        String fxmlFile = switch (viewName) {
            case "home"    -> "DashboardHomeView";
            case "profile" -> "ProfileView";
            case "metrics" -> "HealthMetricsView";
            case "diet"    -> "DietPlanView";
            case "aiChat"  -> "AICoachView";
            default        -> "DashboardHomeView";
        };

        try {
            URL resource = getClass().getResource("/fxml/" + fxmlFile + ".fxml");
            if (resource == null) {
                System.err.println("Sub-view FXML not found: " + fxmlFile);
                return;
            }

            // Replace the content area with the newly loaded sub-view node
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            currentView = viewName;

            updateNavHighlight(viewName);
        } catch (IOException e) {
            System.err.println("Error loading sub-view: " + e.getMessage());
        }
    }

    /**
     * Applies the active CSS class to the currently selected nav button
     * and resets all other buttons to the default nav style.
     */
    private void updateNavHighlight(String active) {
        Button[] buttons = {btnHome, btnProfile, btnMetrics, btnDiet, btnAI};
        String[] names   = {"home", "profile", "metrics", "diet", "aiChat"};

        for (int i = 0; i < buttons.length; i++) {
            if (names[i].equals(active)) {
                buttons[i].getStyleClass().removeAll("btn-nav");
                if (!buttons[i].getStyleClass().contains("btn-nav-active"))
                    buttons[i].getStyleClass().add("btn-nav-active");
            } else {
                buttons[i].getStyleClass().removeAll("btn-nav-active");
                if (!buttons[i].getStyleClass().contains("btn-nav"))
                    buttons[i].getStyleClass().add("btn-nav");
            }
        }
    }

    // Navigation bar button handlers — each delegates to loadSubView
    @FXML private void navHome()    { loadSubView("home"); }
    @FXML private void navProfile() { loadSubView("profile"); }
    @FXML private void navMetrics() { loadSubView("metrics"); }
    @FXML private void navDiet()    { loadSubView("diet"); }
    @FXML private void navAICoach() { loadSubView("aiChat"); }

    /** Signs the user out and returns to the Landing page. */
    @FXML
    private void handleSignOut() {
        AuthService.getInstance().signOut();
        SceneManager.getInstance().navigateTo("LandingView");
    }
}
