package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.ExerciseUser;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

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
     * FIX: Tìm trong cả collection "exercises" và "exerciseUser" (cho custom exercises)
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Exercise> listener) {
        Log.d(TAG, "Đang lấy exercise: " + id);
        
        // Tìm trong collection "exercises" trước
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Exercise exercise = documentSnapshot.toObject(Exercise.class);
                    if (exercise != null) {
                        exercise.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy exercise thành công từ collection 'exercises': " + id);
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
                    // Nếu không tìm thấy trong "exercises", thử tìm trong "exerciseUser" (custom exercises)
                    Log.d(TAG, "Exercise không tồn tại trong 'exercises', thử tìm trong 'exerciseUser': " + id);
                    firestoreContext.getDocument("exerciseUser", id)
                        .get()
                        .addOnSuccessListener(exerciseUserSnapshot -> {
                            if (exerciseUserSnapshot.exists()) {
                                ExerciseUser exerciseUser = exerciseUserSnapshot.toObject(ExerciseUser.class);
                                if (exerciseUser != null) {
                                    // Convert ExerciseUser sang Exercise
                                    Exercise exercise = exerciseUser.toExercise();
                                    exercise.setId(exerciseUserSnapshot.getId());
                                    Log.d(TAG, "✅ Lấy exercise thành công từ collection 'exerciseUser': " + id);
                                    if (listener != null) {
                                        listener.onComplete(Tasks.forResult(exercise));
                                    }
                                } else {
                                    Log.e(TAG, "❌ Không thể parse ExerciseUser");
                                    if (listener != null) {
                                        listener.onComplete(Tasks.<Exercise>forException(new Exception("Không thể parse ExerciseUser")));
                                    }
                                }
                            } else {
                                Log.d(TAG, "Exercise không tồn tại trong cả 'exercises' và 'exerciseUser': " + id);
                                if (listener != null) {
                                    listener.onComplete(Tasks.forResult(null));
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Lỗi lấy ExerciseUser: " + id, e);
                            if (listener != null) {
                                listener.onComplete(Tasks.<Exercise>forException(e));
                            }
                        });
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
    
    /**
     * Load all exercises from Firebase
     * Tương thích với FirebaseService interface
     */
    public void loadAllExercises(@NonNull AppCompatActivity activity, 
                                @Nullable OnExercisesLoadedListener listener) {
        Log.d(TAG, "Loading all exercises from Firebase");
        
        firestoreContext.getCollection(COLLECTION_NAME)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase query successful, found " + task.getResult().size() + " exercises");
                    ArrayList<Exercise> exercises = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            Exercise exercise = document.toObject(Exercise.class);
                            if (exercise != null) {
                                exercise.setId(document.getId());
                                exercises.add(exercise);
                                Log.d(TAG, "Loaded exercise: " + exercise.getName() + " (ID: " + document.getId() + ")");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing exercise: " + e.getMessage());
                        }
                    }
                    
                    Log.d(TAG, "Total exercises loaded: " + exercises.size());
                    activity.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onExercisesLoaded(exercises);
                        }
                    });
                } else {
                    Log.e(TAG, "Error getting exercises: ", task.getException());
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Error loading exercises", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onExercisesLoaded(new ArrayList<>());
                        }
                    });
                }
            });
    }
    
    /**
     * Load exercises for a user workout
     * Tương thích với FirebaseService interface
     */
    public void loadExercisesForUserWorkout(@NonNull UserWorkout userWorkout, 
                                           @NonNull AppCompatActivity activity,
                                           @Nullable OnExercisesLoadedListener listener) {
        if (userWorkout == null || userWorkout.getItems() == null) {
            Log.e(TAG, "UserWorkout or items is null");
            if (listener != null) {
                activity.runOnUiThread(() -> listener.onExercisesLoaded(new ArrayList<>()));
            }
            return;
        }
        
        Log.d(TAG, "Loading exercises for user workout: " + userWorkout.getTitle());
        
        List<String> exerciseIds = new ArrayList<>();
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            exerciseIds.add(item.getExerciseId());
        }
        
        if (exerciseIds.isEmpty()) {
            Log.w(TAG, "No exercise IDs found in user workout");
            if (listener != null) {
                activity.runOnUiThread(() -> listener.onExercisesLoaded(new ArrayList<>()));
            }
            return;
        }
        
        getByIds(exerciseIds, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<Exercise> exercises = new ArrayList<>(task.getResult());
                activity.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onExercisesLoaded(exercises);
                    }
                });
            } else {
                activity.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onExercisesLoaded(new ArrayList<>());
                    }
                });
            }
        });
    }
    
    // Interface tương thích với FirebaseService
    public interface OnExercisesLoadedListener {
        void onExercisesLoaded(ArrayList<Exercise> exercises);
    }
}

