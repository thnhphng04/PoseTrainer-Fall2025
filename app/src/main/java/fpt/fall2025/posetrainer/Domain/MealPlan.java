package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class MealPlan implements Serializable {
    private String id;
    private String uid;
    private String title;
    private int targetKcal;
    private String goalFit; // "lose_fat", "gain_muscle", "maintain"
    private List<MealPlanDay> days;
    private boolean isPublic;
    private int version;

    public MealPlan() {}

    public MealPlan(String id, String uid, String title, int targetKcal, String goalFit, 
                   List<MealPlanDay> days, boolean isPublic, int version) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.targetKcal = targetKcal;
        this.goalFit = goalFit;
        this.days = days;
        this.isPublic = isPublic;
        this.version = version;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTargetKcal() {
        return targetKcal;
    }

    public void setTargetKcal(int targetKcal) {
        this.targetKcal = targetKcal;
    }

    public String getGoalFit() {
        return goalFit;
    }

    public void setGoalFit(String goalFit) {
        this.goalFit = goalFit;
    }

    public List<MealPlanDay> getDays() {
        return days;
    }

    public void setDays(List<MealPlanDay> days) {
        this.days = days;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    // Inner class for MealPlanDay
    public static class MealPlanDay implements Serializable {
        private int dayNo; // 1-7 (Monday-Sunday)
        private List<MealPlanMeal> meals;

        public MealPlanDay() {}

        public MealPlanDay(int dayNo, List<MealPlanMeal> meals) {
            this.dayNo = dayNo;
            this.meals = meals;
        }

        public int getDayNo() {
            return dayNo;
        }

        public void setDayNo(int dayNo) {
            this.dayNo = dayNo;
        }

        public List<MealPlanMeal> getMeals() {
            return meals;
        }

        public void setMeals(List<MealPlanMeal> meals) {
            this.meals = meals;
        }
    }

    // Inner class for MealPlanMeal
    public static class MealPlanMeal implements Serializable {
        private String type; // "breakfast", "lunch", "dinner", "snack"
        private String mealId;
        private List<String> swaps; // alternative meal IDs

        public MealPlanMeal() {}

        public MealPlanMeal(String type, String mealId, List<String> swaps) {
            this.type = type;
            this.mealId = mealId;
            this.swaps = swaps;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMealId() {
            return mealId;
        }

        public void setMealId(String mealId) {
            this.mealId = mealId;
        }

        public List<String> getSwaps() {
            return swaps;
        }

        public void setSwaps(List<String> swaps) {
            this.swaps = swaps;
        }
    }
}

