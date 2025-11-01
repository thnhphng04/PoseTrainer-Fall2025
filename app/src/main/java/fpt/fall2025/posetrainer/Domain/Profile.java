package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

/**
 * Firestore POJO for collection: profiles/{uid}
 * - Dùng String cho enum-like fields để tương thích Firestore & dễ query.
 * - Lưu birthday (YYYY-MM-DD). Tuổi => tính động từ birthday (không lưu cứng).
 * - lastUpdatedAt: epoch millis.
 */
public class Profile implements Serializable {

    /* ===== Core identity & base metrics ===== */
    private String uid;               // Firebase Auth uid
    private String birthday;          // ISO-8601: "YYYY-MM-DD" (từ câu hỏi "tuổi")
    private String gender;            // "male" | "female" | "other"
    private int heightCm;             // chiều cao hiện tại
    private int weightKg;             // cân nặng hiện tại
    private Integer bodyFatPct;       // % mỡ (optional)

    /* ===== Body status (from questionnaire) ===== */
    // Người dùng chọn 1 trong các mẫu body (theo ảnh template theo giới tính)
    // Gợi ý values: "very_lean","lean","normal","overweight","obese"
    private String currentBodyType;
    private List<String> bodyPhotos;      // nếu bạn cho phép upload/đính kèm ảnh thật (optional)

    // Thời gian tập/ngày & kinh nghiệm cá nhân
    private int dailyTrainingMinutes;     // “Thời gian tập luyện mỗi ngày”
    // Có thể lưu theo level hoặc theo số tháng. Ở đây dùng level: "beginner"|"intermediate"|"advanced"
    private String experienceLevel;

    /* ===== Goals (including future body/weight) ===== */
    private Goals goals;

    /* ===== App preferences ===== */
    private Preferences preferences;

    /* ===== System fields ===== */
    private long lastUpdatedAt;           // epoch millis

    public Profile() {}

    public Profile(String uid,
                   String birthday,
                   String gender,
                   int heightCm,
                   int weightKg,
                   Integer bodyFatPct,
                   String currentBodyType,
                   List<String> bodyPhotos,
                   int dailyTrainingMinutes,
                   String experienceLevel,
                   Goals goals,
                   Preferences preferences,
                   long lastUpdatedAt) {
        this.uid = uid;
        this.birthday = birthday;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bodyFatPct = bodyFatPct;
        this.currentBodyType = currentBodyType;
        this.bodyPhotos = bodyPhotos;
        this.dailyTrainingMinutes = dailyTrainingMinutes;
        this.experienceLevel = experienceLevel;
        this.goals = goals;
        this.preferences = preferences;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /* ===================== Getters / Setters ===================== */
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getHeightCm() { return heightCm; }
    public void setHeightCm(int heightCm) { this.heightCm = heightCm; }

    public int getWeightKg() { return weightKg; }
    public void setWeightKg(int weightKg) { this.weightKg = weightKg; }

    public Integer getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(Integer bodyFatPct) { this.bodyFatPct = bodyFatPct; }

    public String getCurrentBodyType() { return currentBodyType; }
    public void setCurrentBodyType(String currentBodyType) { this.currentBodyType = currentBodyType; }

    public List<String> getBodyPhotos() { return bodyPhotos; }
    public void setBodyPhotos(List<String> bodyPhotos) { this.bodyPhotos = bodyPhotos; }

    public int getDailyTrainingMinutes() { return dailyTrainingMinutes; }
    public void setDailyTrainingMinutes(int dailyTrainingMinutes) { this.dailyTrainingMinutes = dailyTrainingMinutes; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public Goals getGoals() { return goals; }
    public void setGoals(Goals goals) { this.goals = goals; }

    public Preferences getPreferences() { return preferences; }
    public void setPreferences(Preferences preferences) { this.preferences = preferences; }

    public long getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    /* ===================== Inner types ===================== */

    /**
     * goals:
     * - type: "lose_fat" | "gain_muscle" | "maintain" | …
     * - targetWeightKg: cân nặng mục tiêu (từ “Mục tiêu trong tương lai: cân nặng”)
     * - targetBodyType: body mục tiêu (từ “Body trong tương lai” – dùng cùng hệ “*_BodyType”)
     * - timeframeWeeks: (optional) mốc thời gian mong muốn để lập lộ trình
     */
    public static class Goals implements Serializable {
        private String type;
        private Integer targetWeightKg;
        private String targetBodyType;
        private Integer timeframeWeeks;

        public Goals() {}

        public Goals(String type, Integer targetWeightKg, String targetBodyType, Integer timeframeWeeks) {
            this.type = type;
            this.targetWeightKg = targetWeightKg;
            this.targetBodyType = targetBodyType;
            this.timeframeWeeks = timeframeWeeks;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Integer getTargetWeightKg() { return targetWeightKg; }
        public void setTargetWeightKg(Integer targetWeightKg) { this.targetWeightKg = targetWeightKg; }

        public String getTargetBodyType() { return targetBodyType; }
        public void setTargetBodyType(String targetBodyType) { this.targetBodyType = targetBodyType; }

        public Integer getTimeframeWeeks() { return timeframeWeeks; }
        public void setTimeframeWeeks(Integer timeframeWeeks) { this.timeframeWeeks = timeframeWeeks; }
    }

    /**
     * preferences: giữ nguyên nhưng đặt default hợp lý
     * - units: "metric" | "imperial"
     * - cameraMode: "front" | "back"
     */
    public static class Preferences implements Serializable {
        private String units;
        private String cameraMode;

        public Preferences() {}

        public Preferences(String units, String cameraMode) {
            this.units = units;
            this.cameraMode = cameraMode;
        }

        public String getUnits() { return units; }
        public void setUnits(String units) { this.units = units; }

        public String getCameraMode() { return cameraMode; }
        public void setCameraMode(String cameraMode) { this.cameraMode = cameraMode; }
    }
}

