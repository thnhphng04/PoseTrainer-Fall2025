package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import fpt.fall2025.posetrainer.Domain.Streak;
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
}

