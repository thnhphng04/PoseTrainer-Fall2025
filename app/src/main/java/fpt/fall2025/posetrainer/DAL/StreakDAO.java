package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.Streak;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * StreakDAO - Data Access Object cho Streak
 * Quản lý các thao tác CRUD với collection "streaks" trong Firestore
 */
public class StreakDAO {
    private static final String TAG = "StreakDAO";
    private static final String COLLECTION_NAME = "streaks";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public StreakDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu streak vào Firestore
     */
    public void save(@NonNull Streak streak, @Nullable OnCompleteListener<Void> listener) {
        if (streak == null || streak.getUid() == null) {
            Log.e(TAG, "Streak hoặc UID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Streak hoặc UID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu streak: " + streak.getUid());
        firestoreContext.getDocument(COLLECTION_NAME, streak.getUid())
            .set(streak)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu streak thành công: " + streak.getUid());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu streak: " + streak.getUid(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy streak theo UID
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<Streak> listener) {
        Log.d(TAG, "Đang lấy streak: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Streak streak = documentSnapshot.toObject(Streak.class);
                    if (streak != null) {
                        Log.d(TAG, "✅ Lấy streak thành công: " + uid);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(streak));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse streak");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Streak>forException(new Exception("Không thể parse streak")));
                        }
                    }
                } else {
                    Log.d(TAG, "Streak không tồn tại: " + uid);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy streak: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Streak>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật streak
     */
    public void update(@NonNull String uid, @NonNull Streak streak, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật streak: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .set(streak)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật streak thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật streak: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Kiểm tra session đã completed chưa
     * QUAN TRỌNG: Chỉ tính streak khi TẤT CẢ PerExercise có state = "completed"
     */
    private boolean isSessionCompleted(@NonNull Session session) {
        if (session == null) {
            Log.d(TAG, "ℹ️ Session là null");
            return false;
        }
        
        if (session.getEndedAt() == 0 || session.getEndedAt() <= session.getStartedAt()) {
            Log.d(TAG, "ℹ️ Session chưa kết thúc (endedAt = " + session.getEndedAt() + ", startedAt = " + session.getStartedAt() + ")");
            return false;
        }
        
        if (session.getPerExercise() == null || session.getPerExercise().isEmpty()) {
            Log.d(TAG, "ℹ️ Session không có PerExercise nào");
            return false;
        }
        
        for (Session.PerExercise perExercise : session.getPerExercise()) {
            String exerciseState = perExercise.getState();
            if (exerciseState == null || !"completed".equals(exerciseState)) {
                return false;
            }
        }
        
        Log.d(TAG, "✅ Session đã completed: endedAt > 0 và TẤT CẢ exercises đã completed");
        return true;
    }
    
    /**
     * Update user streak based on new session
     * CHỈ cập nhật streak khi session đã completed
     * Tương thích với FirebaseService interface
     */
    public void updateStreak(@NonNull String uid, @NonNull Session session, @Nullable OnStreakUpdatedListener listener) {
        if (session == null || session.getStartedAt() == 0) {
            Log.w(TAG, "⚠️ Không thể cập nhật streak: session là null hoặc không hợp lệ");
            if (listener != null) {
                listener.onStreakUpdated(null);
            }
            return;
        }

        if (!isSessionCompleted(session)) {
            Log.w(TAG, "⚠️ Không cập nhật streak: session chưa completed");
            if (listener != null) {
                listener.onStreakUpdated(null);
            }
            return;
        }

        Calendar sessionDate = Calendar.getInstance();
        sessionDate.setTimeInMillis(session.getStartedAt() * 1000L);
        sessionDate.set(Calendar.HOUR_OF_DAY, 0);
        sessionDate.set(Calendar.MINUTE, 0);
        sessionDate.set(Calendar.SECOND, 0);
        sessionDate.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String workoutDate = dateFormat.format(sessionDate.getTime());

        Log.d(TAG, "🔥 Đang cập nhật streak cho user: " + uid + ", ngày tập: " + workoutDate);

        getByUserId(uid, task -> {
            Streak streak;
            if (task.isSuccessful() && task.getResult() != null) {
                streak = task.getResult();
            } else {
                streak = new Streak(uid, 0, 0, null);
            }

            String lastWorkoutDate = streak.getLastWorkoutDate();
            
            if (lastWorkoutDate == null || lastWorkoutDate.isEmpty()) {
                streak.setCurrentStreak(1);
                streak.setLongestStreak(1);
                streak.setLastWorkoutDate(workoutDate);
            } else if (lastWorkoutDate.equals(workoutDate)) {
                Log.d(TAG, "📅 Cùng ngày tập, streak không thay đổi: " + streak.getCurrentStreak());
            } else {
                try {
                    Calendar lastDate = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    lastDate.setTime(sdf.parse(lastWorkoutDate));
                    lastDate.set(Calendar.HOUR_OF_DAY, 0);
                    lastDate.set(Calendar.MINUTE, 0);
                    lastDate.set(Calendar.SECOND, 0);
                    lastDate.set(Calendar.MILLISECOND, 0);

                    long diffInMillis = sessionDate.getTimeInMillis() - lastDate.getTimeInMillis();
                    long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

                    if (diffInDays == 1) {
                        streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                        Log.d(TAG, "🔥 Ngày liên tiếp, streak tăng lên: " + streak.getCurrentStreak());
                    } else if (diffInDays >= 2) {
                        streak.setCurrentStreak(1);
                        Log.d(TAG, "⚠️ Cách " + diffInDays + " ngày, streak reset về 1");
                    }

                    if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                        streak.setLongestStreak(streak.getCurrentStreak());
                    }

                    streak.setLastWorkoutDate(workoutDate);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Lỗi khi parse ngày tập cuối", e);
                    streak.setCurrentStreak(1);
                    streak.setLastWorkoutDate(workoutDate);
                }
            }

            final Streak finalStreak = streak;
            
            if (finalStreak.getUid() == null || finalStreak.getUid().isEmpty()) {
                finalStreak.setUid(uid);
            } else if (!finalStreak.getUid().equals(uid)) {
                Log.w(TAG, "⚠️ Streak UID không khớp, đang cập nhật...");
                finalStreak.setUid(uid);
            }

            save(finalStreak, saveTask -> {
                if (saveTask.isSuccessful()) {
                    Log.d(TAG, "✅ Cập nhật streak thành công: hiện tại=" + finalStreak.getCurrentStreak() + 
                        " ngày, dài nhất=" + finalStreak.getLongestStreak() + " ngày");
                    if (listener != null) {
                        listener.onStreakUpdated(finalStreak);
                    }
                } else {
                    Log.e(TAG, "❌ Lỗi khi lưu streak", saveTask.getException());
                    if (listener != null) {
                        listener.onStreakUpdated(null);
                    }
                }
            });
        });
    }
    
    /**
     * Load user streak với callback interface tương thích FirebaseService
     */
    public void loadUserStreak(@NonNull String uid, @Nullable OnStreakLoadedListener listener) {
        Log.d(TAG, "📥 Đang tải streak cho user: " + uid);
        
        getByUserId(uid, task -> {
            if (task.isSuccessful()) {
                Streak streak = task.getResult();
                if (streak != null) {
                    Log.d(TAG, "✅ Đã tải streak: hiện tại=" + streak.getCurrentStreak() + 
                        " ngày, dài nhất=" + streak.getLongestStreak() + " ngày");
                } else {
                    Log.d(TAG, "ℹ️ Không tìm thấy streak cho user này");
                }
                if (listener != null) {
                    listener.onStreakLoaded(streak);
                }
            } else {
                Log.e(TAG, "❌ Lỗi khi tải streak", task.getException());
                if (listener != null) {
                    listener.onStreakLoaded(null);
                }
            }
        });
    }
    
    // Interfaces tương thích với FirebaseService
    public interface OnStreakUpdatedListener {
        void onStreakUpdated(Streak streak);
    }
    
    public interface OnStreakLoadedListener {
        void onStreakLoaded(Streak streak);
    }
}

