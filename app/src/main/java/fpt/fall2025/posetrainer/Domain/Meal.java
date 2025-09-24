package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class Meal implements Serializable {
    private String id;
    private String name;
    private String type; // "breakfast", "lunch", "dinner", "snack"
    private int kcal;
    private MacroNutrients macros;
    private List<String> ingredients;
    private String imageUrl;
    private List<String> tags;
    private String level; // "beginner", "intermediate", "advanced"
    private boolean isPublic;

    public Meal() {}

    public Meal(String id, String name, String type, int kcal, MacroNutrients macros, 
               List<String> ingredients, String imageUrl, List<String> tags, 
               String level, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.kcal = kcal;
        this.macros = macros;
        this.ingredients = ingredients;
        this.imageUrl = imageUrl;
        this.tags = tags;
        this.level = level;
        this.isPublic = isPublic;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    // Inner class for MacroNutrients
    public static class MacroNutrients implements Serializable {
        private int protein; // grams
        private int carb; // grams
        private int fat; // grams

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

