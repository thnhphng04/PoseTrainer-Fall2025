package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class Collection implements Serializable {
    private String id;
    private String title;
    private String description;
    private String category;
    private List<String> workoutTemplateIds;
    private String thumbnailUrl;
    private boolean isPublic;
    private String createdBy;
    private int order;
    private List<String> tags;
    private long createdAt;
    private long updatedAt;

    public Collection() {}

    public Collection(String id, String title, String description, String category,
                     List<String> workoutTemplateIds, String thumbnailUrl, boolean isPublic,
                     String createdBy, int order, List<String> tags, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.workoutTemplateIds = workoutTemplateIds;
        this.thumbnailUrl = thumbnailUrl;
        this.isPublic = isPublic;
        this.createdBy = createdBy;
        this.order = order;
        this.tags = tags;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getWorkoutTemplateIds() {
        return workoutTemplateIds;
    }

    public void setWorkoutTemplateIds(List<String> workoutTemplateIds) {
        this.workoutTemplateIds = workoutTemplateIds;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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
}

