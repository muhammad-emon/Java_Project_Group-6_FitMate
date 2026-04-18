package com.fitmate.util;

/**
 * Centralized color constants matching the FitMate dark theme.
 * Used across all views for consistent styling.
 */
public final class AppColors {

    private AppColors() { }

    // ── Backgrounds ──
    public static final String PRIMARY_BG    = "#121212";
    public static final String SECONDARY_BG  = "#1E1E1E";

    // ── Accents ──
    public static final String PRIMARY_ACCENT   = "#00C897";   // teal / green
    public static final String SECONDARY_ACCENT = "#FF884B";   // orange

    // ── Text ──
    public static final String PRIMARY_TEXT   = "#E0E0E0";
    public static final String SECONDARY_TEXT = "#9E9E9E";

    // ── Extra ──
    public static final String ERROR_RED     = "#DC2626";
    public static final String ACCENT_YELLOW = "#FACC15";
    public static final String ACCENT_PURPLE = "#A855F7";

    /** CSS-ready -fx-background-color string */
    public static String bg(String hex) {
        return "-fx-background-color: " + hex + ";";
    }

    /** CSS-ready -fx-text-fill string */
    public static String text(String hex) {
        return "-fx-text-fill: " + hex + ";";
    }
}
