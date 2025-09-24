package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id;
    private String uid;
    private String type; // "reminder", "achievement", "social", etc.
    private String title;
    private String body;
    private long sentAt;
    private boolean read;

    public Notification() {}

    public Notification(String id, String uid, String type, String title, 
                       String body, long sentAt, boolean read) {
        this.id = id;
        this.uid = uid;
        this.type = type;
        this.title = title;
        this.body = body;
        this.sentAt = sentAt;
        this.read = read;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

