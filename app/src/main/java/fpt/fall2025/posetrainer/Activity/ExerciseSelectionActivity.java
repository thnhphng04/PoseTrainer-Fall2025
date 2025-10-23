package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Adapter.ExerciseSelectionAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityExerciseSelectionBinding;

/**
 * ExerciseSelectionActivity - Activity để chọn exercise từ danh sách có sẵn
 * Hiển thị danh sách exercises với khả năng tìm kiếm và lọc
 */
public class ExerciseSelectionActivity extends AppCompatActivity implements ExerciseSelectionAdapter.OnExerciseSelectedListener {
    private static final String TAG = "ExerciseSelectionActivity";
    
    private ActivityExerciseSelectionBinding binding;
    private ArrayList<Exercise> allExercises;
    private ArrayList<Exercise> filteredExercises;
    private ExerciseSelectionAdapter adapter;
    private ArrayList<String> currentWorkoutExerciseIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Get current workout exercise IDs from intent
        currentWorkoutExerciseIds = getIntent().getStringArrayListExtra("currentWorkoutExerciseIds");
        if (currentWorkoutExerciseIds == null) {
            currentWorkoutExerciseIds = new ArrayList<>();
        }

        initializeData();
        setupUI();
        loadExercises();
    }

    /**
     * Initialize data structures
     */
    private void initializeData() {
        allExercises = new ArrayList<>();
        filteredExercises = new ArrayList<>();
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

        // Overlay click to close
        binding.overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Search functionality
        binding.searchEditText.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterExercises(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterExercises(newText);
                return true;
            }
        });

        // Setup RecyclerView
        setupRecyclerView();
    }

    /**
     * Setup RecyclerView
     */
    private void setupRecyclerView() {
        binding.exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new ExerciseSelectionAdapter(filteredExercises, this);
        binding.exercisesRecyclerView.setAdapter(adapter);
    }

    /**
     * Load exercises from Firebase
     */
    private void loadExercises() {
        Log.d(TAG, "Loading exercises from Firebase");
        
        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.exercisesRecyclerView.setVisibility(View.GONE);
        
        FirebaseService.getInstance().loadAllExercises(this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> exercises) {
                binding.progressBar.setVisibility(View.GONE);
                binding.exercisesRecyclerView.setVisibility(View.VISIBLE);
                
                if (exercises != null && !exercises.isEmpty()) {
                    allExercises = exercises;
                    // Filter out exercises that are already in the workout
                    filteredExercises = filterAvailableExercises(exercises);
                    adapter.updateExercises(filteredExercises);
                    
                    Log.d(TAG, "Loaded " + exercises.size() + " exercises, " + filteredExercises.size() + " available");
                } else {
                    Log.e(TAG, "No exercises loaded from Firebase");
                    Toast.makeText(ExerciseSelectionActivity.this, "No exercises available", Toast.LENGTH_SHORT).show();
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
            boolean alreadyInWorkout = currentWorkoutExerciseIds.contains(exercise.getId());
            if (!alreadyInWorkout) {
                filteredExercises.add(exercise);
            }
        }
        
        Log.d(TAG, "Filtered " + filteredExercises.size() + " available exercises (removed " + (allExercises.size() - filteredExercises.size()) + " duplicates)");
        return filteredExercises;
    }

    /**
     * Filter exercises based on search query
     */
    private void filterExercises(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredExercises = filterAvailableExercises(allExercises);
        } else {
            filteredExercises = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();
            
            for (Exercise exercise : allExercises) {
                boolean alreadyInWorkout = currentWorkoutExerciseIds.contains(exercise.getId());
                if (!alreadyInWorkout) {
                    // Search in name, level, and categories
                    boolean matchesName = exercise.getName().toLowerCase().contains(lowerQuery);
                    boolean matchesLevel = exercise.getLevel().toLowerCase().contains(lowerQuery);
                    boolean matchesCategory = false;
                    
                    if (exercise.getCategory() != null) {
                        for (String category : exercise.getCategory()) {
                            if (category.toLowerCase().contains(lowerQuery)) {
                                matchesCategory = true;
                                break;
                            }
                        }
                    }
                    
                    if (matchesName || matchesLevel || matchesCategory) {
                        filteredExercises.add(exercise);
                    }
                }
            }
        }
        
        adapter.updateExercises(filteredExercises);
        Log.d(TAG, "Filtered to " + filteredExercises.size() + " exercises for query: " + query);
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        Log.d(TAG, "Exercise selected: " + exercise.getName());
        
        // Return selected exercise to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedExercise", exercise);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
