package com.fitmate.controller;

import com.fitmate.service.AuthService;
import com.fitmate.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Sign Up screen.
 * Validates input, creates a new Firebase account via AuthService,
 * and redirects to the Login screen on success.
 */
public class SignupController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Button        signupButton;
    @FXML private Label         errorLabel;

    @FXML
    private void handleSignup() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        // Ensure all fields are filled before proceeding
        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // Passwords must match before calling the API
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // Disable button and show loading state while account is being created
        signupButton.setDisable(true);
        signupButton.setText("Creating Account...");
        hideError();

        // Register the user; on success navigate to Login so they sign in
        AuthService.getInstance().signUp(email, password)
                .thenAccept(uid -> Platform.runLater(() ->
                        SceneManager.getInstance().navigateTo("LoginView")
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                        signupButton.setDisable(false);
                        signupButton.setText("Sign Up");
                    });
                    return null;
                });
    }

    @FXML private void goToLogin()   { SceneManager.getInstance().navigateTo("LoginView"); }
    @FXML private void goToLanding() { SceneManager.getInstance().navigateTo("LandingView"); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
