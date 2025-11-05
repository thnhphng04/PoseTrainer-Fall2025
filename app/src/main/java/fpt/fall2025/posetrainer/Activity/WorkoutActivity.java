package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import fpt.fall2025.posetrainer.Adapter.ExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityWorkoutBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkoutActivity extends AppCompatActivity implements ExerciseAdapter.OnSetsRepsChangedListener, ExerciseAdapter.OnDifficultyChangedListener {
    private static final String TAG = "WorkoutActivity";
    ActivityWorkoutBinding binding;
    private WorkoutTemplate workoutTemplate;
    private ArrayList<Exercise> exercises;
    private int[] exerciseSets;
    private int[] exerciseReps;
    private String[] exerciseDifficulties;
    private Session currentSession;
    private boolean hasActiveSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize data
        exercises = new ArrayList<>();
        exerciseSets = new int[10]; // Max 10 exercises
        exerciseReps = new int[10]; // Max 10 exercises
        exerciseDifficulties = new String[10]; // Max 10 exercises
        
        // Initialize default values - will be updated when workout template is loaded
        for (int i = 0; i < 10; i++) {
            exerciseSets[i] = 3; // Default 3 sets
            exerciseReps[i] = 12; // Default 12 reps
            exerciseDifficulties[i] = "beginner"; // Default beginner
        }

        // Get workout template ID from intent
        String workoutTemplateId = getIntent().getStringExtra("workoutTemplateId");
        
        if (workoutTemplateId != null) {
            loadWorkoutTemplateById(workoutTemplateId);
        } else {
            Toast.makeText(this, "No workout template ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupUI();
    }

    private void setupUI() {
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // Start Workout button
        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWorkout();
            }
        });
        
        // More options button
        binding.moreOptionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptionsMenu(v);
            }
        });
    }
    
    /**
     * Show more options popup menu
     */
    private void showMoreOptionsMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_workout, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_edit_workout) {
                    openEditWorkoutActivity();
                    return true;
                }
                return false;
            }
        });
        
        // Set gravity to ensure menu appears on the left side of the button
        popupMenu.setGravity(Gravity.END);
        popupMenu.show();
    }
    
    /**
     * Open EditWorkoutActivity
     */
    private void openEditWorkoutActivity() {
        if (workoutTemplate == null) {
            Toast.makeText(this, "Workout template not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, EditWorkoutActivity.class);
        intent.putExtra("workoutTemplateId", workoutTemplate.getId());
        startActivity(intent);
    }
    
    private void startWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "No exercises available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if coming from MainActivity
        boolean isFromMainActivity = getIntent().getBooleanExtra("fromMainActivity", true);
        
        if (hasActiveSession && !isFromMainActivity) {
            // Có session đang diễn ra VÀ không phải từ MainActivity - resume workout
            resumeWorkout();
        } else {
            // Không có session HOẶC từ MainActivity - tạo session mới
            createWorkoutSession();
        }
    }

    /**
     * Resume workout hiện tại
     */
    private void resumeWorkout() {
        Log.d(TAG, "Resuming existing workout session");
        
        // Sync session với database trước khi resume
        syncSessionBeforeResume();
    }
    
    /**
     * Sync session với database trước khi resume để đảm bảo có data mới nhất
     */
    private void syncSessionBeforeResume() {
        Log.d(TAG, "Syncing session with database before resume");
        
        // Nếu có currentSession, load lại từ Firebase bằng ID
        if (currentSession != null && currentSession.getId() != null) {
            Log.d(TAG, "Reloading current session by ID: " + currentSession.getId());
            FirebaseService.getInstance().loadSessionById(currentSession.getId(), new FirebaseService.OnSessionLoadedListener() {
                @Override
                public void onSessionLoaded(Session syncedSession) {
                    if (syncedSession != null) {
                        currentSession = syncedSession;
                        Log.d(TAG, "Session synced before resume: " + syncedSession.getId());
                        
                        // Tìm exercise đầu tiên có state "doing" hoặc "not_started"
                        List<Session.PerExercise> perExercises = currentSession.getPerExercise();
                        int exerciseIndex = 0;
                        
                        for (int i = 0; i < perExercises.size(); i++) {
                            String state = perExercises.get(i).getState();
                            if ("doing".equals(state) || "not_started".equals(state)) {
                                exerciseIndex = i;
                                break;
                            }
                        }
                        
                        // Start ExerciseActivity với session đã sync
                        startWorkoutWithExistingSession(exerciseIndex);
                    } else {
                        Log.e(TAG, "Failed to reload session by ID, creating new session");
                        createWorkoutSession();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error reloading session by ID: " + error);
                    createWorkoutSession();
                }
            });
        } else {
            Log.e(TAG, "No current session to reload, creating new session");
            createWorkoutSession();
        }
    }
    
    /**
     * Resume workout with existing session - Navigate to SessionActivity
     */
    private void startWorkoutWithExistingSession(int exerciseIndex) {
        // Navigate to SessionActivity instead of directly to ExerciseActivity
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra("sessionId", currentSession.getId());
        intent.putExtra("isResume", true); // Flag để phân biệt resume vs new workout
        
        startActivityForResult(intent, 1001);
    }
    
    /**
     * Force tạo session mới (bỏ qua session cũ)
     */
    private void startNewWorkout() {
        Log.d(TAG, "Starting new workout (ignoring existing session)");
        
        // Reset session state
        currentSession = null;
        
        // Tạo session mới
        createWorkoutSession();
    }
    
    /**
     * Create a new workout session and start the workout
     */
    private void createWorkoutSession() {
        Log.d(TAG, "Creating new workout session");
        
        // Generate unique session ID
        String sessionId = "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Create session object
        currentSession = new Session();
        currentSession.setId(sessionId);
        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentSession.setUid(currentUser.getUid());
        } else {
            Log.w(TAG, "No user logged in, using default UID");
            currentSession.setUid("uid_1"); // Fallback
        }
        currentSession.setTitle(workoutTemplate.getTitle());
        currentSession.setDescription(workoutTemplate.getDescription());
        currentSession.setStartedAt(System.currentTimeMillis() / 1000); // Convert to seconds
        currentSession.setEndedAt(0); // Will be set when workout is completed
        
        // Initialize session summary
        Session.SessionSummary summary = new Session.SessionSummary();
        summary.setDurationSec(0);
        summary.setEstKcal(0);
        currentSession.setSummary(summary);
        
        // Initialize per-exercise data
            ArrayList<Session.PerExercise> perExercises = new ArrayList<>();
            for (int i = 0; i < exercises.size(); i++) {
                Session.PerExercise perExercise = new Session.PerExercise();
                perExercise.setExerciseNo(i + 1); // exerciseNo starts from 1
                perExercise.setExerciseId(exercises.get(i).getId());
                perExercise.setDifficultyUsed(exerciseDifficulties[i]);
                perExercise.setState("not_started"); // Initial state
                
                // Tạo SetData ban đầu cho tất cả sets
                ArrayList<Session.SetData> initialSets = new ArrayList<>();
                for (int setNo = 1; setNo <= exerciseSets[i]; setNo++) {
                    Session.SetData setData = new Session.SetData();
                    setData.setSetNo(setNo);
                    setData.setTargetReps(exerciseReps[i]);
                    setData.setCorrectReps(0);
                    setData.setState("incomplete"); // Initial state
                    initialSets.add(setData);
                }
                perExercise.setSets(initialSets);
                perExercise.setMedia(new Session.ExerciseMedia());
                perExercises.add(perExercise);
            }
        currentSession.setPerExercise(perExercises);
        
        // Initialize session flags
        Session.SessionFlags flags = new Session.SessionFlags();
        flags.setUploaded(false);
        flags.setExportable(true);
        currentSession.setFlags(flags);
        
        // Initialize device info
        Session.DeviceInfo deviceInfo = new Session.DeviceInfo();
        deviceInfo.setModel(android.os.Build.MODEL);
        deviceInfo.setOs("Android " + android.os.Build.VERSION.RELEASE);
        currentSession.setDeviceInfo(deviceInfo);
        
        currentSession.setAppVersion("1.0.0");
        
        Log.d(TAG, "Session created with ID: " + sessionId);
        
        // Set hasActiveSession to true
        hasActiveSession = true;
        
        // Save session to Firebase (optional - can be saved when workout is completed)
        saveSessionToFirebase();
        
        // Start workout with session
        startWorkoutWithSession();
    }
    
    /**
     * Save session to Firebase
     */
    private void saveSessionToFirebase() {
        if (currentSession != null) {
            FirebaseService.getInstance().saveSession(currentSession, new FirebaseService.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    if (success) {
                        Log.d(TAG, "Session saved to Firebase successfully");
                    } else {
                        Log.e(TAG, "Failed to save session to Firebase");
                    }
                }
            });
        }
    }
    
    /**
     * Start workout with the created session - Navigate to SessionActivity
     */
    private void startWorkoutWithSession() {
        // Navigate to SessionActivity instead of directly to ExerciseActivity
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra("sessionId", currentSession.getId());
        
        startActivityForResult(intent, 1001); // Use startActivityForResult to handle completion
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                // Exercise completed, check if there are more exercises
                int completedIndex = data.getIntExtra("completedIndex", 0);
                if (completedIndex + 1 < exercises.size()) {
                    // Show rest screen and next exercise preview
                    showRestScreen(completedIndex + 1);
                } else {
                    // All exercises completed
                    completeWorkoutSession();
                    hasActiveSession = false; // Reset session state
                    updateButtonUI(); // Update button back to "Start Workout"
                    Toast.makeText(this, "Workout completed!", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // User back từ ExerciseActivity - session vẫn đang diễn ra
                Log.d(TAG, "User backed from ExerciseActivity, session still active");
                
                // Update Intent để biết là từ back, không phải từ MainActivity
                getIntent().putExtra("fromMainActivity", false);
                
                // Session đã được cập nhật real-time trong database
                // Load session từ database để cập nhật currentSession
                syncSessionAfterBack();
            }
        }
    }
    
    /**
     * Sync session với database sau khi back từ ExerciseActivity
     */
    private void syncSessionAfterBack() {
        Log.d(TAG, "Syncing session with database after back from ExerciseActivity");
        
        // Nếu có currentSession, load lại từ Firebase bằng ID
        if (currentSession != null && currentSession.getId() != null) {
            Log.d(TAG, "Reloading session after back by ID: " + currentSession.getId());
            FirebaseService.getInstance().loadSessionById(currentSession.getId(), new FirebaseService.OnSessionLoadedListener() {
                @Override
                public void onSessionLoaded(Session updatedSession) {
                    if (updatedSession != null) {
                        currentSession = updatedSession;
                        Log.d(TAG, "Session synced after back: " + updatedSession.getId());
                    } else {
                        Log.e(TAG, "Failed to reload session after back");
                    }
                    
                    hasActiveSession = true;
                    updateButtonUI(); // Update button to "Resume Workout"
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error reloading session after back: " + error);
                    hasActiveSession = true;
                    updateButtonUI(); // Update button to "Resume Workout"
                }
            });
        } else {
            Log.e(TAG, "No current session to reload after back");
            hasActiveSession = false;
            updateButtonUI();
        }
    }
    
    private void showRestScreen(int nextExerciseIndex) {
        // Start next exercise with rest mode
        Exercise nextExercise = exercises.get(nextExerciseIndex);
        Intent intent = new Intent(this, ExerciseActivity.class);
        intent.putExtra("exercise", nextExercise);
        intent.putExtra("sets", exerciseSets[nextExerciseIndex]);
        intent.putExtra("reps", exerciseReps[nextExerciseIndex]);
        intent.putExtra("workoutTemplate", workoutTemplate);
        intent.putExtra("exerciseIndex", nextExerciseIndex);
        intent.putExtra("totalExercises", exercises.size());
        intent.putExtra("isRestMode", true);
        intent.putExtra("session", currentSession); // Pass session to ExerciseActivity
        
        startActivityForResult(intent, 1001);
    }
    
    
    /**
     * Complete workout session and save final data
     */
    private void completeWorkoutSession() {
        if (currentSession != null) {
            Log.d(TAG, "Completing workout session: " + currentSession.getId());
            
            // Set end time
            currentSession.setEndedAt(System.currentTimeMillis() / 1000); // Convert to seconds
            
            // Calculate duration (both are now in seconds)
            long durationSec = currentSession.getEndedAt() - currentSession.getStartedAt();
            
            // Update session summary
            Session.SessionSummary summary = currentSession.getSummary();
            summary.setDurationSec((int) durationSec);
            
            // Estimate calories (simple calculation based on duration)
            int estimatedKcal = (int) (durationSec * 0.1); // Rough estimate: 0.1 kcal per second
            summary.setEstKcal(estimatedKcal);
            
            // Save updated session to Firebase
            FirebaseService.getInstance().saveSession(currentSession, new FirebaseService.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    if (success) {
                        Log.d(TAG, "Workout session completed and saved successfully");
                        Toast.makeText(WorkoutActivity.this, "Session saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to save completed session");
                        Toast.makeText(WorkoutActivity.this, "Failed to save session", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Load workout template from Firebase Firestore
     */
    private void loadWorkoutTemplateById(String workoutTemplateId) {
        Log.d(TAG, "Loading workout template: " + workoutTemplateId);
        
        FirebaseService.getInstance().loadWorkoutTemplateById(workoutTemplateId, this, new FirebaseService.OnWorkoutTemplateLoadedListener() {
            @Override
            public void onWorkoutTemplateLoaded(WorkoutTemplate template) {
                workoutTemplate = template;
                
                // Update UI
                updateWorkoutTemplateUI();
                
                // Load exercises for this template
                loadExercises();
                
                // Check if coming from MainActivity
                boolean isFromMainActivity = getIntent().getBooleanExtra("fromMainActivity", true);
                
                if (isFromMainActivity) {
                    // Từ MainActivity: luôn hiển thị "Start Workout"
                    hasActiveSession = false;
                    updateButtonUI();
                    Log.d(TAG, "From MainActivity: Always show 'Start Workout' button");
                } else {
                    // Check for existing session to determine button state
                    loadExistingSession();
                }
            }
        });
    }
    
    /**
     * Load session hiện tại từ Firebase/local storage
     */
    private void loadExistingSession() {
        // Load session từ Firebase cho workout hiện tại
        FirebaseService.getInstance().loadActiveSession(workoutTemplate.getId(), new FirebaseService.OnSessionLoadedListener() {
            @Override
            public void onSessionLoaded(Session session) {
                if (session != null) {
                    // Có session đang diễn ra
                    currentSession = session;
                    hasActiveSession = true;
                    updateButtonUI();
                    Log.d(TAG, "Loaded existing session: " + session.getId());
                } else {
                    // Không có session đang diễn ra
                    hasActiveSession = false;
                    updateButtonUI();
                    Log.d(TAG, "No active session found");
                }
            }
            
            @Override
            public void onError(String error) {
                // Lỗi load session, giả sử không có session
                hasActiveSession = false;
                updateButtonUI();
                Log.e(TAG, "Error loading session: " + error);
            }
        });
    }
    
    /**
     * Cập nhật UI của button dựa trên trạng thái session
     */
    private void updateButtonUI() {
        if (hasActiveSession) {
            binding.button.setText("Resume Workout");
            binding.button.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            binding.button.setText("Start Workout");
            binding.button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    /**
     * Update UI with workout template data
     */
    private void updateWorkoutTemplateUI() {
        if (workoutTemplate == null) return;

        // Set title and description
        binding.titleTxt.setText(workoutTemplate.getTitle());
        binding.descriptionTxt.setText(workoutTemplate.getDescription());
        
        // Set exercise count
        binding.excerciseTxt.setText(workoutTemplate.getItems().size() + " Exercise");
        
        // Set duration
        binding.durationTxt.setText(workoutTemplate.getEstDurationMin() + " min");

        // Set image based on focus
        int resId = getImageResourceForWorkout(workoutTemplate);
        Glide.with(this)
                .load(resId)
                .into(binding.pic);
    }

    /**
     * Load exercises for the workout template
     */
    private void loadExercises() {
        if (workoutTemplate == null || workoutTemplate.getItems() == null) {
            Log.e(TAG, "No workout items to load");
            return;
        }

        FirebaseService.getInstance().loadExercises(workoutTemplate, this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> loadedExercises) {
                // Sort exercises by WorkoutTemplate order
                exercises = sortExercisesByTemplateOrder(loadedExercises);
                
                // Initialize exerciseSets and exerciseReps from WorkoutTemplate
                initializeExerciseConfigsFromTemplate();
                
                // Update UI
                binding.view3.setLayoutManager(new LinearLayoutManager(WorkoutActivity.this, LinearLayoutManager.VERTICAL, false));
                        ExerciseAdapter adapter = new ExerciseAdapter(exercises, workoutTemplate);
                        adapter.setOnSetsRepsChangedListener(WorkoutActivity.this);
                        adapter.setOnDifficultyChangedListener(WorkoutActivity.this);
                        binding.view3.setAdapter(adapter);
                Log.d(TAG, "Loaded " + exercises.size() + " exercises");
            }
        });
    }

    private ArrayList<Exercise> sortExercisesByTemplateOrder(ArrayList<Exercise> loadedExercises) {
        if (workoutTemplate == null || workoutTemplate.getItems() == null || loadedExercises == null) {
            return loadedExercises;
        }
        
        ArrayList<Exercise> sortedExercises = new ArrayList<>();
        
        Log.d(TAG, "=== SORTING EXERCISES IN WORKOUTACTIVITY ===");
        Log.d(TAG, "Loaded exercises (" + loadedExercises.size() + "):");
        for (int i = 0; i < loadedExercises.size(); i++) {
            Log.d(TAG, "  " + i + ": " + loadedExercises.get(i).getName() + " (" + loadedExercises.get(i).getId() + ")");
        }
        
        Log.d(TAG, "WorkoutTemplate items (" + workoutTemplate.getItems().size() + "):");
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            Log.d(TAG, "  Order " + item.getOrder() + ": " + item.getExerciseId());
        }
        
        // Sort by WorkoutTemplate order
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            for (Exercise exercise : loadedExercises) {
                if (exercise.getId().equals(item.getExerciseId())) {
                    sortedExercises.add(exercise);
                    Log.d(TAG, "  Added: " + exercise.getName());
                    break;
                }
            }
        }
        
        Log.d(TAG, "Sorted exercises (" + sortedExercises.size() + "):");
        for (int i = 0; i < sortedExercises.size(); i++) {
            Log.d(TAG, "  " + i + ": " + sortedExercises.get(i).getName() + " (" + sortedExercises.get(i).getId() + ")");
        }
        Log.d(TAG, "=== END SORTING ===");
        
        return sortedExercises;
    }

    /**
     * Initialize exerciseSets and exerciseReps from WorkoutTemplate configOverride
     */
    private void initializeExerciseConfigsFromTemplate() {
        if (workoutTemplate == null || workoutTemplate.getItems() == null || exercises == null) {
            return;
        }

        Log.d(TAG, "Initializing exercise configs from WorkoutTemplate");
        
        for (int i = 0; i < exercises.size() && i < 10; i++) {
            Exercise exercise = exercises.get(i);
            WorkoutTemplate.WorkoutItem workoutItem = getWorkoutItemForExercise(exercise.getId());
            
            if (workoutItem != null && workoutItem.getConfigOverride() != null) {
                // Use configOverride from WorkoutTemplate
                exerciseSets[i] = workoutItem.getConfigOverride().getSets();
                exerciseReps[i] = workoutItem.getConfigOverride().getReps();
                exerciseDifficulties[i] = workoutItem.getConfigOverride().getDifficulty();
                
                Log.d(TAG, "Exercise " + i + " (" + exercise.getName() + "): " + 
                    exerciseSets[i] + " sets x " + exerciseReps[i] + " reps, difficulty: " + exerciseDifficulties[i] + " (from WorkoutTemplate)");
            } else {
                // Fallback to default config from Exercise
                if (exercise.getDefaultConfig() != null) {
                    exerciseSets[i] = exercise.getDefaultConfig().getSets();
                    exerciseReps[i] = exercise.getDefaultConfig().getReps();
                    exerciseDifficulties[i] = exercise.getDefaultConfig().getDifficulty();
                    
                    Log.d(TAG, "Exercise " + i + " (" + exercise.getName() + ") using default config: " + 
                        exerciseSets[i] + " sets x " + exerciseReps[i] + " reps, difficulty: " + exerciseDifficulties[i]);
                }
            }
        }
    }

    /**
     * Get WorkoutItem for a specific exercise ID
     */
    private WorkoutTemplate.WorkoutItem getWorkoutItemForExercise(String exerciseId) {
        if (workoutTemplate == null || workoutTemplate.getItems() == null) {
            return null;
        }
        
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            if (exerciseId.equals(item.getExerciseId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get image resource based on workout template focus/type
     */
    private int getImageResourceForWorkout(WorkoutTemplate workoutTemplate) {
        return FirebaseService.getInstance().getImageResourceForWorkout(workoutTemplate, this);
    }
    
    @Override
    public void onSetsRepsChanged(int exerciseIndex, int sets, int reps) {
        // Since exercises is sorted by WorkoutTemplate order and exerciseSets is initialized in same order,
        // exerciseIndex should directly correspond to array index
        if (exerciseIndex < exerciseSets.length && exerciseIndex < exerciseReps.length) {
            exerciseSets[exerciseIndex] = sets;
            exerciseReps[exerciseIndex] = reps;
            Log.d(TAG, "Exercise " + exerciseIndex + " updated: " + sets + " sets x " + reps + " reps");
        }
    }
    
    @Override
    public void onDifficultyChanged(int exerciseIndex, String difficulty) {
        // Since exercises is sorted by WorkoutTemplate order and exerciseDifficulties is initialized in same order,
        // exerciseIndex should directly correspond to array index
        if (exerciseIndex < exerciseDifficulties.length) {
            exerciseDifficulties[exerciseIndex] = difficulty;
            Log.d(TAG, "Exercise " + exerciseIndex + " difficulty updated: " + difficulty);
        }
    }
}