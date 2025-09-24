package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class AIPlan implements Serializable {
    private String id;
    private String uid;
    private InputSnapshot inputSnapshot;
    private PlanData plan;
    private String status; // "ready", "processing", "completed", "failed"
    private long createdAt;
    private long updatedAt;

    public AIPlan() {}

    public AIPlan(String id, String uid, InputSnapshot inputSnapshot, PlanData plan, 
                  String status, long createdAt, long updatedAt) {
        this.id = id;
        this.uid = uid;
        this.inputSnapshot = inputSnapshot;
        this.plan = plan;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public InputSnapshot getInputSnapshot() {
        return inputSnapshot;
    }

    public void setInputSnapshot(InputSnapshot inputSnapshot) {
        this.inputSnapshot = inputSnapshot;
    }

    public PlanData getPlan() {
        return plan;
    }

    public void setPlan(PlanData plan) {
        this.plan = plan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Inner class for InputSnapshot
    public static class InputSnapshot implements Serializable {
        private ProfileSnapshot profile;
        private GoalsSnapshot goals;
        private ConstraintsSnapshot constraints;

        public InputSnapshot() {}

        public InputSnapshot(ProfileSnapshot profile, GoalsSnapshot goals, ConstraintsSnapshot constraints) {
            this.profile = profile;
            this.goals = goals;
            this.constraints = constraints;
        }

        public ProfileSnapshot getProfile() {
            return profile;
        }

        public void setProfile(ProfileSnapshot profile) {
            this.profile = profile;
        }

        public GoalsSnapshot getGoals() {
            return goals;
        }

        public void setGoals(GoalsSnapshot goals) {
            this.goals = goals;
        }

        public ConstraintsSnapshot getConstraints() {
            return constraints;
        }

        public void setConstraints(ConstraintsSnapshot constraints) {
            this.constraints = constraints;
        }
    }

    // Inner class for ProfileSnapshot
    public static class ProfileSnapshot implements Serializable {
        private int heightCm;
        private int weightKg;

        public ProfileSnapshot() {}

        public ProfileSnapshot(int heightCm, int weightKg) {
            this.heightCm = heightCm;
            this.weightKg = weightKg;
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
    }

    // Inner class for GoalsSnapshot
    public static class GoalsSnapshot implements Serializable {
        private String type;

        public GoalsSnapshot() {}

        public GoalsSnapshot(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // Inner class for ConstraintsSnapshot
    public static class ConstraintsSnapshot implements Serializable {
        // Can be extended with specific constraints
        public ConstraintsSnapshot() {}
    }

    // Inner class for PlanData
    public static class PlanData implements Serializable {
        private List<PlanWeek> weeks;
        private String rationale;
        private String version;

        public PlanData() {}

        public PlanData(List<PlanWeek> weeks, String rationale, String version) {
            this.weeks = weeks;
            this.rationale = rationale;
            this.version = version;
        }

        public List<PlanWeek> getWeeks() {
            return weeks;
        }

        public void setWeeks(List<PlanWeek> weeks) {
            this.weeks = weeks;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    // Inner class for PlanWeek
    public static class PlanWeek implements Serializable {
        private int weekNo;
        private List<PlanDay> days;

        public PlanWeek() {}

        public PlanWeek(int weekNo, List<PlanDay> days) {
            this.weekNo = weekNo;
            this.days = days;
        }

        public int getWeekNo() {
            return weekNo;
        }

        public void setWeekNo(int weekNo) {
            this.weekNo = weekNo;
        }

        public List<PlanDay> getDays() {
            return days;
        }

        public void setDays(List<PlanDay> days) {
            this.days = days;
        }
    }

    // Inner class for PlanDay
    public static class PlanDay implements Serializable {
        private int dayNo;
        private String userWorkoutId;
        private List<InlineItem> inlineItems;

        public PlanDay() {}

        public PlanDay(int dayNo, String userWorkoutId, List<InlineItem> inlineItems) {
            this.dayNo = dayNo;
            this.userWorkoutId = userWorkoutId;
            this.inlineItems = inlineItems;
        }

        public int getDayNo() {
            return dayNo;
        }

        public void setDayNo(int dayNo) {
            this.dayNo = dayNo;
        }

        public String getUserWorkoutId() {
            return userWorkoutId;
        }

        public void setUserWorkoutId(String userWorkoutId) {
            this.userWorkoutId = userWorkoutId;
        }

        public List<InlineItem> getInlineItems() {
            return inlineItems;
        }

        public void setInlineItems(List<InlineItem> inlineItems) {
            this.inlineItems = inlineItems;
        }
    }

    // Inner class for InlineItem
    public static class InlineItem implements Serializable {
        private String exerciseId;
        private ExerciseConfig config;

        public InlineItem() {}

        public InlineItem(String exerciseId, ExerciseConfig config) {
            this.exerciseId = exerciseId;
            this.config = config;
        }

        public String getExerciseId() {
            return exerciseId;
        }

        public void setExerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
        }

        public ExerciseConfig getConfig() {
            return config;
        }

        public void setConfig(ExerciseConfig config) {
            this.config = config;
        }
    }

    // Inner class for ExerciseConfig
    public static class ExerciseConfig implements Serializable {
        private int sets;
        private int reps;
        private int restSec;
        private String difficulty;

        public ExerciseConfig() {}

        public ExerciseConfig(int sets, int reps, int restSec, String difficulty) {
            this.sets = sets;
            this.reps = reps;
            this.restSec = restSec;
            this.difficulty = difficulty;
        }

        public int getSets() {
            return sets;
        }

        public void setSets(int sets) {
            this.sets = sets;
        }

        public int getReps() {
            return reps;
        }

        public void setReps(int reps) {
            this.reps = reps;
        }

        public int getRestSec() {
            return restSec;
        }

        public void setRestSec(int restSec) {
            this.restSec = restSec;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }
    }
}

