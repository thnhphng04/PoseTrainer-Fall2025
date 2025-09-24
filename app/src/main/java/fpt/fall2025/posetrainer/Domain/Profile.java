package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class Profile implements Serializable {
    private String uid;
    private int heightCm;
    private int weightKg;
    private int bodyFatPct;
    private String birthday;
    private String gender;
    private List<String> bodyPhotos;
    private Goals goals;
    private Preferences preferences;
    private long lastUpdatedAt;

    public Profile() {}

    public Profile(String uid, int heightCm, int weightKg, int bodyFatPct, String birthday, 
                   String gender, List<String> bodyPhotos, Goals goals, 
                   Preferences preferences, long lastUpdatedAt) {
        this.uid = uid;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bodyFatPct = bodyFatPct;
        this.birthday = birthday;
        this.gender = gender;
        this.bodyPhotos = bodyPhotos;
        this.goals = goals;
        this.preferences = preferences;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(int heightCm) {
        this.heightCm = heightCm;
    }

    public int getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(int weightKg) {
        this.weightKg = weightKg;
    }

    public int getBodyFatPct() {
        return bodyFatPct;
    }

    public void setBodyFatPct(int bodyFatPct) {
        this.bodyFatPct = bodyFatPct;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<String> getBodyPhotos() {
        return bodyPhotos;
    }

    public void setBodyPhotos(List<String> bodyPhotos) {
        this.bodyPhotos = bodyPhotos;
    }

    public Goals getGoals() {
        return goals;
    }

    public void setGoals(Goals goals) {
        this.goals = goals;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    // Inner class for Goals
    public static class Goals implements Serializable {
        private String type; // "lose_fat", "gain_muscle", "maintain", etc.

        public Goals() {}

        public Goals(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // Inner class for Preferences
    public static class Preferences implements Serializable {
        private String units; // "metric", "imperial"
        private String cameraMode; // "front", "back"

        public Preferences() {}

        public Preferences(String units, String cameraMode) {
            this.units = units;
            this.cameraMode = cameraMode;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public String getCameraMode() {
            return cameraMode;
        }

        public void setCameraMode(String cameraMode) {
            this.cameraMode = cameraMode;
        }
    }
}

