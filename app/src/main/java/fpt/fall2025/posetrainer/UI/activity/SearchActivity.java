package fpt.fall2025.posetrainer.UI.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.UI.adapter.workout.SearchWorkoutAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.ExerciseDAO;
import fpt.fall2025.posetrainer.DAL.WorkoutTemplateDAO;
import fpt.fall2025.posetrainer.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private ActivitySearchBinding binding;
    private ArrayList<WorkoutTemplate> allWorkouts;
    private ArrayList<WorkoutTemplate> filteredWorkouts;
    private SearchWorkoutAdapter adapter;
    // Exercise cache
    private Map<String, Exercise> exerciseMap; // exerciseId -> Exercise
    private ArrayList<Exercise> allExercises;
    private ExerciseDAO exerciseDAO;
    private WorkoutTemplateDAO workoutTemplateDAO;
    
    // Filter states
    private String selectedCategory = "Tất cả";
    private String selectedDuration = "Tất cả";
    private String selectedGoalFit = "Tất cả";
    private Set<String> selectedBodyParts = new HashSet<>(); // Multiple body parts can be selected
    private String searchQuery = "";
    
    // GoalFit mapping (English -> Vietnamese)
    private static final Map<String, String> GOAL_FIT_MAP = new HashMap<String, String>() {{
        put("general_fitness", "Thể dục tổng quát");
        put("gain_muscle", "Tăng cơ");
        put("lose_fat", "Giảm mỡ");
    }};
    
    // GoalFit reverse mapping (Vietnamese -> English)
    private static final Map<String, String> GOAL_FIT_REVERSE_MAP = new HashMap<String, String>() {{
        put("Thể dục tổng quát", "general_fitness");
        put("Tăng cơ", "gain_muscle");
        put("Giảm mỡ", "lose_fat");
    }};
    
    // Body Part mapping (Vietnamese text -> English body part name)
    private static final Map<String, String> BODY_PART_MAP = new HashMap<String, String>() {{
        put("Toàn thân", "fullbody");
        put("Chân", "legs");
        put("Tay", "arms");
        put("Ngực", "chest");
        put("Bụng", "core");
        put("Hông", "hips");
        put("Vai", "shoulders");
        put("Lưng", "back");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        allWorkouts = new ArrayList<>();
        filteredWorkouts = new ArrayList<>();
        exerciseMap = new HashMap<>();
        allExercises = new ArrayList<>();
        exerciseDAO = new ExerciseDAO();
        workoutTemplateDAO = new WorkoutTemplateDAO();

        setupRecyclerView();
        setupSearchBar();
        setupFilterChips();
        setupBodyPartGridClicks();
        setupBackButton();
        loadExercises(); // Load exercises for search functionality
        loadWorkouts();
    }

    private void setupRecyclerView() {
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchWorkoutAdapter(filteredWorkouts); // ĐỔI TÊN CLASS
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
        
        // GoalFit chips
        binding.chipGoalFitAll.setOnClickListener(v -> selectGoalFit("Tất cả"));
        binding.chipGeneralFitness.setOnClickListener(v -> selectGoalFit("Thể dục tổng quát"));
        binding.chipGainMuscle.setOnClickListener(v -> selectGoalFit("Tăng cơ"));
        binding.chipLoseFat.setOnClickListener(v -> selectGoalFit("Giảm mỡ"));
        
        // Body part filters will be added dynamically after exercises are loaded
    }
    
    /**
     * Setup body part grid clicks (using GridLayout CardViews)
     */
    private void setupBodyPartGridClicks() {
        // Full Body
        binding.bodyPartFullBody.setOnClickListener(v -> selectBodyPartFromGrid("Toàn thân"));
        
        // Legs (core CardView with "Chân" text)
        binding.bodyPartCore.setOnClickListener(v -> selectBodyPartFromGrid("Chân"));
        
        // Arms
        binding.bodyPartArm.setOnClickListener(v -> selectBodyPartFromGrid("Tay"));
        
        // Chest
        binding.bodyPartChest.setOnClickListener(v -> selectBodyPartFromGrid("Ngực"));
        
        // Core (butt_leg CardView with "Bụng" text)
        binding.bodyPartButtLeg.setOnClickListener(v -> selectBodyPartFromGrid("Bụng"));
        
        // Hips (back CardView with "Hông" text)
        binding.bodyPartBack.setOnClickListener(v -> selectBodyPartFromGrid("Hông"));
        
        // Shoulders
        binding.bodyPartShoulder.setOnClickListener(v -> selectBodyPartFromGrid("Vai"));
        
        // Back (custom CardView with "Lưng" text)
        binding.bodyPartCustom.setOnClickListener(v -> selectBodyPartFromGrid("Lưng"));
    }
    
    /**
     * Select body part from grid (convert Vietnamese to English)
     * Toggle behavior: if already selected, remove it; if not selected, add it
     */
    private void selectBodyPartFromGrid(String vietnameseText) {
        // Convert Vietnamese text to English body part name
        String englishBodyPart = BODY_PART_MAP.get(vietnameseText);
        if (englishBodyPart != null) {
            // Toggle: if already in set, remove it; otherwise add it
            if (selectedBodyParts.contains(englishBodyPart)) {
                selectedBodyParts.remove(englishBodyPart);
            } else {
                selectedBodyParts.add(englishBodyPart);
            }
        }
        updateBodyPartGridStates();
        filterWorkouts();
    }
    
    
    
    private void selectGoalFit(String goalFit) {
        selectedGoalFit = goalFit;
        updateGoalFitChipStates();
        filterWorkouts();
    }
    
    private void selectBodyPart(String bodyPart) {
        // Legacy method - not used anymore, but kept for compatibility
        if (bodyPart == null || bodyPart.equals("Tất cả")) {
            selectedBodyParts.clear();
        } else {
            selectedBodyParts.clear();
            selectedBodyParts.add(bodyPart);
        }
        updateBodyPartGridStates();
        filterWorkouts();
    }
    
    private void updateGoalFitChipStates() {
        // Reset all
        binding.chipGoalFitAll.setBackgroundResource(R.drawable.chip_bg);
        binding.chipGeneralFitness.setBackgroundResource(R.drawable.chip_bg);
        binding.chipGainMuscle.setBackgroundResource(R.drawable.chip_bg);
        binding.chipLoseFat.setBackgroundResource(R.drawable.chip_bg);

        // Set selected
        switch (selectedGoalFit) {
            case "Tất cả":
                binding.chipGoalFitAll.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Thể dục tổng quát":
                binding.chipGeneralFitness.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Tăng cơ":
                binding.chipGainMuscle.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Giảm mỡ":
                binding.chipLoseFat.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
        }
    }
    
    private void updateBodyPartGridStates() {
        // Default card background color (#ff2a3142)
        int defaultColor = 0xff2a3142;
        // Selected color (slightly lighter or use primary color)
        int selectedColor = 0xff4d9df2; // Primary blue color
        
        // Reset all cards to default
        binding.bodyPartFullBody.setCardBackgroundColor(defaultColor);
        binding.bodyPartCore.setCardBackgroundColor(defaultColor);
        binding.bodyPartArm.setCardBackgroundColor(defaultColor);
        binding.bodyPartChest.setCardBackgroundColor(defaultColor);
        binding.bodyPartButtLeg.setCardBackgroundColor(defaultColor);
        binding.bodyPartBack.setCardBackgroundColor(defaultColor);
        binding.bodyPartShoulder.setCardBackgroundColor(defaultColor);
        binding.bodyPartCustom.setCardBackgroundColor(defaultColor);
        
        // If no body parts selected, all remain default
        if (selectedBodyParts == null || selectedBodyParts.isEmpty()) {
            return;
        }
        
        // Highlight all selected body parts
        // Reverse lookup: English body part -> Vietnamese text -> CardView
        for (Map.Entry<String, String> entry : BODY_PART_MAP.entrySet()) {
            String englishBodyPart = entry.getValue();
            if (selectedBodyParts.contains(englishBodyPart)) {
                String vietnameseText = entry.getKey();
                switch (vietnameseText) {
                    case "Toàn thân":
                        binding.bodyPartFullBody.setCardBackgroundColor(selectedColor);
                        break;
                    case "Chân":
                        binding.bodyPartCore.setCardBackgroundColor(selectedColor);
                        break;
                    case "Tay":
                        binding.bodyPartArm.setCardBackgroundColor(selectedColor);
                        break;
                    case "Ngực":
                        binding.bodyPartChest.setCardBackgroundColor(selectedColor);
                        break;
                    case "Bụng":
                        binding.bodyPartButtLeg.setCardBackgroundColor(selectedColor);
                        break;
                    case "Hông":
                        binding.bodyPartBack.setCardBackgroundColor(selectedColor);
                        break;
                    case "Vai":
                        binding.bodyPartShoulder.setCardBackgroundColor(selectedColor);
                        break;
                    case "Lưng":
                        binding.bodyPartCustom.setCardBackgroundColor(selectedColor);
                        break;
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
        exerciseDAO.loadAllExercises(this, exercises -> {
            if (exercises != null && !exercises.isEmpty()) {
                allExercises = exercises;
                // Build exercise map for quick lookup
                exerciseMap.clear();
                for (Exercise exercise : exercises) {
                    if (exercise.getId() != null) {
                        exerciseMap.put(exercise.getId(), exercise);
                    }
                }
                Log.d(TAG, "Loaded " + exercises.size() + " exercises");
                filterWorkouts(); // Re-filter after exercises are loaded
            } else {
                allExercises = new ArrayList<>();
                exerciseMap.clear();
                Log.e(TAG, "Failed to load exercises");
                Log.e(TAG, "Failed to load exercises");
            }
        });
    }

    private void loadWorkouts() {
        Log.d(TAG, "Loading workouts...");
        workoutTemplateDAO.getPublicTemplates(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<WorkoutTemplate> workouts = new ArrayList<>(task.getResult());
                if (workouts != null) {
                    allWorkouts = workouts;
                    Log.d(TAG, "Loaded " + workouts.size() + " workouts");
                    filterWorkouts();
                } else {
                    allWorkouts = new ArrayList<>();
                    filterWorkouts();
                }
            } else {
                allWorkouts = new ArrayList<>();
                filterWorkouts();
            }
        });
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
     * Check if workout matches the selected goalFit
     */
    private boolean workoutMatchesGoalFit(WorkoutTemplate workout, String goalFitVietnamese) {
        if (goalFitVietnamese == null || goalFitVietnamese.equals("Tất cả")) {
            return true;
        }
        
        // Convert Vietnamese to English
        String goalFitEnglish = GOAL_FIT_REVERSE_MAP.get(goalFitVietnamese);
        if (goalFitEnglish == null) {
            return true; // If mapping not found, show all
        }
        
        // Check if workout's goalFit matches
        String workoutGoalFit = workout.getGoalFit();
        return workoutGoalFit != null && workoutGoalFit.equalsIgnoreCase(goalFitEnglish);
    }

    /**
     * Check if workout has exercises matching any of the selected body parts
     * Returns true if workout matches ANY selected body part (OR logic)
     */
    private boolean workoutMatchesBodyPart(WorkoutTemplate workout, Set<String> selectedBodyParts) {
        // If no body parts selected, show all workouts
        if (selectedBodyParts == null || selectedBodyParts.isEmpty()) {
            return true;
        }
        
        if (workout.getItems() == null || workout.getItems().isEmpty()) {
            return false;
        }

        // Collect all muscles from all exercises in this workout
        Set<String> workoutMuscles = new HashSet<>();
        for (WorkoutTemplate.WorkoutItem item : workout.getItems()) {
            Exercise exercise = exerciseMap.get(item.getExerciseId());
            if (exercise != null && exercise.getMuscles() != null) {
                for (String muscle : exercise.getMuscles()) {
                    if (muscle != null && !muscle.isEmpty()) {
                        workoutMuscles.add(muscle.toLowerCase());
                    }
                }
            }
        }
        
        // Check if any selected body part matches any muscle in the workout
        for (String selectedBodyPart : selectedBodyParts) {
            if (workoutMuscles.contains(selectedBodyPart.toLowerCase())) {
                return true; // Match found
            }
        }
        
        return false; // No match
    }

    private void filterWorkouts() {
        filteredWorkouts.clear();

        for (WorkoutTemplate workout : allWorkouts) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;
            boolean matchesDuration = true;
            boolean matchesGoalFit = true;
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

            // Filter by goalFit
            if (!selectedGoalFit.equals("Tất cả")) {
                matchesGoalFit = workoutMatchesGoalFit(workout, selectedGoalFit);
            }

            // Filter by body parts (muscles) - multiple selection supported
            matchesBodyPart = workoutMatchesBodyPart(workout, selectedBodyParts);

            if (matchesSearch && matchesCategory && matchesDuration && 
                matchesGoalFit && matchesBodyPart) {
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

