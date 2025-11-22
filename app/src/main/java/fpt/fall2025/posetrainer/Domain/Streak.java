package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;

public class Streak implements Serializable {
    private String uid;
    private int currentStreak;
    private int longestStreak;
    private String lastWorkoutDate; // Format: "yyyy-MM-dd"

    public Streak() {}

    public Streak(String uid, int currentStreak, int longestStreak, String lastWorkoutDate) {
        this.uid = uid;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastWorkoutDate = lastWorkoutDate;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public String getLastWorkoutDate() {
        return lastWorkoutDate;
    }

    public void setLastWorkoutDate(String lastWorkoutDate) {
        this.lastWorkoutDate = lastWorkoutDate;
    }
}

