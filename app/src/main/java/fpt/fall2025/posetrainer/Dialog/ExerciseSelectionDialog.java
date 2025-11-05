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
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

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
    private TextView tvEmptyState;
    private ImageButton btnClose;
    private ExerciseSelectionAdapter adapter;
    
    private ArrayList<Exercise> allExercises;
    private ArrayList<Exercise> filteredExercises;
    private OnExerciseSelectedListener listener;

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
                filterExercises(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterExercises(newText);
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
     * Filter exercises based on search query
     */
    private void filterExercises(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredExercises = new ArrayList<>(allExercises);
        } else {
            filteredExercises = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();
            
            for (Exercise exercise : allExercises) {
                if (exercise == null || exercise.getName() == null) {
                    continue;
                }
                
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
                }
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

