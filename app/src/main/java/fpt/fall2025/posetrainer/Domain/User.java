package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class User implements Serializable {
    private String uid;
    private String email;
    private String displayName;
    private String photoURL;
    private List<String> providerIds;
    private long createdAt;
    private long lastLoginAt;
    private NotificationSettings notification;
    private List<String> roles;

    public User() {}

    public User(String uid, String email, String displayName, String photoURL, 
                List<String> providerIds, long createdAt, long lastLoginAt, 
                NotificationSettings notification, List<String> roles) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.providerIds = providerIds;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.notification = notification;
        this.roles = roles;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }

    public List<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(List<String> providerIds) {
        this.providerIds = providerIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public NotificationSettings getNotification() {
        return notification;
    }

    public void setNotification(NotificationSettings notification) {
        this.notification = notification;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Inner class for notification settings
    public static class NotificationSettings implements Serializable {
        private String fcmToken;
        private boolean allowNotification;

        public NotificationSettings() {}

        public NotificationSettings(String fcmToken, boolean allowNotification) {
            this.fcmToken = fcmToken;
            this.allowNotification = allowNotification;
        }

        public String getFcmToken() {
            return fcmToken;
        }

        public void setFcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
        }

        public boolean isAllowNotification() {
            return allowNotification;
        }

        public void setAllowNotification(boolean allowNotification) {
            this.allowNotification = allowNotification;
        }
    }
}

