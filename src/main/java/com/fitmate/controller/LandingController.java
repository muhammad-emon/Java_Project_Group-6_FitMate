package com.fitmate.controller;

import com.fitmate.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Landing page.
 * Handles smooth scroll navigation to the Features and Contact Us sections,
 * and provides entry points to the Login and Sign Up screens.
 */
public class LandingController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox       featuresSection;
    @FXML private VBox       footerSection;

    /**
     * Smoothly scrolls the landing page to the Features section.
     * Calculates the required vValue based on content vs. viewport height.
     */
    @FXML
    private void scrollToFeatures() {
        scrollToSection(featuresSection);
    }

    /**
     * Smoothly scrolls the landing page to the Contact Us / footer section.
     */
    @FXML
    private void scrollToContactUs() {
        scrollToSection(footerSection);
    }

    /**
     * Calculates and applies the scroll position to bring the given section into view.
     */
    private void scrollToSection(VBox section) {
        if (scrollPane == null || section == null) return;

        double sectionY       = section.getBoundsInParent().getMinY();
        double contentHeight  = scrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double vmax           = scrollPane.getVmax();

        if (contentHeight > viewportHeight) {
            double vvalue = sectionY / (contentHeight - viewportHeight) * vmax;
            scrollPane.setVvalue(vvalue);
        }
    }

    @FXML private void goToSignup() { SceneManager.getInstance().navigateTo("SignupView"); }
    @FXML private void goToLogin()  { SceneManager.getInstance().navigateTo("LoginView"); }
}
