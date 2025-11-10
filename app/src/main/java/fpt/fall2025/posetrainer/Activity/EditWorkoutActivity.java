package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import fpt.fall2025.posetrainer.Adapter.EditWorkoutAdapter;
import fpt.fall2025.posetrainer.Adapter.ExerciseItemTouchHelper;
import fpt.fall2025.posetrainer.Dialog.ExerciseSelectionDialog;
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
    private String selectedLevel = "Người mới bắt đầu"; // Default level (Vietnamese)
    
    // Level mapping: Vietnamese display <-> English database value
    private static final String[] LEVELS_VI = {"Người mới bắt đầu", "Trung bình", "Nâng cao"};
    private static final String[] LEVELS_EN = {"Beginner", "Intermediate", "Advanced"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Get workout template ID from intent
        workoutTemplateId = getIntent().getStringExtra("workoutTemplateId");
        if (workoutTemplateId == null) {
            Toast.makeText(this, "Không có ID workout template", Toast.LENGTH_SHORT).show();
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
            Log.d(TAG, "UID người dùng hiện tại: " + userId);
        } else {
            Log.e(TAG, "Không có người dùng đăng nhập");
            Toast.makeText(this, "Không có người dùng đăng nhập", Toast.LENGTH_SHORT).show();
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
        
        // Setup level spinner
        setupLevelSpinner();
    }

    /**
     * Setup RecyclerView with drag & drop support
     */
    private void setupRecyclerView() {
        binding.exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new EditWorkoutAdapter(exercises, this);
        // Set reorder listener to update duration when exercises are reordered
        adapter.setOnExerciseReorderListener(new EditWorkoutAdapter.OnExerciseReorderListener() {
            @Override
            public void onExerciseMoved(int fromPosition, int toPosition) {
                // Not needed for duration update
            }
            
            @Override
            public void onExercisesReordered() {
                // Update duration when exercises are reordered (drag & drop complete)
                updateDuration();
            }
        });
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
        Log.d(TAG, "Đang tải workout template: " + workoutTemplateId);
        
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
     * Setup level spinner for selecting workout level
     */
    private void setupLevelSpinner() {
        // Set click listener on TextView to show dialog
        binding.levelTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLevelSelectionDialog();
            }
        });
    }
    
    /**
     * Show dialog for level selection (Vietnamese)
     */
    private void showLevelSelectionDialog() {
        int currentSelection = Arrays.asList(LEVELS_VI).indexOf(selectedLevel);
        if (currentSelection < 0) currentSelection = 0;
        
        new AlertDialog.Builder(this)
                .setTitle("Chọn mức độ")
                .setSingleChoiceItems(LEVELS_VI, currentSelection, (dialog, which) -> {
                    selectedLevel = LEVELS_VI[which];
                    binding.levelTxt.setText(selectedLevel);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Convert English level to Vietnamese for display
     */
    private String convertLevelToVietnamese(String englishLevel) {
        if (englishLevel == null || englishLevel.isEmpty()) {
            return LEVELS_VI[0]; // Default to "Người mới bắt đầu"
        }
        
        for (int i = 0; i < LEVELS_EN.length; i++) {
            if (LEVELS_EN[i].equalsIgnoreCase(englishLevel)) {
                return LEVELS_VI[i];
            }
        }
        
        // If not found, try to match case-insensitive
        String lowerLevel = englishLevel.toLowerCase();
        if (lowerLevel.contains("beginner") || lowerLevel.contains("mới")) {
            return LEVELS_VI[0];
        } else if (lowerLevel.contains("intermediate") || lowerLevel.contains("trung")) {
            return LEVELS_VI[1];
        } else if (lowerLevel.contains("advanced") || lowerLevel.contains("nâng")) {
            return LEVELS_VI[2];
        }
        
        return LEVELS_VI[0]; // Default
    }
    
    /**
     * Convert Vietnamese level to English for database
     */
    private String convertLevelToEnglish(String vietnameseLevel) {
        if (vietnameseLevel == null || vietnameseLevel.isEmpty()) {
            return LEVELS_EN[0]; // Default to "Beginner"
        }
        
        for (int i = 0; i < LEVELS_VI.length; i++) {
            if (LEVELS_VI[i].equals(vietnameseLevel)) {
                return LEVELS_EN[i];
            }
        }
        
        // If not found, return as is (might already be English)
        return vietnameseLevel;
    }

    /**
     * Update UI with workout template data
     */
    private void updateWorkoutTemplateUI() {
        if (originalWorkoutTemplate == null) return;

        // Set editable text fields
        binding.titleTxt.setText(originalWorkoutTemplate.getTitle());
        binding.descriptionTxt.setText(originalWorkoutTemplate.getDescription());
        
        // Set level (use template level as default, convert to Vietnamese for display)
        String templateLevel = originalWorkoutTemplate.getLevel();
        selectedLevel = convertLevelToVietnamese(templateLevel);
        binding.levelTxt.setText(selectedLevel);
        
        // Calculate and display duration from exercises
        updateDuration();
    }

    /**
     * Load exercises for the workout template
     */
    private void loadExercises() {
        if (originalWorkoutTemplate == null || originalWorkoutTemplate.getItems() == null) {
            Log.e(TAG, "Không có workout items để tải");
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
                
                // Update duration after loading exercises
                updateDuration();
                
                Log.d(TAG, "Đã tải " + exercises.size() + " bài tập để chỉnh sửa");
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
     * Open exercise selection dialog
     */
    private void openExerciseSelection() {
        ExerciseSelectionDialog dialog = new ExerciseSelectionDialog();
        dialog.setOnExerciseSelectedListener(exercise -> {
            addExerciseToWorkout(exercise);
        });
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        dialog.show(fragmentManager, "ExerciseSelectionDialog");
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
        
        // Update duration after adding exercise
        updateDuration();
        
        Log.d(TAG, "Đã thêm bài tập: " + exercise.getName());
        Toast.makeText(this, "Đã thêm " + exercise.getName() + " vào bài tập", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Calculate total duration from all exercises
     * Formula: For each exercise: (sets * reps * timePerRep) + (sets - 1) * restSec
     * Time per rep is estimated as 2 seconds
     */
    private int calculateTotalDurationMinutes() {
        if (exercises == null || exercises.isEmpty()) {
            return 0;
        }
        
        int totalSeconds = 0;
        final int TIME_PER_REP_SECONDS = 2; // Estimated 2 seconds per rep
        
        for (Exercise exercise : exercises) {
            if (exercise.getDefaultConfig() != null) {
                int sets = exercise.getDefaultConfig().getSets();
                int reps = exercise.getDefaultConfig().getReps();
                int restSec = exercise.getDefaultConfig().getRestSec();
                
                // Default values if not set
                if (sets <= 0) sets = 3;
                if (reps <= 0) reps = 12;
                if (restSec <= 0) restSec = 30;
                
                // Calculate time for this exercise
                // Exercise time = sets * reps * timePerRep
                int exerciseTime = sets * reps * TIME_PER_REP_SECONDS;
                
                // Rest time = (sets - 1) * restSec (rest between sets, not after last set)
                int restTime = (sets - 1) * restSec;
                
                totalSeconds += exerciseTime + restTime;
            } else {
                // Default values if no config
                int sets = 3;
                int reps = 12;
                int restSec = 30;
                
                int exerciseTime = sets * reps * TIME_PER_REP_SECONDS;
                int restTime = (sets - 1) * restSec;
                
                totalSeconds += exerciseTime + restTime;
            }
        }
        
        // Convert to minutes (round up)
        return (int) Math.ceil(totalSeconds / 60.0);
    }
    
    /**
     * Update duration text based on current exercises
     */
    private void updateDuration() {
        int totalMinutes = calculateTotalDurationMinutes();
        if (totalMinutes > 0) {
            binding.durationTxt.setText(totalMinutes + " min");
        } else {
            binding.durationTxt.setText("0 min");
        }
    }

    /**
     * Save user workout to Firebase in new format
     */
    private void saveWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "Không có bài tập để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate title input
        String workoutTitle = binding.titleTxt.getText().toString().trim();
        if (workoutTitle.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bài tập", Toast.LENGTH_SHORT).show();
            binding.titleTxt.requestFocus();
            return;
        }

        Log.d(TAG, "Đang lưu user workout với " + exercises.size() + " bài tập cho userId: " + userId);
        
        // Create user workout in new format
        UserWorkout userWorkout = createUserWorkout();
        
        Log.d(TAG, "Đã tạo user workout: " + userWorkout.getTitle() + " (ID: " + userWorkout.getId() + ", UID: " + userWorkout.getUid() + ")");
        
        // Save to Firebase
        FirebaseService.getInstance().saveUserWorkout(userWorkout, new FirebaseService.OnUserWorkoutSavedListener() {
            @Override
            public void onUserWorkoutSaved(boolean success) {
                if (success) {
                    Log.d(TAG, "Đã lưu user workout thành công: " + userWorkout.toString());
                    Toast.makeText(EditWorkoutActivity.this, "Lưu bài tập thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Lỗi khi lưu user workout");
                    Toast.makeText(EditWorkoutActivity.this, "Lỗi khi lưu bài tập", Toast.LENGTH_SHORT).show();
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
    public void onExerciseRemoved(int position) {
        if (position >= 0 && position < exercises.size()) {
            Exercise removedExercise = exercises.remove(position);
            adapter.notifyItemRemoved(position);
            
            // Update order numbers for remaining items
            adapter.notifyDataSetChanged();
            
            // Update duration after removing exercise
            updateDuration();
            
            Log.d(TAG, "Đã xóa bài tập: " + removedExercise.getName());
            Toast.makeText(this, "Đã xóa " + removedExercise.getName(), Toast.LENGTH_SHORT).show();
        }
    }
}

