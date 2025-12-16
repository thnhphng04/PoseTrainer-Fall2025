package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

/**
 * Feedback - Domain class cho feedback của người dùng
 * Lưu trong collection "feedbacks" trên Firestore
 * 
 * Status values:
 * - "pending": Admin chưa xác nhận
 * - "read": Admin đã đọc
 * - "resolved": Đã xử lý
 */
@IgnoreExtraProperties
public class Feedback implements Serializable {
    private String id;
    private String uid; // UID của người dùng gửi feedback
    private String type; // "exercise", "app", hoặc "post"
    private String exerciseId; // ID của bài tập (nếu type = "exercise")
    private String exerciseName; // Tên bài tập (để hiển thị, không cần query lại)
    private String postId; // ID của bài viết (nếu type = "post")
    private String postContent; // Nội dung bài viết (để hiển thị, không cần query lại)
    private String content; // Nội dung feedback
    private String status; // "pending", "read", "resolved"
    private long createdAt; // Timestamp khi tạo feedback
    private long updatedAt; // Timestamp khi cập nhật (khi admin xử lý)

    public Feedback() {}

    public Feedback(String id, String uid, String type, String exerciseId, String exerciseName, 
                   String postId, String postContent, String content, String status, long createdAt, long updatedAt) {
        this.id = id;
        this.uid = uid;
        this.type = type;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.postId = postId;
        this.postContent = postContent;
        this.content = content;
        this.status = status;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }
}

