package com.fitmate.model;

/**
 * Represents a user's profile stored in Firestore under users/{uid}.
 */
public class UserProfile {

    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String age;
    private String gender;
    private String height;      // cm
    private String weight;      // kg
    private String targetWeight; // kg
    private String goal;        // "Lose Weight", "Maintain", "Gain Weight"
    private String activityLevel; // 1.2, 1.375, 1.55, 1.725, 1.9
    private String country;
    private String profilePic;  // URL

    public UserProfile() { }

    // ── Getters & Setters ──

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getTargetWeight() { return targetWeight; }
    public void setTargetWeight(String targetWeight) { this.targetWeight = targetWeight; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    /** Display name for greeting; prefers firstName. */
    public String getDisplayName() {
        if (firstName != null && !firstName.isBlank()) return firstName;
        return "there";
    }
}
