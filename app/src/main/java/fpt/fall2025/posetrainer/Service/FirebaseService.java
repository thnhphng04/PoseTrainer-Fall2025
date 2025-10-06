package fpt.fall2025.posetrainer.Service;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.Session;

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
    
    public interface OnSessionSavedListener {
        void onSessionSaved(boolean success);
    }
    
    public interface OnSessionLoadedListener {
        void onSessionLoaded(Session session);
        void onError(String error);
    }
    
    /**
     * Save session to Firebase Firestore
     */
    public void saveSession(Session session, OnSessionSavedListener listener) {
        Log.d(TAG, "Saving session to Firestore: " + session.getId());
        
        db.collection("sessions")
                .document(session.getId())
                .set(session)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session saved successfully");
                    if (listener != null) {
                        listener.onSessionSaved(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving session", e);
                    if (listener != null) {
                        listener.onSessionSaved(false);
                    }
                });
    }
    
    /**
     * Load session by ID from Firebase
     */
    public void loadSessionById(String sessionId, OnSessionLoadedListener listener) {
        Log.d(TAG, "Loading session by ID: " + sessionId);
        
        db.collection("sessions")
                .document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Session session = documentSnapshot.toObject(Session.class);
                        Log.d(TAG, "Loaded session by ID: " + session.getId());
                        if (listener != null) {
                            listener.onSessionLoaded(session);
                        }
                    } else {
                        Log.d(TAG, "Session not found with ID: " + sessionId);
                        if (listener != null) {
                            listener.onSessionLoaded(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading session by ID", e);
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
    }

    /**
     * Load active session for a specific workout from Firebase
     */
    public void loadActiveSession(String workoutId, OnSessionLoadedListener listener) {
        Log.d(TAG, "Loading active session for workout: " + workoutId);
        
        db.collection("sessions")
                .whereEqualTo("workoutId", workoutId)
                .whereEqualTo("uid", "uid_1") // TODO: Get from authenticated user
                .whereEqualTo("endedAt", 0) // Session chưa kết thúc (endedAt = 0)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Lấy session mới nhất
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No active session found for workout: " + workoutId);
                        if (listener != null) {
                            listener.onSessionLoaded(null);
                        }
                    } else {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Session session = document.toObject(Session.class);
                        Log.d(TAG, "Loaded active session: " + session.getId());
                        if (listener != null) {
                            listener.onSessionLoaded(session);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading active session", e);
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
    }

    public void loadUserSessions(String uid, AppCompatActivity activity, OnSessionsLoadedListener listener) {
        Log.d(TAG, "=== FIREBASE LOADING SESSIONS ===");
        Log.d(TAG, "Loading sessions for user: " + uid);
        
        db.collection("sessions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(activity, task -> {
                    Log.d(TAG, "Firebase query completed");
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase query successful");
                        Log.d(TAG, "Document count: " + task.getResult().size());
                        
                        ArrayList<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Processing document: " + document.getId());
                            try {
                                Session session = document.toObject(Session.class);
                                if (session != null) {
                                    session.setId(document.getId());
                                    sessions.add(session);
                                    Log.d(TAG, "Successfully loaded session: " + session.getId() + " for uid: " + session.getUid());
                                } else {
                                    Log.w(TAG, "Session object is null for document: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing session: " + e.getMessage());
                            }
                        }
                        // Sort by startedAt descending (newest first)
                        // All sessions now use seconds format
                        sessions.sort((s1, s2) -> Long.compare(s2.getStartedAt(), s1.getStartedAt()));
                        
                        Log.d(TAG, "Total sessions loaded and sorted: " + sessions.size());
                        listener.onSessionsLoaded(sessions);
                    } else {
                        Log.e(TAG, "Firebase query failed: ", task.getException());
                        listener.onSessionsLoaded(new ArrayList<>());
                    }
                    Log.d(TAG, "=== END FIREBASE LOADING SESSIONS ===");
                });
    }

    public interface OnSessionsLoadedListener {
        void onSessionsLoaded(ArrayList<Session> sessions);
    }
    
    public interface OnExerciseLoadedListener {
        void onExerciseLoaded(Exercise exercise);
    }
    
    public interface OnWorkoutTemplateSavedListener {
        void onWorkoutTemplateSaved(boolean success);
    }
    
    
    public void loadExercisesByIds(ArrayList<String> exerciseIds, AppCompatActivity activity, OnExercisesLoadedListener listener) {
        Log.d(TAG, "Loading exercises by IDs: " + exerciseIds);
        
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            Log.e(TAG, "No exercise IDs provided");
            listener.onExercisesLoaded(new ArrayList<>());
            return;
        }
        
        ArrayList<Exercise> exercises = new ArrayList<>();
        final int[] loadedItems = {0};
        final int totalItems = exerciseIds.size();
        
        for (String exerciseId : exerciseIds) {
            db.collection("exercises")
                    .document(exerciseId)
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
                                Log.e(TAG, "Exercise document not found: " + exerciseId);
                            }
                            
                            loadedItems[0]++;
                            if (loadedItems[0] == totalItems) {
                                // All exercises loaded
                                activity.runOnUiThread(() -> {
                                    listener.onExercisesLoaded(exercises);
                                });
                            }
                        } else {
                            Log.e(TAG, "Error loading exercise: " + exerciseId, task.getException());
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
    
    public void loadExerciseById(String exerciseId, AppCompatActivity activity, OnExerciseLoadedListener listener) {
        Log.d(TAG, "Loading exercise by ID: " + exerciseId);
        
        db.collection("exercises")
                .document(exerciseId)
                .get()
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            try {
                                Exercise exercise = document.toObject(Exercise.class);
                                listener.onExerciseLoaded(exercise);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing exercise: " + e.getMessage());
                                listener.onExerciseLoaded(null);
                            }
                        } else {
                            Log.e(TAG, "Exercise not found: " + exerciseId);
                            listener.onExerciseLoaded(null);
                        }
                    } else {
                        Log.e(TAG, "Error loading exercise", task.getException());
                        listener.onExerciseLoaded(null);
                    }
                });
    }
    
    /**
     * Save user workout template to Firebase
     */
    public void saveUserWorkoutTemplate(String userId, WorkoutTemplate workout, OnWorkoutTemplateSavedListener listener) {
        Log.d(TAG, "Saving user workout template: " + workout.getTitle());
        
        db.collection("user_workouts")
                .document(userId)
                .collection("workouts")
                .document(workout.getId())
                .set(workout)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User workout template saved successfully");
                    if (listener != null) {
                        listener.onWorkoutTemplateSaved(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user workout template", e);
                    if (listener != null) {
                        listener.onWorkoutTemplateSaved(false);
                    }
                });
    }
    
    /**
     * Load all exercises from Firebase
     */
    public void loadAllExercises(AppCompatActivity activity, OnExercisesLoadedListener listener) {
        Log.d(TAG, "Loading all exercises from Firebase");
        
        db.collection("exercises")
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
                            listener.onExercisesLoaded(exercises);
                        });
                    } else {
                        Log.e(TAG, "Error getting exercises: ", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Error loading exercises", Toast.LENGTH_SHORT).show();
                            listener.onExercisesLoaded(new ArrayList<>());
                        });
                    }
                });
    }

    /**
     * Load user workout templates
     */
    public void loadUserWorkoutTemplates(String userId, AppCompatActivity activity, OnWorkoutTemplatesLoadedListener listener) {
        Log.d(TAG, "Loading user workout templates for user: " + userId);
        
        db.collection("user_workouts")
                .document(userId)
                .collection("workouts")
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "Firebase query completed for user: " + userId);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase query successful, found " + task.getResult().size() + " documents");
                        ArrayList<WorkoutTemplate> workoutTemplates = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                WorkoutTemplate workoutTemplate = document.toObject(WorkoutTemplate.class);
                                if (workoutTemplate != null) {
                                    workoutTemplate.setId(document.getId());
                                    workoutTemplates.add(workoutTemplate);
                                    Log.d(TAG, "Loaded user template: " + workoutTemplate.getTitle() + " (ID: " + document.getId() + ")");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user template: " + e.getMessage());
                            }
                        }
                        
                        Log.d(TAG, "Total user workout templates loaded: " + workoutTemplates.size());
                        activity.runOnUiThread(() -> {
                            listener.onWorkoutTemplatesLoaded(workoutTemplates);
                        });
                    } else {
                        Log.e(TAG, "Error getting user workout templates: ", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Error loading user workout templates", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
}
