package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class DeviceSync implements Serializable {
    private String id;
    private String uid;
    private String deviceId;
    private long lastSyncAt;
    private String dataHash;
    private List<String> scopes; // "sessions", "workouts", "nutrition", etc.

    public DeviceSync() {}

    public DeviceSync(String id, String uid, String deviceId, long lastSyncAt, 
                     String dataHash, List<String> scopes) {
        this.id = id;
        this.uid = uid;
        this.deviceId = deviceId;
        this.lastSyncAt = lastSyncAt;
        this.dataHash = dataHash;
        this.scopes = scopes;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(long lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getDataHash() {
        return dataHash;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}

