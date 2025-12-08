package fpt.fall2025.posetrainer.UI.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fpt.fall2025.posetrainer.UI.adapter.session.SessionExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Profile;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Util.CalorieCalculator;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.ProfileDAO;
import fpt.fall2025.posetrainer.DAL.SessionDAO;
import fpt.fall2025.posetrainer.DAL.ExerciseDAO;
import fpt.fall2025.posetrainer.databinding.ActivitySessionBinding;

public class SessionActivity extends AppCompatActivity implements SessionExerciseAdapter.OnExerciseClickListener {

    private static final String TAG = "SessionActivity";
    private static final int EXERCISE_REQUEST_CODE = 1001;

    private ActivitySessionBinding binding;
    private Session currentSession;
    private WorkoutTemplate workoutTemplate;
    private List<Exercise> exercises;
    private SessionExerciseAdapter sessionAdapter;

    private CountDownTimer sessionTimer;
    private long sessionStartTime; // Fallback nếu startedAt không có
    private long sessionResumeTime; // Thời điểm mở màn hình lần này (để tính thời gian thực tế đã tập)
    private AuthService authService;
    private ProfileDAO profileDAO;
    private SessionDAO sessionDAO;
    private ExerciseDAO exerciseDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase
        authService = new AuthService();
        profileDAO = new ProfileDAO();
        sessionDAO = new SessionDAO();
        exerciseDAO = new ExerciseDAO();

