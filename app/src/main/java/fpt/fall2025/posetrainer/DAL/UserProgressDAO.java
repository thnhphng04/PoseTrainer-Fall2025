package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.UserProgress;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

/**
 * UserProgressDAO - Data Access Object cho UserProgress
 * Quản lý các thao tác CRUD với collection "user_progress" trong Firestore
 */
public class UserProgressDAO {
    private static final String TAG = "UserProgressDAO";
    private static final String COLLECTION_NAME = "user_progress";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public UserProgressDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu user progress vào Firestore
     */
    public void save(@NonNull UserProgress progress, @Nullable OnCompleteListener<Void> listener) {
        if (progress == null || progress.getUid() == null) {
            Log.e(TAG, "UserProgress hoặc UID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("UserProgress hoặc UID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu user progress: " + progress.getUid());
        firestoreContext.getDocument(COLLECTION_NAME, progress.getUid())
            .set(progress)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu user progress thành công: " + progress.getUid());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu user progress: " + progress.getUid(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy user progress theo UID
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<UserProgress> listener) {
        Log.d(TAG, "Đang lấy user progress: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserProgress progress = documentSnapshot.toObject(UserProgress.class);
                    if (progress != null) {
                        Log.d(TAG, "✅ Lấy user progress thành công: " + uid);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(progress));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse user progress");
                        if (listener != null) {
                            listener.onComplete(Tasks.<UserProgress>forException(new Exception("Không thể parse user progress")));
                        }
                    }
                } else {
                    Log.d(TAG, "User progress không tồn tại: " + uid);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy user progress: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<UserProgress>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật user progress
     */
    public void update(@NonNull String uid, @NonNull UserProgress progress, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật user progress: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .set(progress)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật user progress thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật user progress: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Update user progress (calendar heatmap)
     * Tương thích với FirebaseService interface
     */
    public void updateUserProgress(@NonNull String uid, @Nullable OnProgressUpdatedListener listener) {
        Log.d(TAG, "Updating user progress for: " + uid);

        SessionDAO sessionDAO = new SessionDAO();
        sessionDAO.getByUserId(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Boolean> calendar = new HashMap<>();
                int totalSessions = task.getResult().size();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (Session session : task.getResult()) {
                    if (session != null && session.getStartedAt() > 0) {
                        Calendar sessionDate = Calendar.getInstance();
                        sessionDate.setTimeInMillis(session.getStartedAt() * 1000L);
                        sessionDate.set(Calendar.HOUR_OF_DAY, 0);
                        sessionDate.set(Calendar.MINUTE, 0);
                        sessionDate.set(Calendar.SECOND, 0);
                        sessionDate.set(Calendar.MILLISECOND, 0);

                        String dateKey = dateFormat.format(sessionDate.getTime());
                        calendar.put(dateKey, true);
                    }
                }

                int totalWorkoutDays = calendar.size();

                UserProgress progress = new UserProgress(uid, totalWorkoutDays, totalSessions, calendar);

                save(progress, saveTask -> {
                    if (saveTask.isSuccessful()) {
                        Log.d(TAG, "User progress updated successfully: " + totalWorkoutDays + " days, " + totalSessions + " sessions");
                        if (listener != null) {
                            listener.onProgressUpdated(progress);
                        }
                    } else {
                        Log.e(TAG, "Error saving user progress", saveTask.getException());
                        if (listener != null) {
                            listener.onProgressUpdated(null);
                        }
                    }
                });
            } else {
                Log.e(TAG, "Error loading sessions for user progress", task.getException());
                if (listener != null) {
                    listener.onProgressUpdated(null);
                }
            }
        });
    }
    
    /**
     * Load user progress với callback interface tương thích FirebaseService
     */
    public void loadUserProgress(@NonNull String uid, @Nullable OnProgressLoadedListener listener) {
        Log.d(TAG, "Loading user progress for: " + uid);

        getByUserId(uid, task -> {
            if (task.isSuccessful()) {
                UserProgress progress = task.getResult();
                if (progress != null) {
                    Log.d(TAG, "User progress loaded: " + progress.getTotalWorkoutDays() + " days, " + progress.getTotalSessions() + " sessions");
                } else {
                    Log.d(TAG, "No user progress found");
                }
                if (listener != null) {
                    listener.onProgressLoaded(progress);
                }
            } else {
                Log.e(TAG, "Error loading user progress", task.getException());
                if (listener != null) {
                    listener.onProgressLoaded(null);
                }
            }
        });
    }
    
    // Interfaces tương thích với FirebaseService
    public interface OnProgressUpdatedListener {
        void onProgressUpdated(UserProgress progress);
    }
    
    public interface OnProgressLoadedListener {
        void onProgressLoaded(UserProgress progress);
    }
}

