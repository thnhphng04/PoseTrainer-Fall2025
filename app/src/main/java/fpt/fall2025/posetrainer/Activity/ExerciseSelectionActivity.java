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

        // No need to filter current exercises - allow duplicates
        currentWorkoutExerciseIds = new ArrayList<>();

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
                Log.d(TAG, "Search submitted: " + query);
                filterExercises(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Search text changed: " + newText);
                filterExercises(newText);
                return true;
            }
        });
        
        // Set search view properties
        binding.searchEditText.setIconifiedByDefault(false);
        binding.searchEditText.setFocusable(true);
        binding.searchEditText.requestFocus();

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
                Log.d(TAG, "onExercisesLoaded callback received");
                binding.progressBar.setVisibility(View.GONE);
                binding.exercisesRecyclerView.setVisibility(View.VISIBLE);
                
                if (exercises != null && !exercises.isEmpty()) {
                    allExercises = exercises;
                    // Allow all exercises to be selected (including duplicates)
                    filteredExercises = new ArrayList<>(exercises);
                    
                    Log.d(TAG, "Loaded " + exercises.size() + " exercises from Firebase");
                    
                    // Debug: Log first few exercise names
                    for (int i = 0; i < Math.min(5, exercises.size()); i++) {
                        Exercise ex = exercises.get(i);
                        Log.d(TAG, "Exercise " + i + ": " + (ex != null ? ex.getName() : "null"));
                    }
                    
                    if (adapter != null) {
                        adapter.updateExercises(filteredExercises);
                        Log.d(TAG, "Updated adapter with " + filteredExercises.size() + " exercises");
                    } else {
                        Log.e(TAG, "Adapter is null when trying to update exercises");
                    }
                } else {
                    Log.e(TAG, "No exercises loaded from Firebase - exercises is " + (exercises == null ? "null" : "empty"));
                    Toast.makeText(ExerciseSelectionActivity.this, "No exercises available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Filter exercises based on search query
     */
    private void filterExercises(String query) {
        Log.d(TAG, "filterExercises called with query: '" + query + "'");
        Log.d(TAG, "Total exercises available: " + (allExercises != null ? allExercises.size() : "null"));
        
        if (query == null || query.trim().isEmpty()) {
            // Show all exercises (including duplicates)
            filteredExercises = new ArrayList<>(allExercises);
            Log.d(TAG, "Showing all exercises: " + filteredExercises.size());
        } else {
            filteredExercises = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();
            Log.d(TAG, "Searching for: '" + lowerQuery + "'");
            
            for (Exercise exercise : allExercises) {
                if (exercise == null || exercise.getName() == null) {
                    Log.w(TAG, "Skipping null exercise or exercise with null name");
                    continue;
                }
                
                // Search in name, level, and categories (allow all exercises)
                boolean matchesName = exercise.getName().toLowerCase().contains(lowerQuery);
                boolean matchesLevel = exercise.getLevel() != null && exercise.getLevel().toLowerCase().contains(lowerQuery);
                boolean matchesCategory = false;
                
                if (exercise.getCategory() != null) {
                    for (String category : exercise.getCategory()) {
                        if (category != null && category.toLowerCase().contains(lowerQuery)) {
                            matchesCategory = true;
                            break;
                        }
                    }
                }
                
                if (matchesName || matchesLevel || matchesCategory) {
                    filteredExercises.add(exercise);
                    Log.d(TAG, "Match found: " + exercise.getName() + 
                          " (name:" + matchesName + ", level:" + matchesLevel + ", category:" + matchesCategory + ")");
                }
            }
        }
        
        if (adapter != null) {
            adapter.updateExercises(filteredExercises);
            Log.d(TAG, "Updated adapter with " + filteredExercises.size() + " exercises");
        } else {
            Log.e(TAG, "Adapter is null, cannot update exercises");
        }
        
        Log.d(TAG, "Filtered to " + filteredExercises.size() + " exercises for query: '" + query + "'");
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
