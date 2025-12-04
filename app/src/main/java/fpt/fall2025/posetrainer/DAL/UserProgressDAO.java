package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import fpt.fall2025.posetrainer.Domain.UserProgress;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

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
}

