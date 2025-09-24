package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class UserWorkout implements Serializable {
    private String id;
    private String uid;
    private String title;
    private String description;
    private String source; // "custom", "template", "ai"
    private List<UserWorkoutItem> items;
    private long createdAt;
    private long updatedAt;

    public UserWorkout() {}

    public UserWorkout(String id, String uid, String title, String description, 
                      String source, List<UserWorkoutItem> items, 
                      long createdAt, long updatedAt) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.description = description;
        this.source = source;
        this.items = items;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<UserWorkoutItem> getItems() {
        return items;
    }

    public void setItems(List<UserWorkoutItem> items) {
        this.items = items;
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

    // Inner class for UserWorkoutItem
    public static class UserWorkoutItem implements Serializable {
        private int order;
        private String exerciseId;
        private ExerciseConfig config;

        public UserWorkoutItem() {}

        public UserWorkoutItem(int order, String exerciseId, ExerciseConfig config) {
            this.order = order;
            this.exerciseId = exerciseId;
            this.config = config;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
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

