package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class Session implements Serializable {
    private String id;
    private String uid;
    private String workoutId;
    private long startedAt;
    private long endedAt;
    private SessionSummary summary;
    private List<PerExercise> perExercise;
    private SessionFlags flags;
    private DeviceInfo deviceInfo;
    private String appVersion;

    public Session() {}

    public Session(String id, String uid, String workoutId, long startedAt, long endedAt, 
                   SessionSummary summary, List<PerExercise> perExercise, 
                   SessionFlags flags, DeviceInfo deviceInfo, String appVersion) {
        this.id = id;
        this.uid = uid;
        this.workoutId = workoutId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.summary = summary;
        this.perExercise = perExercise;
        this.flags = flags;
        this.deviceInfo = deviceInfo;
        this.appVersion = appVersion;
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

    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(String workoutId) {
        this.workoutId = workoutId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public SessionSummary getSummary() {
        return summary;
    }

    public void setSummary(SessionSummary summary) {
        this.summary = summary;
    }

    public List<PerExercise> getPerExercise() {
        return perExercise;
    }

    public void setPerExercise(List<PerExercise> perExercise) {
        this.perExercise = perExercise;
    }

    public SessionFlags getFlags() {
        return flags;
    }

    public void setFlags(SessionFlags flags) {
        this.flags = flags;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    // Inner class for SessionSummary
    public static class SessionSummary implements Serializable {
        private int totalReps;
        private int totalSets;
        private int durationSec;
        private int estKcal;
        private double avgFormScore;

        public SessionSummary() {}

        public SessionSummary(int totalReps, int totalSets, int durationSec, 
                             int estKcal, double avgFormScore) {
            this.totalReps = totalReps;
            this.totalSets = totalSets;
            this.durationSec = durationSec;
            this.estKcal = estKcal;
            this.avgFormScore = avgFormScore;
        }

        public int getTotalReps() {
            return totalReps;
        }

        public void setTotalReps(int totalReps) {
            this.totalReps = totalReps;
        }

        public int getTotalSets() {
            return totalSets;
        }

        public void setTotalSets(int totalSets) {
            this.totalSets = totalSets;
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

        public double getAvgFormScore() {
            return avgFormScore;
        }

        public void setAvgFormScore(double avgFormScore) {
            this.avgFormScore = avgFormScore;
        }
    }

    // Inner class for PerExercise
    public static class PerExercise implements Serializable {
        private String exerciseId;
        private String difficultyUsed;
        private int cameraIssuesCount;
        private List<SetData> sets;
        private ExerciseMedia media;

        public PerExercise() {}

        public PerExercise(String exerciseId, String difficultyUsed, int cameraIssuesCount, 
                          List<SetData> sets, ExerciseMedia media) {
            this.exerciseId = exerciseId;
            this.difficultyUsed = difficultyUsed;
            this.cameraIssuesCount = cameraIssuesCount;
            this.sets = sets;
            this.media = media;
        }

        public String getExerciseId() {
            return exerciseId;
        }

        public void setExerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
        }

        public String getDifficultyUsed() {
            return difficultyUsed;
        }

        public void setDifficultyUsed(String difficultyUsed) {
            this.difficultyUsed = difficultyUsed;
        }

        public int getCameraIssuesCount() {
            return cameraIssuesCount;
        }

        public void setCameraIssuesCount(int cameraIssuesCount) {
            this.cameraIssuesCount = cameraIssuesCount;
        }

        public List<SetData> getSets() {
            return sets;
        }

        public void setSets(List<SetData> sets) {
            this.sets = sets;
        }

        public ExerciseMedia getMedia() {
            return media;
        }

        public void setMedia(ExerciseMedia media) {
            this.media = media;
        }
    }

    // Inner class for SetData
    public static class SetData implements Serializable {
        private int setNo;
        private int targetReps;
        private int actualReps;
        private int correctReps;

        public SetData() {}

        public SetData(int setNo, int targetReps, int actualReps, int correctReps) {
            this.setNo = setNo;
            this.targetReps = targetReps;
            this.actualReps = actualReps;
            this.correctReps = correctReps;
        }

        public int getSetNo() {
            return setNo;
        }

        public void setSetNo(int setNo) {
            this.setNo = setNo;
        }

        public int getTargetReps() {
            return targetReps;
        }

        public void setTargetReps(int targetReps) {
            this.targetReps = targetReps;
        }

        public int getActualReps() {
            return actualReps;
        }

        public void setActualReps(int actualReps) {
            this.actualReps = actualReps;
        }

        public int getCorrectRepsReps() {
            return correctReps;
        }

        public void setCorrectReps(int correctReps) {
            this.correctReps = correctReps;
        }
    }

    // Inner class for ExerciseMedia
    public static class ExerciseMedia implements Serializable {
        private LocalVideo localVideo;
        private LocalOverlay localOverlay;
        private Storage storage;

        public ExerciseMedia() {}

        public ExerciseMedia(LocalVideo localVideo, LocalOverlay localOverlay, Storage storage) {
            this.localVideo = localVideo;
            this.localOverlay = localOverlay;
            this.storage = storage;
        }

        public LocalVideo getLocalVideo() {
            return localVideo;
        }

        public void setLocalVideo(LocalVideo localVideo) {
            this.localVideo = localVideo;
        }

        public LocalOverlay getLocalOverlay() {
            return localOverlay;
        }

        public void setLocalOverlay(LocalOverlay localOverlay) {
            this.localOverlay = localOverlay;
        }

        public Storage getStorage() {
            return storage;
        }

        public void setStorage(Storage storage) {
            this.storage = storage;
        }
    }

    // Inner class for LocalVideo
    public static class LocalVideo implements Serializable {
        private String uri;
        private String fileName;
        private long fileSizeBytes;
        private int durationSec;
        private String mime;
        private long createdAt;

        public LocalVideo() {}

        public LocalVideo(String uri, String fileName, long fileSizeBytes, 
                         int durationSec, String mime, long createdAt) {
            this.uri = uri;
            this.fileName = fileName;
            this.fileSizeBytes = fileSizeBytes;
            this.durationSec = durationSec;
            this.mime = mime;
            this.createdAt = createdAt;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileSizeBytes() {
            return fileSizeBytes;
        }

        public void setFileSizeBytes(long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }

        public int getDurationSec() {
            return durationSec;
        }

        public void setDurationSec(int durationSec) {
            this.durationSec = durationSec;
        }

        public String getMime() {
            return mime;
        }

        public void setMime(String mime) {
            this.mime = mime;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
    }

    // Inner class for LocalOverlay
    public static class LocalOverlay implements Serializable {
        private String uri;
        private String fileName;

        public LocalOverlay() {}

        public LocalOverlay(String uri, String fileName) {
            this.uri = uri;
            this.fileName = fileName;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    // Inner class for Storage
    public static class Storage implements Serializable {
        private String location; // "local_only", "cloud", "both"

        public Storage() {}

        public Storage(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    // Inner class for SessionFlags
    public static class SessionFlags implements Serializable {
        private boolean uploaded;
        private boolean exportable;

        public SessionFlags() {}

        public SessionFlags(boolean uploaded, boolean exportable) {
            this.uploaded = uploaded;
            this.exportable = exportable;
        }

        public boolean isUploaded() {
            return uploaded;
        }

        public void setUploaded(boolean uploaded) {
            this.uploaded = uploaded;
        }

        public boolean isExportable() {
            return exportable;
        }

        public void setExportable(boolean exportable) {
            this.exportable = exportable;
        }
    }

    // Inner class for DeviceInfo
    public static class DeviceInfo implements Serializable {
        private String model;
        private String os;

        public DeviceInfo() {}

        public DeviceInfo(String model, String os) {
            this.model = model;
            this.os = os;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }
    }
}

