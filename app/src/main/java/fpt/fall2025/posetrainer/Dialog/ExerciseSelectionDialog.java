package fpt.fall2025.posetrainer.Dialog;

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
import java.util.HashSet;
import java.util.Set;

import fpt.fall2025.posetrainer.Adapter.ExerciseSelectionAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Dialog to display and select exercises
 */
public class ExerciseSelectionDialog extends DialogFragment {
    private static final String TAG = "ExerciseSelectionDialog";
    
    private RecyclerView recyclerViewExercises;
    private SearchView searchView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutCategoryChips;
    private TextView tvEmptyState;
    private ImageButton btnClose;
    private ExerciseSelectionAdapter adapter;
    
    private ArrayList<Exercise> allExercises;
    private ArrayList<Exercise> filteredExercises;
    private OnExerciseSelectedListener listener;
    private String selectedCategory = null;
    private String currentSearchQuery = "";
    private ArrayList<TextView> categoryChips = new ArrayList<>();

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

        recyclerViewExercises = view.findViewById(R.id.recycler_view_exercises);
        searchView = view.findViewById(R.id.search_view);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutCategoryChips = view.findViewById(R.id.layout_category_chips);
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
        
        FirebaseService.getInstance().loadAllExercises(activity, exercises -> {
            progressBar.setVisibility(View.GONE);
            
            if (exercises != null && !exercises.isEmpty()) {
                allExercises = exercises;
                filteredExercises = new ArrayList<>(exercises);
                
                // Setup category chips
                setupCategoryChips();
                
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
     * Setup category filter chips
     */
    private void setupCategoryChips() {
        layoutCategoryChips.removeAllViews();
        categoryChips.clear();
        
        // Extract unique categories from exercises
        Set<String> uniqueCategories = new HashSet<>();
        for (Exercise exercise : allExercises) {
            if (exercise.getCategory() != null) {
                for (String category : exercise.getCategory()) {
                    if (category != null && !category.trim().isEmpty()) {
                        uniqueCategories.add(category.trim());
                    }
                }
            }
        }
        
        // Create "Tất cả" (All) chip
        TextView allChip = createCategoryChip("Tất cả", true);
        layoutCategoryChips.addView(allChip);
        categoryChips.add(allChip);
        
        // Create chips for each unique category
        ArrayList<String> sortedCategories = new ArrayList<>(uniqueCategories);
        java.util.Collections.sort(sortedCategories);
        
        for (String category : sortedCategories) {
            TextView chip = createCategoryChip(category, false);
            layoutCategoryChips.addView(chip);
            categoryChips.add(chip);
        }
    }
    
    /**
     * Create a category chip TextView
     */
    private TextView createCategoryChip(String categoryName, boolean isSelected) {
        TextView chip = new TextView(getContext());
        chip.setText(categoryName);
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
        if (isSelected && categoryName.equals("Tất cả")) {
            chip.setBackgroundResource(R.drawable.chip_selected_bg);
        } else {
            chip.setBackgroundResource(R.drawable.chip_bg);
        }
        
        // Set click listener
        chip.setOnClickListener(v -> {
            if (categoryName.equals("Tất cả")) {
                selectedCategory = null;
            } else {
                selectedCategory = categoryName;
            }
            updateCategoryChipStates();
            applyFilters();
        });
        
        chip.setClickable(true);
        chip.setFocusable(true);
        
        return chip;
    }
    
    /**
     * Update visual states of category chips
     */
    private void updateCategoryChipStates() {
        for (TextView chip : categoryChips) {
            String chipText = chip.getText().toString();
            if (chipText.equals("Tất cả") && selectedCategory == null) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else if (chipText.equals(selectedCategory)) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg);
            } else {
                chip.setBackgroundResource(R.drawable.chip_bg);
            }
        }
    }
    
    /**
     * Apply both search and category filters
     */
    private void applyFilters() {
        filteredExercises = new ArrayList<>();
        String lowerQuery = (currentSearchQuery != null) ? currentSearchQuery.toLowerCase().trim() : "";
        
        for (Exercise exercise : allExercises) {
            if (exercise == null || exercise.getName() == null) {
                continue;
            }
            
            // Check category filter
            boolean matchesCategory = true;
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                matchesCategory = false;
                if (exercise.getCategory() != null) {
                    for (String category : exercise.getCategory()) {
                        if (category != null && category.equals(selectedCategory)) {
                            matchesCategory = true;
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
                boolean matchesCategoryInSearch = false;
                
                if (exercise.getCategory() != null) {
                    for (String category : exercise.getCategory()) {
                        if (category != null && category.toLowerCase().contains(lowerQuery)) {
                            matchesCategoryInSearch = true;
                            break;
                        }
                    }
                }
                
                matchesSearch = matchesName || matchesLevel || matchesCategoryInSearch;
            }
            
            // Add if matches both filters
            if (matchesCategory && matchesSearch) {
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

