package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session implements Serializable {
    private String id;
    private String uid;
    private String title;
    private String description;
    private long startedAt;
    private long endedAt;
    private SessionSummary summary;
    private List<PerExercise> perExercise;
    private SessionFlags flags;
    private DeviceInfo deviceInfo;
    private String appVersion;

    public Session() {}

    public Session(String id, String uid, String title, String description, long startedAt, long endedAt, 
                   SessionSummary summary, List<PerExercise> perExercise, 
                   SessionFlags flags, DeviceInfo deviceInfo, String appVersion) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.description = description;
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
        private int durationSec;
        private int estKcal;

        public SessionSummary() {}

        public SessionSummary(int durationSec, int estKcal) {
            this.durationSec = durationSec;
            this.estKcal = estKcal;
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
    }

    // Inner class for PerExercise
    public static class PerExercise implements Serializable {
        private int exerciseNo;
        private String exerciseId;
        private String difficultyUsed;
        private String state; // "not_started", "doing", "completed"

        private List<SetData> sets;
        private ExerciseMedia media;

        public PerExercise() {}

        public PerExercise(int exerciseNo, String exerciseId, String difficultyUsed, String state,
                          List<SetData> sets, ExerciseMedia media) {
            this.exerciseNo = exerciseNo;
            this.exerciseId = exerciseId;
            this.difficultyUsed = difficultyUsed;
            this.state = state;
            this.sets = sets;
            this.media = media;
        }

        public int getExerciseNo() {
            return exerciseNo;
        }

        public void setExerciseNo(int exerciseNo) {
            this.exerciseNo = exerciseNo;
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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
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
        private int correctReps;
        private String state; // "incomplete", "completed", "skipped"
        // Note: Using Map<String, Integer> for Java compatibility, but Kotlin will use Map<String, Int>
        private Map<String, Integer> errorCounts; // Map lưu số lần mỗi lỗi xuất hiện: "Chân bị gập" -> 5

        public SetData() {}

        public SetData(int setNo, int targetReps, int correctReps, String state) {
            this.setNo = setNo;
            this.targetReps = targetReps;
            this.correctReps = correctReps;
            this.state = state;
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

        public int getCorrectReps() {
            return correctReps;
        }

        public void setCorrectReps(int correctReps) {
            this.correctReps = correctReps;
        }


        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Map<String, Integer> getErrorCounts() {
            return errorCounts;
        }

        public void setErrorCounts(Map<String, Integer> errorCounts) {
            this.errorCounts = errorCounts;
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

