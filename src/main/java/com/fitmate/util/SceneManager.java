package com.fitmate.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton utility that manages all scene transitions in the application.
 * Loads FXML views by name, applies the global stylesheet, and caches each
 * view's controller so other components can retrieve it without reloading the FXML.
 */
public class SceneManager {

    private static SceneManager instance;

    private Stage primaryStage;

    // Stores the most recently loaded controller for each FXML view name
    private final Map<String, Object> controllerCache = new HashMap<>();

    private SceneManager() { }

    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) { this.primaryStage = stage; }
    public Stage getPrimaryStage()           { return primaryStage; }

    /**
     * Loads the specified FXML view, applies the global CSS, and sets it as
     * the current scene on the primary stage. The view's controller is stored
     * in the cache so it can be retrieved later via {@link #getController}.
     *
     * @param fxmlName the FXML file name without extension (e.g. "LoginView")
     */
    public void navigateTo(String fxmlName) {
        try {
            String path  = "/fxml/" + fxmlName + ".fxml";
            URL resource = getClass().getResource(path);
            if (resource == null) throw new IOException("FXML not found: " + path);

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // Cache the controller so other classes can reference it without reloading
            Object controller = loader.getController();
            if (controller != null) {
                controllerCache.put(fxmlName, controller);
            }

            // Preserve the current window dimensions during scene transitions
            Scene scene = new Scene(root,
                    primaryStage.getScene().getWidth(),
                    primaryStage.getScene().getHeight());

            // Re-apply the global stylesheet to each new scene
            URL css = getClass().getResource("/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    /**
     * Navigates to the Dashboard shell and immediately loads the specified
     * sub-view inside it. Used after login to land directly on the Home tab.
     *
     * @param subView logical sub-view name passed to DashboardController.loadSubView()
     */
    public void navigateToDashboard(String subView) {
        try {
            URL resource = getClass().getResource("/fxml/DashboardView.fxml");
            if (resource == null) throw new IOException("DashboardView.fxml not found");

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // Cache the dashboard controller for sub-view switching later
            Object controller = loader.getController();
            controllerCache.put("DashboardView", controller);

            Scene scene = new Scene(root,
                    primaryStage.getScene().getWidth(),
                    primaryStage.getScene().getHeight());

            URL css = getClass().getResource("/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            primaryStage.setScene(scene);

            // Tell the dashboard shell which sub-view to show first
            if (controller instanceof com.fitmate.controller.DashboardController dc) {
                dc.loadSubView(subView);
            }
        } catch (IOException e) {
            System.err.println("Dashboard navigation error: " + e.getMessage());
        }
    }

    /**
     * Returns the cached controller for the given FXML view name,
     * or null if that view has not been loaded yet.
     *
     * @param name the FXML file name without extension (e.g. "DashboardView")
     */
    @SuppressWarnings("unchecked")
    public <T> T getController(String name) {
        return (T) controllerCache.get(name);
    }
}
