package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;

public class Progress implements Serializable {
    private String id;
    private String uid;
    private String date; // "YYYY-MM-DD" format
    private ProgressMetrics metrics;

    public Progress() {}

    public Progress(String id, String uid, String date, ProgressMetrics metrics) {
        this.id = id;
        this.uid = uid;
        this.date = date;
        this.metrics = metrics;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public ProgressMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ProgressMetrics metrics) {
        this.metrics = metrics;
    }

    // Inner class for ProgressMetrics
    public static class ProgressMetrics implements Serializable {
        private int totalReps;
        private int durationSec;
        private int estKcal;
        private int workoutsCompleted;
        private int bodyWeightKg;
        private int bodyFatPct;

        public ProgressMetrics() {}

        public ProgressMetrics(int totalReps, int durationSec, int estKcal, 
                              int workoutsCompleted, int bodyWeightKg, int bodyFatPct) {
            this.totalReps = totalReps;
            this.durationSec = durationSec;
            this.estKcal = estKcal;
            this.workoutsCompleted = workoutsCompleted;
            this.bodyWeightKg = bodyWeightKg;
            this.bodyFatPct = bodyFatPct;
        }

        public int getTotalReps() {
            return totalReps;
        }

        public void setTotalReps(int totalReps) {
            this.totalReps = totalReps;
        }

        public int getDurationSec() {
            return durationSec;
        }

        public void setDurationSec(int durationSec) {
            this.durationSec = durationSec;
        }

        public int getEstKcal() {
            return estKcal;
        }

        public void setEstKcal(int estKcal) {
            this.estKcal = estKcal;
        }

        public int getWorkoutsCompleted() {
            return workoutsCompleted;
        }

        public void setWorkoutsCompleted(int workoutsCompleted) {
            this.workoutsCompleted = workoutsCompleted;
        }

        public int getBodyWeightKg() {
            return bodyWeightKg;
        }

        public void setBodyWeightKg(int bodyWeightKg) {
            this.bodyWeightKg = bodyWeightKg;
        }

        public int getBodyFatPct() {
            return bodyFatPct;
        }

        public void setBodyFatPct(int bodyFatPct) {
            this.bodyFatPct = bodyFatPct;
        }
    }
}

