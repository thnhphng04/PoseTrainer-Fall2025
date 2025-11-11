package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;

/**
 * Firestore POJO for collection: profiles/{uid}
 * - Dùng String cho enum-like fields để tương thích Firestore & dễ query.
 * - Lưu birthday (YYYY-MM-DD). Tuổi => tính động từ birthday (không lưu cứng).
 * - lastUpdatedAt: epoch millis.
 * 
 * Cấu trúc theo bộ câu hỏi:
 * 1. tuổi (birthday)
 * 2. Giới tính (gender)
 * 3. chiều cao (Hiện tại) (heightCm)
 * 4. cân nặng (Hiện tại) (weightKg)
 * 5. Body hiện tại (currentBodyType)
 * 6. Thời gian tập luyện mỗi ngày (dailyTrainingMinutes)
 * 7. Số ngày có thể tập luyện trong 1 tuần (weeklyGoal)
 * 8. Kinh nghiệm tập luyện của bản thân (experienceLevel)
 * 9. Mục tiêu trong tương lai (goals):
 *    - Cân nặng (targetWeightKg)
 *    - Body trong tương lai (targetBodyType)
 */
public class Profile implements Serializable {

    /* ===== Core identity & base metrics ===== */
    private String uid;               // Firebase Auth uid
    private String birthday;          // ISO-8601: "YYYY-MM-DD" (tuổi)
    private String gender;            // "male" | "female" | "other"
    private int heightCm;             // chiều cao hiện tại
    private int weightKg;             // cân nặng hiện tại

    /* ===== Body status ===== */
    // Người dùng chọn 1 trong các mẫu body (theo ảnh template theo giới tính)
    // Values: "very_lean","lean","normal","overweight","obese"
    private String currentBodyType;

    /* ===== Training info ===== */
    private int dailyTrainingMinutes;     // Thời gian tập luyện mỗi ngày
    private int weeklyGoal;               // Số ngày có thể tập luyện trong 1 tuần
    private String experienceLevel;       // "beginner"|"intermediate"|"advanced"

    /* ===== Goals (future body/weight) ===== */
    private Goals goals;

    /* ===== System fields ===== */
    private long lastUpdatedAt;           // epoch millis

    public Profile() {}

    public Profile(String uid,
                   String birthday,
                   String gender,
                   int heightCm,
                   int weightKg,
                   String currentBodyType,
                   int dailyTrainingMinutes,
                   int weeklyGoal,
                   String experienceLevel,
                   Goals goals,
                   long lastUpdatedAt) {
        this.uid = uid;
        this.birthday = birthday;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.currentBodyType = currentBodyType;
        this.dailyTrainingMinutes = dailyTrainingMinutes;
        this.weeklyGoal = weeklyGoal;
        this.experienceLevel = experienceLevel;
        this.goals = goals;
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

    public String getCurrentBodyType() { return currentBodyType; }
    public void setCurrentBodyType(String currentBodyType) { this.currentBodyType = currentBodyType; }

    public int getDailyTrainingMinutes() { return dailyTrainingMinutes; }
    public void setDailyTrainingMinutes(int dailyTrainingMinutes) { this.dailyTrainingMinutes = dailyTrainingMinutes; }

    public int getWeeklyGoal() { return weeklyGoal; }
    public void setWeeklyGoal(int weeklyGoal) { this.weeklyGoal = weeklyGoal; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public Goals getGoals() { return goals; }
    public void setGoals(Goals goals) { this.goals = goals; }

    public long getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(long lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    /* ===================== Inner types ===================== */

    /**
     * goals:
     * - targetWeightKg: cân nặng mục tiêu (từ "Mục tiêu trong tương lai: cân nặng")
     * - targetBodyType: body mục tiêu (từ "Body trong tương lai" – dùng cùng hệ "*_BodyType")
     */
    public static class Goals implements Serializable {
        private Integer targetWeightKg;
        private String targetBodyType;

        public Goals() {}

        public Goals(Integer targetWeightKg, String targetBodyType) {
            this.targetWeightKg = targetWeightKg;
            this.targetBodyType = targetBodyType;
        }

        public Integer getTargetWeightKg() { return targetWeightKg; }
        public void setTargetWeightKg(Integer targetWeightKg) { this.targetWeightKg = targetWeightKg; }

        public String getTargetBodyType() { return targetBodyType; }
        public void setTargetBodyType(String targetBodyType) { this.targetBodyType = targetBodyType; }
    }
}