        // Get sessionId from intent
        String sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            Log.e(TAG, "Missing sessionId from intent");
            finish();
            return;
        }

        // Get isCompleted flag from intent (default to false)
        boolean isCompleted = getIntent().getBooleanExtra("isCompleted", false);
        
        // If session is completed, hide both buttons immediately
        // Timer will be handled in loadExercisesFromSession to show completed duration
        if (isCompleted) {
            binding.startResumeBtn.setVisibility(View.GONE);
            binding.finishWorkoutBtn.setVisibility(View.GONE);
            Log.d(TAG, "Session is completed, hiding start/resume and finish buttons");
        }

        // Load session data from Firebase
        loadSessionData(sessionId);
    }

    private void loadSessionData(String sessionId) {
        Log.d(TAG, "Loading session data for ID: " + sessionId);

        sessionDAO.loadSessionById(sessionId, new SessionDAO.OnSessionLoadedListener() {
            @Override
            public void onSessionLoaded(Session session) {
                if (session != null) {
                    currentSession = session;
                    Log.d(TAG, "Session loaded successfully");

                    // Load exercises directly from session data
                    loadExercisesFromSession(session);
                } else {
                    Log.e(TAG, "Failed to load session");
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading session: " + error);
                finish();
            }
        });
    }

    private void loadExercisesFromSession(Session session) {
        Log.d(TAG, "Loading exercises from session data");
        
        // Get unique exercise IDs from session perExercise data
        Set<String> exerciseIds = new HashSet<>();
        if (session.getPerExercise() != null) {
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                exerciseIds.add(perExercise.getExerciseId());
            }
        }
        
        Log.d(TAG, "Found " + exerciseIds.size() + " unique exercise IDs");
        
        // Load exercises by IDs
        exerciseDAO.getByIds(new ArrayList<>(exerciseIds), task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<Exercise> exercisesList = new ArrayList<>(task.getResult());
                if (exercisesList != null) {
                    exercises = exercisesList;
                    Log.d(TAG, "Exercises loaded successfully: " + exercisesList.size());

                    // Create a minimal workout template from session data for UI compatibility
                    createMinimalWorkoutTemplate(session);

                    // Now setup UI
                    setupUI();
                    setupRecyclerView();
                    debugSessionData();
                    updateSessionProgress();
                    
                    // Handle timer based on completion status
                    boolean isCompleted = getIntent().getBooleanExtra("isCompleted", false);
                    if (isCompleted) {
                        // If completed, show the duration from session data (endedAt - startedAt)
                        displayCompletedSessionDuration(session);
                    } else {
                        // If not completed, start live timer
                        startSessionTimer();
                    }
                } else {
                    Log.e(TAG, "Failed to load exercises");
                    finish();
                }
            }
        });
    }

    private void createMinimalWorkoutTemplate(Session session) {
        Log.d(TAG, "Creating minimal workout template from session data");
        
        // Create a minimal workout template for UI compatibility
        workoutTemplate = new WorkoutTemplate();
        workoutTemplate.setId("session_" + session.getId());
        workoutTemplate.setTitle(session.getTitle());
        workoutTemplate.setDescription(session.getDescription());
        workoutTemplate.setLevel("beginner"); // Default level
        workoutTemplate.setFocus(Arrays.asList("fullbody")); // Default focus
        workoutTemplate.setGoalFit("general_fitness"); // Default goal
        workoutTemplate.setEstDurationMin(30); // Default duration
        workoutTemplate.setPublic(false);
        workoutTemplate.setCreatedBy("session");
        workoutTemplate.setVersion(1);
        workoutTemplate.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        // Create workout items from session perExercise data
        List<WorkoutTemplate.WorkoutItem> workoutItems = new ArrayList<>();
        if (session.getPerExercise() != null) {
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                WorkoutTemplate.WorkoutItem item = new WorkoutTemplate.WorkoutItem();
                item.setOrder(perExercise.getExerciseNo());
                item.setExerciseId(perExercise.getExerciseId());
                
                // Create config from perExercise data
                WorkoutTemplate.ExerciseConfig config = new WorkoutTemplate.ExerciseConfig();
                if (perExercise.getSets() != null && !perExercise.getSets().isEmpty()) {
                    config.setSets(perExercise.getSets().size());
                    config.setReps(perExercise.getSets().get(0).getTargetReps());
                } else {
                    config.setSets(3); // Default
                    config.setReps(12); // Default
                }
                config.setRestSec(90); // Default rest
                config.setDifficulty(perExercise.getDifficultyUsed() != null ? perExercise.getDifficultyUsed() : "beginner");
                item.setConfigOverride(config);
                
                workoutItems.add(item);
            }
        }
        workoutTemplate.setItems(workoutItems);
        
        Log.d(TAG, "Minimal workout template created with " + workoutItems.size() + " exercises");
    }

    private void setupUI() {
        // Set back button
        binding.backBtn.setOnClickListener(v -> {
            // End session if not completed
            if (!isSessionCompleted()) {
                endSession();
            }
            finish();
        });

        // Set finish workout button
        binding.finishWorkoutBtn.setOnClickListener(v -> {
            finishWorkoutAndNavigate();
        });

        // Set start/resume button
        binding.startResumeBtn.setOnClickListener(v -> startCurrentExercise());

        // Set workout template info
        binding.workoutTitleTxt.setText(workoutTemplate.getTitle());
        binding.workoutDescriptionTxt.setText(workoutTemplate.getDescription());
        // Null check để tránh NullPointerException
        int exerciseCount = (workoutTemplate.getItems() != null) ? workoutTemplate.getItems().size() : 0;
        binding.exerciseCountTxt.setText(exerciseCount + " Exercises");

        // Set workout image (using default image)
        binding.workoutImage.setImageResource(R.drawable.pic_1);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with " + currentSession.getPerExercise().size() + " exercises");

        // Sort exercises by order to match PerExercise order
        List<Exercise> sortedExercises = getExercisesSortedByOrder();

        sessionAdapter = new SessionExerciseAdapter(
                this,
                currentSession.getPerExercise(),
                sortedExercises,
                currentSession,
                workoutTemplate
        );
        sessionAdapter.setOnExerciseClickListener(this);

        binding.exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.exercisesRecyclerView.setAdapter(sessionAdapter);

        Log.d(TAG, "RecyclerView setup complete");
    }

    private void updateSessionProgress() {
        if (currentSession == null || currentSession.getPerExercise() == null) {
            Log.w(TAG, "Cannot update progress: session or perExercise is null");
            return;
        }

        int totalExercises = currentSession.getPerExercise().size();
        int completedExercises = 0;

        Log.d(TAG, "Updating progress for " + totalExercises + " exercises");

        for (int i = 0; i < currentSession.getPerExercise().size(); i++) {
            Session.PerExercise perExercise = currentSession.getPerExercise().get(i);
            String state = perExercise.getState();
            Log.d(TAG, "Exercise " + i + " state: " + state);

            if ("completed".equals(state)) {
                completedExercises++;
            }
        }

        Log.d(TAG, "Progress: " + completedExercises + "/" + totalExercises + " completed");

        // Update progress text
        binding.progressTxt.setText(completedExercises + "/" + totalExercises + " bài tập đã hoàn thành");

        // Update progress bar
        int progressPercent = totalExercises > 0 ? (completedExercises * 100) / totalExercises : 0;
        binding.progressBar.setProgress(progressPercent);
        binding.progressPercentTxt.setText(progressPercent + "% Hoàn thành");

        // Update start/resume button
        updateStartResumeButton();

        // Check if session is completed (from intent)
        boolean isCompleted = getIntent().getBooleanExtra("isCompleted", false);
        
        // If session is completed, hide both buttons
        if (isCompleted) {
            binding.finishWorkoutBtn.setVisibility(View.GONE);
            binding.startResumeBtn.setVisibility(View.GONE);
            // Duration will be shown from session data (endedAt - startedAt)
            if (currentSession != null) {
                displayCompletedSessionDuration(currentSession);
            }
        } else {
            // Show finish button if all exercises completed (but session not fully completed yet)
            if (completedExercises == totalExercises && totalExercises > 0) {
                binding.finishWorkoutBtn.setVisibility(View.VISIBLE);
                binding.startResumeBtn.setVisibility(View.GONE);
            } else {
                binding.finishWorkoutBtn.setVisibility(View.GONE);
            }
        }

        // Update adapter
        if (sessionAdapter != null) {
            Log.d(TAG, "Updating adapter with new session data");
            sessionAdapter.updateSession(currentSession);
        }
    }

    private void startSessionTimer() {
        sessionStartTime = System.currentTimeMillis(); // Fallback
        sessionResumeTime = System.currentTimeMillis(); // Thời điểm mở màn hình lần này
        
        // Hiển thị thời gian ngay lập tức
        updateDurationDisplay();

        sessionTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateDurationDisplay();
            }

            @Override
            public void onFinish() {
                // This should never be called since we use Long.MAX_VALUE
            }
        };
        sessionTimer.start();
    }

    private void updateDurationDisplay() {
        // Check if session is completed - if so, don't update timer
        boolean isCompleted = getIntent().getBooleanExtra("isCompleted", false);
        if (isCompleted) {
            return;
        }
        
        // Lấy thời gian đã tập trước đó từ summary (thời gian thực tế đã tập)
        int previousDurationSec = 0;
        if (currentSession != null && currentSession.getSummary() != null) {
            previousDurationSec = currentSession.getSummary().getDurationSec();
        }
        
        // Tính thời gian từ khi mở màn hình đến hiện tại
        long elapsedSinceResume = System.currentTimeMillis() - sessionResumeTime;
        long currentSessionSec = elapsedSinceResume / 1000;
        
        // Tổng thời gian = thời gian đã tập trước đó + thời gian hiện tại
        long totalSeconds = previousDurationSec + currentSessionSec;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String timeString = String.format("%02d:%02d", minutes, seconds);
        binding.durationTxt.setText(timeString);
    }
    
    /**
     * Lưu thời gian thực tế đã tập vào summary.durationSec
     * Gọi khi pause, destroy, hoặc finish
     */
    private void saveActiveDuration() {
        if (currentSession == null) {
            return;
        }
        
        // Lấy thời gian đã tập trước đó
        int previousDurationSec = 0;
        if (currentSession.getSummary() != null) {
            previousDurationSec = currentSession.getSummary().getDurationSec();
        }
        
        // Tính thời gian từ khi mở màn hình đến hiện tại
        long elapsedSinceResume = System.currentTimeMillis() - sessionResumeTime;
        long currentSessionSec = elapsedSinceResume / 1000;
        
        // Cộng dồn vào summary
        int totalDurationSec = (int)(previousDurationSec + currentSessionSec);
        
        if (currentSession.getSummary() == null) {
            currentSession.setSummary(new Session.SessionSummary());
        }
        currentSession.getSummary().setDurationSec(totalDurationSec);
        
        Log.d(TAG, "Saving active duration: " + totalDurationSec + " seconds (previous: " + previousDurationSec + ", current: " + currentSessionSec + ")");
        
        // Lưu vào Firebase
        sessionDAO.saveSession(currentSession, success -> {
            if (success) {
                Log.d(TAG, "Active duration saved successfully: " + totalDurationSec + " seconds");
            } else {
                Log.e(TAG, "Failed to save active duration");
            }
        });
    }
    
    /**
     * Lấy thời gian thực tế đã tập từ durationTxt (format MM:SS)
     * @return thời gian tính bằng giây
     */
    private int getCurrentActiveDurationFromDisplay() {
        String durationText = binding.durationTxt.getText().toString();
        if (durationText == null || durationText.isEmpty()) {
            return 0;
        }
        
        try {
            // Parse format "MM:SS"
            String[] parts = durationText.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60 + seconds;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing duration from display: " + durationText, e);
        }
        
        return 0;
    }
    
    /**
     * Display duration from completed session data (endedAt - startedAt)
     * Format: "X phút Y giây" to match CompletedExerciseActivity
     */
    private void displayCompletedSessionDuration(Session session) {
        if (session == null || session.getEndedAt() == 0 || session.getStartedAt() == 0) {
            binding.durationTxt.setVisibility(View.GONE);
            return;
        }
        
        // Calculate duration from session data (both in seconds)
        long durationSec = session.getEndedAt() - session.getStartedAt();
        long minutes = durationSec / 60;
        long seconds = durationSec % 60;
        
        // Format: "X phút Y giây" to match CompletedExerciseActivity
        String timeString;
        if (minutes > 0) {
            if (seconds > 0) {
                timeString = minutes + " phút " + seconds + " giây";
            } else {
                timeString = minutes + " phút";
            }
        } else {
            timeString = seconds + " giây";
        }
        
        binding.durationTxt.setText(timeString);
        binding.durationTxt.setVisibility(View.VISIBLE);
        Log.d(TAG, "Displaying completed session duration: " + timeString + " (" + durationSec + " seconds)");
    }

    private boolean isSessionCompleted() {
        if (currentSession == null || currentSession.getPerExercise() == null) {
            return false;
        }

        for (Session.PerExercise perExercise : currentSession.getPerExercise()) {
            if (!"completed".equals(perExercise.getState())) {
                return false;
            }
        }
        return true;
    }

    private void endSession() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        // Lưu thời gian thực tế đã tập trước khi end
        saveActiveDuration();

        // Update session end time
        if (currentSession != null) {
            currentSession.setEndedAt(System.currentTimeMillis() / 1000);

            // Update summary với thời gian thực tế đã tập (đã được lưu trong saveActiveDuration)
            if (currentSession.getSummary() != null) {
                int durationSec = currentSession.getSummary().getDurationSec();
                
                // Load user profile để lấy weight và tính calories bằng METs formula
                loadUserProfileAndCalculateCalories(durationSec, currentSession.getSummary(), () -> {
                    // Save to Firebase after calories calculated
                    saveSessionToFirebase();
                });
            } else {
                // Save to Firebase if no duration update needed
                saveSessionToFirebase();
            }
            sessionDAO.saveSession(currentSession, new SessionDAO.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    Log.d(TAG, "Session ended and saved: " + success);
                }
            });
        }
    }

    /**
     * Finish workout and navigate to CompletedExerciseActivity
     */
    private void finishWorkoutAndNavigate() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        // Update session end time
        if (currentSession != null) {
            currentSession.setEndedAt(System.currentTimeMillis() / 1000);

            // Lấy thời gian thực tế đã tập từ durationTxt (hiển thị trên màn hình)
            int durationSec = getCurrentActiveDurationFromDisplay();
            
            // Nếu không lấy được từ display, tính từ summary + thời gian hiện tại
            if (durationSec == 0) {
                // Lấy thời gian đã tập trước đó
                int previousDurationSec = 0;
                if (currentSession.getSummary() != null) {
                    previousDurationSec = currentSession.getSummary().getDurationSec();
                }
                
                // Tính thời gian từ khi mở màn hình đến hiện tại
                long elapsedSinceResume = System.currentTimeMillis() - sessionResumeTime;
                long currentSessionSec = elapsedSinceResume / 1000;
                
                durationSec = (int)(previousDurationSec + currentSessionSec);
            }

            // Update summary với thời gian thực tế đã tập
            if (currentSession.getSummary() == null) {
                currentSession.setSummary(new Session.SessionSummary());
            }
            currentSession.getSummary().setDurationSec(durationSec);
            
            // Load user profile để lấy weight và tính calories bằng METs formula
            loadUserProfileAndCalculateCalories(durationSec, currentSession.getSummary(), () -> {
                // Save to Firebase and then navigate after calories calculated
                saveSessionAndNavigate();
            });
        } else {
            Log.e(TAG, "Cannot finish workout: currentSession is null");
            finish();
        }
    }

    @Override
    public void onExerciseClick(Session.PerExercise perExercise, Exercise exercise) {
        // This method is no longer used since we only have one start/resume button
        // The actual exercise start logic is handled by startCurrentExercise()
    }

    private void startCurrentExercise() {
        // Find current exercise (not_started or doing)
        Session.PerExercise currentPerExercise = getCurrentExercise();
        if (currentPerExercise == null) {
            Log.w(TAG, "No current exercise found");
            return;
        }

        Log.d(TAG, "Starting exercise with exerciseNo: " + currentPerExercise.getExerciseNo());

        // Start ExerciseActivity with only sessionId and exerciseNo
        Intent intent = new Intent(this, ExerciseActivity.class);
        intent.putExtra("sessionId", currentSession.getId());
        intent.putExtra("exerciseNo", currentPerExercise.getExerciseNo());

        startActivityForResult(intent, EXERCISE_REQUEST_CODE);
    }

    private Session.PerExercise getCurrentExercise() {
        if (currentSession == null || currentSession.getPerExercise() == null) {
            return null;
        }

        // Find the exercise with the lowest exerciseNo that is not_started or doing
        Session.PerExercise currentExercise = null;
        int minExerciseNo = Integer.MAX_VALUE;

        for (Session.PerExercise perExercise : currentSession.getPerExercise()) {
            String state = perExercise.getState();
            if (("not_started".equals(state) || "doing".equals(state)) &&
                    perExercise.getExerciseNo() < minExerciseNo) {
                currentExercise = perExercise;
                minExerciseNo = perExercise.getExerciseNo();
            }
        }
        return currentExercise;
    }

    private Exercise getNextExercise() {
        Session.PerExercise currentPerExercise = getCurrentExercise();
        if (currentPerExercise == null || exercises == null) {
            return null;
        }

        int currentExerciseNo = currentPerExercise.getExerciseNo();

        // Find next not_started exercise with higher exerciseNo
        for (Session.PerExercise nextPerExercise : currentSession.getPerExercise()) {
            if ("not_started".equals(nextPerExercise.getState()) &&
                    nextPerExercise.getExerciseNo() > currentExerciseNo) {
                return getExerciseByOrder(nextPerExercise.getExerciseNo());
            }
        }

        return null; // No next exercise
    }

    private void updateStartResumeButton() {
        // Check if session is completed (from intent) - if so, hide button
        boolean isCompleted = getIntent().getBooleanExtra("isCompleted", false);
        if (isCompleted) {
            binding.startResumeBtn.setVisibility(View.GONE);
            Log.d(TAG, "Session is completed, hiding start/resume button");
            return;
        }
        
        Session.PerExercise currentExercise = getCurrentExercise();
        if (currentExercise == null) {
            binding.startResumeBtn.setVisibility(View.GONE);
            Log.d(TAG, "No current exercise found, hiding start/resume button");
            return;
        }

        Exercise exercise = getExerciseByOrder(currentExercise.getExerciseNo());
        if (exercise == null) {
            binding.startResumeBtn.setVisibility(View.GONE);
            Log.w(TAG, "Exercise not found for ID: " + currentExercise.getExerciseId());
            return;
        }

        binding.startResumeBtn.setVisibility(View.VISIBLE);

        String state = currentExercise.getState();
        if ("doing".equals(state)) {
            binding.startResumeBtn.setText("Tiếp tục " + exercise.getName());
        } else {
            binding.startResumeBtn.setText("Bắt đầu " + exercise.getName());
        }

        Log.d(TAG, "Updated start/resume button for exercise: " + exercise.getName() + " (state: " + state + ")");
    }

    private Exercise getExerciseByOrder(int exerciseOrder) {
        if (exercises == null || workoutTemplate == null || workoutTemplate.getItems() == null) {
            return null;
        }

        // Find WorkoutItem with matching order
        WorkoutTemplate.WorkoutItem workoutItem = null;
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            if (item.getOrder() == exerciseOrder) {
                workoutItem = item;
                break;
            }
        }

        if (workoutItem == null) {
            return null;
        }

        // Find Exercise with matching ID
        for (Exercise exercise : exercises) {
            if (exercise.getId().equals(workoutItem.getExerciseId())) {
                return exercise;
            }
        }

        return null;
    }

    private List<Exercise> getExercisesSortedByOrder() {
        if (exercises == null || workoutTemplate == null || workoutTemplate.getItems() == null) {
            return exercises;
        }

        List<Exercise> sortedExercises = new ArrayList<>();

        // Sort by WorkoutTemplate order
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            for (Exercise exercise : exercises) {
                if (exercise.getId().equals(item.getExerciseId())) {
                    sortedExercises.add(exercise);
                    break;
                }
            }
        }

        Log.d(TAG, "Sorted " + sortedExercises.size() + " exercises by order");
        return sortedExercises;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXERCISE_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

            // Refresh session data from Firebase regardless of result code
            // Because ExerciseActivity saves session data in real-time
            refreshSessionFromFirebase();
        }
    }

    private void refreshSessionFromFirebase() {
        if (currentSession != null && currentSession.getId() != null) {
            Log.d(TAG, "Refreshing session from Firebase: " + currentSession.getId());

            sessionDAO.loadSessionById(currentSession.getId(), new SessionDAO.OnSessionLoadedListener() {
                @Override
                public void onSessionLoaded(Session updatedSession) {
                    if (updatedSession != null) {
                        Log.d(TAG, "Session loaded successfully, updating UI");
                        currentSession = updatedSession;

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            debugSessionData(); // Debug session data
                            updateSessionProgress();
                            Log.d(TAG, "Session progress updated in UI");
                        });
                    } else {
                        Log.w(TAG, "Updated session is null");
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error refreshing session: " + error);
                }
            });
        } else {
            Log.w(TAG, "Cannot refresh session: currentSession or ID is null");
        }
    }

    private void debugSessionData() {
        if (currentSession != null) {
            Log.d(TAG, "=== DEBUG SESSION DATA ===");
            Log.d(TAG, "Session ID: " + currentSession.getId());
            Log.d(TAG, "PerExercises count: " +
                    (currentSession.getPerExercise() != null ? currentSession.getPerExercise().size() : "null"));

            if (currentSession.getPerExercise() != null) {
                for (int i = 0; i < currentSession.getPerExercise().size(); i++) {
                    Session.PerExercise perExercise = currentSession.getPerExercise().get(i);
                    Log.d(TAG, "Exercise " + i + ": ID=" + perExercise.getExerciseId() +
                            ", State=" + perExercise.getState() +
                            ", Sets=" + (perExercise.getSets() != null ? perExercise.getSets().size() : "null"));
                }
            }
            Log.d(TAG, "=== END DEBUG ===");
        } else {
            Log.w(TAG, "Session is null - cannot debug");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Lưu thời gian thực tế đã tập khi pause (người dùng thoát hoặc chuyển app)
        if (sessionTimer != null && !isFinishing()) {
            saveActiveDuration();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }
        // Lưu thời gian thực tế đã tập khi destroy (nếu chưa lưu trong onPause)
        if (!isFinishing()) {
            saveActiveDuration();
        }
    }

    @Override
    public void onBackPressed() {
        // End session if not completed
        if (!isSessionCompleted()) {
            endSession();
        }
        super.onBackPressed();
    }
    
    /**
     * Tính METs trung bình của các exercises trong workout
     * @return METs trung bình, mặc định 5.0 nếu không có exercises
     */
    private double calculateAverageMets() {
        if (exercises == null || exercises.isEmpty()) {
            return 5.0; // Default METs for calisthenics moderate effort
        }
        
        double totalMets = 0;
        int count = 0;
        
        for (Exercise exercise : exercises) {
            if (exercise != null && exercise.getDefaultConfig() != null) {
                double mets = exercise.getDefaultConfig().getMets();
                if (mets > 0) {
                    totalMets += mets;
                    count++;
                }
            }
        }
        
        if (count > 0) {
            return totalMets / count;
        }
        
        return 5.0; // Default
    }
    
    /**
     * Load user profile để lấy weight và tính calories bằng METs formula
     */
    private void loadUserProfileAndCalculateCalories(long durationSec, Session.SessionSummary summary, Runnable onComplete) {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            // Không có user, dùng weight mặc định
            calculateCaloriesWithWeight(70.0, durationSec, summary, onComplete);
            return;
        }
        
        String uid = currentUser.getUid();
        profileDAO.getDocument(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    double weightKg = 70.0; // Default weight
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        if (profile != null && profile.getWeightKg() > 0) {
                            weightKg = profile.getWeightKg();
                        }
                    }
                    
                    calculateCaloriesWithWeight(weightKg, durationSec, summary, onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi load profile, sử dụng weight mặc định", e);
                    // Sử dụng weight mặc định
                    calculateCaloriesWithWeight(70.0, durationSec, summary, onComplete);
                });
    }
    
    /**
     * Tính calories với weight đã có
     * Chỉ tính calories dựa trên exercises đã completed (calculateTotalCaloriesForSession)
     * Nếu chưa tập (estimatedKcal == 0), giữ nguyên giá trị 0
     */
    private void calculateCaloriesWithWeight(double weightKg, long durationSec, Session.SessionSummary summary, Runnable onComplete) {
        int estimatedKcal = 0;
        
        // Nếu có exercises đã load, sử dụng calculateTotalCaloriesForSession (dựa trên thời gian thực của từng set)
        if (exercises != null && !exercises.isEmpty() && currentSession != null && currentSession.getPerExercise() != null) {
            Exercise[] exerciseArray = exercises.toArray(new Exercise[0]);
            estimatedKcal = CalorieCalculator.calculateTotalCaloriesForSession(currentSession, weightKg, exerciseArray);
            if (estimatedKcal > 0) {
                Log.d(TAG, "✅ Calories tính bằng calculateTotalCaloriesForSession: " + estimatedKcal + " kcal (weight: " + weightKg + " kg)");
            } else {
                Log.d(TAG, "ℹ️ Chưa có exercises completed, estKcal = 0");
            }
        }
        
        summary.setEstKcal(estimatedKcal);
        
        if (onComplete != null) {
            onComplete.run();
        }
    }
    
    /**
     * Save session to Firebase
     */
    private void saveSessionToFirebase() {
        if (currentSession != null) {
            sessionDAO.saveSession(currentSession, new SessionDAO.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    Log.d(TAG, "Session ended and saved: " + success);
                }
            });
        }
    }
    
    /**
     * Save session to Firebase and navigate to CompletedExerciseActivity
     */
    private void saveSessionAndNavigate() {
        if (currentSession != null) {
            int durationSec = currentSession.getSummary() != null ? currentSession.getSummary().getDurationSec() : 0;
            int estimatedKcal = currentSession.getSummary() != null ? currentSession.getSummary().getEstKcal() : 0;
            
            Log.d(TAG, "Finishing workout - Duration: " + durationSec + "s (from display), Calories: " + estimatedKcal);
            
            sessionDAO.saveSession(currentSession, new SessionDAO.OnSessionSavedListener() {
                @Override
                public void onSessionSaved(boolean success) {
                    if (success) {
                        Log.d(TAG, "Session saved successfully, navigating to CompletedExerciseActivity");
                        
                        // Navigate to CompletedExerciseActivity
                        Intent intent = new Intent(SessionActivity.this, CompletedExerciseActivity.class);
                        intent.putExtra("sessionId", currentSession.getId());
                        startActivity(intent);
                        
                        // Finish this activity
                        finish();
                    } else {
                        Log.e(TAG, "Failed to save session");
                        // Still navigate even if save failed
                        Intent intent = new Intent(SessionActivity.this, CompletedExerciseActivity.class);
                        intent.putExtra("sessionId", currentSession.getId());
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}
