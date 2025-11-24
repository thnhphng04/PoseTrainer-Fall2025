package fpt.fall2025.posetrainer.Manager;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Achievement;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.Streak;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Manager để kiểm tra và unlock achievements
 */
public class AchievementManager {
    private static final String TAG = "AchievementManager";
    private static AchievementManager instance;
    private FirebaseFirestore db;

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
    }

    private AchievementManager() {
        db = FirebaseFirestore.getInstance();
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
        FirebaseService.getInstance().loadUserStreak(uid, streak -> {
            int currentStreak = (streak != null) ? streak.getCurrentStreak() : 0;

            // Load total workout count
            db.collection("sessions")
                    .whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int totalWorkouts = queryDocumentSnapshots.size();

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

                                    // Check workout count achievements
                                    if (totalWorkouts >= 1 && !achievement.isBadgeUnlocked("workout_1")) {
                                        achievement.unlockBadge("workout_1");
                                        newlyUnlocked.add("workout_1");
                                        Log.d(TAG, "🎉 Mở khóa achievement: Bắt Đầu Hành Trình (workout_1)");
                                    }
                                    if (totalWorkouts >= 10 && !achievement.isBadgeUnlocked("workout_10")) {
                                        achievement.unlockBadge("workout_10");
                                        newlyUnlocked.add("workout_10");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 10 Buổi Tập (workout_10)");
                                    }
                                    if (totalWorkouts >= 30 && !achievement.isBadgeUnlocked("workout_30")) {
                                        achievement.unlockBadge("workout_30");
                                        newlyUnlocked.add("workout_30");
                                        Log.d(TAG, "🎉 Mở khóa achievement: 30 Buổi Tập (workout_30)");
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

