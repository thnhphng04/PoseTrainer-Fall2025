package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Achievement implements Serializable {
    private String uid;
    private Map<String, Boolean> badges; // Key: badge ID, Value: unlocked status
    private Map<String, Long> unlockedAt; // Key: badge ID, Value: timestamp when unlocked

    public Achievement() {
        this.badges = new HashMap<>();
        this.unlockedAt = new HashMap<>();
    }

    public Achievement(String uid, Map<String, Boolean> badges, Map<String, Long> unlockedAt) {
        this.uid = uid;
        this.badges = badges != null ? badges : new HashMap<>();
        this.unlockedAt = unlockedAt != null ? unlockedAt : new HashMap<>();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Map<String, Boolean> getBadges() {
        return badges;
    }

    public void setBadges(Map<String, Boolean> badges) {
        this.badges = badges != null ? badges : new HashMap<>();
    }

    public Map<String, Long> getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(Map<String, Long> unlockedAt) {
        this.unlockedAt = unlockedAt != null ? unlockedAt : new HashMap<>();
    }

    /**
     * Check if a badge is unlocked
     */
    public boolean isBadgeUnlocked(String badgeKey) {
        return badges != null && badges.containsKey(badgeKey) && Boolean.TRUE.equals(badges.get(badgeKey));
    }

    /**
     * Unlock a badge
     */
    public void unlockBadge(String badgeKey) {
        if (badges == null) {
            badges = new HashMap<>();
        }
        if (unlockedAt == null) {
            unlockedAt = new HashMap<>();
        }
        badges.put(badgeKey, true);
        unlockedAt.put(badgeKey, System.currentTimeMillis() / 1000); // Store in seconds
    }
}

