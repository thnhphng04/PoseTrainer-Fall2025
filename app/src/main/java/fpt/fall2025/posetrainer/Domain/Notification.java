package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.Map;

public class Notification implements Serializable {
    private String id;
    private String uid;
    private String type; // "reminder", "achievement", "social", "ai_feedback", "ai_reminder_smart", "ai_feedback_posture", "ai_feedback_consistency", "ai_achievement", "ai_plan_update"
    private String title;
    private String body;
    private long sentAt;
    private boolean read;
    
    // Các trường mới cho AI notifications
    private boolean isAiGenerated; // Đánh dấu thông báo do AI tạo ra
    private String aiPrompt; // Prompt đã gửi cho AI (để tracking)
    private String actionType; // "open_workout", "open_exercise", "view_progress", "none" - Hành động khi tap vào
    private String actionData; // Data cho action (ví dụ: workoutId, exerciseId)
    private String feedback; // "accepted", "ignored", "dismissed" - Phản hồi của người dùng
    private Map<String, Object> metadata; // Dữ liệu bổ sung (tùy chỉnh theo nhu cầu)

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
        this.isAiGenerated = false; // Mặc định không phải AI
    }
    
    // Constructor đầy đủ cho AI notifications
    public Notification(String id, String uid, String type, String title, 
                       String body, long sentAt, boolean read, boolean isAiGenerated,
                       String aiPrompt, String actionType, String actionData) {
        this.id = id;
        this.uid = uid;
        this.type = type;
        this.title = title;
        this.body = body;
        this.sentAt = sentAt;
        this.read = read;
        this.isAiGenerated = isAiGenerated;
        this.aiPrompt = aiPrompt;
        this.actionType = actionType;
        this.actionData = actionData;
        this.feedback = "none"; // Mặc định chưa có phản hồi
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

    // Getters và Setters cho các trường mới
    public boolean isAiGenerated() {
        return isAiGenerated;
    }

    public void setAiGenerated(boolean aiGenerated) {
        isAiGenerated = aiGenerated;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public void setAiPrompt(String aiPrompt) {
        this.aiPrompt = aiPrompt;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionData() {
        return actionData;
    }

    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Helper method: Kiểm tra xem thông báo có phải do AI tạo không
     */
    public boolean isFromAI() {
        return isAiGenerated || (type != null && type.startsWith("ai_"));
    }
    
    /**
     * Helper method: Lấy icon phù hợp theo loại thông báo
     */
    public String getNotificationIcon() {
        if (type == null) return "default";
        
        switch (type) {
            case "ai_reminder_smart":
            case "workout_reminder_sent":
                return "calendar";
            case "ai_feedback_posture":
            case "ai_feedback_consistency":
                return "feedback";
            case "ai_achievement":
            case "achievement":
                return "trophy";
            case "ai_plan_update":
                return "plan";
            case "social":
                return "social";
            default:
                return "bell";
        }
    }
}

