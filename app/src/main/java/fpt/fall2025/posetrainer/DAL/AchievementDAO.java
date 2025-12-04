package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Achievement;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * AchievementDAO - Data Access Object cho Achievement
 * Quản lý các thao tác CRUD với collection "achievements" trong Firestore
 */
public class AchievementDAO {
    private static final String TAG = "AchievementDAO";
    private static final String COLLECTION_NAME = "achievements";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public AchievementDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lấy achievement theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Achievement> listener) {
        Log.d(TAG, "Đang lấy achievement: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Achievement achievement = documentSnapshot.toObject(Achievement.class);
                    if (achievement != null) {
                        // Achievement không có field id, chỉ có uid
                        Log.d(TAG, "✅ Lấy achievement thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(achievement));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse achievement");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Achievement>forException(new Exception("Không thể parse achievement")));
                        }
                    }
                } else {
                    Log.d(TAG, "Achievement không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy achievement: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Achievement>forException(e));
                }
            });
    }
    
    /**
     * Lấy achievements của user (subcollection)
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<Achievement>> listener) {
        Log.d(TAG, "Đang lấy achievements của user: " + uid);
        firestoreContext.getDocument("users", uid)
            .collection(COLLECTION_NAME)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Achievement> achievements = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Achievement achievement = doc.toObject(Achievement.class);
                    if (achievement != null) {
                        // Achievement không có field id, chỉ có uid
                        achievements.add(achievement);
                    }
                }
                Log.d(TAG, "✅ Lấy " + achievements.size() + " achievements thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(achievements));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy achievements của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Achievement>>forException(e));
                }
            });
    }
}

