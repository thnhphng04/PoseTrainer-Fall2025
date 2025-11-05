package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fpt.fall2025.posetrainer.Adapter.EditWorkoutAdapter;
import fpt.fall2025.posetrainer.Adapter.ExerciseItemTouchHelper;
import fpt.fall2025.posetrainer.Activity.ExerciseSelectionActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityEditWorkoutBinding;

/**
 * EditWorkoutActivity - Activity để chỉnh sửa workout template
 * Cho phép thêm, xóa, sắp xếp lại thứ tự các bài tập
 */
public class EditWorkoutActivity extends AppCompatActivity implements EditWorkoutAdapter.OnExerciseRemovedListener {
    private static final String TAG = "EditWorkoutActivity";
    
    private ActivityEditWorkoutBinding binding;
    private WorkoutTemplate originalWorkoutTemplate;
    private ArrayList<Exercise> exercises;
    private EditWorkoutAdapter adapter;
    private String workoutTemplateId;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Get workout template ID from intent
        workoutTemplateId = getIntent().getStringExtra("workoutTemplateId");
        if (workoutTemplateId == null) {
            Toast.makeText(this, "No workout template ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Get current user UID
        getCurrentUserId();

        initializeData();
        setupUI();
        loadWorkoutTemplate();
    }

    /**
     * Get current user UID from Firebase Authentication
     */
    private void getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Current user UID: " + userId);
        } else {
            Log.e(TAG, "No user logged in");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialize data structures
     */
    private void initializeData() {
        exercises = new ArrayList<>();
    }


    /**
     * Setup UI components
     */
    private void setupUI() {
        // Back button
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Save button
        binding.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWorkout();
            }
        });

        // Add exercise button
        binding.addExerciseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExerciseSelection();
            }
        });

        // Setup RecyclerView
        setupRecyclerView();
    }

    /**
     * Setup RecyclerView with drag & drop support
     */
    private void setupRecyclerView() {
        binding.exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EditWorkoutAdapter(exercises, this);
        binding.exercisesRecyclerView.setAdapter(adapter);

        // Setup drag & drop with ExerciseItemTouchHelper
        ExerciseItemTouchHelper callback = new ExerciseItemTouchHelper(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView);
    }

    /**
     * Load workout template from Firebase
     */
    private void loadWorkoutTemplate() {
        Log.d(TAG, "Loading workout template: " + workoutTemplateId);
        
        FirebaseService.getInstance().loadWorkoutTemplateById(workoutTemplateId, this, new FirebaseService.OnWorkoutTemplateLoadedListener() {
            @Override
            public void onWorkoutTemplateLoaded(WorkoutTemplate template) {
                originalWorkoutTemplate = template;
                
                // Update UI
                updateWorkoutTemplateUI();
                
                // Load exercises for this template
                loadExercises();
            }
        });
    }

    /**
     * Update UI with workout template data
     */
    private void updateWorkoutTemplateUI() {
        if (originalWorkoutTemplate == null) return;

        // Set editable text fields
        binding.titleTxt.setText(originalWorkoutTemplate.getTitle());
        binding.descriptionTxt.setText(originalWorkoutTemplate.getDescription());
        
        // Set read-only display fields
        binding.levelTxt.setText(originalWorkoutTemplate.getLevel());
        binding.durationTxt.setText(originalWorkoutTemplate.getEstDurationMin() + " min");
    }

    /**
     * Load exercises for the workout template
     */
    private void loadExercises() {
        if (originalWorkoutTemplate == null || originalWorkoutTemplate.getItems() == null) {
            Log.e(TAG, "No workout items to load");
            return;
        }

        // Extract exercise IDs from workout template
        ArrayList<String> exerciseIds = new ArrayList<>();
        for (WorkoutTemplate.WorkoutItem item : originalWorkoutTemplate.getItems()) {
            exerciseIds.add(item.getExerciseId());
        }

        FirebaseService.getInstance().loadExercisesByIds(exerciseIds, this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> loadedExercises) {
                // Sort exercises by WorkoutTemplate order
                exercises = sortExercisesByTemplateOrder(loadedExercises);
                
                // Update adapter
                adapter.updateExercises(exercises);
                
                Log.d(TAG, "Loaded " + exercises.size() + " exercises for editing");
            }
        });
    }

    /**
     * Sort exercises by WorkoutTemplate order
     */
    private ArrayList<Exercise> sortExercisesByTemplateOrder(ArrayList<Exercise> loadedExercises) {
        if (originalWorkoutTemplate == null || originalWorkoutTemplate.getItems() == null || loadedExercises == null) {
            return loadedExercises;
        }
        
        ArrayList<Exercise> sortedExercises = new ArrayList<>();
        
        // Sort by WorkoutTemplate order
        for (WorkoutTemplate.WorkoutItem item : originalWorkoutTemplate.getItems()) {
            for (Exercise exercise : loadedExercises) {
                if (exercise.getId().equals(item.getExerciseId())) {
                    sortedExercises.add(exercise);
                    break;
                }
            }
        }
        
        return sortedExercises;
    }

    /**
     * Open exercise selection dialog/activity
     */
    private void openExerciseSelection() {
        // Load all available exercises from Firebase
        loadAllExercises();
    }
    
    /**
     * Load all available exercises from Firebase
     */
    private void loadAllExercises() {
        Log.d(TAG, "Loading all exercises from Firebase for selection");
        
        // Show loading indicator
        Toast.makeText(this, "Loading exercises...", Toast.LENGTH_SHORT).show();
        
        // Load all exercises from Firebase
        FirebaseService.getInstance().loadAllExercises(this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> availableExercises) {
                if (availableExercises != null && !availableExercises.isEmpty()) {
                    Log.d(TAG, "Loaded " + availableExercises.size() + " exercises from Firebase - all available for selection");
                    // Allow all exercises to be selected (including duplicates)
                    showExerciseSelectionDialog(availableExercises);
                } else {
                    Log.e(TAG, "No exercises loaded from Firebase");
                    Toast.makeText(EditWorkoutActivity.this, "No exercises available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    
    /**
     * Show exercise selection activity
     */
    private void showExerciseSelectionDialog(ArrayList<Exercise> availableExercises) {
        // Start ExerciseSelectionActivity without filtering current exercises
        Intent intent = new Intent(this, ExerciseSelectionActivity.class);
        // Don't pass currentWorkoutExerciseIds to allow duplicate exercises
        startActivityForResult(intent, 1001); // Request code for exercise selection
    }
    
    /**
     * Add selected exercise to workout
     */
    private void addExerciseToWorkout(Exercise exercise) {
        // Add exercise to the end of the list
        exercises.add(exercise);
        
        // Update adapter
        adapter.notifyItemInserted(exercises.size() - 1);
        
        // Update order numbers
        adapter.notifyDataSetChanged();
        
        Log.d(TAG, "Added exercise: " + exercise.getName());
        Toast.makeText(this, "Added " + exercise.getName() + " to workout", Toast.LENGTH_SHORT).show();
    }

    /**
     * Save user workout to Firebase in new format
     */
    private void saveWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "No exercises to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate title input
        String workoutTitle = binding.titleTxt.getText().toString().trim();
        if (workoutTitle.isEmpty()) {
            Toast.makeText(this, "Please enter a workout title", Toast.LENGTH_SHORT).show();
            binding.titleTxt.requestFocus();
            return;
        }

        Log.d(TAG, "Saving user workout with " + exercises.size() + " exercises for userId: " + userId);
        
        // Create user workout in new format
        UserWorkout userWorkout = createUserWorkout();
        
        Log.d(TAG, "Created user workout: " + userWorkout.getTitle() + " (ID: " + userWorkout.getId() + ", UID: " + userWorkout.getUid() + ")");
        
        // Save to Firebase
        FirebaseService.getInstance().saveUserWorkout(userWorkout, new FirebaseService.OnUserWorkoutSavedListener() {
            @Override
            public void onUserWorkoutSaved(boolean success) {
                if (success) {
                    Log.d(TAG, "User workout saved successfully: " + userWorkout.toString());
                    Toast.makeText(EditWorkoutActivity.this, "Workout saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Failed to save user workout");
                    Toast.makeText(EditWorkoutActivity.this, "Failed to save workout", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Generate unique ID for user workout with user ID
     * Format: uw_[userId]_[random]
     */
    private String generateUserWorkoutId() {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return "uw_" + userId + "_" + randomPart;
    }

    /**
     * Create UserWorkout from current exercises in new format
     */
    private UserWorkout createUserWorkout() {
        UserWorkout userWorkout = new UserWorkout();
        
        // Generate unique ID for user workout
        userWorkout.setId(generateUserWorkoutId());
        userWorkout.setUid(userId);
        
        // Get title and description from EditText fields
        String workoutTitle = binding.titleTxt.getText().toString().trim();
        String workoutDescription = binding.descriptionTxt.getText().toString().trim();
        
        // Use original values as fallback if fields are empty
        if (workoutTitle.isEmpty()) {
            workoutTitle = originalWorkoutTemplate.getTitle();
        }
        if (workoutDescription.isEmpty()) {
            workoutDescription = originalWorkoutTemplate.getDescription();
        }
        
        userWorkout.setTitle(workoutTitle);
        userWorkout.setDescription(workoutDescription);
        userWorkout.setSource("custom");
        
        // Set timestamps in seconds
        long currentTime = System.currentTimeMillis() / 1000;
        userWorkout.setCreatedAt(currentTime);
        userWorkout.setUpdatedAt(currentTime);

        // Create new workout items based on current exercises
        List<UserWorkout.UserWorkoutItem> newItems = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            
            UserWorkout.UserWorkoutItem item = new UserWorkout.UserWorkoutItem();
            item.setOrder(i + 1);
            item.setExerciseId(exercise.getId());
            
            // Use default config from exercise
            if (exercise.getDefaultConfig() != null) {
                UserWorkout.ExerciseConfig config = new UserWorkout.ExerciseConfig();
                config.setSets(exercise.getDefaultConfig().getSets());
                config.setReps(exercise.getDefaultConfig().getReps());
                config.setRestSec(exercise.getDefaultConfig().getRestSec());
                config.setDifficulty(exercise.getDefaultConfig().getDifficulty());
                item.setConfig(config);
            }
            
            newItems.add(item);
        }
        
        userWorkout.setItems(newItems);
        
        return userWorkout;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Exercise selectedExercise = (Exercise) data.getSerializableExtra("selectedExercise");
            if (selectedExercise != null) {
                addExerciseToWorkout(selectedExercise);
            }
        }
    }

    @Override
    public void onExerciseRemoved(int position) {
        if (position >= 0 && position < exercises.size()) {
            Exercise removedExercise = exercises.remove(position);
            adapter.notifyItemRemoved(position);
            
            // Update order numbers for remaining items
            adapter.notifyDataSetChanged();
            
            Log.d(TAG, "Exercise removed: " + removedExercise.getName());
            Toast.makeText(this, "Removed " + removedExercise.getName(), Toast.LENGTH_SHORT).show();
        }
    }
}

