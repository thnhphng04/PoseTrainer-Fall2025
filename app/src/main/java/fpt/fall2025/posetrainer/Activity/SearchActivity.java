package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import fpt.fall2025.posetrainer.Adapter.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private ActivitySearchBinding binding;
    private ArrayList<WorkoutTemplate> allWorkouts;
    private ArrayList<WorkoutTemplate> filteredWorkouts;
    private WorkoutTemplateAdapter adapter;
    
    // Exercise cache
    private Map<String, Exercise> exerciseMap; // exerciseId -> Exercise
    private ArrayList<Exercise> allExercises;
    
    // Filter states
    private String selectedCategory = "Tất cả";
    private String selectedDuration = "Tất cả";
    private String selectedExerciseCategory = "Tất cả";
    private String selectedBodyPart = "Tất cả";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allWorkouts = new ArrayList<>();
        filteredWorkouts = new ArrayList<>();
        exerciseMap = new HashMap<>();
        allExercises = new ArrayList<>();

        setupRecyclerView();
        setupSearchBar();
        setupFilterChips();
        setupBackButton();
        loadExercises(); // Load exercises first
        loadWorkouts();
    }

    private void setupRecyclerView() {
        // Grid layout với 2 cột
        binding.rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new WorkoutTemplateAdapter(filteredWorkouts);
        binding.rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchBar() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                filterWorkouts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        // Category chips (workout level)
        binding.chipAll.setOnClickListener(v -> selectCategory("Tất cả"));
        binding.chipBeginner.setOnClickListener(v -> selectCategory("Beginner"));
        binding.chipIntermediate.setOnClickListener(v -> selectCategory("Intermediate"));
        binding.chipAdvanced.setOnClickListener(v -> selectCategory("Advanced"));

        // Duration chips
        binding.chipDurationAll.setOnClickListener(v -> selectDuration("Tất cả"));
        binding.chip17min.setOnClickListener(v -> selectDuration("1-7 min"));
        binding.chip815min.setOnClickListener(v -> selectDuration("8-15 min"));
        binding.chip15minPlus.setOnClickListener(v -> selectDuration(">15 min"));
        
        // Exercise category and body part filters will be added dynamically after exercises are loaded
    }
    
    /**
     * Setup dynamic filter chips based on available exercise categories and muscles
     */
    private void setupDynamicFilterChips() {
        if (allExercises == null || allExercises.isEmpty()) {
            return;
        }
        
        // Collect unique exercise categories
        Set<String> exerciseCategories = new HashSet<>();
        Set<String> bodyParts = new HashSet<>();
        
        for (Exercise exercise : allExercises) {
            if (exercise.getCategory() != null) {
                for (String category : exercise.getCategory()) {
                    if (category != null && !category.isEmpty()) {
                        exerciseCategories.add(category);
                    }
                }
            }
            if (exercise.getMuscles() != null) {
                for (String muscle : exercise.getMuscles()) {
                    if (muscle != null && !muscle.isEmpty()) {
                        bodyParts.add(muscle);
                    }
                }
            }
        }
        
        // Setup exercise category chips
        setupExerciseCategoryChips(new ArrayList<>(exerciseCategories));
        
        // Setup body part chips
        setupBodyPartChips(new ArrayList<>(bodyParts));
    }
    
    private void setupExerciseCategoryChips(ArrayList<String> categories) {
        binding.llExerciseCategoryFilter.removeAllViews();
        
        // Add "Tất cả" chip
        TextView allChip = createFilterChip("Tất cả", true);
        allChip.setOnClickListener(v -> selectExerciseCategory("Tất cả"));
        binding.llExerciseCategoryFilter.addView(allChip);
        
        // Sort categories for better UX
        java.util.Collections.sort(categories);
        
        // Add category chips
        for (String category : categories) {
            TextView chip = createFilterChip(category, false);
            chip.setOnClickListener(v -> selectExerciseCategory(category));
            binding.llExerciseCategoryFilter.addView(chip);
        }
    }
    
    private void setupBodyPartChips(ArrayList<String> bodyParts) {
        binding.llBodyPartFilter.removeAllViews();
        
        // Add "Tất cả" chip
        TextView allChip = createFilterChip("Tất cả", true);
        allChip.setOnClickListener(v -> selectBodyPart("Tất cả"));
        binding.llBodyPartFilter.addView(allChip);
        
        // Sort body parts for better UX
        java.util.Collections.sort(bodyParts);
        
        // Add body part chips
        for (String bodyPart : bodyParts) {
            TextView chip = createFilterChip(bodyPart, false);
            chip.setOnClickListener(v -> selectBodyPart(bodyPart));
            binding.llBodyPartFilter.addView(chip);
        }
    }
    
    private TextView createFilterChip(String text, boolean isSelected) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setPadding(24, 12, 24, 12);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        chip.setTextSize(12);
        chip.setBackgroundResource(isSelected ? R.drawable.chip_selected_bg : R.drawable.chip_bg);
        chip.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) chip.getLayoutParams();
        params.setMargins(0, 0, 8, 0);
        chip.setLayoutParams(params);
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }
    
    private void selectExerciseCategory(String category) {
        selectedExerciseCategory = category;
        updateExerciseCategoryChipStates();
        filterWorkouts();
    }
    
    private void selectBodyPart(String bodyPart) {
        selectedBodyPart = bodyPart;
        updateBodyPartChipStates();
        filterWorkouts();
    }
    
    private void updateExerciseCategoryChipStates() {
        for (int i = 0; i < binding.llExerciseCategoryFilter.getChildCount(); i++) {
            View child = binding.llExerciseCategoryFilter.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                if (chip.getText().toString().equals(selectedExerciseCategory)) {
                    chip.setBackgroundResource(R.drawable.chip_selected_bg);
                } else {
                    chip.setBackgroundResource(R.drawable.chip_bg);
                }
            }
        }
    }
    
    private void updateBodyPartChipStates() {
        for (int i = 0; i < binding.llBodyPartFilter.getChildCount(); i++) {
            View child = binding.llBodyPartFilter.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                if (chip.getText().toString().equals(selectedBodyPart)) {
                    chip.setBackgroundResource(R.drawable.chip_selected_bg);
                } else {
                    chip.setBackgroundResource(R.drawable.chip_bg);
                }
            }
        }
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateCategoryChipStates();
        filterWorkouts();
    }

    private void selectDuration(String duration) {
        selectedDuration = duration;
        updateDurationChipStates();
        filterWorkouts();
    }

    private void updateCategoryChipStates() {
        // Reset all
        binding.chipAll.setBackgroundResource(R.drawable.chip_bg);
        binding.chipBeginner.setBackgroundResource(R.drawable.chip_bg);
        binding.chipIntermediate.setBackgroundResource(R.drawable.chip_bg);
        binding.chipAdvanced.setBackgroundResource(R.drawable.chip_bg);

        // Set selected
        switch (selectedCategory) {
            case "Tất cả":
                binding.chipAll.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Beginner":
                binding.chipBeginner.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Intermediate":
                binding.chipIntermediate.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Advanced":
                binding.chipAdvanced.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
        }
    }

    private void updateDurationChipStates() {
        // Reset all
        binding.chipDurationAll.setBackgroundResource(R.drawable.chip_bg);
        binding.chip17min.setBackgroundResource(R.drawable.chip_bg);
        binding.chip815min.setBackgroundResource(R.drawable.chip_bg);
        binding.chip15minPlus.setBackgroundResource(R.drawable.chip_bg);

        // Set selected
        switch (selectedDuration) {
            case "Tất cả":
                binding.chipDurationAll.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "1-7 min":
                binding.chip17min.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "8-15 min":
                binding.chip815min.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case ">15 min":
                binding.chip15minPlus.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
        }
    }

    /**
     * Load all exercises from Firebase and cache them
     */
    private void loadExercises() {
        Log.d(TAG, "Loading all exercises...");
        FirebaseService.getInstance().loadAllExercises(
                this,
                exercises -> {
                    if (exercises != null) {
                        allExercises = exercises;
                        // Build exercise map for quick lookup
                        exerciseMap.clear();
                        for (Exercise exercise : exercises) {
                            if (exercise.getId() != null) {
                                exerciseMap.put(exercise.getId(), exercise);
                            }
                        }
                        Log.d(TAG, "Loaded " + exercises.size() + " exercises");
                        setupDynamicFilterChips(); // Setup dynamic filter chips after exercises are loaded
                        filterWorkouts(); // Re-filter after exercises are loaded
                    } else {
                        allExercises = new ArrayList<>();
                        exerciseMap.clear();
                        Log.e(TAG, "Failed to load exercises");
                    }
                }
        );
    }

    private void loadWorkouts() {
        Log.d(TAG, "Loading workouts...");
        FirebaseService.getInstance().loadWorkoutTemplates(
                this,
                workouts -> {
                    if (workouts != null) {
                        allWorkouts = workouts;
                        Log.d(TAG, "Loaded " + workouts.size() + " workouts");
                        filterWorkouts();
                    } else {
                        allWorkouts = new ArrayList<>();
                        filterWorkouts();
                    }
                }
        );
    }

    /**
     * Check if workout has any exercise matching the search query
     */
    private boolean workoutHasMatchingExercise(WorkoutTemplate workout, String query) {
        if (workout.getItems() == null || workout.getItems().isEmpty()) {
            return false;
        }

        String lowerQuery = query.toLowerCase();
        
        for (WorkoutTemplate.WorkoutItem item : workout.getItems()) {
            Exercise exercise = exerciseMap.get(item.getExerciseId());
            if (exercise != null && exercise.getName() != null) {
                String exerciseName = exercise.getName().toLowerCase();
                if (exerciseName.contains(lowerQuery)) {
                    return true;
                }
                
                // Also check in exercise category
                if (exercise.getCategory() != null) {
                    for (String category : exercise.getCategory()) {
                        if (category != null && category.toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                    }
                }
                
                // Check in muscles
                if (exercise.getMuscles() != null) {
                    for (String muscle : exercise.getMuscles()) {
                        if (muscle != null && muscle.toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Check if workout has exercises matching the selected exercise category
     */
    private boolean workoutMatchesExerciseCategory(WorkoutTemplate workout, String category) {
        if (category == null || category.equals("Tất cả")) {
            return true;
        }
        
        if (workout.getItems() == null || workout.getItems().isEmpty()) {
            return false;
        }

        for (WorkoutTemplate.WorkoutItem item : workout.getItems()) {
            Exercise exercise = exerciseMap.get(item.getExerciseId());
            if (exercise != null && exercise.getCategory() != null) {
                for (String cat : exercise.getCategory()) {
                    if (cat != null && cat.equalsIgnoreCase(category)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Check if workout has exercises matching the selected body part
     */
    private boolean workoutMatchesBodyPart(WorkoutTemplate workout, String bodyPart) {
        if (bodyPart == null || bodyPart.equals("Tất cả")) {
            return true;
        }
        
        if (workout.getItems() == null || workout.getItems().isEmpty()) {
            return false;
        }

        for (WorkoutTemplate.WorkoutItem item : workout.getItems()) {
            Exercise exercise = exerciseMap.get(item.getExerciseId());
            if (exercise != null && exercise.getMuscles() != null) {
                for (String muscle : exercise.getMuscles()) {
                    if (muscle != null && muscle.equalsIgnoreCase(bodyPart)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private void filterWorkouts() {
        filteredWorkouts.clear();

        for (WorkoutTemplate workout : allWorkouts) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;
            boolean matchesDuration = true;
            boolean matchesExerciseCategory = true;
            boolean matchesBodyPart = true;

            // Filter by search query (workout title, description, and exercise names)
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String query = searchQuery.toLowerCase();
                String title = workout.getTitle() != null ? workout.getTitle().toLowerCase() : "";
                String description = workout.getDescription() != null ? workout.getDescription().toLowerCase() : "";
                
                // Check workout title and description
                boolean matchesWorkout = title.contains(query) || description.contains(query);
                
                // Check exercise names
                boolean matchesExercise = workoutHasMatchingExercise(workout, query);
                
                matchesSearch = matchesWorkout || matchesExercise;
            }

            // Filter by workout category (level)
            if (!selectedCategory.equals("Tất cả")) {
                matchesCategory = workout.getLevel() != null && 
                    workout.getLevel().equalsIgnoreCase(selectedCategory);
            }

            // Filter by duration
            if (!selectedDuration.equals("Tất cả")) {
                int duration = workout.getEstDurationMin();
                switch (selectedDuration) {
                    case "1-7 min":
                        matchesDuration = duration >= 1 && duration <= 7;
                        break;
                    case "8-15 min":
                        matchesDuration = duration >= 8 && duration <= 15;
                        break;
                    case ">15 min":
                        matchesDuration = duration > 15;
                        break;
                }
            }

            // Filter by exercise category
            if (!selectedExerciseCategory.equals("Tất cả")) {
                matchesExerciseCategory = workoutMatchesExerciseCategory(workout, selectedExerciseCategory);
            }

            // Filter by body part (muscles)
            if (!selectedBodyPart.equals("Tất cả")) {
                matchesBodyPart = workoutMatchesBodyPart(workout, selectedBodyPart);
            }

            if (matchesSearch && matchesCategory && matchesDuration && 
                matchesExerciseCategory && matchesBodyPart) {
                filteredWorkouts.add(workout);
            }
        }

        // Update adapter
        adapter.updateList(filteredWorkouts);

        // Update results count
        int count = filteredWorkouts.size();
        binding.tvResultsCount.setText("Tìm thấy " + count + " kết quả");

        // Show/hide empty state
        if (count == 0) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvSearchResults.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvSearchResults.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Filtered workouts: " + filteredWorkouts.size() + " / " + allWorkouts.size());
    }
}
