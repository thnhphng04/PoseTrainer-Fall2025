package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Adapter.ExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityWorkoutBinding;

import java.util.ArrayList;

public class WorkoutActivity extends AppCompatActivity implements ExerciseAdapter.OnSetsRepsChangedListener, ExerciseAdapter.OnDifficultyChangedListener {
    private static final String TAG = "WorkoutActivity";
    ActivityWorkoutBinding binding;
    private WorkoutTemplate workoutTemplate;
    private ArrayList<Exercise> exercises;
    private int[] exerciseSets;
    private int[] exerciseReps;
    private String[] exerciseDifficulties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutBinding.inflate(getLayoutInflater());
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
    }
    
    private void startWorkout() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "No exercises available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Start with first exercise
        Exercise firstExercise = exercises.get(0);
        Intent intent = new Intent(this, ExerciseActivity.class);
        intent.putExtra("exercise", firstExercise);
        intent.putExtra("sets", exerciseSets[0]); // Use adjusted sets
        intent.putExtra("reps", exerciseReps[0]); // Use adjusted reps
        intent.putExtra("workoutTemplate", workoutTemplate);
        intent.putExtra("exerciseIndex", 0);
        intent.putExtra("totalExercises", exercises.size());
        
        // Pass next exercise if available
        if (exercises.size() > 1) {
            intent.putExtra("nextExercise", exercises.get(1));
        }
        
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
                    Toast.makeText(this, "Workout completed!", Toast.LENGTH_LONG).show();
                }
            }
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
        
        // If there's a next exercise after this one, pass it
        if (nextExerciseIndex + 1 < exercises.size()) {
            intent.putExtra("nextExercise", exercises.get(nextExerciseIndex + 1));
        }
        
        startActivityForResult(intent, 1001);
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
            }
        });
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
                exercises = loadedExercises;
                
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

    /**
     * Get image resource based on workout template focus/type
     */
    private int getImageResourceForWorkout(WorkoutTemplate workoutTemplate) {
        return FirebaseService.getInstance().getImageResourceForWorkout(workoutTemplate, this);
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