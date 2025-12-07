package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Favorite;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

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
    
    /**
     * Thêm workout template vào danh sách yêu thích của user
     * Tương thích với FirebaseService interface
     */
    public void addFavoriteWorkoutTemplate(@NonNull String userId, @NonNull String workoutTemplateId, 
                                          @Nullable OnFavoriteWorkoutUpdatedListener listener) {
        Log.d(TAG, "Thêm workout template vào yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        Favorite favorite = new Favorite(
            workoutTemplateId,
            userId,
            System.currentTimeMillis() / 1000
        );
        
        save(userId, favorite, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "✓ Đã thêm workout template vào yêu thích thành công");
                if (listener != null) {
                    listener.onFavoriteWorkoutUpdated(true);
                }
            } else {
                Log.e(TAG, "✗ Lỗi thêm workout template vào yêu thích: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                if (listener != null) {
                    listener.onFavoriteWorkoutUpdated(false);
                }
            }
        });
    }
    
    /**
     * Xóa workout template khỏi danh sách yêu thích của user
     * Tương thích với FirebaseService interface
     */
    public void removeFavoriteWorkoutTemplate(@NonNull String userId, @NonNull String workoutTemplateId, 
                                            @Nullable OnFavoriteWorkoutUpdatedListener listener) {
        Log.d(TAG, "Xóa workout template khỏi yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        delete(userId, workoutTemplateId, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "✓ Đã xóa workout template khỏi yêu thích thành công");
                if (listener != null) {
                    listener.onFavoriteWorkoutUpdated(true);
                }
            } else {
                Log.e(TAG, "✗ Lỗi xóa workout template khỏi yêu thích: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                if (listener != null) {
                    listener.onFavoriteWorkoutUpdated(false);
                }
            }
        });
    }
    
    /**
     * Kiểm tra xem workout template có trong danh sách yêu thích không
     * Tương thích với FirebaseService interface
     */
    public void checkFavoriteWorkoutTemplate(@NonNull String userId, @NonNull String workoutTemplateId, 
                                           @Nullable OnFavoriteWorkoutCheckedListener listener) {
        Log.d(TAG, "Kiểm tra workout template yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        isFavorite(userId, workoutTemplateId, task -> {
            if (task.isSuccessful()) {
                boolean isFavorite = task.getResult() != null && task.getResult();
                Log.d(TAG, "✓ Workout template " + (isFavorite ? "có" : "không có") + " trong yêu thích");
                if (listener != null) {
                    listener.onFavoriteWorkoutChecked(isFavorite);
                }
            } else {
                Log.e(TAG, "✗ Lỗi kiểm tra workout template yêu thích: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                if (listener != null) {
                    listener.onFavoriteWorkoutChecked(false);
                }
            }
        });
    }
    
    /**
     * Load tất cả favorite workout template IDs của user
     * Tương thích với FirebaseService interface
     */
    public void loadFavoriteWorkoutTemplateIds(@NonNull String userId, 
                                             @Nullable OnFavoriteWorkoutTemplateIdsLoadedListener listener) {
        Log.d(TAG, "Load danh sách favorite workout template IDs cho user: " + userId);
        
        getByUserId(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<String> favoriteIds = new ArrayList<>();
                for (Favorite favorite : task.getResult()) {
                    String workoutTemplateId = favorite.getWorkoutTemplateId();
                    if (workoutTemplateId != null && !workoutTemplateId.isEmpty()) {
                        favoriteIds.add(workoutTemplateId);
                    }
                }
                Log.d(TAG, "✓ Đã load " + favoriteIds.size() + " favorite workout template IDs");
                if (listener != null) {
                    listener.onFavoriteWorkoutTemplateIdsLoaded(favoriteIds);
                }
            } else {
                Log.e(TAG, "✗ Lỗi load favorite workout template IDs: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                if (listener != null) {
                    listener.onFavoriteWorkoutTemplateIdsLoaded(new ArrayList<>());
                }
            }
        });
    }
    
    // Interfaces tương thích với FirebaseService
    public interface OnFavoriteWorkoutUpdatedListener {
        void onFavoriteWorkoutUpdated(boolean success);
    }
    
    public interface OnFavoriteWorkoutCheckedListener {
        void onFavoriteWorkoutChecked(boolean isFavorite);
    }
    
    public interface OnFavoriteWorkoutTemplateIdsLoadedListener {
        void onFavoriteWorkoutTemplateIdsLoaded(ArrayList<String> favoriteIds);
    }
}

