package fpt.fall2025.posetrainer.Domain;

public class OnboardingData {
    private String gender;
    private String currentBodyType;
    private String targetBodyType;  // NEW
    private String goal;
    private String activityLevel;
    private int weeklyGoal;
    private float weight;
    private float height;
    private String birthday;
    private int dailyMinutes;
    private String experienceLevel;
    private float targetWeight;

    public OnboardingData() {
        this.weeklyGoal = 4;
        this.weight = 75.0f;
        this.height = 175.0f;
        this.dailyMinutes = 30;
        this.targetWeight = 70.0f;
    }

    // Getters
    public String getGender() { return gender; }
    public String getCurrentBodyType() { return currentBodyType; }
    public String getTargetBodyType() { return targetBodyType; }
    public String getGoal() { return goal; }
    public String getActivityLevel() { return activityLevel; }
    public int getWeeklyGoal() { return weeklyGoal; }
    public float getWeight() { return weight; }
    public float getHeight() { return height; }
    public String getBirthday() { return birthday; }
    public int getDailyMinutes() { return dailyMinutes; }
    public String getExperienceLevel() { return experienceLevel; }
    public float getTargetWeight() { return targetWeight; }

    // Setters
    public void setGender(String gender) { this.gender = gender; }
    public void setCurrentBodyType(String currentBodyType) { this.currentBodyType = currentBodyType; }
    public void setTargetBodyType(String targetBodyType) { this.targetBodyType = targetBodyType; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public void setWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }
    public void setWeight(float weight) { this.weight = weight; }
    public void setHeight(float height) { this.height = height; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public void setDailyMinutes(int dailyMinutes) { this.dailyMinutes = dailyMinutes; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public void setTargetWeight(float targetWeight) { this.targetWeight = targetWeight; }


    public void setBodyPart(String bodyPart) {
        this.currentBodyType = bodyPart;
    }

    public String getBodyPart() {
        return currentBodyType;
    }
}