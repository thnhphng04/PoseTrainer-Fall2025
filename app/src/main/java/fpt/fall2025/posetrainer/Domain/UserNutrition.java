package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class UserNutrition implements Serializable {
    private String id;
    private String uid;
    private NutritionTargets targets;
    private long updatedAt;
    private List<NutritionLog> logs;

    public UserNutrition() {}

    public UserNutrition(String id, String uid, NutritionTargets targets, 
                        long updatedAt, List<NutritionLog> logs) {
        this.id = id;
        this.uid = uid;
        this.targets = targets;
        this.updatedAt = updatedAt;
        this.logs = logs;
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

    public NutritionTargets getTargets() {
        return targets;
    }

    public void setTargets(NutritionTargets targets) {
        this.targets = targets;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<NutritionLog> getLogs() {
        return logs;
    }

    public void setLogs(List<NutritionLog> logs) {
        this.logs = logs;
    }

    // Inner class for NutritionTargets
    public static class NutritionTargets implements Serializable {
        private int dailyKcal;
        private int proteinG;
        private int carbG;
        private int fatG;

        public NutritionTargets() {}

        public NutritionTargets(int dailyKcal, int proteinG, int carbG, int fatG) {
            this.dailyKcal = dailyKcal;
            this.proteinG = proteinG;
            this.carbG = carbG;
            this.fatG = fatG;
        }

        public int getDailyKcal() {
            return dailyKcal;
        }

        public void setDailyKcal(int dailyKcal) {
            this.dailyKcal = dailyKcal;
        }

        public int getProteinG() {
            return proteinG;
        }

        public void setProteinG(int proteinG) {
            this.proteinG = proteinG;
        }

        public int getCarbG() {
            return carbG;
        }

        public void setCarbG(int carbG) {
            this.carbG = carbG;
        }

        public int getFatG() {
            return fatG;
        }

        public void setFatG(int fatG) {
            this.fatG = fatG;
        }
    }

    // Inner class for NutritionLog
    public static class NutritionLog implements Serializable {
        private String date; // "YYYY-MM-DD" format
        private List<NutritionMeal> meals;
        private NutritionTotal total;

        public NutritionLog() {}

        public NutritionLog(String date, List<NutritionMeal> meals, NutritionTotal total) {
            this.date = date;
            this.meals = meals;
            this.total = total;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<NutritionMeal> getMeals() {
            return meals;
        }

        public void setMeals(List<NutritionMeal> meals) {
            this.meals = meals;
        }

        public NutritionTotal getTotal() {
            return total;
        }

        public void setTotal(NutritionTotal total) {
            this.total = total;
        }
    }

    // Inner class for NutritionMeal
    public static class NutritionMeal implements Serializable {
        private String type; // "breakfast", "lunch", "dinner", "snack"
        private String mealId;
        private int kcal;
        private MacroNutrients macros;

        public NutritionMeal() {}

        public NutritionMeal(String type, String mealId, int kcal, MacroNutrients macros) {
            this.type = type;
            this.mealId = mealId;
            this.kcal = kcal;
            this.macros = macros;
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

        public int getKcal() {
            return kcal;
        }

        public void setKcal(int kcal) {
            this.kcal = kcal;
        }

        public MacroNutrients getMacros() {
            return macros;
        }

        public void setMacros(MacroNutrients macros) {
            this.macros = macros;
        }
    }

    // Inner class for NutritionTotal
    public static class NutritionTotal implements Serializable {
        private int kcal;
        private int protein;
        private int carb;
        private int fat;

        public NutritionTotal() {}

        public NutritionTotal(int kcal, int protein, int carb, int fat) {
            this.kcal = kcal;
            this.protein = protein;
            this.carb = carb;
            this.fat = fat;
        }

        public int getKcal() {
            return kcal;
        }

        public void setKcal(int kcal) {
            this.kcal = kcal;
        }

        public int getProtein() {
            return protein;
        }

        public void setProtein(int protein) {
            this.protein = protein;
        }

        public int getCarb() {
            return carb;
        }

        public void setCarb(int carb) {
            this.carb = carb;
        }

        public int getFat() {
            return fat;
        }

        public void setFat(int fat) {
            this.fat = fat;
        }
    }

    // Inner class for MacroNutrients
    public static class MacroNutrients implements Serializable {
        private int protein;
        private int carb;
        private int fat;

        public MacroNutrients() {}

        public MacroNutrients(int protein, int carb, int fat) {
            this.protein = protein;
            this.carb = carb;
            this.fat = fat;
        }

        public int getProtein() {
            return protein;
        }

        public void setProtein(int protein) {
            this.protein = protein;
        }

        public int getCarb() {
            return carb;
        }

        public void setCarb(int carb) {
            this.carb = carb;
        }

        public int getFat() {
            return fat;
        }

        public void setFat(int fat) {
            this.fat = fat;
        }
    }
}

