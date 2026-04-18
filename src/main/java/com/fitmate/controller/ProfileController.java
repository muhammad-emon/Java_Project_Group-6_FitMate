package com.fitmate.controller;

import com.fitmate.dao.*;
import com.fitmate.model.UserProfile;
import com.fitmate.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

/**
 * Controller for the Profile sub-view.
 * Loads the user's existing profile from Firestore on initialization,
 * allows editing of personal data and fitness goal, and persists changes back.
 */
public class ProfileController {

    @FXML private TextField  firstNameField, lastNameField, ageField, heightField;
    @FXML private TextField  weightField, targetWeightField, countryField;
    @FXML private ComboBox<String> genderCombo, activityCombo;
    @FXML private Button     btnLose, btnMaintain, btnGain, saveButton;
    @FXML private Label      statusLabel, avatarLabel;

    private final UserDAO userDAO = new UserDAOImpl();
    private String selectedGoal = "";

    @FXML
    public void initialize() {
        // Populate gender and activity level dropdown options
        genderCombo.getItems().addAll("Male", "Female", "Other");
        activityCombo.getItems().addAll(
                "1.2 - Sedentary (office job)",
                "1.375 - Lightly Active (1-3 days/wk)",
                "1.55 - Moderately Active (3-5 days/wk)",
                "1.725 - Very Active (6-7 days/wk)",
                "1.9 - Extra Active (hard labor/2x day)"
        );

        // Load the saved profile from Firestore and pre-fill all fields
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        userDAO.getUserProfile(uid).thenAccept(profile -> Platform.runLater(() -> {
            if (profile == null) return;

            firstNameField.setText(profile.getFirstName()     != null ? profile.getFirstName()     : "");
            lastNameField.setText(profile.getLastName()       != null ? profile.getLastName()       : "");
            ageField.setText(profile.getAge()                 != null ? profile.getAge()            : "");
            heightField.setText(profile.getHeight()           != null ? profile.getHeight()         : "");
            weightField.setText(profile.getWeight()           != null ? profile.getWeight()         : "");
            targetWeightField.setText(profile.getTargetWeight()!= null ? profile.getTargetWeight()  : "");
            countryField.setText(profile.getCountry()         != null ? profile.getCountry()        : "");

            if (profile.getGender() != null) {
                genderCombo.setValue(profile.getGender());
            }

            // Match the stored activity multiplier to the corresponding combo item
            if (profile.getActivityLevel() != null) {
                for (String item : activityCombo.getItems()) {
                    if (item.startsWith(profile.getActivityLevel())) {
                        activityCombo.setValue(item);
                        break;
                    }
                }
            }

            // Highlight the previously selected goal button
            if (profile.getGoal() != null) {
                selectedGoal = profile.getGoal();
                updateGoalButtons();
            }

            // Show a check mark if a profile photo has been saved
            if (profile.getProfilePic() != null && !profile.getProfilePic().isBlank()) {
                avatarLabel.setText("✓");
            }
        }));
    }

    /** Updates selectedGoal and refreshes button highlight styles on click. */
    @FXML
    private void selectGoal(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        selectedGoal = source.getText();
        updateGoalButtons();
    }

    /**
     * Applies the active style to the selected goal button
     * and resets all others to the default style.
     */
    private void updateGoalButtons() {
        Button[] btns = {btnLose, btnMaintain, btnGain};
        for (Button b : btns) {
            if (b.getText().equals(selectedGoal)) {
                b.getStyleClass().removeAll("btn-goal");
                if (!b.getStyleClass().contains("btn-goal-active"))
                    b.getStyleClass().add("btn-goal-active");
            } else {
                b.getStyleClass().removeAll("btn-goal-active");
                if (!b.getStyleClass().contains("btn-goal"))
                    b.getStyleClass().add("btn-goal");
            }
        }
    }

    /**
     * Collects all form values and saves them to Firestore via UserDAO.
     * Extracts the numeric activity multiplier from the combo selection string.
     */
    @FXML
    private void handleSave() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        saveButton.setDisable(true);
        saveButton.setText("Saving...");

        Map<String, Object> data = new HashMap<>();
        data.put("firstName",    firstNameField.getText().trim());
        data.put("lastName",     lastNameField.getText().trim());
        data.put("age",          ageField.getText().trim());
        data.put("gender",       genderCombo.getValue());
        data.put("height",       heightField.getText().trim());
        data.put("weight",       weightField.getText().trim());
        data.put("targetWeight", targetWeightField.getText().trim());
        data.put("country",      countryField.getText().trim());
        data.put("goal",         selectedGoal);

        // Extract just the numeric multiplier prefix (e.g. "1.55" from "1.55 - Moderately Active")
        String activityStr = activityCombo.getValue();
        if (activityStr != null && activityStr.contains(" - ")) {
            data.put("activityLevel", activityStr.split(" - ")[0].trim());
        }

        userDAO.saveUserProfile(uid, data)
                .thenRun(() -> Platform.runLater(() -> {
                    showStatus("Profile updated successfully!");
                    saveButton.setDisable(false);
                    saveButton.setText("Save Changes");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showStatus("Error saving profile. Please try again.");
                        saveButton.setDisable(false);
                        saveButton.setText("Save Changes");
                    });
                    return null;
                });
    }

    @FXML
    private void handleUploadPhoto() {
        // Photo upload via FileChooser + Firebase Storage is not yet implemented
        showStatus("Photo upload coming soon.");
    }

    private void showStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
