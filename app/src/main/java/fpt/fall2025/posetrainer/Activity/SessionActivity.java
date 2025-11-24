package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fpt.fall2025.posetrainer.Adapter.SessionExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
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
    private long sessionStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get sessionId from intent
        String sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            Log.e(TAG, "Missing sessionId from intent");
            finish();
            return;
        }

        // Load session data from Firebase
        loadSessionData(sessionId);
    }

    private void loadSessionData(String sessionId) {
        Log.d(TAG, "Loading session data for ID: " + sessionId);

        FirebaseService.getInstance().loadSessionById(sessionId, new FirebaseService.OnSessionLoadedListener() {
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
        FirebaseService.getInstance().loadExercisesByIds(new ArrayList<>(exerciseIds), this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> exercisesList) {
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
                    startSessionTimer();
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
        binding.exerciseCountTxt.setText(workoutTemplate.getItems().size() + " Exercises");

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
        binding.progressTxt.setText(completedExercises + "/" + totalExercises + " exercises completed");

        // Update progress bar
        int progressPercent = totalExercises > 0 ? (completedExercises * 100) / totalExercises : 0;
        binding.progressBar.setProgress(progressPercent);
        binding.progressPercentTxt.setText(progressPercent + "% Complete");

        // Update start/resume button
        updateStartResumeButton();

        // Show finish button if all exercises completed
        if (completedExercises == totalExercises && totalExercises > 0) {
            binding.finishWorkoutBtn.setVisibility(View.VISIBLE);
            binding.startResumeBtn.setVisibility(View.GONE);
        } else {
            binding.finishWorkoutBtn.setVisibility(View.GONE);
        }

        // Update adapter
        if (sessionAdapter != null) {
            Log.d(TAG, "Updating adapter with new session data");
            sessionAdapter.updateSession(currentSession);
        }
    }

    private void startSessionTimer() {
        sessionStartTime = System.currentTimeMillis();

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
        long elapsedTime = System.currentTimeMillis() - sessionStartTime;
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        String timeString = String.format("%02d:%02d", minutes, seconds);
        binding.durationTxt.setText(timeString);
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

        // Update session end time
        if (currentSession != null) {
            currentSession.setEndedAt(System.currentTimeMillis() / 1000);

            // Update summary
            if (currentSession.getSummary() != null) {
                long durationSec = (System.currentTimeMillis() - sessionStartTime) / 1000;
                currentSession.getSummary().setDurationSec((int) durationSec);
                
                // Estimate calories (simple calculation based on duration)
                int estimatedKcal = (int) (durationSec * 0.1); // Rough estimate: 0.1 kcal per second
                currentSession.getSummary().setEstKcal(estimatedKcal);
            }

            // Save to Firebase
            FirebaseService.getInstance().saveSession(currentSession, new FirebaseService.OnSessionSavedListener() {
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

            // Calculate duration
            long durationSec;
            if (currentSession.getStartedAt() > 0) {
                durationSec = currentSession.getEndedAt() - currentSession.getStartedAt();
            } else {
                // Fallback: use sessionStartTime if startedAt is not set
                durationSec = (System.currentTimeMillis() - sessionStartTime) / 1000;
            }

            // Update summary
            if (currentSession.getSummary() == null) {
                currentSession.setSummary(new Session.SessionSummary());
            }
            currentSession.getSummary().setDurationSec((int) durationSec);
            
            // Estimate calories (simple calculation based on duration)
            int estimatedKcal = (int) (durationSec * 0.1); // Rough estimate: 0.1 kcal per second
            currentSession.getSummary().setEstKcal(estimatedKcal);

            Log.d(TAG, "Finishing workout - Duration: " + durationSec + "s, Calories: " + estimatedKcal);

            // Save to Firebase and then navigate
            FirebaseService.getInstance().saveSession(currentSession, new FirebaseService.OnSessionSavedListener() {
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
            binding.startResumeBtn.setText("Resume " + exercise.getName());
        } else {
            binding.startResumeBtn.setText("Start " + exercise.getName());
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

            FirebaseService.getInstance().loadSessionById(currentSession.getId(), new FirebaseService.OnSessionLoadedListener() {
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
    protected void onDestroy() {
        super.onDestroy();
        if (sessionTimer != null) {
            sessionTimer.cancel();
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
}
