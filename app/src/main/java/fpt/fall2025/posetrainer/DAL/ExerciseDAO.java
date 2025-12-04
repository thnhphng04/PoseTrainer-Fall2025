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

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * ExerciseDAO - Data Access Object cho Exercise
 * Quản lý các thao tác CRUD với collection "exercises" trong Firestore
 */
public class ExerciseDAO {
    private static final String TAG = "ExerciseDAO";
    private static final String COLLECTION_NAME = "exercises";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public ExerciseDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lấy exercise theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Exercise> listener) {
        Log.d(TAG, "Đang lấy exercise: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Exercise exercise = documentSnapshot.toObject(Exercise.class);
                    if (exercise != null) {
                        exercise.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy exercise thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(exercise));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse exercise");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Exercise>forException(new Exception("Không thể parse exercise")));
                        }
                    }
                } else {
                    Log.d(TAG, "Exercise không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy exercise: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Exercise>forException(e));
                }
            });
    }
    
    /**
     * Lấy nhiều exercises theo danh sách IDs
     */
    public void getByIds(@NonNull List<String> ids, @Nullable OnCompleteListener<List<Exercise>> listener) {
        Log.d(TAG, "Đang lấy exercises: " + ids.size() + " IDs");
        if (ids.isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forResult(new ArrayList<>()));
            }
            return;
        }
        
        List<Exercise> exercises = new ArrayList<>();
        final int[] completed = {0};
        final int total = ids.size();
        
        for (String id : ids) {
            getById(id, task -> {
                completed[0]++;
                if (task.isSuccessful() && task.getResult() != null) {
                    exercises.add(task.getResult());
                }
                
                if (completed[0] == total) {
                    Log.d(TAG, "✅ Lấy " + exercises.size() + " exercises thành công");
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(exercises));
                    }
                }
            });
        }
    }
}

