package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Adapter.EditWorkoutAdapter;
import fpt.fall2025.posetrainer.Adapter.ExerciseItemTouchHelper;
import fpt.fall2025.posetrainer.Activity.ExerciseSelectionActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
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
    private String userId = "uid_1"; // TODO: Get from authenticated user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Get workout template ID from intent
        workoutTemplateId = getIntent().getStringExtra("workoutTemplateId");
        if (workoutTemplateId == null) {
            Toast.makeText(this, "No workout template ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeData();
        setupUI();
        loadWorkoutTemplate();
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

        binding.titleTxt.setText(originalWorkoutTemplate.getTitle());
        binding.descriptionTxt.setText(originalWorkoutTemplate.getDescription());
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
                    Log.d(TAG, "Loaded " + availableExercises.size() + " exercises from Firebase");
                    // Filter out exercises that are already in the workout
                    ArrayList<Exercise> filteredExercises = filterAvailableExercises(availableExercises);
                    showExerciseSelectionDialog(filteredExercises);
                } else {
                    Log.e(TAG, "No exercises loaded from Firebase");
                    Toast.makeText(EditWorkoutActivity.this, "No exercises available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Filter out exercises that are already in the workout
     */
    private ArrayList<Exercise> filterAvailableExercises(ArrayList<Exercise> allExercises) {
        ArrayList<Exercise> filteredExercises = new ArrayList<>();
        
        for (Exercise exercise : allExercises) {
            boolean alreadyInWorkout = false;
            for (Exercise workoutExercise : exercises) {
                if (exercise.getId().equals(workoutExercise.getId())) {
                    alreadyInWorkout = true;
                    break;
                }
            }
            if (!alreadyInWorkout) {
                filteredExercises.add(exercise);
            }
        }
        
        Log.d(TAG, "Filtered " + filteredExercises.size() + " available exercises (removed " + (allExercises.size() - filteredExercises.size()) + " duplicates)");
        return filteredExercises;
    }
    
    /**
     * Show exercise selection activity
     */
    private void showExerciseSelectionDialog(ArrayList<Exercise> availableExercises) {
        // Get current exercise IDs to filter out
        ArrayList<String> currentExerciseIds = new ArrayList<>();
        for (Exercise exercise : exercises) {
            currentExerciseIds.add(exercise.getId());
        }
        
        // Start ExerciseSelectionActivity
        Intent intent = new Intent(this, ExerciseSelectionActivity.class);
        intent.putStringArrayListExtra("currentWorkoutExerciseIds", currentExerciseIds);
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
     * Save workout template to Firebase
     */
    private void saveWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "No exercises to save", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving workout with " + exercises.size() + " exercises for userId: " + userId);
        
        // Create updated workout template
        WorkoutTemplate updatedTemplate = createUpdatedWorkoutTemplate();
        
        Log.d(TAG, "Created workout template: " + updatedTemplate.getTitle() + " (ID: " + updatedTemplate.getId() + ", CreatedBy: " + updatedTemplate.getCreatedBy() + ")");
        
        // Save to Firebase
        FirebaseService.getInstance().saveUserWorkoutTemplate(userId, updatedTemplate, new FirebaseService.OnWorkoutTemplateSavedListener() {
            @Override
            public void onWorkoutTemplateSaved(boolean success) {
                if (success) {
                    Log.d(TAG, "Workout saved successfully: " + updatedTemplate.toString());
                    Toast.makeText(EditWorkoutActivity.this, "Workout saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Failed to save workout");
                    Toast.makeText(EditWorkoutActivity.this, "Failed to save workout", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Create updated workout template with current exercises
     */
    private WorkoutTemplate createUpdatedWorkoutTemplate() {
        WorkoutTemplate updatedTemplate = new WorkoutTemplate();
        
        // Copy basic info from original template
        updatedTemplate.setId(originalWorkoutTemplate.getId());
        updatedTemplate.setTitle(originalWorkoutTemplate.getTitle());
        updatedTemplate.setDescription(originalWorkoutTemplate.getDescription());
        updatedTemplate.setLevel(originalWorkoutTemplate.getLevel());
        updatedTemplate.setFocus(originalWorkoutTemplate.getFocus());
        updatedTemplate.setGoalFit(originalWorkoutTemplate.getGoalFit());
        updatedTemplate.setEstDurationMin(originalWorkoutTemplate.getEstDurationMin());
        updatedTemplate.setPublic(false); // User's custom workout
        updatedTemplate.setCreatedBy("user_" + userId);
        updatedTemplate.setVersion(originalWorkoutTemplate.getVersion() + 1);
        updatedTemplate.setUpdatedAt(System.currentTimeMillis());

        // Create new workout items based on current exercises
        List<WorkoutTemplate.WorkoutItem> newItems = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            
            WorkoutTemplate.WorkoutItem item = new WorkoutTemplate.WorkoutItem();
            item.setOrder(i + 1);
            item.setExerciseId(exercise.getId());
            
            // Use default config from exercise
            if (exercise.getDefaultConfig() != null) {
                WorkoutTemplate.ExerciseConfig config = new WorkoutTemplate.ExerciseConfig();
                config.setSets(exercise.getDefaultConfig().getSets());
                config.setReps(exercise.getDefaultConfig().getReps());
                config.setRestSec(exercise.getDefaultConfig().getRestSec());
                config.setDifficulty(exercise.getDefaultConfig().getDifficulty());
                item.setConfigOverride(config);
            }
            
            newItems.add(item);
        }
        
        updatedTemplate.setItems(newItems);
        
        return updatedTemplate;
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

