package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fpt.fall2025.posetrainer.UI.adapter.exercise.ExerciseSelectionAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.ExerciseDAO;

/**
 * Dialog to display and select exercises
 */
public class ExerciseSelectionDialog extends DialogFragment {
    private static final String TAG = "ExerciseSelectionDialog";
    
    // Muscle mapping (English -> Vietnamese)
    private static final Map<String, String> MUSCLE_MAP = new HashMap<String, String>() {{
        put("fullbody", "Toàn thân");
        put("legs", "Chân");
        put("arms", "Tay");
        put("chest", "Ngực");
        put("core", "Bụng");
        put("hips", "Hông");
        put("shoulders", "Vai");
        put("back", "Lưng");
    }};
    
    // Level mapping (English -> Vietnamese)
    private static final Map<String, String> LEVEL_MAP = new HashMap<String, String>() {{
        put("beginner", "Dễ");
        put("intermediate", "Trung bình");
        put("pro", "Khó");
        put("advanced", "Khó");
    }};
    
    private RecyclerView recyclerViewExercises;
    private SearchView searchView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutDifficultyChips;
    private LinearLayout layoutMuscleChips;
    private TextView tvEmptyState;
    private ImageButton btnClose;
    private ExerciseSelectionAdapter adapter;
    
    private ArrayList<Exercise> allExercises;
    private ArrayList<Exercise> filteredExercises;
    private OnExerciseSelectedListener listener;
    private ExerciseDAO exerciseDAO;
    private String selectedDifficulty = null;
    private String selectedMuscle = null;
    private String currentSearchQuery = "";
    private ArrayList<TextView> difficultyChips = new ArrayList<>();
    private ArrayList<TextView> muscleChips = new ArrayList<>();

