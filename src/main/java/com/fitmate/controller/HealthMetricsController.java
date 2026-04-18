package com.fitmate.controller;

import com.fitmate.dao.*;
import com.fitmate.model.*;
import com.fitmate.service.*;
import com.fitmate.util.AppColors;
import com.fitmate.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller for the Health Metrics sub-view.
 * Computes BMI, BMR, TDEE, and BFP from the user's saved profile,
 * draws a semicircular BMI gauge and a line chart of weight history,
 * and allows the user to log a new weight entry.
 */
public class HealthMetricsController {

    @FXML private VBox  incompleteBox, bfpCard, chartCard;
    @FXML private HBox  mainContent;
    @FXML private Canvas weightChartCanvas, bmiGaugeCanvas;
    @FXML private TextField weightInput;
    @FXML private Button    logWeightBtn;
    @FXML private Label     bmiValueLabel, bmiCategoryLabel, bmrLabel, tdeeLabel, bfpLabel, bfpUnitLabel;

    private final UserDAO       userDAO       = new UserDAOImpl();
    private final WeightLogDAO  weightLogDAO  = new WeightLogDAOImpl();
    private List<WeightLogEntry> weightLog    = new ArrayList<>();

    @FXML
    public void initialize() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        userDAO.getUserProfile(uid).thenAccept(profile -> Platform.runLater(() -> {
            if (profile == null) {
                showIncomplete();
                return;
            }

            // Compute all health metrics from stored profile values
            HealthMetrics m = MetricsCalculator.calculate(
                    profile.getWeight(), profile.getHeight(),
                    profile.getAge(), profile.getGender(),
                    profile.getActivityLevel());

            if (m == null) {
                showIncomplete();
                return;
            }

            showMainContent();
            bfpUnitLabel.setText("%");

            // Populate metric labels and draw the BMI gauge arc
            bmiValueLabel.setText(String.format("%.1f", m.getBmi()));
            bmiCategoryLabel.setText(m.getBmiCategory());
            bmiCategoryLabel.setStyle("-fx-text-fill: " + getBmiColor(m.getBmi()) + "; -fx-font-weight: bold; -fx-font-size: 14px;");
            drawBmiGauge(m.getBmi());

            bmrLabel.setText(String.format("%.0f", m.getBmr()));
            tdeeLabel.setText(String.format("%.0f", m.getTdee()));

            // Show BFP card only when the value was successfully estimated
            if (m.getBfp() != null) {
                bfpLabel.setText(String.format("%.1f", m.getBfp()));
                bfpCard.setVisible(true);
                bfpCard.setManaged(true);
            }

            if (profile.getWeight() != null) {
                weightInput.setText(profile.getWeight());
            }

            // Re-draw the weight chart whenever the card is resized
            chartCard.widthProperty().addListener((obs, oldW, newW) -> {
                if (newW.doubleValue() > 100) {
                    weightChartCanvas.setWidth(newW.doubleValue() - 60);
                    drawWeightChart(this.weightLog);
                }
            });

            loadWeightLog(uid);
        }));
    }

    /** Fetches all weight log entries and triggers a chart redraw. */
    private void loadWeightLog(String uid) {
        weightLogDAO.getWeightLog(uid).thenAccept(entries -> Platform.runLater(() -> {
            this.weightLog = entries;
            drawWeightChart(entries);
        }));
    }

    /**
     * Validates the weight input, saves a new WeightLogEntry to Firestore,
     * updates the profile's current weight field, and refreshes the chart.
     */
    @FXML
    private void handleLogWeight() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) return;

        String val = weightInput.getText().trim();
        try {
            double w = Double.parseDouble(val);
            if (w <= 0) throw new NumberFormatException();

            logWeightBtn.setDisable(true);
            WeightLogEntry entry = new WeightLogEntry(w, new Date());

            weightLogDAO.addWeightLog(uid, entry).thenRun(() -> {
                // Also update the weight field in the main profile document
                Map<String, Object> data = new HashMap<>();
                data.put("weight", w);
                userDAO.saveUserProfile(uid, data);

                Platform.runLater(() -> {
                    logWeightBtn.setDisable(false);
                    loadWeightLog(uid);
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> logWeightBtn.setDisable(false));
                return null;
            });
        } catch (NumberFormatException e) {
            // Invalid or non-positive input — ignore silently
        }
    }

    /** Navigates to the Profile sub-view via the parent DashboardController. */
    @FXML
    private void goToProfile() {
        DashboardController dc = SceneManager.getInstance().getController("DashboardView");
        if (dc != null) dc.loadSubView("profile");
    }

    // ── Canvas Drawing ────────────────────────────────────────────────────────

    /**
     * Draws a semicircular BMI gauge arc on the bmiGaugeCanvas.
     * A dark background arc spans the full 180°; a colored foreground arc
     * represents the user's BMI clamped to the range [15, 40].
     */
    private void drawBmiGauge(double bmi) {
        GraphicsContext gc = bmiGaugeCanvas.getGraphicsContext2D();
        double w  = bmiGaugeCanvas.getWidth();
        double h  = bmiGaugeCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        double cx        = w / 2;
        double cy        = h - 10;
        double radius    = 80;
        double lineWidth = 14;

        // Background track arc
        gc.setStroke(Color.web("#2A2A2A"));
        gc.setLineWidth(lineWidth);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                0, 180, javafx.scene.shape.ArcType.OPEN);

        // Foreground colored arc sized to the user's BMI position
        double clamped = Math.max(15, Math.min(40, bmi));
        double pct     = (clamped - 15) / 25.0;
        double angle   = 180 * pct;

        gc.setStroke(Color.web(getBmiColor(bmi)));
        gc.setLineWidth(lineWidth);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                180, -angle, javafx.scene.shape.ArcType.OPEN);
    }

    /**
     * Draws a line chart of weight history on the weightChartCanvas.
     * Includes dashed horizontal grid lines, a shaded area under the line,
     * dot markers at each data point, weight value labels, and date labels.
     * Requires at least 2 entries to render; otherwise shows a placeholder message.
     */
    private void drawWeightChart(List<WeightLogEntry> data) {
        GraphicsContext gc = weightChartCanvas.getGraphicsContext2D();
        double cw = weightChartCanvas.getWidth();
        double ch = weightChartCanvas.getHeight();
        gc.clearRect(0, 0, cw, ch);

        if (data == null || data.size() < 2) {
            gc.setFill(Color.web(AppColors.SECONDARY_TEXT));
            gc.setFont(Font.font(14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Log two weights to see your chart.", cw / 2, ch / 2);
            return;
        }

        double pad = 50;
        double pw  = cw - pad * 2;
        double ph  = ch - pad * 2;

        double[] weights = data.stream().mapToDouble(WeightLogEntry::getWeight).toArray();
        double minW = Arrays.stream(weights).min().orElse(0) - 2;
        double maxW = Arrays.stream(weights).max().orElse(100) + 2;

        // Draw three dashed horizontal grid lines at min, mid, and max weight
        gc.setStroke(Color.web("#404040"));
        gc.setLineWidth(1);
        gc.setLineDashes(2, 3);
        double midW = (minW + maxW) / 2;
        for (double val : new double[]{minW, midW, maxW}) {
            double y = pad + ph - ((val - minW) / (maxW - minW)) * ph;
            gc.strokeLine(pad, y, cw - pad, y);
            gc.setFill(Color.web(AppColors.SECONDARY_TEXT));
            gc.setFont(Font.font(10));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(String.format("%.0fkg", val), pad - 5, y + 3);
        }
        gc.setLineDashes(null);

        // Compute pixel coordinates for each data point
        double[] xs = new double[data.size()];
        double[] ys = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            xs[i] = pad + (i / (double) (data.size() - 1)) * pw;
            ys[i] = pad + ph - ((data.get(i).getWeight() - minW) / (maxW - minW)) * ph;
        }

        // Shaded fill area under the line
        gc.setFill(Color.web(AppColors.PRIMARY_ACCENT, 0.15));
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
        gc.lineTo(xs[xs.length - 1], pad + ph);
        gc.lineTo(xs[0], pad + ph);
        gc.closePath();
        gc.fill();

        // Accent-colored line connecting the data points
        gc.setStroke(Color.web(AppColors.PRIMARY_ACCENT));
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
        gc.stroke();

        // Dot markers with weight value above and date label below each point
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
        for (int i = 0; i < data.size(); i++) {
            gc.setFill(Color.web(AppColors.PRIMARY_ACCENT));
            gc.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);

            gc.setFill(Color.web(AppColors.PRIMARY_TEXT));
            gc.setFont(Font.font(10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.format("%.1f", data.get(i).getWeight()), xs[i], ys[i] - 10);

            if (data.get(i).getDate() != null) {
                gc.setFill(Color.web(AppColors.SECONDARY_TEXT));
                gc.fillText(sdf.format(data.get(i).getDate()), xs[i], pad + ph + 15);
            }
        }
    }

    /**
     * Returns the hex color representing the BMI category:
     * purple = underweight, green = normal, orange = overweight, red = obese.
     */
    private String getBmiColor(double bmi) {
        if (bmi < 18.5) return AppColors.ACCENT_PURPLE;
        if (bmi < 25)   return AppColors.PRIMARY_ACCENT;
        if (bmi < 30)   return AppColors.SECONDARY_ACCENT;
        return AppColors.ERROR_RED;
    }

    private void showIncomplete() {
        incompleteBox.setVisible(true);
        incompleteBox.setManaged(true);
        mainContent.setVisible(false);
        mainContent.setManaged(false);
    }

    private void showMainContent() {
        incompleteBox.setVisible(false);
        incompleteBox.setManaged(false);
        mainContent.setVisible(true);
        mainContent.setManaged(true);
    }
}