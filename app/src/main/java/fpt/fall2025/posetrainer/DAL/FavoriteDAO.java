package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Favorite;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * FavoriteDAO - Data Access Object cho Favorite
 * Quản lý các thao tác CRUD với subcollection "favorites" trong Firestore
 */
public class FavoriteDAO {
    private static final String TAG = "FavoriteDAO";
    private static final String SUBCOLLECTION_NAME = "favorites";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public FavoriteDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu favorite vào Firestore (subcollection của user)
     */
    public void save(@NonNull String uid, @NonNull Favorite favorite, @Nullable OnCompleteListener<Void> listener) {
        if (favorite == null || favorite.getWorkoutTemplateId() == null) {
            Log.e(TAG, "Favorite hoặc workoutTemplateId không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Favorite hoặc workoutTemplateId không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu favorite: " + favorite.getWorkoutTemplateId());
        firestoreContext.getDocument("users", uid)
            .collection(SUBCOLLECTION_NAME)
            .document(favorite.getWorkoutTemplateId())
            .set(favorite)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu favorite thành công: " + favorite.getWorkoutTemplateId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu favorite: " + favorite.getWorkoutTemplateId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả favorites của user
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<Favorite>> listener) {
        Log.d(TAG, "Đang lấy favorites của user: " + uid);
        firestoreContext.getDocument("users", uid)
            .collection(SUBCOLLECTION_NAME)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Favorite> favorites = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Favorite favorite = doc.toObject(Favorite.class);
                    if (favorite != null) {
                        favorite.setWorkoutTemplateId(doc.getId());
                        favorites.add(favorite);
                    }
                }
                Log.d(TAG, "✅ Lấy " + favorites.size() + " favorites thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(favorites));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy favorites của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Favorite>>forException(e));
                }
            });
    }
    
    /**
     * Kiểm tra workout template có trong favorites không
     */
    public void isFavorite(@NonNull String uid, @NonNull String workoutTemplateId, @Nullable OnCompleteListener<Boolean> listener) {
        Log.d(TAG, "Đang kiểm tra favorite: " + workoutTemplateId);
        firestoreContext.getDocument("users", uid)
            .collection(SUBCOLLECTION_NAME)
            .document(workoutTemplateId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                boolean isFavorite = documentSnapshot.exists();
                Log.d(TAG, "✅ Kiểm tra favorite thành công: " + isFavorite);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(isFavorite));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi kiểm tra favorite: " + workoutTemplateId, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Boolean>forException(e));
                }
            });
    }
    
    /**
     * Xóa favorite
     */
    public void delete(@NonNull String uid, @NonNull String workoutTemplateId, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa favorite: " + workoutTemplateId);
        firestoreContext.getDocument("users", uid)
            .collection(SUBCOLLECTION_NAME)
            .document(workoutTemplateId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa favorite thành công: " + workoutTemplateId);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa favorite: " + workoutTemplateId, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
}

