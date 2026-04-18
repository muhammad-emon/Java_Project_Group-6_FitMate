package com.fitmate.service;

import com.fitmate.model.HealthMetrics;

/**
 * Utility class for computing all health metrics from a user's profile data.
 * All formulas match the original frontend implementation for consistency.
 *
 * Metrics calculated:
 *  - BMI  : Body Mass Index (kg/m²)
 *  - BMI Category : Underweight / Normal / Overweight / Obese
 *  - BMR  : Basal Metabolic Rate using the Mifflin-St Jeor equation
 *  - TDEE : Total Daily Energy Expenditure (BMR × activity multiplier)
 *  - BFP  : Body Fat Percentage estimate (Deurenberg formula)
 */
public final class MetricsCalculator {

    private MetricsCalculator() { /* static utility class — no instances */ }

    /**
     * Computes all health metrics from the user's raw profile strings.
     * Returns null if any required field is missing or cannot be parsed as a number.
     *
     * @param weightStr      body weight in kilograms (e.g. "75")
     * @param heightStr      height in centimetres (e.g. "175")
     * @param ageStr         age in years (e.g. "25")
     * @param gender         "Male", "Female", or other
     * @param activityLevelStr  activity multiplier (e.g. "1.55")
     * @return populated HealthMetrics, or null if input is invalid / incomplete
     */
    public static HealthMetrics calculate(String weightStr, String heightStr,
                                          String ageStr, String gender,
                                          String activityLevelStr) {
        if (weightStr == null || heightStr == null || ageStr == null
                || gender == null || activityLevelStr == null) {
            return null;
        }

        try {
            double weight   = Double.parseDouble(weightStr);
            double height   = Double.parseDouble(heightStr);
            double age      = Double.parseDouble(ageStr);
            double activity = Double.parseDouble(activityLevelStr);

            if (weight <= 0 || height <= 0 || age <= 0) return null;

            HealthMetrics m = new HealthMetrics();

            // BMI = weight (kg) / height² (m)
            double hMeters = height / 100.0;
            double bmi     = weight / (hMeters * hMeters);
            m.setBmi(bmi);

            // Classify BMI into standard WHO categories
            if      (bmi < 18.5) m.setBmiCategory("Underweight");
            else if (bmi < 25)   m.setBmiCategory("Normal");
            else if (bmi < 30)   m.setBmiCategory("Overweight");
            else                 m.setBmiCategory("Obese");

            // BMR using Mifflin-St Jeor equation (gender-specific constant)
            double bmr;
            if      ("Male".equals(gender))   bmr = 10 * weight + 6.25 * height - 5 * age + 5;
            else if ("Female".equals(gender)) bmr = 10 * weight + 6.25 * height - 5 * age - 161;
            else                              bmr = 10 * weight + 6.25 * height - 5 * age - 78;
            m.setBmr(bmr);

            // TDEE = BMR × PAL (Physical Activity Level)
            m.setTdee(bmr * activity);

            // BFP estimate using Deurenberg formula (gender-specific intercept)
            if      ("Male".equals(gender))   m.setBfp(1.2 * bmi + 0.23 * age - 10.8 - 5.4);
            else if ("Female".equals(gender)) m.setBfp(1.2 * bmi + 0.23 * age - 5.4);
            // BFP is left null for "Other" gender (formula not applicable)

            return m;
        } catch (NumberFormatException e) {
            return null; // Unparseable profile data — caller should show an incomplete state
        }
    }
}
