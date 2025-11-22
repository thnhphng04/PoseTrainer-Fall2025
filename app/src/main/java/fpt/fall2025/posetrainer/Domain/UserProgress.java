package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class UserProgress implements Serializable {
    private String uid;
    private int totalWorkoutDays;
    private int totalSessions;
    private Map<String, Boolean> calendar; // Key: "yyyy-MM-dd", Value: true/false (có tập/không tập)

    public UserProgress() {
        this.calendar = new HashMap<>();
    }

    public UserProgress(String uid, int totalWorkoutDays, int totalSessions, Map<String, Boolean> calendar) {
        this.uid = uid;
        this.totalWorkoutDays = totalWorkoutDays;
        this.totalSessions = totalSessions;
        this.calendar = calendar != null ? calendar : new HashMap<>();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getTotalWorkoutDays() {
        return totalWorkoutDays;
    }

    public void setTotalWorkoutDays(int totalWorkoutDays) {
        this.totalWorkoutDays = totalWorkoutDays;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Map<String, Boolean> getCalendar() {
        return calendar;
    }

    public void setCalendar(Map<String, Boolean> calendar) {
        this.calendar = calendar != null ? calendar : new HashMap<>();
    }

    /**
     * Check if user worked out on a specific date
     */
    public boolean hasWorkoutOnDate(String date) {
        return calendar != null && Boolean.TRUE.equals(calendar.get(date));
    }

    /**
     * Mark a date as having a workout
     */
    public void markWorkoutDate(String date) {
        if (calendar == null) {
            calendar = new HashMap<>();
        }
        calendar.put(date, true);
    }
}

