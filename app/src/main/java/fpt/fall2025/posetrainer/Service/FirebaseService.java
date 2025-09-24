package fpt.fall2025.posetrainer.Service;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;

/**
 * FirebaseService - Service để quản lý tất cả Firebase operations
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;
    private FirebaseFirestore db;
    
    private FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }
    
    public static FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }
    
    /**
     * Load all public workout templates
     */
    public void loadWorkoutTemplates(AppCompatActivity activity, OnWorkoutTemplatesLoadedListener listener) {
        Log.d(TAG, "Loading workout templates");
        
        db.collection("workouts_templates")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<WorkoutTemplate> workoutTemplates = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                WorkoutTemplate workoutTemplate = document.toObject(WorkoutTemplate.class);
                                if (workoutTemplate != null) {
                                    workoutTemplate.setId(document.getId());
                                    workoutTemplates.add(workoutTemplate);
                                    Log.d(TAG, "Loaded template: " + workoutTemplate.getTitle());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing template: " + e.getMessage());
                            }
                        }
                        
                        activity.runOnUiThread(() -> {
                            listener.onWorkoutTemplatesLoaded(workoutTemplates);
                        });
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Error loading workout templates", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    /**
     * Load workout template by ID
     */
    public void loadWorkoutTemplateById(String workoutTemplateId, AppCompatActivity activity, OnWorkoutTemplateLoadedListener listener) {
        Log.d(TAG, "Loading workout template: " + workoutTemplateId);
        
        db.collection("workouts_templates")
                .document(workoutTemplateId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            try {
                                WorkoutTemplate workoutTemplate = document.toObject(WorkoutTemplate.class);
                                if (workoutTemplate != null) {
                                    workoutTemplate.setId(document.getId());
                                    Log.d(TAG, "Loaded template: " + workoutTemplate.getTitle());
                                    
                                    activity.runOnUiThread(() -> {
                                        listener.onWorkoutTemplateLoaded(workoutTemplate);
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing template: " + e.getMessage());
                                activity.runOnUiThread(() -> {
                                    Toast.makeText(activity, "Error loading workout template", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            Log.e(TAG, "No such document");
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Workout template not found", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        Log.e(TAG, "Error getting document: ", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Error loading workout template", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    /**
     * Load exercises for a workout template
     */
    public void loadExercises(WorkoutTemplate workoutTemplate, AppCompatActivity activity, OnExercisesLoadedListener listener) {
        if (workoutTemplate == null || workoutTemplate.getItems() == null) {
            Log.e(TAG, "No workout items to load");
            return;
        }
        
        ArrayList<Exercise> exercises = new ArrayList<>();
        final int totalItems = workoutTemplate.getItems().size();
        final int[] loadedItems = {0}; // Use array to make it effectively final
        
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            db.collection("exercises")
                    .document(item.getExerciseId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                try {
                                    Exercise exercise = document.toObject(Exercise.class);
                                    if (exercise != null) {
                                        exercise.setId(document.getId());
                                        exercises.add(exercise);
                                        Log.d(TAG, "Loaded exercise: " + exercise.getName());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing exercise: " + e.getMessage());
                                }
                            } else {
                                Log.e(TAG, "Error getting exercise: ", task.getException());
                            }
                            
                            loadedItems[0]++;
                            if (loadedItems[0] == totalItems) {
                                // All exercises loaded
                                activity.runOnUiThread(() -> {
                                    listener.onExercisesLoaded(exercises);
                                });
                            }
                        } else {
                            Log.e(TAG, "Error getting exercise: ", task.getException());
                            loadedItems[0]++;
                            if (loadedItems[0] == totalItems) {
                                activity.runOnUiThread(() -> {
                                    listener.onExercisesLoaded(exercises);
                                });
                            }
                        }
                    });
        }
    }
    
    /**
     * Get image resource based on workout template focus/type
     */
    public int getImageResourceForWorkout(WorkoutTemplate workoutTemplate, AppCompatActivity activity) {
        // Default image
        int defaultResId = activity.getResources().getIdentifier("pic_1", "drawable", activity.getPackageName());
        
        if (workoutTemplate.getFocus() != null && !workoutTemplate.getFocus().isEmpty()) {
            String focus = workoutTemplate.getFocus().get(0);
            switch (focus) {
                case "push":
                    return activity.getResources().getIdentifier("pic_1", "drawable", activity.getPackageName());
                case "legs":
                    return activity.getResources().getIdentifier("pic_2", "drawable", activity.getPackageName());
                case "cardio":
                    return activity.getResources().getIdentifier("pic_3", "drawable", activity.getPackageName());
                case "fullbody":
                    return activity.getResources().getIdentifier("pic_1", "drawable", activity.getPackageName());
                default:
                    return defaultResId;
            }
        }
        
        return defaultResId;
    }
    
    // Interface definitions
    public interface OnWorkoutTemplatesLoadedListener {
        void onWorkoutTemplatesLoaded(ArrayList<WorkoutTemplate> workoutTemplates);
    }
    
    public interface OnWorkoutTemplateLoadedListener {
        void onWorkoutTemplateLoaded(WorkoutTemplate workoutTemplate);
    }
    
    public interface OnExercisesLoadedListener {
        void onExercisesLoaded(ArrayList<Exercise> exercises);
    }
}
