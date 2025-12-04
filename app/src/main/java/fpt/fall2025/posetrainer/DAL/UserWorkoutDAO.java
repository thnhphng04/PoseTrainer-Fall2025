package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * UserWorkoutDAO - Data Access Object cho UserWorkout
 * Quản lý các thao tác CRUD với collection "user_workouts" trong Firestore
 */
public class UserWorkoutDAO {
    private static final String TAG = "UserWorkoutDAO";
    private static final String COLLECTION_NAME = "user_workouts";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public UserWorkoutDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu user workout vào Firestore
     */
    public void save(@NonNull UserWorkout workout, @Nullable OnCompleteListener<Void> listener) {
        if (workout == null || workout.getId() == null) {
            Log.e(TAG, "UserWorkout hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("UserWorkout hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu user workout: " + workout.getId());
        firestoreContext.getDocument(COLLECTION_NAME, workout.getId())
            .set(workout)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu user workout thành công: " + workout.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu user workout: " + workout.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy user workout theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<UserWorkout> listener) {
        Log.d(TAG, "Đang lấy user workout: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserWorkout workout = documentSnapshot.toObject(UserWorkout.class);
                    if (workout != null) {
                        workout.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy user workout thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(workout));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse user workout");
                        if (listener != null) {
                            listener.onComplete(Tasks.<UserWorkout>forException(new Exception("Không thể parse user workout")));
                        }
                    }
                } else {
                    Log.d(TAG, "User workout không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy user workout: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<UserWorkout>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả user workouts của user
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<UserWorkout>> listener) {
        Log.d(TAG, "Đang lấy user workouts của user: " + uid);
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<UserWorkout> workouts = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    UserWorkout workout = doc.toObject(UserWorkout.class);
                    if (workout != null) {
                        workout.setId(doc.getId());
                        workouts.add(workout);
                    }
                }
                Log.d(TAG, "✅ Lấy " + workouts.size() + " user workouts thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(workouts));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy user workouts của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<UserWorkout>>forException(e));
                }
            });
    }
    
    /**
     * Xóa user workout
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa user workout: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa user workout thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa user workout: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy collection reference để query
     * @return CollectionReference cho user_workouts
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lấy document reference
     * @param workoutId Workout ID
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String workoutId) {
        return firestoreContext.getDocument(COLLECTION_NAME, workoutId);
    }
}

