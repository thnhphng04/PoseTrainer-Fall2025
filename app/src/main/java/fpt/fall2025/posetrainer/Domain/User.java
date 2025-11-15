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
        private String fcmToken; // Token FCM của thiết bị để gửi push notification
        private boolean allowNotification; // Bật/tắt nhận thông báo
        
        // Các trường mới cho AI notifications
        private boolean enableAiNotifications; // Bật/tắt thông báo từ AI
        private List<String> preferredTimes; // Giờ mong muốn nhận thông báo (ví dụ: ["06:30", "18:00"])
        private List<String> preferredType; // Loại thông báo yêu thích (ví dụ: ["ai_feedback", "achievement", "ai_reminder_smart"])
        private String language; // Ngôn ngữ cho AI sinh nội dung ("vi", "en")
        private boolean allowMotivationalMessages; // Cho phép AI gửi tin nhắn động viên
        private int maxNotificationsPerDay; // Giới hạn số thông báo mỗi ngày (mặc định 30)

        public NotificationSettings() {
            // Khởi tạo giá trị mặc định
            this.allowNotification = true;
            this.enableAiNotifications = true;
            this.language = "vi"; // Mặc định tiếng Việt
            this.allowMotivationalMessages = true;
            this.maxNotificationsPerDay = 30;
        }

        public NotificationSettings(String fcmToken, boolean allowNotification) {
            this.fcmToken = fcmToken;
            this.allowNotification = allowNotification;
            this.enableAiNotifications = true;
            this.language = "vi";
            this.allowMotivationalMessages = true;
            this.maxNotificationsPerDay = 30;
        }

        // Getters và Setters
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

        public boolean isEnableAiNotifications() {
            return enableAiNotifications;
        }

        public void setEnableAiNotifications(boolean enableAiNotifications) {
            this.enableAiNotifications = enableAiNotifications;
        }

        public List<String> getPreferredTimes() {
            return preferredTimes;
        }

        public void setPreferredTimes(List<String> preferredTimes) {
            this.preferredTimes = preferredTimes;
        }

        public List<String> getPreferredType() {
            return preferredType;
        }

        public void setPreferredType(List<String> preferredType) {
            this.preferredType = preferredType;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isAllowMotivationalMessages() {
            return allowMotivationalMessages;
        }

        public void setAllowMotivationalMessages(boolean allowMotivationalMessages) {
            this.allowMotivationalMessages = allowMotivationalMessages;
        }

        public int getMaxNotificationsPerDay() {
            return maxNotificationsPerDay;
        }

        public void setMaxNotificationsPerDay(int maxNotificationsPerDay) {
            this.maxNotificationsPerDay = maxNotificationsPerDay;
        }
    }
}

