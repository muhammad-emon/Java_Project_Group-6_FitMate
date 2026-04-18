package com.fitmate.controller;

import com.fitmate.service.AuthService;
import com.fitmate.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Login screen.
 * Delegates authentication to AuthService and navigates to the
 * Dashboard on success, or shows an inline error on failure.
 */
public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate that both fields are filled before attempting login
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // Disable button and show loading state while auth request is in flight
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        hideError();

        // Perform sign-in asynchronously; navigate to Dashboard on success
        AuthService.getInstance().signIn(email, password)
                .thenAccept(uid -> Platform.runLater(() ->
                        SceneManager.getInstance().navigateToDashboard("home")
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                        loginButton.setDisable(false);
                        loginButton.setText("Log In");
                    });
                    return null;
                });
    }

    @FXML private void goToSignup()  { SceneManager.getInstance().navigateTo("SignupView"); }
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