    /**
     * Interface for exercise selection callback
     */
    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }

    /**
     * Set listener for exercise selection
     */
    public void setOnExerciseSelectedListener(OnExerciseSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_exercise_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        exerciseDAO = new ExerciseDAO();

        recyclerViewExercises = view.findViewById(R.id.recycler_view_exercises);
        searchView = view.findViewById(R.id.search_view);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutDifficultyChips = view.findViewById(R.id.layout_difficulty_chips);
        layoutMuscleChips = view.findViewById(R.id.layout_muscle_chips);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        btnClose = view.findViewById(R.id.btn_close);

        // Initialize data
        allExercises = new ArrayList<>();
        filteredExercises = new ArrayList<>();

        // Setup RecyclerView
        adapter = new ExerciseSelectionAdapter(filteredExercises, exercise -> {
            if (listener != null) {
                listener.onExerciseSelected(exercise);
            }
            dismiss();
        });
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewExercises.setAdapter(adapter);

        // Setup search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFilters();
                return true;
            }
        });
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Load exercises
        loadExercises();
    }

    /**
     * Load exercises from Firebase
     */
    private void loadExercises() {
        Log.d(TAG, "Loading exercises from Firebase");
        
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            Log.e(TAG, "Activity is null or not AppCompatActivity");
            return;
        }
        
        androidx.appcompat.app.AppCompatActivity activity = 
            (androidx.appcompat.app.AppCompatActivity) getActivity();
        
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        
        exerciseDAO.loadAllExercises(activity, exercises -> {
            progressBar.setVisibility(View.GONE);
            
            if (exercises != null && !exercises.isEmpty()) {
                allExercises = exercises;
                filteredExercises = new ArrayList<>(exercises);
                
                // Setup filter chips
                setupDifficultyChips();
                setupMuscleChips();
                
                recyclerViewExercises.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                
                adapter.updateExercises(filteredExercises);
                Log.d(TAG, "Loaded " + exercises.size() + " exercises");
            } else {
                recyclerViewExercises.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                Log.e(TAG, "No exercises loaded");
            }
        });
    }

    /**
     * Setup difficulty filter chips (hardcoded: Dễ, Trung bình, Khó)
     */
    private void setupDifficultyChips() {
        layoutDifficultyChips.removeAllViews();
        difficultyChips.clear();
        
        // Hardcoded difficulty levels in Vietnamese
        String[] difficultyNames = {"Tất cả", "Dễ", "Trung bình", "Khó"};
        
        for (int i = 0; i < difficultyNames.length; i++) {
            TextView chip = createChip(difficultyNames[i], i == 0, true);
            layoutDifficultyChips.addView(chip);
            difficultyChips.add(chip);
        }
    }
    
    /**
     * Setup muscle filter chips (hardcoded 8 muscles in Vietnamese)
     */
    private void setupMuscleChips() {
        layoutMuscleChips.removeAllViews();
        muscleChips.clear();
        
        // Hardcoded muscle types in Vietnamese
        String[] muscleNames = {"Tất cả", "Toàn thân", "Chân", "Tay", "Ngực", "Bụng", "Hông", "Vai", "Lưng"};
        
        for (int i = 0; i < muscleNames.length; i++) {
            TextView chip = createChip(muscleNames[i], i == 0, false);
            layoutMuscleChips.addView(chip);
            muscleChips.add(chip);
        }
    }
    
    /**
     * Create a chip TextView
     * @param isDifficultyChip true for difficulty chips, false for muscle chips
     */
    private TextView createChip(String chipName, boolean isSelected, boolean isDifficultyChip) {
        TextView chip = new TextView(getContext());
        chip.setText(chipName);
        chip.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        chip.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        chip.setTextSize(14);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        chip.setLayoutParams(params);
        
        // Set background based on selection
        chip.setBackgroundResource(isSelected ? R.drawable.chip_selected_bg : R.drawable.chip_bg);
        
        // Set click listener
        chip.setOnClickListener(v -> {
            if (isDifficultyChip) {
                selectedDifficulty = chipName.equals("Tất cả") ? null : chipName;
                updateDifficultyChipStates();
            } else {
                selectedMuscle = chipName.equals("Tất cả") ? null : chipName;
                updateMuscleChipStates();
            }
            applyFilters();
        });
        
        chip.setClickable(true);
        chip.setFocusable(true);
        
        return chip;
    }
    
    /**
     * Update visual states of difficulty chips
     */
    private void updateDifficultyChipStates() {
        for (TextView chip : difficultyChips) {
            String chipText = chip.getText().toString();
            if (chipText.equals("Tất cả") && selectedDifficulty == null) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else if (chipText.equals(selectedDifficulty)) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else {
                chip.setBackgroundResource(R.drawable.chip_bg);
            }
        }
    }
    
    /**
     * Update visual states of muscle chips
     */
    private void updateMuscleChipStates() {
        for (TextView chip : muscleChips) {
            String chipText = chip.getText().toString();
            if (chipText.equals("Tất cả") && selectedMuscle == null) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else if (chipText.equals(selectedMuscle)) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else {
                chip.setBackgroundResource(R.drawable.chip_bg);
            }
        }
    }
    
    /**
     * Convert Vietnamese muscle name to English
     */
    private String getEnglishMuscle(String vietnameseMuscle) {
        for (Map.Entry<String, String> entry : MUSCLE_MAP.entrySet()) {
            if (entry.getValue().equals(vietnameseMuscle)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Convert Vietnamese difficulty to English
     */
    private String getEnglishDifficulty(String vietnameseDifficulty) {
        for (Map.Entry<String, String> entry : LEVEL_MAP.entrySet()) {
            if (entry.getValue().equals(vietnameseDifficulty)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Apply search, difficulty and muscle filters
     */
    private void applyFilters() {
        filteredExercises = new ArrayList<>();
        String lowerQuery = (currentSearchQuery != null) ? currentSearchQuery.toLowerCase().trim() : "";
        
        // Convert selected Vietnamese muscle to English
        String selectedMuscleEnglish = (selectedMuscle != null) ? getEnglishMuscle(selectedMuscle) : null;
        
        for (Exercise exercise : allExercises) {
            if (exercise == null || exercise.getName() == null) {
                continue;
            }
            
            // Check difficulty filter
            boolean matchesDifficulty = true;
            if (selectedDifficulty != null) {
                matchesDifficulty = false;
                String exerciseLevel = exercise.getLevel();
                if (exerciseLevel != null) {
                    String lowerLevel = exerciseLevel.toLowerCase();
                    if (selectedDifficulty.equals("Dễ") && lowerLevel.contains("beginner")) {
                        matchesDifficulty = true;
                    } else if (selectedDifficulty.equals("Trung bình") && lowerLevel.contains("intermediate")) {
                        matchesDifficulty = true;
                    } else if (selectedDifficulty.equals("Khó") && (lowerLevel.contains("advanced") || lowerLevel.contains("pro"))) {
                        matchesDifficulty = true;
                    }
                }
            }
            
            // Check muscle filter
            boolean matchesMuscle = true;
            if (selectedMuscleEnglish != null) {
                matchesMuscle = false;
                if (exercise.getMuscles() != null) {
                    for (String muscle : exercise.getMuscles()) {
                        if (muscle != null && muscle.equalsIgnoreCase(selectedMuscleEnglish)) {
                            matchesMuscle = true;
                            break;
                        }
                    }
                }
            }
            
            // Check search query filter
            boolean matchesSearch = true;
            if (!lowerQuery.isEmpty()) {
                matchesSearch = false;
                
                boolean matchesName = exercise.getName().toLowerCase().contains(lowerQuery);
                boolean matchesLevel = exercise.getLevel() != null && 
                    exercise.getLevel().toLowerCase().contains(lowerQuery);
                boolean matchesMuscleInSearch = false;
                
                if (exercise.getMuscles() != null) {
                    for (String muscle : exercise.getMuscles()) {
                        if (muscle != null && muscle.toLowerCase().contains(lowerQuery)) {
                            matchesMuscleInSearch = true;
                            break;
                        }
                    }
                }
                
                matchesSearch = matchesName || matchesLevel || matchesMuscleInSearch;
            }
            
            // Add if matches all filters
            if (matchesDifficulty && matchesMuscle && matchesSearch) {
                filteredExercises.add(exercise);
            }
        }
        
        adapter.updateExercises(filteredExercises);
        
        // Show/hide empty state
        if (filteredExercises.isEmpty()) {
            recyclerViewExercises.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewExercises.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            int maxHeight = (int)(getResources().getDisplayMetrics().heightPixels * 0.80);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}

