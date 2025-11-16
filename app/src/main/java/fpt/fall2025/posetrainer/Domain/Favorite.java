package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

/**
 * Domain class để lưu thông tin workout template yêu thích của user
 * Lưu trong Firestore tại: users/{userId}/favorites/{workoutTemplateId}
 */
@IgnoreExtraProperties
public class Favorite implements Serializable {
    private String id; // ID của document (workoutTemplateId)
    private String workoutTemplateId; // ID của workout template được yêu thích
    private String userId; // ID của user (có thể lấy từ path hoặc lưu trong document)
    private long addedAt; // Timestamp (seconds) khi được thêm vào yêu thích

    // Constructor mặc định (required cho Firestore)
    public Favorite() {}

    // Constructor với các tham số cần thiết
    public Favorite(String workoutTemplateId, String userId, long addedAt) {
        this.id = workoutTemplateId; // ID của document = workoutTemplateId
        this.workoutTemplateId = workoutTemplateId;
        this.userId = userId;
        this.addedAt = addedAt;
    }

    // Constructor đầy đủ
    public Favorite(String id, String workoutTemplateId, String userId, long addedAt) {
        this.id = id;
        this.workoutTemplateId = workoutTemplateId;
        this.userId = userId;
        this.addedAt = addedAt;
    }

    // Getters and Setters

    /**
     * Lấy ID của document (cũng chính là workoutTemplateId)
     */
    public String getId() {
        return id;
    }

    /**
     * Đặt ID của document
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Lấy ID của workout template được yêu thích
     */
    public String getWorkoutTemplateId() {
        return workoutTemplateId;
    }

    /**
     * Đặt ID của workout template được yêu thích
     */
    public void setWorkoutTemplateId(String workoutTemplateId) {
        this.workoutTemplateId = workoutTemplateId;
    }

    /**
     * Lấy ID của user sở hữu favorite này
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Đặt ID của user sở hữu favorite này
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Lấy timestamp (seconds) khi workout template được thêm vào yêu thích
     */
    public long getAddedAt() {
        return addedAt;
    }

    /**
     * Đặt timestamp (seconds) khi workout template được thêm vào yêu thích
     */
    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public String toString() {
        return "Favorite{" +
                "id='" + id + '\'' +
                ", workoutTemplateId='" + workoutTemplateId + '\'' +
                ", userId='" + userId + '\'' +
                ", addedAt=" + addedAt +
                '}';
    }
}

