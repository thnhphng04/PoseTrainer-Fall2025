package fpt.fall2025.posetrainer.UI.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.UI.adapter.workout.SearchWorkoutAdapter;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.DAL.WorkoutTemplateDAO;
import fpt.fall2025.posetrainer.databinding.ActivityGoalWorkoutListBinding;

import java.util.ArrayList;

public class GoalWorkoutListActivity extends AppCompatActivity {
    private static final String TAG = "GoalWorkoutListActivity";
    private ActivityGoalWorkoutListBinding binding;
    private String goalFit; // "gain_muscle", "lose_fat", "general_fitness"
    private String goalFitTitle; // Title tiếng Việt
    private String goalFitDescription; // Description tiếng Việt
    private ArrayList<WorkoutTemplate> workouts;
    private SearchWorkoutAdapter adapter;
    private WorkoutTemplateDAO workoutTemplateDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalWorkoutListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        goalFit = getIntent().getStringExtra("goalFit");
        goalFitTitle = getIntent().getStringExtra("goalFitTitle");
        goalFitDescription = getIntent().getStringExtra("goalFitDescription");

        if (goalFit == null) {
            Log.e(TAG, "GoalFit is null");
            finish();
            return;
        }

        workouts = new ArrayList<>();
        workoutTemplateDAO = new WorkoutTemplateDAO();

        setupRecyclerView();
        setupBackButton();
        updateUI();
        loadWorkouts();
    }

    private void setupRecyclerView() {
        binding.rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchWorkoutAdapter(workouts);
        binding.rvWorkouts.setAdapter(adapter);
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void updateUI() {
        // Set title
        if (goalFitTitle != null && !goalFitTitle.isEmpty()) {
            binding.tvTitle.setText(goalFitTitle);
        } else {
            // Fallback based on goalFit
            switch (goalFit) {
                case "gain_muscle":
                    binding.tvTitle.setText("Tăng cơ bắp");
                    break;
                case "lose_fat":
                    binding.tvTitle.setText("Giảm mỡ");
                    break;
                case "general_fitness":
                    binding.tvTitle.setText("Thể dục tổng quát");
                    break;
                default:
                    binding.tvTitle.setText("Bài tập");
                    break;
            }
        }

        // Set description
        if (goalFitDescription != null && !goalFitDescription.isEmpty()) {
            binding.tvDescription.setText(goalFitDescription);
            binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            // Fallback description
            String description = "";
            switch (goalFit) {
                case "gain_muscle":
                    description = "Các bài tập giúp bạn tăng cơ bắp hiệu quả";
                    break;
                case "lose_fat":
                    description = "Các bài tập giúp bạn giảm mỡ và đốt cháy calo";
                    break;
                case "general_fitness":
                    description = "Các bài tập thể dục tổng quát cho sức khỏe";
                    break;
            }
            if (!description.isEmpty()) {
                binding.tvDescription.setText(description);
                binding.tvDescription.setVisibility(View.VISIBLE);
            } else {
                binding.tvDescription.setVisibility(View.GONE);
            }
        }
    }

    private void loadWorkouts() {
        Log.d(TAG, "Loading workouts for goalFit: " + goalFit);
        
        workoutTemplateDAO.getPublicTemplates(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<WorkoutTemplate> allWorkouts = new ArrayList<>(task.getResult());
                
                // Filter workouts theo goalFit
                ArrayList<WorkoutTemplate> filteredWorkouts = new ArrayList<>();
                for (WorkoutTemplate template : allWorkouts) {
                    if (template.getGoalFit() != null && 
                        template.getGoalFit().equalsIgnoreCase(goalFit)) {
                        filteredWorkouts.add(template);
                    }
                }
                
                // Sort theo thời gian tạo (updatedAt - giảm dần, mới nhất trước)
                filteredWorkouts.sort((a, b) -> Long.compare(
                    b.getUpdatedAt() != 0 ? b.getUpdatedAt() : 0,
                    a.getUpdatedAt() != 0 ? a.getUpdatedAt() : 0
                ));
                
                workouts = filteredWorkouts;
                adapter.updateList(workouts);
                
                // Update workout count
                binding.tvWorkoutCount.setText(workouts.size() + " bài tập");
                
                Log.d(TAG, "Loaded " + workouts.size() + " workouts for goalFit: " + goalFit);
                
                // Show/hide empty state
                if (workouts.isEmpty()) {
                    binding.llEmptyState.setVisibility(View.VISIBLE);
                    binding.rvWorkouts.setVisibility(View.GONE);
                } else {
                    binding.llEmptyState.setVisibility(View.GONE);
                    binding.rvWorkouts.setVisibility(View.VISIBLE);
                }
            } else {
                Log.e(TAG, "Error loading workouts: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                workouts = new ArrayList<>();
                adapter.updateList(workouts);
                binding.tvWorkoutCount.setText("0 bài tập");
                binding.llEmptyState.setVisibility(View.VISIBLE);
                binding.rvWorkouts.setVisibility(View.GONE);
            }
        });
    }
}

