package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.List;

/**
 * ExerciseUser - Domain class cho custom exercises của user
 * Lưu trong collection "exerciseUser" trên Firestore
 */
@IgnoreExtraProperties
public class ExerciseUser implements Serializable {
    private String id;
    private String name;
    private String slug;
    private List<String> category;
    private List<String> muscles;
    private String level;
    private List<String> equipment;
    private List<String> tags;
    private Media media;
    private MediaPipe mediapipe;
    private DefaultConfig defaultConfig;
    private boolean isPublic;
    private long updatedAt;
    private String uid; // UID của user tạo custom exercise (bắt buộc)

    public ExerciseUser() {}

    public ExerciseUser(String id, String name, String slug, List<String> category, 
                   List<String> muscles, String level, List<String> equipment, 
                   List<String> tags, Media media, MediaPipe mediapipe, 
                   DefaultConfig defaultConfig, boolean isPublic, long updatedAt, String uid) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.category = category;
        this.muscles = muscles;
        this.level = level;
        this.equipment = equipment;
        this.tags = tags;
        this.media = media;
        this.mediapipe = mediapipe;
        this.defaultConfig = defaultConfig;
        this.isPublic = isPublic;
        this.updatedAt = updatedAt;
        this.uid = uid;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<String> getCategory() {
        return category;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public List<String> getMuscles() {
        return muscles;
    }

    public void setMuscles(List<String> muscles) {
        this.muscles = muscles;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<String> equipment) {
        this.equipment = equipment;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public MediaPipe getMediapipe() {
        return mediapipe;
    }

    public void setMediapipe(MediaPipe mediapipe) {
        this.mediapipe = mediapipe;
    }

    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // Inner class for Media
    public static class Media implements Serializable {
        private String demoVideoUrl;
        private String thumbnailUrl;

        public Media() {}

        public Media(String demoVideoUrl, String thumbnailUrl) {
            this.demoVideoUrl = demoVideoUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getDemoVideoUrl() {
            return demoVideoUrl;
        }

        public void setDemoVideoUrl(String demoVideoUrl) {
            this.demoVideoUrl = demoVideoUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    // Inner class for MediaPipe
    public static class MediaPipe implements Serializable {
        private String analyzerType;
        private String version;
        private java.util.Map<String, Object> config; // Config cho custom exercises từ Gemini

        public MediaPipe() {}

        public MediaPipe(String analyzerType, String version) {
            this.analyzerType = analyzerType;
            this.version = version;
        }

        public String getAnalyzerType() {
            return analyzerType;
        }

        public void setAnalyzerType(String analyzerType) {
            this.analyzerType = analyzerType;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public java.util.Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(java.util.Map<String, Object> config) {
            this.config = config;
        }
    }

    // Inner class for DefaultConfig
    public static class DefaultConfig implements Serializable {
        private int sets;
        private int reps;
        private int restSec;
        private String difficulty;

        public DefaultConfig() {}

        public DefaultConfig(int sets, int reps, int restSec, String difficulty) {
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

    /**
     * Convert ExerciseUser to Exercise để tương thích với các phần khác của app
     * (ví dụ: Session, WorkoutTemplate sử dụng Exercise)
     */
    public Exercise toExercise() {
        Exercise exercise = new Exercise();
        exercise.setId(this.id);
        exercise.setName(this.name);
        exercise.setSlug(this.slug);
        exercise.setCategory(this.category);
        exercise.setMuscles(this.muscles);
        exercise.setLevel(this.level);
        exercise.setEquipment(this.equipment);
        exercise.setTags(this.tags);
        exercise.setPublic(this.isPublic);
        exercise.setUpdatedAt(this.updatedAt);
        exercise.setUid(this.uid);

        // Convert Media
        if (this.media != null) {
            Exercise.Media exerciseMedia = new Exercise.Media();
            exerciseMedia.setDemoVideoUrl(this.media.getDemoVideoUrl());
            exerciseMedia.setThumbnailUrl(this.media.getThumbnailUrl());
            exercise.setMedia(exerciseMedia);
        }

        // Convert MediaPipe
        if (this.mediapipe != null) {
            Exercise.MediaPipe exerciseMediaPipe = new Exercise.MediaPipe();
            exerciseMediaPipe.setAnalyzerType(this.mediapipe.getAnalyzerType());
            exerciseMediaPipe.setVersion(this.mediapipe.getVersion());
            exerciseMediaPipe.setConfig(this.mediapipe.getConfig());
            exercise.setMediapipe(exerciseMediaPipe);
        }

        // Convert DefaultConfig
        if (this.defaultConfig != null) {
            Exercise.DefaultConfig exerciseDefaultConfig = new Exercise.DefaultConfig();
            exerciseDefaultConfig.setSets(this.defaultConfig.getSets());
            exerciseDefaultConfig.setReps(this.defaultConfig.getReps());
            exerciseDefaultConfig.setRestSec(this.defaultConfig.getRestSec());
            exerciseDefaultConfig.setDifficulty(this.defaultConfig.getDifficulty());
            exercise.setDefaultConfig(exerciseDefaultConfig);
        }

        return exercise;
    }
}

