package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class WorkoutTemplate implements Serializable {
    private String id;
    private String title;
    private String description;
    private String level;
    private List<String> focus;
    private String goalFit;
    private List<WorkoutItem> items;
    private int estDurationMin;
    private boolean isPublic;
    private String createdBy;
    private int version;
    private long updatedAt;

    public WorkoutTemplate() {}

    public WorkoutTemplate(String id, String title, String description, String level, 
                          List<String> focus, String goalFit, List<WorkoutItem> items, 
                          int estDurationMin, boolean isPublic,
                          String createdBy, int version, long updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
        this.focus = focus;
        this.goalFit = goalFit;
        this.items = items;
        this.estDurationMin = estDurationMin;
        this.isPublic = isPublic;
        this.createdBy = createdBy;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getFocus() {
        return focus;
    }

    public void setFocus(List<String> focus) {
        this.focus = focus;
    }

    public String getGoalFit() {
        return goalFit;
    }

    public void setGoalFit(String goalFit) {
        this.goalFit = goalFit;
    }

    public List<WorkoutItem> getItems() {
        return items;
    }

    public void setItems(List<WorkoutItem> items) {
        this.items = items;
    }

    public int getEstDurationMin() {
        return estDurationMin;
    }

    public void setEstDurationMin(int estDurationMin) {
        this.estDurationMin = estDurationMin;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Inner class for WorkoutItem
    public static class WorkoutItem implements Serializable {
        private int order;
        private String exerciseId;
        private ExerciseConfig configOverride;

        public WorkoutItem() {}

        public WorkoutItem(int order, String exerciseId, ExerciseConfig configOverride) {
            this.order = order;
            this.exerciseId = exerciseId;
            this.configOverride = configOverride;
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

        public ExerciseConfig getConfigOverride() {
            return configOverride;
        }

        public void setConfigOverride(ExerciseConfig configOverride) {
            this.configOverride = configOverride;
        }
    }

    // Inner class for ExerciseConfig
    public static class ExerciseConfig implements Serializable {
        private int sets;
        private int reps;
        private int restSec;

        public ExerciseConfig() {}

        public ExerciseConfig(int sets, int reps, int restSec) {
            this.sets = sets;
            this.reps = reps;
            this.restSec = restSec;
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
    }
}

