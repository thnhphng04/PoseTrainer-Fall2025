package fpt.fall2025.posetrainer.Service;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.Domain.Favorite;
import fpt.fall2025.posetrainer.Domain.Streak;
import fpt.fall2025.posetrainer.Domain.Achievement;
import fpt.fall2025.posetrainer.Domain.UserProgress;

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
        Log.d(TAG, "Đang tải mẫu bài tập...");

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
                                    Log.d(TAG, "Đã tải mẫu: " + workoutTemplate.getTitle());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi: Lỗi phân tích mẫu: " + e.getMessage(), e);
                            }
                        }

                        activity.runOnUiThread(() -> {
                            listener.onWorkoutTemplatesLoaded(workoutTemplates);
                        });
                    } else {
                        Log.e(TAG, "Lỗi: Lỗi lấy tài liệu", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Lỗi tải mẫu bài tập", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Load workout template by ID
     */
    public void loadWorkoutTemplateById(String workoutTemplateId, AppCompatActivity activity, OnWorkoutTemplateLoadedListener listener) {
        Log.d(TAG, "Đang tải mẫu bài tập với ID: " + workoutTemplateId);

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
                                } else {
                                    Log.e(TAG, "WorkoutTemplate object is null");
                                    activity.runOnUiThread(() -> {
                                        listener.onWorkoutTemplateLoaded(null);
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing template: " + e.getMessage());
                                activity.runOnUiThread(() -> {
                                    listener.onWorkoutTemplateLoaded(null);
                                });
                            }
                        } else {
                            Log.e(TAG, "No such document");
                            activity.runOnUiThread(() -> {
                                listener.onWorkoutTemplateLoaded(null);
                            });
                        }
                    } else {
                        Log.e(TAG, "Error getting document: ", task.getException());
                        activity.runOnUiThread(() -> {
                            listener.onWorkoutTemplateLoaded(null);
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
        // Null check để tránh NullPointerException
        final int totalItems = (workoutTemplate.getItems() != null) ? workoutTemplate.getItems().size() : 0;
        final int[] loadedItems = {0}; // Use array to make it effectively final

        if (workoutTemplate.getItems() == null || totalItems == 0) {
            Log.e(TAG, "WorkoutTemplate has no items");
            activity.runOnUiThread(() -> {
                listener.onExercisesLoaded(exercises);
            });
            return;
        }

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

    public interface OnCollectionsLoadedListener {
        void onCollectionsLoaded(ArrayList<fpt.fall2025.posetrainer.Domain.Collection> collections);
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
        if (session == null) {
            Log.e(TAG, "❌ Không thể lưu session: session là null");
            if (listener != null) {
                listener.onSessionSaved(false);
            }
            return;
        }

        // Kiểm tra uid trước khi lưu
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Không thể lưu session: người dùng chưa đăng nhập");
            if (listener != null) {
                listener.onSessionSaved(false);
            }
            return;
        }

        String currentUid = currentUser.getUid();
        String sessionUid = session.getUid();
        
        // Đảm bảo uid được set đúng
        if (sessionUid == null || sessionUid.isEmpty()) {
            Log.w(TAG, "⚠️ Session UID trống, đang set thành UID của người dùng hiện tại: " + currentUid);
            session.setUid(currentUid);
        } else if (!sessionUid.equals(currentUid)) {
            Log.w(TAG, "⚠️ Session UID (" + sessionUid + ") không khớp với UID hiện tại (" + currentUid + "), đang cập nhật...");
            session.setUid(currentUid);
        }

        Log.d(TAG, "💾 Đang lưu session vào Firestore: " + session.getId() + ", UID: " + session.getUid());

        db.collection("sessions")
                .document(session.getId())
                .set(session)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Lưu session thành công: " + session.getId());
                    if (listener != null) {
                        listener.onSessionSaved(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi lưu session: " + session.getId(), e);
                    Log.e(TAG, "📋 Mã lỗi: " + (e instanceof com.google.firebase.firestore.FirebaseFirestoreException 
                        ? ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() 
                        : "Không xác định"));
                    Log.e(TAG, "📋 Chi tiết lỗi: " + e.getMessage());
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

        // Get current user UID from FirebaseAuth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            if (listener != null) {
                listener.onError("User not authenticated");
            }
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Loading active session for user: " + uid);

        db.collection("sessions")
                .whereEqualTo("workoutId", workoutId)
                .whereEqualTo("uid", uid) // Use authenticated user UID
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
                        if (session != null) {
                            session.setId(document.getId());
                            Log.d(TAG, "Loaded active session: " + session.getId());
                            if (listener != null) {
                                listener.onSessionLoaded(session);
                            }
                        } else {
                            Log.e(TAG, "Failed to parse session object");
                            if (listener != null) {
                                listener.onError("Failed to parse session");
                            }
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

    public interface OnUserWorkoutSavedListener {
        void onUserWorkoutSaved(boolean success);
    }

    public interface OnUserWorkoutsLoadedListener {
        void onUserWorkoutsLoaded(ArrayList<UserWorkout> userWorkouts);
    }

    public interface OnUserWorkoutDeletedListener {
        void onUserWorkoutDeleted(boolean success);
    }

    public interface OnUserWorkoutLoadedListener {
        void onUserWorkoutLoaded(UserWorkout userWorkout);
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
                                if (exercise != null) {
                                    // QUAN TRỌNG: Set ID cho exercise
                                    exercise.setId(document.getId());
                                    Log.d(TAG, "Exercise loaded: " + exercise.getName() + " (ID: " + exercise.getId() + ")");
                                }
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
     * Save user workout to Firebase in new format
     */
    public void saveUserWorkout(UserWorkout userWorkout, OnUserWorkoutSavedListener listener) {
        Log.d(TAG, "Saving user workout: " + userWorkout.getTitle());

        db.collection("user_workouts")
                .document(userWorkout.getId())
                .set(userWorkout)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User workout saved successfully");
                    if (listener != null) {
                        listener.onUserWorkoutSaved(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user workout", e);
                    if (listener != null) {
                        listener.onUserWorkoutSaved(false);
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

    /**
     * Load user workouts for a specific user
     */
    public void loadUserWorkouts(String userId, AppCompatActivity activity, OnUserWorkoutsLoadedListener listener) {
        Log.d(TAG, "=== LOADING USER WORKOUTS ===");
        Log.d(TAG, "Loading user workouts for user: " + userId);

        // First try without orderBy to avoid index issues
        db.collection("user_workouts")
                .whereEqualTo("uid", userId)
                .get()
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "Firebase query completed for user: " + userId);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase query successful, found " + task.getResult().size() + " documents");
                        ArrayList<UserWorkout> userWorkouts = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "Processing document: " + document.getId());
                            try {
                                UserWorkout userWorkout = document.toObject(UserWorkout.class);
                                if (userWorkout != null) {
                                    userWorkout.setId(document.getId());
                                    userWorkouts.add(userWorkout);
                                    Log.d(TAG, "Successfully loaded user workout: " + userWorkout.getTitle() + " (ID: " + document.getId() + ", UID: " + userWorkout.getUid() + ")");
                                } else {
                                    Log.w(TAG, "UserWorkout object is null for document: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user workout: " + e.getMessage());
                            }
                        }

                        // Sort by updatedAt descending (newest first)
                        userWorkouts.sort((w1, w2) -> Long.compare(w2.getUpdatedAt(), w1.getUpdatedAt()));

                        Log.d(TAG, "Total user workouts loaded and sorted: " + userWorkouts.size());
                        activity.runOnUiThread(() -> {
                            listener.onUserWorkoutsLoaded(userWorkouts);
                        });
                    } else {
                        Log.e(TAG, "Firebase query failed: ", task.getException());
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Error loading user workouts: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                    Log.d(TAG, "=== END LOADING USER WORKOUTS ===");
                });
    }

    /**
     * Delete user workout
     */
    public void deleteUserWorkout(String userWorkoutId, OnUserWorkoutDeletedListener listener) {
        Log.d(TAG, "Deleting user workout: " + userWorkoutId);

        db.collection("user_workouts")
                .document(userWorkoutId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User workout deleted successfully");
                    if (listener != null) {
                        listener.onUserWorkoutDeleted(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user workout", e);
                    if (listener != null) {
                        listener.onUserWorkoutDeleted(false);
                    }
                });
    }

    /**
     * Debug method to load all user workouts (for debugging purposes)
     */
    public void debugLoadAllUserWorkouts(AppCompatActivity activity, OnUserWorkoutsLoadedListener listener) {
        Log.d(TAG, "=== DEBUG: LOADING ALL USER WORKOUTS ===");

        db.collection("user_workouts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "DEBUG: Found " + task.getResult().size() + " total user workouts in database");
                        ArrayList<UserWorkout> userWorkouts = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, "DEBUG: Document ID: " + document.getId() + ", Data: " + document.getData());
                            try {
                                UserWorkout userWorkout = document.toObject(UserWorkout.class);
                                if (userWorkout != null) {
                                    userWorkout.setId(document.getId());
                                    userWorkouts.add(userWorkout);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "DEBUG: Error parsing document: " + e.getMessage());
                            }
                        }

                        activity.runOnUiThread(() -> {
                            listener.onUserWorkoutsLoaded(userWorkouts);
                        });
                    } else {
                        Log.e(TAG, "DEBUG: Error loading all user workouts: ", task.getException());
                        activity.runOnUiThread(() -> {
                            listener.onUserWorkoutsLoaded(new ArrayList<>());
                        });
                    }
                });
    }
    
    /**
     * Load a specific user workout by ID
     */
    public void loadUserWorkoutById(String userWorkoutId, AppCompatActivity activity, OnUserWorkoutLoadedListener listener) {
        Log.d(TAG, "=== LOADING USER WORKOUT BY ID ===");
        Log.d(TAG, "Collection: user_workouts");
        Log.d(TAG, "Document ID: " + userWorkoutId);
        
        if (userWorkoutId == null || userWorkoutId.isEmpty()) {
            Log.e(TAG, "UserWorkoutId is null or empty!");
            activity.runOnUiThread(() -> {
                listener.onUserWorkoutLoaded(null);
            });
            return;
        }
        
        db.collection("user_workouts")
                .document(userWorkoutId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "✓ Document exists in user_workouts collection");
                            try {
                                UserWorkout userWorkout = document.toObject(UserWorkout.class);
                                if (userWorkout != null) {
                                    userWorkout.setId(document.getId());
                                    Log.d(TAG, "✓ Successfully loaded user workout: " + userWorkout.getTitle());
                                    Log.d(TAG, "  - ID: " + userWorkout.getId());
                                    Log.d(TAG, "  - UID: " + userWorkout.getUid());
                                    Log.d(TAG, "  - Items count: " + (userWorkout.getItems() != null ? userWorkout.getItems().size() : 0));
                                    activity.runOnUiThread(() -> {
                                        listener.onUserWorkoutLoaded(userWorkout);
                                    });
                                } else {
                                    Log.e(TAG, "✗ UserWorkout object is null after parsing");
                                    activity.runOnUiThread(() -> {
                                        listener.onUserWorkoutLoaded(null);
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "✗ Error parsing user workout: " + e.getMessage(), e);
                                activity.runOnUiThread(() -> {
                                    listener.onUserWorkoutLoaded(null);
                                });
                            }
                        } else {
                            Log.w(TAG, "✗ User workout document does not exist in user_workouts collection");
                            Log.w(TAG, "  Document ID: " + userWorkoutId);
                            activity.runOnUiThread(() -> {
                                listener.onUserWorkoutLoaded(null);
                            });
                        }
                    } else {
                        Log.e(TAG, "✗ Error loading user workout from Firebase: ", task.getException());
                        if (task.getException() != null) {
                            Log.e(TAG, "  Exception message: " + task.getException().getMessage());
                        }
                        activity.runOnUiThread(() -> {
                            listener.onUserWorkoutLoaded(null);
                        });
                    }
                    Log.d(TAG, "=== END LOADING USER WORKOUT ===");
                });
    }
    
    /**
     * Load exercises for a user workout
     */
    public void loadExercisesForUserWorkout(UserWorkout userWorkout, AppCompatActivity activity, OnExercisesLoadedListener listener) {
        if (userWorkout == null || userWorkout.getItems() == null) {
            Log.e(TAG, "UserWorkout or items is null");
            activity.runOnUiThread(() -> {
                listener.onExercisesLoaded(new ArrayList<>());
            });
            return;
        }
        
        Log.d(TAG, "Loading exercises for user workout: " + userWorkout.getTitle());
        
        // Get all exercise IDs from user workout
        List<String> exerciseIds = new ArrayList<>();
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            exerciseIds.add(item.getExerciseId());
        }
        
        if (exerciseIds.isEmpty()) {
            Log.w(TAG, "No exercise IDs found in user workout");
            activity.runOnUiThread(() -> {
                listener.onExercisesLoaded(new ArrayList<>());
            });
            return;
        }
        
        // Load exercises by IDs
        loadExercisesByIds(exerciseIds, activity, listener);
    }
    
    /**
     * Load exercises by their IDs
     */
    /**
     * Load exercises by their IDs - Alternative approach using individual queries
     */
    public void loadExercisesByIds(List<String> exerciseIds, AppCompatActivity activity, OnExercisesLoadedListener listener) {
        Log.d(TAG, "Loading exercises by IDs: " + exerciseIds);
        
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            Log.e(TAG, "No exercise IDs provided");
            activity.runOnUiThread(() -> {
                listener.onExercisesLoaded(new ArrayList<>());
            });
            return;
        }
        
        ArrayList<Exercise> exercises = new ArrayList<>();
        final int[] loadedItems = {0};
        final int totalItems = exerciseIds.size();
        
        Log.d(TAG, "Starting to load " + totalItems + " exercises individually");
        
        for (String exerciseId : exerciseIds) {
            Log.d(TAG, "Loading exercise: " + exerciseId);
            
            db.collection("exercises")
                    .document(exerciseId)
                    .get()
                    .addOnCompleteListener(task -> {
                        loadedItems[0]++;
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Query for " + exerciseId + " successful");
                            
                            if (task.getResult().exists()) {
                                DocumentSnapshot document = task.getResult();
                                try {
                                    Exercise exercise = document.toObject(Exercise.class);
                                    if (exercise != null) {
                                        exercise.setId(document.getId());
                                        exercises.add(exercise);
                                        Log.d(TAG, "Successfully loaded exercise: " + exercise.getName() + " (ID: " + exercise.getId() + ")");
                                    } else {
                                        Log.e(TAG, "Exercise object is null for ID: " + exerciseId);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing exercise " + exerciseId + ": " + e.getMessage());
                                }
                            } else {
                                Log.e(TAG, "No document found for exercise ID: " + exerciseId);
                            }
                        } else {
                            Log.e(TAG, "Error loading exercise " + exerciseId + ": ", task.getException());
                        }
                        
                        // Check if all exercises are loaded
                        if (loadedItems[0] == totalItems) {
                            Log.d(TAG, "All exercises loaded. Final count: " + exercises.size());
                            activity.runOnUiThread(() -> {
                                Log.d(TAG, "Calling listener.onExercisesLoaded with " + exercises.size() + " exercises");
                                listener.onExercisesLoaded(exercises);
                            });
                        }
                    });
        }
    }

    // ===================== SCHEDULE METHODS (Firestore) =====================

    /**
     * Load user schedule from Firestore
     * Nếu có nhiều schedule documents, sẽ xóa các document cũ và chỉ giữ lại document mới nhất
     */
    public void loadUserSchedule(String uid, OnScheduleLoadedListener listener) {
        Log.d(TAG, "Loading schedule for userId: " + uid);
        
        db.collection("schedules")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Nếu có nhiều documents, xóa các document cũ và chỉ giữ lại document đầu tiên
                        if (queryDocumentSnapshots.size() > 1) {
                            Log.w(TAG, "Found " + queryDocumentSnapshots.size() + " schedule documents for user, cleaning up duplicates...");
                            
                            // Lấy document đầu tiên làm document chính
                            DocumentSnapshot mainDocument = queryDocumentSnapshots.getDocuments().get(0);
                            Schedule schedule = mainDocument.toObject(Schedule.class);
                            if (schedule != null) {
                                schedule.setId(mainDocument.getId());
                                
                                // Xóa các document còn lại
                                for (int i = 1; i < queryDocumentSnapshots.size(); i++) {
                                    DocumentSnapshot docToDelete = queryDocumentSnapshots.getDocuments().get(i);
                                    Log.d(TAG, "Deleting duplicate schedule document: " + docToDelete.getId());
                                    docToDelete.getReference().delete()
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted duplicate schedule: " + docToDelete.getId()))
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete duplicate schedule: " + docToDelete.getId(), e));
                                }
                                
                                Log.d(TAG, "Loaded schedule: " + schedule.getTitle() + " (ID: " + schedule.getId() + ")");
                                if (listener != null) {
                                    listener.onScheduleLoaded(schedule);
                                }
                            } else {
                                Log.w(TAG, "Schedule object is null");
                                if (listener != null) {
                                    listener.onScheduleLoaded(null);
                                }
                            }
                        } else {
                            // Chỉ có 1 document
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            Schedule schedule = document.toObject(Schedule.class);
                            if (schedule != null) {
                                schedule.setId(document.getId());
                                Log.d(TAG, "Loaded schedule: " + schedule.getTitle() + " (ID: " + schedule.getId() + ")");
                                if (listener != null) {
                                    listener.onScheduleLoaded(schedule);
                                }
                            } else {
                                Log.w(TAG, "Schedule object is null");
                                if (listener != null) {
                                    listener.onScheduleLoaded(null);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No schedule found for user");
                        if (listener != null) {
                            listener.onScheduleLoaded(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule", e);
                    if (listener != null) {
                        listener.onScheduleLoaded(null);
                    }
                });
    }

    /**
     * Save or update user schedule
     */
    public void saveSchedule(Schedule schedule, OnScheduleSavedListener listener) {
        Log.d(TAG, "Saving schedule: " + schedule.getTitle());
        
        if (schedule.getId() == null || schedule.getId().isEmpty()) {
            // Create new schedule
            db.collection("schedules")
                    .add(schedule)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Schedule created successfully: " + documentReference.getId());
                        schedule.setId(documentReference.getId());
                        if (listener != null) {
                            listener.onScheduleSaved(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating schedule", e);
                        if (listener != null) {
                            listener.onScheduleSaved(false);
                        }
                    });
        } else {
            // Update existing schedule
            db.collection("schedules")
                    .document(schedule.getId())
                    .set(schedule)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Schedule updated successfully");
                        if (listener != null) {
                            listener.onScheduleSaved(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating schedule", e);
                        if (listener != null) {
                            listener.onScheduleSaved(false);
                        }
                    });
        }
    }

    /**
     * Get formatted date string (yyyy-MM-dd)
     */
    public static String getFormattedDate(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * Get formatted date string for today
     */
    public static String getTodayFormattedDate() {
        return getFormattedDate(Calendar.getInstance());
    }

    // Interface for Schedule callbacks
    public interface OnScheduleLoadedListener {
        void onScheduleLoaded(Schedule schedule);
    }

    public interface OnScheduleSavedListener {
        void onScheduleSaved(boolean success);
    }

    // ===================== FAVORITE WORKOUT TEMPLATES METHODS =====================

    /**
     * Thêm workout template vào danh sách yêu thích của user
     * Lưu vào collection: users/{userId}/favorites/{workoutTemplateId}
     */
    public void addFavoriteWorkoutTemplate(String userId, String workoutTemplateId, OnFavoriteWorkoutUpdatedListener listener) {
        Log.d(TAG, "Thêm workout template vào yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        // Tạo Favorite object để lưu
        Favorite favorite = new Favorite(
            workoutTemplateId, // ID của document = workoutTemplateId
            userId, // ID của user
            System.currentTimeMillis() / 1000 // Timestamp dạng seconds
        );
        
        // Lưu vào Firestore
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(workoutTemplateId)
                .set(favorite)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã thêm workout template vào yêu thích thành công");
                    if (listener != null) {
                        listener.onFavoriteWorkoutUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi thêm workout template vào yêu thích: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onFavoriteWorkoutUpdated(false);
                    }
                });
    }

    /**
     * Xóa workout template khỏi danh sách yêu thích của user
     */
    public void removeFavoriteWorkoutTemplate(String userId, String workoutTemplateId, OnFavoriteWorkoutUpdatedListener listener) {
        Log.d(TAG, "Xóa workout template khỏi yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(workoutTemplateId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã xóa workout template khỏi yêu thích thành công");
                    if (listener != null) {
                        listener.onFavoriteWorkoutUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi xóa workout template khỏi yêu thích: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onFavoriteWorkoutUpdated(false);
                    }
                });
    }

    /**
     * Kiểm tra xem workout template có trong danh sách yêu thích không
     */
    public void checkFavoriteWorkoutTemplate(String userId, String workoutTemplateId, OnFavoriteWorkoutCheckedListener listener) {
        Log.d(TAG, "Kiểm tra workout template yêu thích: " + workoutTemplateId + " cho user: " + userId);
        
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(workoutTemplateId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isFavorite = documentSnapshot.exists();
                    Log.d(TAG, "✓ Workout template " + (isFavorite ? "có" : "không có") + " trong yêu thích");
                    if (listener != null) {
                        listener.onFavoriteWorkoutChecked(isFavorite);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi kiểm tra workout template yêu thích: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onFavoriteWorkoutChecked(false);
                    }
                });
    }

    /**
     * Load tất cả favorite workout template IDs của user
     */
    public void loadFavoriteWorkoutTemplateIds(String userId, OnFavoriteWorkoutTemplateIdsLoadedListener listener) {
        Log.d(TAG, "Load danh sách favorite workout template IDs cho user: " + userId);
        
        db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> favoriteIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Chuyển đổi document thành Favorite object
                            Favorite favorite = document.toObject(Favorite.class);
                            if (favorite != null) {
                                // Lấy workoutTemplateId từ object
                                String workoutTemplateId = favorite.getWorkoutTemplateId();
                                if (workoutTemplateId != null && !workoutTemplateId.isEmpty()) {
                                    favoriteIds.add(workoutTemplateId);
                                } else {
                                    // Fallback: sử dụng document ID nếu không có field workoutTemplateId
                                    favoriteIds.add(document.getId());
                                }
                            } else {
                                // Fallback: nếu không parse được object, sử dụng document ID
                                favoriteIds.add(document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "✗ Lỗi parse favorite document: " + document.getId(), e);
                            // Fallback: sử dụng document ID
                            favoriteIds.add(document.getId());
                        }
                    }
                    Log.d(TAG, "✓ Đã load " + favoriteIds.size() + " favorite workout template IDs");
                    if (listener != null) {
                        listener.onFavoriteWorkoutTemplateIdsLoaded(favoriteIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi load favorite workout template IDs: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onFavoriteWorkoutTemplateIdsLoaded(new ArrayList<>());
                    }
                });
    }

    /**
     * Load tất cả collections từ Firebase
     */
    public void loadCollections(AppCompatActivity activity, OnCollectionsLoadedListener listener) {
        Log.d(TAG, "Loading collections from Firebase...");
        
        db.collection("collections")
                .whereEqualTo("isPublic", true)
                .orderBy("order")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<fpt.fall2025.posetrainer.Domain.Collection> collections = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                fpt.fall2025.posetrainer.Domain.Collection collection = document.toObject(fpt.fall2025.posetrainer.Domain.Collection.class);
                                if (collection != null) {
                                    collection.setId(document.getId());
                                    collections.add(collection);
                                    Log.d(TAG, "Loaded collection: " + collection.getTitle() + " (ID: " + document.getId() + ")");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing collection: " + e.getMessage());
                            }
                        }
                        
                        Log.d(TAG, "Total collections loaded: " + collections.size());
                        activity.runOnUiThread(() -> {
                            listener.onCollectionsLoaded(collections);
                        });
                    } else {
                        Log.e(TAG, "Error getting collections: ", task.getException());
                        activity.runOnUiThread(() -> {
                            listener.onCollectionsLoaded(new ArrayList<>());
                        });
                    }
                });
    }

    /**
     * Load tất cả favorite workout templates của user (load full WorkoutTemplate objects)
     */
    public void loadFavoriteWorkoutTemplates(String userId, AppCompatActivity activity, OnWorkoutTemplatesLoadedListener listener) {
        Log.d(TAG, "Load favorite workout templates cho user: " + userId);
        
        // Bước 1: Load danh sách favorite IDs
        loadFavoriteWorkoutTemplateIds(userId, favoriteIds -> {
            if (favoriteIds == null || favoriteIds.isEmpty()) {
                Log.d(TAG, "Không có favorite workout templates");
                activity.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onWorkoutTemplatesLoaded(new ArrayList<>());
                    }
                });
                return;
            }
            
            // Bước 2: Load từng workout template từ workouts_templates collection
            ArrayList<WorkoutTemplate> favoriteTemplates = new ArrayList<>();
            final int[] loadedCount = {0};
            final int totalCount = favoriteIds.size();
            
            for (String workoutTemplateId : favoriteIds) {
                loadWorkoutTemplateById(workoutTemplateId, activity, template -> {
                    if (template != null) {
                        favoriteTemplates.add(template);
                    }
                    loadedCount[0]++;
                    
                    // Khi đã load xong tất cả
                    if (loadedCount[0] == totalCount) {
                        Log.d(TAG, "✓ Đã load " + favoriteTemplates.size() + " favorite workout templates");
                        activity.runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onWorkoutTemplatesLoaded(favoriteTemplates);
                            }
                        });
                    }
                });
            }
        });
    }

    // Interfaces cho favorite workout templates
    public interface OnFavoriteWorkoutUpdatedListener {
        void onFavoriteWorkoutUpdated(boolean success);
    }

    public interface OnFavoriteWorkoutCheckedListener {
        void onFavoriteWorkoutChecked(boolean isFavorite);
    }

    public interface OnFavoriteWorkoutTemplateIdsLoadedListener {
        void onFavoriteWorkoutTemplateIdsLoaded(ArrayList<String> favoriteIds);
    }

    // ===================== NOTIFICATION METHODS (Firestore) =====================

    /**
     * Save notification to Firestore
     */
    public void saveNotification(Notification notification, OnNotificationSavedListener listener) {
        Log.d(TAG, "Saving notification: " + notification.getTitle());
        
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification saved successfully: " + documentReference.getId());
                    notification.setId(documentReference.getId());
                    if (listener != null) {
                        listener.onNotificationSaved(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving notification", e);
                    if (listener != null) {
                        listener.onNotificationSaved(false);
                    }
                });
    }

    /**
     * Load notifications for a user
     */
    public void loadUserNotifications(String uid, OnNotificationsLoadedListener listener) {
        Log.d(TAG, "=== LOADING NOTIFICATIONS ===");
        Log.d(TAG, "Loading notifications for userId: " + uid);
        
        // Query với whereEqualTo + orderBy → Cần Firestore index
        db.collection("notifications")
                .whereEqualTo("uid", uid)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✓ Query thành công, số documents: " + queryDocumentSnapshots.size());
                    
                    ArrayList<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Notification notification = document.toObject(Notification.class);
                            if (notification != null) {
                                notification.setId(document.getId());
                                notifications.add(notification);
                                Log.d(TAG, "✓ Loaded notification: " + notification.getTitle() + " (ID: " + notification.getId() + ")");
                            } else {
                                Log.w(TAG, "⚠ Notification object is null cho document: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "✗ Error parsing notification: " + e.getMessage(), e);
                        }
                    }
                    
                    Log.d(TAG, "=== TỔNG KẾT ===");
                    Log.d(TAG, "Đã load thành công: " + notifications.size() + " notifications");
                    
                    if (listener != null) {
                        listener.onNotificationsLoaded(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗✗✗ LỖI LOAD NOTIFICATIONS ✗✗✗");
                    Log.e(TAG, "Error message: " + e.getMessage());
                    Log.e(TAG, "Error class: " + e.getClass().getName());
                    
                    // Kiểm tra xem có phải lỗi thiếu index không
                    if (e.getMessage() != null && e.getMessage().contains("index")) {
                        Log.e(TAG, "⚠⚠⚠ THIẾU FIRESTORE INDEX ⚠⚠⚠");
                        Log.e(TAG, "Query cần index: notifications collection với fields: uid + sentAt");
                        Log.e(TAG, "Truy cập Firebase Console để tạo index hoặc check link trong error message");
                        
                        // Thử query đơn giản hơn (không có orderBy) để vẫn load được
                        Log.d(TAG, "Đang thử load không có orderBy...");
                        db.collection("notifications")
                                .whereEqualTo("uid", uid)
                                .limit(50)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    Log.d(TAG, "✓ Load thành công (không orderBy), số documents: " + queryDocumentSnapshots.size());
                                    
                                    ArrayList<Notification> notifications = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        try {
                                            Notification notification = document.toObject(Notification.class);
                                            if (notification != null) {
                                                notification.setId(document.getId());
                                                notifications.add(notification);
                                            }
                                        } catch (Exception ex) {
                                            Log.e(TAG, "Error parsing notification: " + ex.getMessage());
                                        }
                                    }
                                    
                                    // Sort ở client-side
                                    notifications.sort((n1, n2) -> Long.compare(n2.getSentAt(), n1.getSentAt()));
                                    
                                    Log.d(TAG, "✓ Đã load " + notifications.size() + " notifications (sorted client-side)");
                                    
                                    if (listener != null) {
                                        listener.onNotificationsLoaded(notifications);
                                    }
                                })
                                .addOnFailureListener(e2 -> {
                                    Log.e(TAG, "✗ Lỗi load notifications (fallback): " + e2.getMessage());
                                    if (listener != null) {
                                        listener.onNotificationsLoaded(new ArrayList<>());
                                    }
                                });
                    } else {
                        // Lỗi khác
                        Log.e(TAG, "✗ Lỗi khác: " + e.getMessage(), e);
                        if (listener != null) {
                            listener.onNotificationsLoaded(new ArrayList<>());
                        }
                    }
                });
    }

    public interface OnNotificationSavedListener {
        void onNotificationSaved(boolean success);
    }

    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(ArrayList<Notification> notifications);
    }

    // ===================== AI NOTIFICATION METHODS =====================

    /**
     * Đánh dấu thông báo là đã đọc
     */
    public void markNotificationAsRead(String notificationId, OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Đánh dấu thông báo đã đọc: " + notificationId);
        
        db.collection("notifications")
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã đánh dấu thông báo là đã đọc");
                    if (listener != null) {
                        listener.onNotificationUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi đánh dấu thông báo: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationUpdated(false);
                    }
                });
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     */
    public void markAllNotificationsAsRead(String uid, OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Đánh dấu tất cả thông báo đã đọc cho user: " + uid);
        
        db.collection("notifications")
                .whereEqualTo("uid", uid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Không có thông báo chưa đọc");
                        if (listener != null) {
                            listener.onNotificationUpdated(true);
                        }
                        return;
                    }
                    
                    // Cập nhật từng thông báo
                    int totalNotifications = queryDocumentSnapshots.size();
                    final int[] updatedCount = {0};
                    
                    queryDocumentSnapshots.forEach(document -> {
                        document.getReference().update("read", true)
                            .addOnCompleteListener(task -> {
                                updatedCount[0]++;
                                if (updatedCount[0] == totalNotifications) {
                                    Log.d(TAG, "✓ Đã đánh dấu " + totalNotifications + " thông báo là đã đọc");
                                    if (listener != null) {
                                        listener.onNotificationUpdated(true);
                                    }
                                }
                            });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi đánh dấu thông báo: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationUpdated(false);
                    }
                });
    }

    /**
     * Gửi feedback cho thông báo AI (accepted, ignored, dismissed)
     */
    public void sendNotificationFeedback(String notificationId, String feedback, OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Gửi feedback cho thông báo: " + notificationId + ", feedback: " + feedback);
        
        db.collection("notifications")
                .document(notificationId)
                .update("feedback", feedback)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã gửi feedback thành công");
                    if (listener != null) {
                        listener.onNotificationUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi gửi feedback: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationUpdated(false);
                    }
                });
    }

    /**
     * Xóa thông báo
     */
    public void deleteNotification(String notificationId, OnNotificationDeletedListener listener) {
        Log.d(TAG, "Xóa thông báo: " + notificationId);
        
        db.collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã xóa thông báo thành công");
                    if (listener != null) {
                        listener.onNotificationDeleted(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi xóa thông báo: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationDeleted(false);
                    }
                });
    }

    /**
     * Đếm số thông báo chưa đọc của user
     */
    public void countUnreadNotifications(String uid, OnUnreadCountLoadedListener listener) {
        Log.d(TAG, "Đếm thông báo chưa đọc cho user: " + uid);
        
        db.collection("notifications")
                .whereEqualTo("uid", uid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    Log.d(TAG, "✓ Có " + count + " thông báo chưa đọc");
                    if (listener != null) {
                        listener.onUnreadCountLoaded(count);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi đếm thông báo: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onUnreadCountLoaded(0);
                    }
                });
    }

    /**
     * Load thông báo AI (chỉ lấy thông báo do AI tạo)
     */
    public void loadAiNotifications(String uid, OnNotificationsLoadedListener listener) {
        Log.d(TAG, "Loading AI notifications for userId: " + uid);
        
        db.collection("notifications")
                .whereEqualTo("uid", uid)
                .whereEqualTo("isAiGenerated", true)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Notification notification = document.toObject(Notification.class);
                            if (notification != null) {
                                notification.setId(document.getId());
                                notifications.add(notification);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing AI notification: " + e.getMessage());
                        }
                    }
                    Log.d(TAG, "Loaded " + notifications.size() + " AI notifications");
                    if (listener != null) {
                        listener.onNotificationsLoaded(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading AI notifications", e);
                    if (listener != null) {
                        listener.onNotificationsLoaded(new ArrayList<>());
                    }
                });
    }

    /**
     * Cập nhật FCM token cho user
     */
    public void updateFcmToken(String uid, String fcmToken, OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Cập nhật FCM token cho user: " + uid);
        
        db.collection("users")
                .document(uid)
                .update("notification.fcmToken", fcmToken)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã cập nhật FCM token thành công");
                    if (listener != null) {
                        listener.onNotificationUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi cập nhật FCM token: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationUpdated(false);
                    }
                });
    }

    /**
     * Cập nhật cài đặt thông báo AI cho user
     */
    public void updateAiNotificationSettings(String uid, Map<String, Object> settings, OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Cập nhật cài đặt AI notification cho user: " + uid);
        
        // Tạo map để update
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            updates.put("notification." + entry.getKey(), entry.getValue());
        }
        
        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã cập nhật cài đặt thành công");
                    if (listener != null) {
                        listener.onNotificationUpdated(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi cập nhật cài đặt: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onNotificationUpdated(false);
                    }
                });
    }

    // Interfaces cho AI notifications
    public interface OnNotificationUpdatedListener {
        void onNotificationUpdated(boolean success);
    }

    public interface OnNotificationDeletedListener {
        void onNotificationDeleted(boolean success);
    }

    public interface OnUnreadCountLoadedListener {
        void onUnreadCountLoaded(int count);
    }

    // ===================== STREAK METHODS =====================

    /**
     * Kiểm tra session đã completed chưa
     * QUAN TRỌNG: Chỉ tính streak khi TẤT CẢ PerExercise có state = "completed"
     */
    private boolean isSessionCompleted(Session session) {
        if (session == null) {
            Log.d(TAG, "ℹ️ Session là null");
            return false;
        }
        
        // Kiểm tra 1: Session phải có endedAt > 0 (đã kết thúc)
        if (session.getEndedAt() == 0 || session.getEndedAt() <= session.getStartedAt()) {
            Log.d(TAG, "ℹ️ Session chưa kết thúc (endedAt = " + session.getEndedAt() + ", startedAt = " + session.getStartedAt() + ")");
            return false;
        }
        
        // Kiểm tra 2: QUAN TRỌNG - Tất cả PerExercise phải có state = "completed"
        if (session.getPerExercise() == null || session.getPerExercise().isEmpty()) {
            Log.d(TAG, "ℹ️ Session không có PerExercise nào");
            return false;
        }
        
        int totalExercises = session.getPerExercise().size();
        int completedExercises = 0;
        
        for (Session.PerExercise perExercise : session.getPerExercise()) {
            String exerciseState = perExercise.getState();
            if (exerciseState == null || !"completed".equals(exerciseState)) {
                Log.d(TAG, "⚠️ Session chưa completed: Exercise #" + perExercise.getExerciseNo() + 
                    " có state = '" + (exerciseState != null ? exerciseState : "null") + 
                    "' (cần 'completed'). Đã completed: " + completedExercises + "/" + totalExercises);
                return false;
            }
            completedExercises++;
        }
        
        Log.d(TAG, "✅ Session đã completed: endedAt > 0 và TẤT CẢ " + totalExercises + " exercises đã completed (state = 'completed')");
        return true;
    }

    /**
     * Update user streak based on new session
     * CHỈ cập nhật streak khi session đã completed
     */
    public void updateStreak(String uid, Session session, OnStreakUpdatedListener listener) {
        if (session == null || session.getStartedAt() == 0) {
            Log.w(TAG, "⚠️ Không thể cập nhật streak: session là null hoặc không hợp lệ");
            if (listener != null) {
                listener.onStreakUpdated(null);
            }
            return;
        }

        // QUAN TRỌNG: Chỉ cập nhật streak khi TẤT CẢ PerExercise có state = "completed"
        if (!isSessionCompleted(session)) {
            Log.w(TAG, "⚠️ Không cập nhật streak: session chưa completed - không phải tất cả PerExercise có state = 'completed'");
            if (listener != null) {
                listener.onStreakUpdated(null);
            }
            return;
        }

        // Convert startedAt (seconds) to Calendar and then to "yyyy-MM-dd"
        Calendar sessionDate = Calendar.getInstance();
        sessionDate.setTimeInMillis(session.getStartedAt() * 1000L);
        sessionDate.set(Calendar.HOUR_OF_DAY, 0);
        sessionDate.set(Calendar.MINUTE, 0);
        sessionDate.set(Calendar.SECOND, 0);
        sessionDate.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String workoutDate = dateFormat.format(sessionDate.getTime());

        Log.d(TAG, "🔥 Đang cập nhật streak cho user: " + uid + ", ngày tập: " + workoutDate + " (session đã completed)");

        // Load current streak
        db.collection("streaks")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Streak streak;
                    if (documentSnapshot.exists()) {
                        streak = documentSnapshot.toObject(Streak.class);
                        if (streak == null) {
                            streak = new Streak(uid, 0, 0, null);
                        }
                    } else {
                        streak = new Streak(uid, 0, 0, null);
                    }

                    String lastWorkoutDate = streak.getLastWorkoutDate();
                    
                    if (lastWorkoutDate == null || lastWorkoutDate.isEmpty()) {
                        // First workout
                        streak.setCurrentStreak(1);
                        streak.setLongestStreak(1);
                        streak.setLastWorkoutDate(workoutDate);
                    } else if (lastWorkoutDate.equals(workoutDate)) {
                        // Same day - don't increase streak
                        Log.d(TAG, "📅 Cùng ngày tập, streak không thay đổi: " + streak.getCurrentStreak());
                    } else {
                        // Different day
                        try {
                            Calendar lastDate = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            lastDate.setTime(sdf.parse(lastWorkoutDate));
                            lastDate.set(Calendar.HOUR_OF_DAY, 0);
                            lastDate.set(Calendar.MINUTE, 0);
                            lastDate.set(Calendar.SECOND, 0);
                            lastDate.set(Calendar.MILLISECOND, 0);

                            long diffInMillis = sessionDate.getTimeInMillis() - lastDate.getTimeInMillis();
                            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

                            if (diffInDays == 1) {
                                // Consecutive day - increase streak
                                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                                Log.d(TAG, "🔥 Ngày liên tiếp, streak tăng lên: " + streak.getCurrentStreak());
                            } else if (diffInDays >= 2) {
                                // Gap of 2+ days - reset streak
                                streak.setCurrentStreak(1);
                                Log.d(TAG, "⚠️ Cách " + diffInDays + " ngày, streak reset về 1");
                            }

                            // Update longest streak if needed
                            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                                streak.setLongestStreak(streak.getCurrentStreak());
                            }

                            streak.setLastWorkoutDate(workoutDate);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Lỗi khi parse ngày tập cuối", e);
                            // Fallback: treat as new streak
                            streak.setCurrentStreak(1);
                            streak.setLastWorkoutDate(workoutDate);
                        }
                    }

                    // Create final reference for lambda
                    final Streak finalStreak = streak;
                    
                    // Đảm bảo uid được set đúng (quan trọng cho Firestore rules)
                    if (finalStreak.getUid() == null || finalStreak.getUid().isEmpty()) {
                        finalStreak.setUid(uid);
                        Log.d(TAG, "🔧 Set streak UID thành: " + uid);
                    } else if (!finalStreak.getUid().equals(uid)) {
                        Log.w(TAG, "⚠️ Streak UID không khớp: " + finalStreak.getUid() + " != " + uid + ", đang cập nhật...");
                        finalStreak.setUid(uid);
                    }

                    // Save updated streak
                    db.collection("streaks")
                            .document(uid)
                            .set(finalStreak)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Cập nhật streak thành công: hiện tại=" + finalStreak.getCurrentStreak() + 
                                    " ngày, dài nhất=" + finalStreak.getLongestStreak() + " ngày, uid=" + finalStreak.getUid());
                                if (listener != null) {
                                    listener.onStreakUpdated(finalStreak);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Lỗi khi lưu streak", e);
                                Log.e(TAG, "📋 Mã lỗi: " + (e instanceof com.google.firebase.firestore.FirebaseFirestoreException 
                                    ? ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() 
                                    : "Không xác định"));
                                Log.e(TAG, "📋 Chi tiết lỗi: " + e.getMessage());
                                if (listener != null) {
                                    listener.onStreakUpdated(null);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi tải streak", e);
                    // Create new streak if load fails
                    Streak newStreak = new Streak(uid, 1, 1, workoutDate);
                    db.collection("streaks")
                            .document(uid)
                            .set(newStreak)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Tạo streak mới thành công sau khi load lỗi");
                                if (listener != null) {
                                    listener.onStreakUpdated(newStreak);
                                }
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "❌ Lỗi khi tạo streak mới", e2);
                                if (listener != null) {
                                    listener.onStreakUpdated(null);
                                }
                            });
                });
    }

    /**
     * Load user streak
     */
    public void loadUserStreak(String uid, OnStreakLoadedListener listener) {
        Log.d(TAG, "📥 Đang tải streak cho user: " + uid);
        
        db.collection("streaks")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Streak streak = documentSnapshot.toObject(Streak.class);
                        if (streak != null) {
                            Log.d(TAG, "✅ Đã tải streak: hiện tại=" + streak.getCurrentStreak() + 
                                " ngày, dài nhất=" + streak.getLongestStreak() + " ngày");
                            if (listener != null) {
                                listener.onStreakLoaded(streak);
                            }
                        } else {
                            Log.w(TAG, "⚠️ Streak object là null");
                            if (listener != null) {
                                listener.onStreakLoaded(null);
                            }
                        }
                    } else {
                        Log.d(TAG, "ℹ️ Không tìm thấy streak cho user này");
                        if (listener != null) {
                            listener.onStreakLoaded(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi tải streak", e);
                    if (listener != null) {
                        listener.onStreakLoaded(null);
                    }
                });
    }

    public interface OnStreakUpdatedListener {
        void onStreakUpdated(Streak streak);
    }

    public interface OnStreakLoadedListener {
        void onStreakLoaded(Streak streak);
    }

    // ===================== USER PROGRESS METHODS =====================

    /**
     * Update user progress (calendar heatmap)
     */
    public void updateUserProgress(String uid, OnProgressUpdatedListener listener) {
        Log.d(TAG, "Updating user progress for: " + uid);

        // Query all sessions of user
        db.collection("sessions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Boolean> calendar = new HashMap<>();
                    int totalSessions = queryDocumentSnapshots.size();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    // Group sessions by date
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Session session = document.toObject(Session.class);
                        if (session != null && session.getStartedAt() > 0) {
                            // Convert startedAt (seconds) to Calendar and then to "yyyy-MM-dd"
                            Calendar sessionDate = Calendar.getInstance();
                            sessionDate.setTimeInMillis(session.getStartedAt() * 1000L);
                            sessionDate.set(Calendar.HOUR_OF_DAY, 0);
                            sessionDate.set(Calendar.MINUTE, 0);
                            sessionDate.set(Calendar.SECOND, 0);
                            sessionDate.set(Calendar.MILLISECOND, 0);

                            String dateKey = dateFormat.format(sessionDate.getTime());
                            calendar.put(dateKey, true);
                        }
                    }

                    int totalWorkoutDays = calendar.size();

                    // Create or update UserProgress
                    UserProgress progress = new UserProgress(uid, totalWorkoutDays, totalSessions, calendar);

                    // Save to Firestore
                    db.collection("user_progress")
                            .document(uid)
                            .set(progress)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User progress updated successfully: " + totalWorkoutDays + " days, " + totalSessions + " sessions");
                                if (listener != null) {
                                    listener.onProgressUpdated(progress);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user progress", e);
                                if (listener != null) {
                                    listener.onProgressUpdated(null);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sessions for user progress", e);
                    if (listener != null) {
                        listener.onProgressUpdated(null);
                    }
                });
    }

    /**
     * Load user progress
     */
    public void loadUserProgress(String uid, OnProgressLoadedListener listener) {
        Log.d(TAG, "Loading user progress for: " + uid);

        db.collection("user_progress")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProgress progress = documentSnapshot.toObject(UserProgress.class);
                        if (progress != null) {
                            Log.d(TAG, "User progress loaded: " + progress.getTotalWorkoutDays() + " days, " + progress.getTotalSessions() + " sessions");
                            if (listener != null) {
                                listener.onProgressLoaded(progress);
                            }
                        } else {
                            Log.w(TAG, "UserProgress object is null");
                            if (listener != null) {
                                listener.onProgressLoaded(null);
                            }
                        }
                    } else {
                        Log.d(TAG, "No user progress found");
                        if (listener != null) {
                            listener.onProgressLoaded(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user progress", e);
                    if (listener != null) {
                        listener.onProgressLoaded(null);
                    }
                });
    }

    public interface OnProgressUpdatedListener {
        void onProgressUpdated(UserProgress progress);
    }

    public interface OnProgressLoadedListener {
        void onProgressLoaded(UserProgress progress);
    }

    // ===================== ACHIEVEMENT METHODS =====================

    /**
     * Load user achievements
     */
    public void loadUserAchievements(String uid, OnAchievementsLoadedListener listener) {
        Log.d(TAG, "Loading achievements for: " + uid);

        db.collection("achievements")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Achievement achievement = documentSnapshot.toObject(Achievement.class);
                        if (achievement != null) {
                            Log.d(TAG, "Achievements loaded: " + (achievement.getBadges() != null ? achievement.getBadges().size() : 0) + " badges");
                            if (listener != null) {
                                listener.onAchievementsLoaded(achievement);
                            }
                        } else {
                            Log.w(TAG, "Achievement object is null");
                            if (listener != null) {
                                listener.onAchievementsLoaded(null);
                            }
                        }
                    } else {
                        Log.d(TAG, "No achievements found");
                        if (listener != null) {
                            listener.onAchievementsLoaded(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading achievements", e);
                    if (listener != null) {
                        listener.onAchievementsLoaded(null);
                    }
                });
    }

    public interface OnAchievementsLoadedListener {
        void onAchievementsLoaded(Achievement achievement);
    }
}
