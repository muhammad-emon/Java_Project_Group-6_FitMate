package com.fitmate;

import com.fitmate.service.FirebaseService;
import com.fitmate.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Entry point for the FitMate JavaFX application.
 * Bootstraps Firebase, sets up SceneManager, and loads the Landing page.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize Firebase Admin SDK (reads credentials from .env / serviceAccountKey.json)
        FirebaseService.getInstance();

        // Register the primary stage with SceneManager for global scene switching
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setPrimaryStage(primaryStage);

        // Load the Landing page as the initial scene
        URL fxmlUrl = getClass().getResource("/fxml/LandingView.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("LandingView.fxml not found!");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 800);

        // Apply global stylesheet to the root scene
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Configure and display the primary window
        primaryStage.setTitle("FitMate — Smart Fitness with AI");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
