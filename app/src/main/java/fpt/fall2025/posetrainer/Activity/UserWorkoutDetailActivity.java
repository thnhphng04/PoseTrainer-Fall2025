package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import fpt.fall2025.posetrainer.Adapter.UserWorkoutExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityUserWorkoutDetailBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserWorkoutDetailActivity extends AppCompatActivity implements UserWorkoutExerciseAdapter.OnSetsRepsChangedListener, UserWorkoutExerciseAdapter.OnDifficultyChangedListener {
    private static final String TAG = "UserWorkoutDetailActivity";
    ActivityUserWorkoutDetailBinding binding;
    private UserWorkout userWorkout;
    private ArrayList<Exercise> exercises;
    private int[] exerciseSets;
    private int[] exerciseReps;
    private String[] exerciseDifficulties;
    private Session currentSession;
    private boolean hasActiveSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserWorkoutDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize data
        exercises = new ArrayList<>();
        exerciseSets = new int[10]; // Max 10 exercises
        exerciseReps = new int[10]; // Max 10 exercises
        exerciseDifficulties = new String[10]; // Max 10 exercises
        
        // Initialize default values
        for (int i = 0; i < 10; i++) {
            exerciseSets[i] = 3; // Default 3 sets
            exerciseReps[i] = 12; // Default 12 reps
            exerciseDifficulties[i] = "beginner"; // Default beginner
        }

        // Get user workout ID from intent (could be from notification or direct navigation)
        String userWorkoutId = getIntent().getStringExtra("userWorkoutId");
        boolean fromSchedule = getIntent().getBooleanExtra("fromSchedule", false);
        
        if (userWorkoutId != null) {
            loadUserWorkoutById(userWorkoutId, fromSchedule);
        } else {
            Toast.makeText(this, "No user workout ID provided", Toast.LENGTH_SHORT).show();
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
        
        // Add test button for sample data (for debugging)
        binding.imageView8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Test button clicked - creating sample data");
                createSampleData();
            }
        });
        
        // Add double tap to test RecyclerView directly
        binding.imageView8.setOnTouchListener(new View.OnTouchListener() {
            private long lastTouchTime = 0;
            private int tapCount = 0;
            private static final int DOUBLE_TAP_TIME_DELTA = 300;
            private static final int TRIPLE_TAP_TIME_DELTA = 500;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTouchTime < TRIPLE_TAP_TIME_DELTA) {
                        tapCount++;
                        if (tapCount == 2) {
                            // Double tap detected
                            Log.d(TAG, "Double tap - testing RecyclerView directly");
                            testRecyclerViewDirectly();
                            return true;
                        } else if (tapCount == 3) {
                            // Triple tap detected
                            Log.d(TAG, "Triple tap - checking RecyclerView layout");
                            checkRecyclerViewLayout();
                            tapCount = 0;
                            return true;
                        } else if (tapCount == 4) {
                            // Quad tap detected
                            Log.d(TAG, "Quad tap - testing ScrollView functionality");
                            testScrollViewFunctionality();
                            tapCount = 0;
                            return true;
                        }
                    } else {
                        tapCount = 1;
                    }
                    lastTouchTime = currentTime;
                }
                return false;
            }
        });
        
        // Add long click listener to reload exercises
        binding.imageView8.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "Long click - reloading exercises from Firebase");
                if (userWorkout != null) {
                    loadExercises();
                } else {
                    Log.e(TAG, "UserWorkout is null, cannot reload exercises");
                }
                return true;
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
                    openEditUserWorkoutActivity();
                    return true;
                }
                return false;
            }
        });
        
        popupMenu.setGravity(Gravity.END);
        popupMenu.show();
    }
    
    /**
     * Open EditWorkoutActivity for user workout
     */
    private void openEditUserWorkoutActivity() {
        if (userWorkout == null) {
            Toast.makeText(this, "User workout not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, EditWorkoutActivity.class);
        intent.putExtra("userWorkoutId", userWorkout.getId());
        startActivity(intent);
    }
    
    private void startWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "No exercises available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if coming from MyWorkoutFragment or from schedule
        boolean isFromMyWorkoutFragment = getIntent().getBooleanExtra("fromMyWorkoutFragment", true);
        boolean fromSchedule = getIntent().getBooleanExtra("fromSchedule", false);
        
        // If from schedule, always create new session
        if (fromSchedule) {
            createWorkoutSession();
        } else if (hasActiveSession && !isFromMyWorkoutFragment) {
            // Có session đang diễn ra VÀ không phải từ MyWorkoutFragment - resume workout
            resumeWorkout();
        } else {
            // Không có session HOẶC từ MyWorkoutFragment - tạo session mới
            createWorkoutSession();
        }
    }
    
    /**
     * Resume workout hiện tại
     */
    private void resumeWorkout() {
        Log.d(TAG, "Resuming existing workout session");
        syncSessionBeforeResume();
    }
    
    /**
     * Sync session với database trước khi resume
     */
    private void syncSessionBeforeResume() {
        Log.d(TAG, "Syncing session with database before resume");
        
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
     * Resume workout with existing session
     */
    private void startWorkoutWithExistingSession(int exerciseIndex) {
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra("sessionId", currentSession.getId());
        intent.putExtra("isResume", true);
        
        startActivityForResult(intent, 1001);
    }
    
    /**
     * Create a new workout session and start the workout
     */
    private void createWorkoutSession() {
        Log.d(TAG, "Creating new workout session for user workout");
        
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
        currentSession.setTitle(userWorkout.getTitle());
        currentSession.setDescription(userWorkout.getDescription());
        currentSession.setStartedAt(System.currentTimeMillis() / 1000);
        currentSession.setEndedAt(0);
        
        // Initialize session summary
        Session.SessionSummary summary = new Session.SessionSummary();
        summary.setDurationSec(0);
        summary.setEstKcal(0);
        currentSession.setSummary(summary);
        
        // Initialize per-exercise data
        ArrayList<Session.PerExercise> perExercises = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            Session.PerExercise perExercise = new Session.PerExercise();
            perExercise.setExerciseNo(i + 1);
            perExercise.setExerciseId(exercises.get(i).getId());
            perExercise.setDifficultyUsed(exerciseDifficulties[i]);
            perExercise.setState("not_started");
            
            // Tạo SetData ban đầu cho tất cả sets
            ArrayList<Session.SetData> initialSets = new ArrayList<>();
            for (int setNo = 1; setNo <= exerciseSets[i]; setNo++) {
                Session.SetData setData = new Session.SetData();
                setData.setSetNo(setNo);
                setData.setTargetReps(exerciseReps[i]);
                setData.setCorrectReps(0);
                setData.setState("incomplete");
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
        
        hasActiveSession = true;
        
        // Save session to Firebase
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
     * Start workout with the created session
     */
    private void startWorkoutWithSession() {
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra("sessionId", currentSession.getId());
        
        startActivityForResult(intent, 1001);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                // Exercise completed
                int completedIndex = data.getIntExtra("completedIndex", 0);
                if (completedIndex + 1 < exercises.size()) {
                    showRestScreen(completedIndex + 1);
                } else {
                    completeWorkoutSession();
                    hasActiveSession = false;
                    updateButtonUI();
                    Toast.makeText(this, "Workout completed!", Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // User back từ ExerciseActivity
                Log.d(TAG, "User backed from ExerciseActivity, session still active");
                
                getIntent().putExtra("fromMyWorkoutFragment", false);
                
                syncSessionAfterBack();
            }
        }
    }
    
    /**
     * Sync session với database sau khi back từ ExerciseActivity
     */
    private void syncSessionAfterBack() {
        Log.d(TAG, "Syncing session with database after back from ExerciseActivity");
        
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
                    updateButtonUI();
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error reloading session after back: " + error);
                    hasActiveSession = true;
                    updateButtonUI();
                }
            });
        } else {
            Log.e(TAG, "No current session to reload after back");
            hasActiveSession = false;
            updateButtonUI();
        }
    }
    
    private void showRestScreen(int nextExerciseIndex) {
        Exercise nextExercise = exercises.get(nextExerciseIndex);
        Intent intent = new Intent(this, ExerciseActivity.class);
        intent.putExtra("exercise", nextExercise);
        intent.putExtra("sets", exerciseSets[nextExerciseIndex]);
        intent.putExtra("reps", exerciseReps[nextExerciseIndex]);
        intent.putExtra("exerciseIndex", nextExerciseIndex);
        intent.putExtra("totalExercises", exercises.size());
        intent.putExtra("isRestMode", true);
        intent.putExtra("session", currentSession);
        
        startActivityForResult(intent, 1001);
    }
    
    /**
     * Complete workout session and save final data
     */
    private void completeWorkoutSession() {
        if (currentSession != null) {
            Log.d(TAG, "Completing workout session: " + currentSession.getId());
            
            currentSession.setEndedAt(System.currentTimeMillis() / 1000);
            
            long durationSec = currentSession.getEndedAt() - currentSession.getStartedAt();
            
            Session.SessionSummary summary = currentSession.getSummary();
            summary.setDurationSec((int) durationSec);
            
            int estimatedKcal = (int) (durationSec * 0.1);
            summary.setEstKcal(estimatedKcal);
            
            FirebaseService.getInstance().saveSession(currentSession, new FirebaseService.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    if (success) {
                        Log.d(TAG, "Workout session completed and saved successfully");
                        Toast.makeText(UserWorkoutDetailActivity.this, "Session saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to save completed session");
                        Toast.makeText(UserWorkoutDetailActivity.this, "Failed to save session", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Load user workout from Firebase Firestore
     */
    private void loadUserWorkoutById(String userWorkoutId, boolean fromSchedule) {
        Log.d(TAG, "Loading user workout: " + userWorkoutId + " (fromSchedule: " + fromSchedule + ")");
        
        FirebaseService.getInstance().loadUserWorkoutById(userWorkoutId, this, new FirebaseService.OnUserWorkoutLoadedListener() {
            @Override
            public void onUserWorkoutLoaded(UserWorkout workout) {
                if (workout != null) {
                    userWorkout = workout;
                    
                    // Update UI
                    updateUserWorkoutUI();
                    
                    // Load exercises for this workout
                    loadExercises();
                    
                    // Check if from schedule - if so, always show "Start Workout" button
                    if (fromSchedule) {
                        hasActiveSession = false;
                        updateButtonUI();
                        Log.d(TAG, "From schedule: Always show 'Start Workout' button");
                    } else {
                        // Check for existing session to determine button state
                        loadExistingSession();
                    }
                } else {
                    // UserWorkout not found - may have been deleted
                    Log.e(TAG, "UserWorkout not found: " + userWorkoutId);
                    
                    if (fromSchedule) {
                        // If from schedule notification, show message and navigate back
                        Toast.makeText(UserWorkoutDetailActivity.this, 
                            "Bài tập đã bị xóa hoặc không tồn tại. Lịch tập sẽ được cập nhật tự động.", 
                            Toast.LENGTH_LONG).show();
                        // Navigate back to MainActivity
                        Intent mainIntent = new Intent(UserWorkoutDetailActivity.this, MainActivity.class);
                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent);
                    } else {
                        Toast.makeText(UserWorkoutDetailActivity.this, "Không tìm thấy bài tập", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        });
    }
    
    /**
     * Load session hiện tại từ Firebase/local storage
     */
    private void loadExistingSession() {
        // Load session từ Firebase cho user workout hiện tại
        FirebaseService.getInstance().loadActiveSession(userWorkout.getId(), new FirebaseService.OnSessionLoadedListener() {
            @Override
            public void onSessionLoaded(Session session) {
                if (session != null) {
                    currentSession = session;
                    hasActiveSession = true;
                    updateButtonUI();
                    Log.d(TAG, "Loaded existing session: " + session.getId());
                } else {
                    hasActiveSession = false;
                    updateButtonUI();
                    Log.d(TAG, "No active session found");
                }
            }
            
            @Override
            public void onError(String error) {
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
     * Update UI with user workout data
     */
    private void updateUserWorkoutUI() {
        if (userWorkout == null) return;

        // Set title and description
        binding.titleTxt.setText(userWorkout.getTitle());
        binding.descriptionTxt.setText(userWorkout.getDescription());
        
        // Set exercise count
        binding.excerciseTxt.setText(userWorkout.getItems().size() + " Exercise");
        
        // Calculate estimated duration (rough estimate: 2 minutes per exercise)
        int estimatedDuration = userWorkout.getItems().size() * 2;
        binding.durationTxt.setText(estimatedDuration + " min");

        // Set image based on source or default
        int resId = getImageResourceForUserWorkout(userWorkout);
        Glide.with(this)
                .load(resId)
                .into(binding.pic);
    }

    /**
     * Debug method to test data loading
     */
    private void debugDataLoading() {
        Log.d(TAG, "=== DEBUG DATA LOADING ===");
        Log.d(TAG, "UserWorkout: " + (userWorkout != null ? userWorkout.getTitle() : "null"));
        Log.d(TAG, "UserWorkout ID: " + (userWorkout != null ? userWorkout.getId() : "null"));
        Log.d(TAG, "UserWorkout Items: " + (userWorkout != null && userWorkout.getItems() != null ? userWorkout.getItems().size() : "null"));
        
        if (userWorkout != null && userWorkout.getItems() != null) {
            for (int i = 0; i < userWorkout.getItems().size(); i++) {
                UserWorkout.UserWorkoutItem item = userWorkout.getItems().get(i);
                Log.d(TAG, "Item " + i + ": Order=" + item.getOrder() + ", ExerciseId=" + item.getExerciseId());
                if (item.getConfig() != null) {
                    Log.d(TAG, "  Config: Sets=" + item.getConfig().getSets() + ", Reps=" + item.getConfig().getReps() + ", Difficulty=" + item.getConfig().getDifficulty());
                }
            }
        }
        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * Load exercises for the user workout
     */
    private void loadExercises() {
        if (userWorkout == null || userWorkout.getItems() == null) {
            Log.e(TAG, "No user workout items to load");
            return;
        }

        // Debug data loading
        debugDataLoading();

        Log.d(TAG, "=== LOADING EXERCISES FOR USER WORKOUT ===");
        Log.d(TAG, "UserWorkout: " + userWorkout.getTitle());
        Log.d(TAG, "Items count: " + userWorkout.getItems().size());
        
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            Log.d(TAG, "Item - Order: " + item.getOrder() + ", ExerciseId: " + item.getExerciseId());
            if (item.getConfig() != null) {
                Log.d(TAG, "  Config - Sets: " + item.getConfig().getSets() + ", Reps: " + item.getConfig().getReps() + ", Difficulty: " + item.getConfig().getDifficulty());
            }
        }

        FirebaseService.getInstance().loadExercisesForUserWorkout(userWorkout, this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> loadedExercises) {
                Log.d(TAG, "=== EXERCISES LOADED CALLBACK ===");
                Log.d(TAG, "Loaded exercises count: " + (loadedExercises != null ? loadedExercises.size() : "null"));
                
                if (loadedExercises == null || loadedExercises.isEmpty()) {
                    Log.e(TAG, "No exercises loaded! Creating sample data for testing.");
                    createSampleData();
                    return;
                }
                
                // Sort exercises by UserWorkout order
                exercises = sortExercisesByUserWorkoutOrder(loadedExercises);
                
                // Initialize exerciseSets and exerciseReps from UserWorkout
                initializeExerciseConfigsFromUserWorkout();
                
                Log.d(TAG, "Final exercises count: " + exercises.size());
                
                // Update UI
                Log.d(TAG, "Setting up RecyclerView...");
                setupRecyclerView();
            }
        });
    }

    /**
     * Setup RecyclerView with exercises
     */
    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with " + exercises.size() + " exercises");
        
        // Set layout manager
        binding.view3.setLayoutManager(new LinearLayoutManager(UserWorkoutDetailActivity.this, LinearLayoutManager.VERTICAL, false));
        
        // Create and set adapter
        UserWorkoutExerciseAdapter adapter = new UserWorkoutExerciseAdapter(exercises, userWorkout);
        adapter.setOnSetsRepsChangedListener(UserWorkoutDetailActivity.this);
        adapter.setOnDifficultyChangedListener(UserWorkoutDetailActivity.this);
        
        // Debug adapter state
        adapter.debugAdapterState();
        
        binding.view3.setAdapter(adapter);
        
        Log.d(TAG, "RecyclerView setup completed with adapter");
        
        // Force notify adapter
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter notifyDataSetChanged() called");
        
        // Debug: Log each exercise in the adapter
        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            Log.d(TAG, "Exercise " + i + ": " + exercise.getName() + " (ID: " + exercise.getId() + ")");
        }
        
        // ScrollView will handle scrolling automatically
        Log.d(TAG, "ScrollView will handle scrolling for all " + exercises.size() + " exercises");
    }

    /**
     * Create sample data for testing if no real data is available
     */
    private void createSampleData() {
        Log.d(TAG, "Creating sample data for testing");
        
        // Create sample exercises
        exercises = new ArrayList<>();
        
        // Sample Exercise 1
        Exercise exercise1 = new Exercise();
        exercise1.setId("sample_exercise_1");
        exercise1.setName("Push-ups");
        exercise1.setLevel("beginner");
        
        // Set sample media
        Exercise.Media media1 = new Exercise.Media();
        media1.setThumbnailUrl("pic_1");
        exercise1.setMedia(media1);
        
        // Set default config
        Exercise.DefaultConfig config1 = new Exercise.DefaultConfig();
        config1.setSets(3);
        config1.setReps(10);
        config1.setDifficulty("beginner");
        exercise1.setDefaultConfig(config1);
        
        exercises.add(exercise1);
        
        // Sample Exercise 2
        Exercise exercise2 = new Exercise();
        exercise2.setId("sample_exercise_2");
        exercise2.setName("Squats");
        exercise2.setLevel("beginner");
        
        Exercise.Media media2 = new Exercise.Media();
        media2.setThumbnailUrl("pic_2");
        exercise2.setMedia(media2);
        
        Exercise.DefaultConfig config2 = new Exercise.DefaultConfig();
        config2.setSets(3);
        config2.setReps(15);
        config2.setDifficulty("beginner");
        exercise2.setDefaultConfig(config2);
        
        exercises.add(exercise2);
        
        Log.d(TAG, "Sample data created with " + exercises.size() + " exercises");
        
        // Setup RecyclerView with sample data
        setupRecyclerView();
    }
    
    /**
     * Test RecyclerView directly with minimal data
     */
    private void testRecyclerViewDirectly() {
        Log.d(TAG, "Testing RecyclerView directly");
        
        // Create minimal test data
        exercises = new ArrayList<>();
        
        Exercise testExercise = new Exercise();
        testExercise.setId("test_exercise");
        testExercise.setName("Test Exercise");
        testExercise.setLevel("beginner");
        
        Exercise.Media media = new Exercise.Media();
        media.setThumbnailUrl("pic_1");
        testExercise.setMedia(media);
        
        Exercise.DefaultConfig config = new Exercise.DefaultConfig();
        config.setSets(3);
        config.setReps(10);
        config.setDifficulty("beginner");
        testExercise.setDefaultConfig(config);
        
        exercises.add(testExercise);
        
        Log.d(TAG, "Created test exercise: " + testExercise.getName());
        
        // Setup RecyclerView
        setupRecyclerView();
    }
    
    /**
     * Check RecyclerView layout and visibility
     */
    private void checkRecyclerViewLayout() {
        Log.d(TAG, "=== CHECKING RECYCLERVIEW LAYOUT ===");
        
        if (binding.view3 != null) {
            Log.d(TAG, "RecyclerView exists: " + (binding.view3 != null));
            Log.d(TAG, "RecyclerView visibility: " + binding.view3.getVisibility());
            Log.d(TAG, "RecyclerView height: " + binding.view3.getHeight());
            Log.d(TAG, "RecyclerView measured height: " + binding.view3.getMeasuredHeight());
            Log.d(TAG, "RecyclerView adapter: " + (binding.view3.getAdapter() != null ? "exists" : "null"));
            
            if (binding.view3.getAdapter() != null) {
                Log.d(TAG, "Adapter item count: " + binding.view3.getAdapter().getItemCount());
                
                // Check if RecyclerView can scroll
                Log.d(TAG, "Can scroll vertically: " + binding.view3.canScrollVertically(1));
                Log.d(TAG, "Can scroll vertically (down): " + binding.view3.canScrollVertically(-1));
                
                // Get layout manager
                RecyclerView.LayoutManager layoutManager = binding.view3.getLayoutManager();
                if (layoutManager != null) {
                    Log.d(TAG, "Layout manager: " + layoutManager.getClass().getSimpleName());
                    if (layoutManager instanceof LinearLayoutManager) {
                        LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
                        Log.d(TAG, "First visible position: " + llm.findFirstVisibleItemPosition());
                        Log.d(TAG, "Last visible position: " + llm.findLastVisibleItemPosition());
                        Log.d(TAG, "Total item count: " + llm.getItemCount());
                    }
                }
            }
        }
        
        // Check button layout
        if (binding.button != null) {
            Log.d(TAG, "Button height: " + binding.button.getHeight());
            Log.d(TAG, "Button Y position: " + binding.button.getY());
        }
        
        Log.d(TAG, "=== END RECYCLERVIEW LAYOUT CHECK ===");
    }
    
    /**
     * Force show all exercises by adjusting RecyclerView height
     * Note: With ScrollView, this method is no longer needed as ScrollView handles scrolling automatically
     */
    private void forceShowAllExercises() {
        Log.d(TAG, "=== FORCING SHOW ALL EXERCISES ===");
        Log.d(TAG, "ScrollView now handles scrolling automatically for all " + exercises.size() + " exercises");
        Log.d(TAG, "You can scroll down to see all exercises including Jumping Jack");
        Log.d(TAG, "=== END FORCE SHOW ALL EXERCISES ===");
    }
    
    /**
     * Test ScrollView functionality
     */
    private void testScrollViewFunctionality() {
        Log.d(TAG, "=== TESTING SCROLLVIEW FUNCTIONALITY ===");
        
        // Find ScrollView in the layout
        ScrollView scrollView = findViewById(android.R.id.content);
        if (scrollView == null) {
            // Try to find ScrollView in the main layout
            scrollView = binding.getRoot().findViewById(R.id.main);
            if (scrollView instanceof ScrollView) {
                scrollView = (ScrollView) scrollView;
            } else {
                // Look for ScrollView in the layout hierarchy
                scrollView = findScrollView(binding.getRoot());
            }
        }
        
        if (scrollView != null) {
            Log.d(TAG, "ScrollView found: " + scrollView.getClass().getSimpleName());
            Log.d(TAG, "ScrollView height: " + scrollView.getHeight());
            Log.d(TAG, "ScrollView can scroll vertically: " + scrollView.canScrollVertically(1));
            Log.d(TAG, "ScrollView scroll Y: " + scrollView.getScrollY());
            
            // Test scroll to bottom
            final ScrollView finalScrollView = scrollView;
            finalScrollView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Scrolling to bottom to show all exercises");
                    finalScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        } else {
            Log.e(TAG, "ScrollView not found!");
        }
        
        Log.d(TAG, "=== END SCROLLVIEW FUNCTIONALITY TEST ===");
    }
    
    /**
     * Helper method to find ScrollView in layout hierarchy
     */
    private ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                ScrollView scrollView = findScrollView(viewGroup.getChildAt(i));
                if (scrollView != null) {
                    return scrollView;
                }
            }
        }
        
        return null;
    }

    private ArrayList<Exercise> sortExercisesByUserWorkoutOrder(ArrayList<Exercise> loadedExercises) {
        if (userWorkout == null || userWorkout.getItems() == null || loadedExercises == null) {
            return loadedExercises;
        }
        
        ArrayList<Exercise> sortedExercises = new ArrayList<>();
        
        Log.d(TAG, "=== SORTING EXERCISES IN USERWORKOUTDETAILACTIVITY ===");
        Log.d(TAG, "Loaded exercises (" + loadedExercises.size() + "):");
        for (int i = 0; i < loadedExercises.size(); i++) {
            Log.d(TAG, "  " + i + ": " + loadedExercises.get(i).getName() + " (" + loadedExercises.get(i).getId() + ")");
        }
        
        Log.d(TAG, "UserWorkout items (" + userWorkout.getItems().size() + "):");
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            Log.d(TAG, "  Order " + item.getOrder() + ": " + item.getExerciseId());
        }
        
        // Sort by UserWorkout order
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
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
     * Initialize exerciseSets and exerciseReps from UserWorkout config
     */
    private void initializeExerciseConfigsFromUserWorkout() {
        if (userWorkout == null || userWorkout.getItems() == null || exercises == null) {
            return;
        }

        Log.d(TAG, "Initializing exercise configs from UserWorkout");
        
        for (int i = 0; i < exercises.size() && i < 10; i++) {
            Exercise exercise = exercises.get(i);
            UserWorkout.UserWorkoutItem workoutItem = getUserWorkoutItemForExercise(exercise.getId());
            
            if (workoutItem != null && workoutItem.getConfig() != null) {
                // Use config from UserWorkout
                exerciseSets[i] = workoutItem.getConfig().getSets();
                exerciseReps[i] = workoutItem.getConfig().getReps();
                exerciseDifficulties[i] = workoutItem.getConfig().getDifficulty();
                
                Log.d(TAG, "Exercise " + i + " (" + exercise.getName() + "): " + 
                    exerciseSets[i] + " sets x " + exerciseReps[i] + " reps, difficulty: " + exerciseDifficulties[i] + " (from UserWorkout)");
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
     * Get UserWorkoutItem for a specific exercise ID
     */
    private UserWorkout.UserWorkoutItem getUserWorkoutItemForExercise(String exerciseId) {
        if (userWorkout == null || userWorkout.getItems() == null) {
            return null;
        }
        
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            if (exerciseId.equals(item.getExerciseId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get image resource based on user workout source/type
     */
    private int getImageResourceForUserWorkout(UserWorkout userWorkout) {
        // Default image for user workouts
        return R.drawable.pic_1;
    }
    
    @Override
    public void onSetsRepsChanged(int exerciseIndex, int sets, int reps) {
        if (exerciseIndex < exerciseSets.length && exerciseIndex < exerciseReps.length) {
            exerciseSets[exerciseIndex] = sets;
            exerciseReps[exerciseIndex] = reps;
            Log.d(TAG, "Exercise " + exerciseIndex + " updated: " + sets + " sets x " + reps + " reps");
        }
    }
    
    @Override
    public void onDifficultyChanged(int exerciseIndex, String difficulty) {
        if (exerciseIndex < exerciseDifficulties.length) {
            exerciseDifficulties[exerciseIndex] = difficulty;
            Log.d(TAG, "Exercise " + exerciseIndex + " difficulty updated: " + difficulty);
        }
    }
}
