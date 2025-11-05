package fpt.fall2025.posetrainer.Domain;

public class OnboardingData {
    private String gender;
    private String bodyPart;
    private String goal;
    private String activityLevel;
    private int weeklyGoal;
    private float weight;
    private float height;

    public OnboardingData() {
        // Default values
        this.weeklyGoal = 4;
        this.weight = 75.0f;
        this.height = 175.0f;
    }

    // Getters
    public String getGender() { return gender; }
    public String getBodyPart() { return bodyPart; }
    public String getGoal() { return goal; }
    public String getActivityLevel() { return activityLevel; }
    public int getWeeklyGoal() { return weeklyGoal; }
    public float getWeight() { return weight; }
    public float getHeight() { return height; }

    // Setters
    public void setGender(String gender) { this.gender = gender; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public void setWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }
    public void setWeight(float weight) { this.weight = weight; }
    public void setHeight(float height) { this.height = height; }
}