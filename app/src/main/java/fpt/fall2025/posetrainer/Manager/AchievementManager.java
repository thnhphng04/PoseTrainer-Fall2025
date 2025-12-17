package fpt.fall2025.posetrainer.Manager;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fpt.fall2025.posetrainer.Domain.Achievement;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.DAL.StreakDAO;

/**
 * Manager để kiểm tra và unlock achievements
 */
public class AchievementManager {
    private static final String TAG = "AchievementManager";
    private static AchievementManager instance;
    private FirebaseFirestore db;
    private StreakDAO streakDAO;

    // Achievement definitions
    public static class AchievementInfo {
        public String key;
        public String name;
        public String description;
        public String emoji;
        public int drawableResId; // 0 if using emoji

        public AchievementInfo(String key, String name, String description, String emoji, int drawableResId) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.emoji = emoji;
            this.drawableResId = drawableResId;
        }
    }

    private static final Map<String, AchievementInfo> ACHIEVEMENT_DEFINITIONS = new HashMap<>();

    static {
        // Streak achievements
        ACHIEVEMENT_DEFINITIONS.put("streak_3", new AchievementInfo(
            "streak_3", "3 Ngày Liên Tiếp", "Tập luyện 3 ngày liên tiếp", "🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("streak_7", new AchievementInfo(
            "streak_7", "1 Tuần Kiên Trì", "Tập luyện 7 ngày liên tiếp", "🔥🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("streak_14", new AchievementInfo(
            "streak_14", "2 Tuần Xuất Sắc", "Tập luyện 14 ngày liên tiếp", "🔥🔥🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("streak_30", new AchievementInfo(
            "streak_30", "1 Tháng Bất Bại", "Tập luyện 30 ngày liên tiếp", "🔥🔥🔥🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("streak_60", new AchievementInfo(
            "streak_60", "2 Tháng Kiên Cường", "Tập luyện 60 ngày liên tiếp", "🔥🔥🔥🔥🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("streak_100", new AchievementInfo(
            "streak_100", "100 Ngày Huyền Thoại", "Tập luyện 100 ngày liên tiếp", "💎", 0
        ));

        // Workout count achievements
        ACHIEVEMENT_DEFINITIONS.put("workout_1", new AchievementInfo(
            "workout_1", "Bắt Đầu Hành Trình", "Hoàn thành buổi tập đầu tiên", "🎯", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_10", new AchievementInfo(
            "workout_10", "10 Buổi Tập", "Hoàn thành 10 buổi tập", "⭐", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_30", new AchievementInfo(
            "workout_30", "30 Buổi Tập", "Hoàn thành 30 buổi tập", "🏆", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_50", new AchievementInfo(
            "workout_50", "50 Buổi Tập", "Hoàn thành 50 buổi tập", "🌟", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_100", new AchievementInfo(
            "workout_100", "100 Buổi Tập", "Hoàn thành 100 buổi tập", "💪", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_200", new AchievementInfo(
            "workout_200", "200 Buổi Tập", "Hoàn thành 200 buổi tập", "👑", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("workout_500", new AchievementInfo(
            "workout_500", "500 Buổi Tập", "Hoàn thành 500 buổi tập", "🏅", 0
        ));

        // Duration achievements (tổng thời gian tập luyện)
        ACHIEVEMENT_DEFINITIONS.put("duration_1h", new AchievementInfo(
            "duration_1h", "1 Giờ Tập Luyện", "Tổng cộng 1 giờ tập luyện", "⏱️", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("duration_10h", new AchievementInfo(
            "duration_10h", "10 Giờ Tập Luyện", "Tổng cộng 10 giờ tập luyện", "⏰", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("duration_50h", new AchievementInfo(
            "duration_50h", "50 Giờ Tập Luyện", "Tổng cộng 50 giờ tập luyện", "⌛", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("duration_100h", new AchievementInfo(
            "duration_100h", "100 Giờ Tập Luyện", "Tổng cộng 100 giờ tập luyện", "⏳", 0
        ));

        // Calories achievements
        ACHIEVEMENT_DEFINITIONS.put("calories_1000", new AchievementInfo(
            "calories_1000", "1000 Calo Đốt Cháy", "Đốt cháy tổng cộng 1000 calo", "🔥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("calories_5000", new AchievementInfo(
            "calories_5000", "5000 Calo Đốt Cháy", "Đốt cháy tổng cộng 5000 calo", "💥", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("calories_10000", new AchievementInfo(
            "calories_10000", "10000 Calo Đốt Cháy", "Đốt cháy tổng cộng 10000 calo", "⚡", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("calories_50000", new AchievementInfo(
            "calories_50000", "50000 Calo Đốt Cháy", "Đốt cháy tổng cộng 50000 calo", "🌋", 0
        ));

        // Weekly achievements (tập đủ số tuần)
        ACHIEVEMENT_DEFINITIONS.put("week_1", new AchievementInfo(
            "week_1", "1 Tuần Đều Đặn", "Tập luyện đều đặn trong 1 tuần", "📅", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("week_4", new AchievementInfo(
            "week_4", "1 Tháng Đều Đặn", "Tập luyện đều đặn trong 4 tuần", "📆", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("week_12", new AchievementInfo(
            "week_12", "3 Tháng Đều Đặn", "Tập luyện đều đặn trong 12 tuần", "🗓️", 0
        ));

        // Special achievements
        ACHIEVEMENT_DEFINITIONS.put("early_bird", new AchievementInfo(
            "early_bird", "Chim Sớm", "Tập luyện vào buổi sáng sớm (5-8h)", "🌅", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("night_owl", new AchievementInfo(
            "night_owl", "Cú Đêm", "Tập luyện vào buổi tối muộn (21-24h)", "🦉", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("weekend_warrior", new AchievementInfo(
            "weekend_warrior", "Chiến Binh Cuối Tuần", "Tập luyện vào cuối tuần", "⚔️", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("perfectionist", new AchievementInfo(
            "perfectionist", "Người Hoàn Hảo", "Hoàn thành 10 buổi tập với độ chính xác 100%", "✨", 0
        ));
        ACHIEVEMENT_DEFINITIONS.put("marathon", new AchievementInfo(
            "marathon", "Marathon", "Hoàn thành một buổi tập dài hơn 60 phút", "🏃", 0
        ));
    }

    private AchievementManager() {
        db = FirebaseFirestore.getInstance();
        this.streakDAO = new StreakDAO();
    }

    public static synchronized AchievementManager getInstance() {
        if (instance == null) {
            instance = new AchievementManager();
        }
        return instance;
    }

    /**
     * Get achievement info by key
     */
    public AchievementInfo getAchievementInfo(String badgeKey) {
        return ACHIEVEMENT_DEFINITIONS.get(badgeKey);
    }

    /**
     * Check achievements and unlock new ones
     */
    public void checkAchievements(String uid, Session session, OnAchievementCheckedListener listener) {
        Log.d(TAG, "Checking achievements for user: " + uid);

        // Load current streak
        streakDAO.loadUserStreak(uid, streak -> {
            int currentStreak = (streak != null) ? streak.getCurrentStreak() : 0;

            // Load total workout count, duration, calories
            db.collection("sessions")
                    .whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int totalWorkouts = 0;
                        int totalDurationSec = 0;
                        int totalCalories = 0;
                        int perfectWorkouts = 0; // Workouts with 100% accuracy
                        int longWorkouts = 0; // Workouts > 60 minutes
                        boolean hasEarlyBird = false;
                        boolean hasNightOwl = false;
                        boolean hasWeekendWarrior = false;
                        Set<Long> weeksWithWorkouts = new HashSet<>();

                        // Calculate stats from all sessions
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Session sessionDoc = document.toObject(Session.class);
                            if (sessionDoc == null || sessionDoc.getEndedAt() <= 0) continue;

                            totalWorkouts++;

                            // Calculate duration
                            int durationSec = 0;
                            if (sessionDoc.getSummary() != null && sessionDoc.getSummary().getDurationSec() > 0) {
                                durationSec = sessionDoc.getSummary().getDurationSec();
                            } else if (sessionDoc.getEndedAt() > 0 && sessionDoc.getStartedAt() > 0) {
                                durationSec = (int) (sessionDoc.getEndedAt() - sessionDoc.getStartedAt());
                            }
                            totalDurationSec += durationSec;

                            // Calculate calories
                            if (sessionDoc.getSummary() != null && sessionDoc.getSummary().getEstKcal() > 0) {
                                totalCalories += sessionDoc.getSummary().getEstKcal();
                            }

                            // Check for long workouts (> 60 minutes = 3600 seconds)
                            if (durationSec >= 3600) {
                                longWorkouts++;
                            }

                            // Check for perfect workouts (100% accuracy)
                            if (sessionDoc.getPerExercise() != null && !sessionDoc.getPerExercise().isEmpty()) {
                                boolean isPerfect = true;
                                for (Session.PerExercise perEx : sessionDoc.getPerExercise()) {
                                    if (perEx.getSets() != null) {
                                        for (Session.SetData set : perEx.getSets()) {
                                            if (set.getTargetReps() > 0 && set.getCorrectReps() < set.getTargetReps()) {
                                                isPerfect = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (!isPerfect) break;
                                }
                                if (isPerfect) {
                                    perfectWorkouts++;
                                }
                            }

                            // Check time-based achievements
                            if (sessionDoc.getStartedAt() > 0) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(sessionDoc.getStartedAt() * 1000L);
                                int hour = cal.get(Calendar.HOUR_OF_DAY);
                                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                                // Early bird: 5-8 AM
                                if (hour >= 5 && hour < 8) {
                                    hasEarlyBird = true;
                                }

                                // Night owl: 21-24 (9 PM - 12 AM)
                                if (hour >= 21 || hour < 1) {
                                    hasNightOwl = true;
                                }

                                // Weekend warrior: Saturday (7) or Sunday (1)
                                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                                    hasWeekendWarrior = true;
                                }

                                // Track weeks with workouts
                                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                long weekStart = cal.getTimeInMillis() / 1000;
                                weeksWithWorkouts.add(weekStart);
                            }
                        }

                        // Create final variables for use in lambda
                        final int finalTotalWorkouts = totalWorkouts;
                        final int finalTotalDurationSec = totalDurationSec;
                        final int finalTotalCalories = totalCalories;
                        final int finalPerfectWorkouts = perfectWorkouts;
                        final int finalLongWorkouts = longWorkouts;
                        final boolean finalHasEarlyBird = hasEarlyBird;
                        final boolean finalHasNightOwl = hasNightOwl;
                        final boolean finalHasWeekendWarrior = hasWeekendWarrior;
                        final Set<Long> finalWeeksWithWorkouts = new HashSet<>(weeksWithWorkouts);

                        // Load current achievements
                        db.collection("achievements")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    Achievement achievement;
                                    if (documentSnapshot.exists()) {
                                        achievement = documentSnapshot.toObject(Achievement.class);
                                        if (achievement == null) {
                                            achievement = new Achievement(uid, new HashMap<>(), new HashMap<>());
                                        }
                                    } else {
                                        achievement = new Achievement(uid, new HashMap<>(), new HashMap<>());
                                    }

                                    List<String> newlyUnlocked = new ArrayList<>();

                                    // Check streak achievements
                                    if (currentStreak >= 3 && !achievement.isBadgeUnlocked("streak_3")) {
                                        achievement.unlockBadge("streak_3");
                                        newlyUnlocked.add("streak_3");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 3 Ngày Liên Tiếp (streak_3)");
                                    }
                                    if (currentStreak >= 7 && !achievement.isBadgeUnlocked("streak_7")) {
                                        achievement.unlockBadge("streak_7");
                                        newlyUnlocked.add("streak_7");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1 Tuần Kiên Trì (streak_7)");
                                    }
                                    if (currentStreak >= 14 && !achievement.isBadgeUnlocked("streak_14")) {
                                        achievement.unlockBadge("streak_14");
                                        newlyUnlocked.add("streak_14");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 2 Tuần Xuất Sắc (streak_14)");
                                    }
                                    if (currentStreak >= 30 && !achievement.isBadgeUnlocked("streak_30")) {
                                        achievement.unlockBadge("streak_30");
                                        newlyUnlocked.add("streak_30");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1 Tháng Bất Bại (streak_30)");
                                    }
                                    if (currentStreak >= 60 && !achievement.isBadgeUnlocked("streak_60")) {
                                        achievement.unlockBadge("streak_60");
                                        newlyUnlocked.add("streak_60");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 2 Tháng Kiên Cường (streak_60)");
                                    }
                                    if (currentStreak >= 100 && !achievement.isBadgeUnlocked("streak_100")) {
                                        achievement.unlockBadge("streak_100");
                                        newlyUnlocked.add("streak_100");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 100 Ngày Huyền Thoại (streak_100)");
                                    }

                                    // Check workout count achievements
                                    if (finalTotalWorkouts >= 1 && !achievement.isBadgeUnlocked("workout_1")) {
                                        achievement.unlockBadge("workout_1");
                                        newlyUnlocked.add("workout_1");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Bắt Đầu Hành Trình (workout_1)");
                                    }
                                    if (finalTotalWorkouts >= 10 && !achievement.isBadgeUnlocked("workout_10")) {
                                        achievement.unlockBadge("workout_10");
                                        newlyUnlocked.add("workout_10");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 10 Buổi Tập (workout_10)");
                                    }
                                    if (finalTotalWorkouts >= 30 && !achievement.isBadgeUnlocked("workout_30")) {
                                        achievement.unlockBadge("workout_30");
                                        newlyUnlocked.add("workout_30");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 30 Buổi Tập (workout_30)");
                                    }
                                    if (finalTotalWorkouts >= 50 && !achievement.isBadgeUnlocked("workout_50")) {
                                        achievement.unlockBadge("workout_50");
                                        newlyUnlocked.add("workout_50");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 50 Buổi Tập (workout_50)");
                                    }
                                    if (finalTotalWorkouts >= 100 && !achievement.isBadgeUnlocked("workout_100")) {
                                        achievement.unlockBadge("workout_100");
                                        newlyUnlocked.add("workout_100");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 100 Buổi Tập (workout_100)");
                                    }
                                    if (finalTotalWorkouts >= 200 && !achievement.isBadgeUnlocked("workout_200")) {
                                        achievement.unlockBadge("workout_200");
                                        newlyUnlocked.add("workout_200");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 200 Buổi Tập (workout_200)");
                                    }
                                    if (finalTotalWorkouts >= 500 && !achievement.isBadgeUnlocked("workout_500")) {
                                        achievement.unlockBadge("workout_500");
                                        newlyUnlocked.add("workout_500");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 500 Buổi Tập (workout_500)");
                                    }

                                    // Check duration achievements (1h = 3600s, 10h = 36000s, 50h = 180000s, 100h = 360000s)
                                    if (finalTotalDurationSec >= 3600 && !achievement.isBadgeUnlocked("duration_1h")) {
                                        achievement.unlockBadge("duration_1h");
                                        newlyUnlocked.add("duration_1h");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1 Giờ Tập Luyện (duration_1h)");
                                    }
                                    if (finalTotalDurationSec >= 36000 && !achievement.isBadgeUnlocked("duration_10h")) {
                                        achievement.unlockBadge("duration_10h");
                                        newlyUnlocked.add("duration_10h");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 10 Giờ Tập Luyện (duration_10h)");
                                    }
                                    if (finalTotalDurationSec >= 180000 && !achievement.isBadgeUnlocked("duration_50h")) {
                                        achievement.unlockBadge("duration_50h");
                                        newlyUnlocked.add("duration_50h");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 50 Giờ Tập Luyện (duration_50h)");
                                    }
                                    if (finalTotalDurationSec >= 360000 && !achievement.isBadgeUnlocked("duration_100h")) {
                                        achievement.unlockBadge("duration_100h");
                                        newlyUnlocked.add("duration_100h");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 100 Giờ Tập Luyện (duration_100h)");
                                    }

                                    // Check calories achievements
                                    if (finalTotalCalories >= 1000 && !achievement.isBadgeUnlocked("calories_1000")) {
                                        achievement.unlockBadge("calories_1000");
                                        newlyUnlocked.add("calories_1000");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1000 Calo Đốt Cháy (calories_1000)");
                                    }
                                    if (finalTotalCalories >= 5000 && !achievement.isBadgeUnlocked("calories_5000")) {
                                        achievement.unlockBadge("calories_5000");
                                        newlyUnlocked.add("calories_5000");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 5000 Calo Đốt Cháy (calories_5000)");
                                    }
                                    if (finalTotalCalories >= 10000 && !achievement.isBadgeUnlocked("calories_10000")) {
                                        achievement.unlockBadge("calories_10000");
                                        newlyUnlocked.add("calories_10000");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 10000 Calo Đốt Cháy (calories_10000)");
                                    }
                                    if (finalTotalCalories >= 50000 && !achievement.isBadgeUnlocked("calories_50000")) {
                                        achievement.unlockBadge("calories_50000");
                                        newlyUnlocked.add("calories_50000");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 50000 Calo Đốt Cháy (calories_50000)");
                                    }

                                    // Check weekly achievements
                                    if (finalWeeksWithWorkouts.size() >= 1 && !achievement.isBadgeUnlocked("week_1")) {
                                        achievement.unlockBadge("week_1");
                                        newlyUnlocked.add("week_1");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1 Tuần Đều Đặn (week_1)");
                                    }
                                    if (finalWeeksWithWorkouts.size() >= 4 && !achievement.isBadgeUnlocked("week_4")) {
                                        achievement.unlockBadge("week_4");
                                        newlyUnlocked.add("week_4");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 1 Tháng Đều Đặn (week_4)");
                                    }
                                    if (finalWeeksWithWorkouts.size() >= 12 && !achievement.isBadgeUnlocked("week_12")) {
                                        achievement.unlockBadge("week_12");
                                        newlyUnlocked.add("week_12");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 3 Tháng Đều Đặn (week_12)");
                                    }

                                    // Check special achievements
                                    if (finalHasEarlyBird && !achievement.isBadgeUnlocked("early_bird")) {
                                        achievement.unlockBadge("early_bird");
                                        newlyUnlocked.add("early_bird");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Chim Sớm (early_bird)");
                                    }
                                    if (finalHasNightOwl && !achievement.isBadgeUnlocked("night_owl")) {
                                        achievement.unlockBadge("night_owl");
                                        newlyUnlocked.add("night_owl");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Cú Đêm (night_owl)");
                                    }
                                    if (finalHasWeekendWarrior && !achievement.isBadgeUnlocked("weekend_warrior")) {
                                        achievement.unlockBadge("weekend_warrior");
                                        newlyUnlocked.add("weekend_warrior");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Chiến Binh Cuối Tuần (weekend_warrior)");
                                    }
                                    if (finalPerfectWorkouts >= 10 && !achievement.isBadgeUnlocked("perfectionist")) {
                                        achievement.unlockBadge("perfectionist");
                                        newlyUnlocked.add("perfectionist");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Người Hoàn Hảo (perfectionist)");
                                    }
                                    if (finalLongWorkouts >= 1 && !achievement.isBadgeUnlocked("marathon")) {
                                        achievement.unlockBadge("marathon");
                                        newlyUnlocked.add("marathon");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Marathon (marathon)");
                                    }

                                    // Đảm bảo uid được set đúng (quan trọng cho Firestore rules)
                                    if (achievement.getUid() == null || achievement.getUid().isEmpty()) {
                                        achievement.setUid(uid);
                                        Log.d(TAG, "🔧 Set achievement UID thành: " + uid);
                                    } else if (!achievement.getUid().equals(uid)) {
                                        Log.w(TAG, "⚠️ Achievement UID không khớp: " + achievement.getUid() + " != " + uid + ", đang cập nhật...");
                                        achievement.setUid(uid);
                                    }

                                    // Tạo biến final để sử dụng trong lambda
                                    final Achievement finalAchievement = achievement;
                                    final List<String> finalNewlyUnlocked = newlyUnlocked;

                                    // Save updated achievements
                                    if (!newlyUnlocked.isEmpty() || !documentSnapshot.exists()) {
                                        db.collection("achievements")
                                                .document(uid)
                                                .set(finalAchievement)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "✅ Lưu achievements thành công. Mới mở khóa: " + finalNewlyUnlocked.size() + " achievement(s), uid=" + finalAchievement.getUid());
                                                    if (listener != null) {
                                                        listener.onAchievementChecked(finalNewlyUnlocked);
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "❌ Lỗi khi lưu achievements", e);
                                                    Log.e(TAG, "📋 Mã lỗi: " + (e instanceof com.google.firebase.firestore.FirebaseFirestoreException 
                                                        ? ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() 
                                                        : "Không xác định"));
                                                    Log.e(TAG, "📋 Chi tiết lỗi: " + e.getMessage());
                                                    if (listener != null) {
                                                        listener.onAchievementChecked(finalNewlyUnlocked);
                                                    }
                                                });
                                    } else {
                                        // No new achievements
                                        Log.d(TAG, "ℹ️ Không có achievement mới nào được mở khóa");
                                        if (listener != null) {
                                            listener.onAchievementChecked(newlyUnlocked);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Lỗi khi tải achievements", e);
                                    if (listener != null) {
                                        listener.onAchievementChecked(new ArrayList<>());
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Lỗi khi tải số lượng workout", e);
                        if (listener != null) {
                            listener.onAchievementChecked(new ArrayList<>());
                        }
                    });
        });
    }

    public interface OnAchievementCheckedListener {
        void onAchievementChecked(List<String> newlyUnlockedBadges);
    }
}

