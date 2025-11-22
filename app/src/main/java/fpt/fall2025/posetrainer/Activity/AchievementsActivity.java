package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Adapter.AchievementAdapter;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityAchievementsBinding;

public class AchievementsActivity extends AppCompatActivity {
    private static final String TAG = "AchievementsActivity";
    private ActivityAchievementsBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAchievementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupToolbar();
        loadUserAchievements();
        loadUserProgress();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Load user achievements and display
     */
    private void loadUserAchievements() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (binding.rvAchievements == null) {
            return;
        }

        // Setup RecyclerView
        binding.rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        AchievementAdapter adapter = new AchievementAdapter();
        binding.rvAchievements.setAdapter(adapter);

        // Load achievements
        FirebaseService.getInstance().loadUserAchievements(currentUser.getUid(), achievement -> {
            if (achievement != null) {
                adapter.setAchievements(achievement);
            } else {
                // Create empty achievement object
                fpt.fall2025.posetrainer.Domain.Achievement emptyAchievement = 
                    new fpt.fall2025.posetrainer.Domain.Achievement();
                adapter.setAchievements(emptyAchievement);
            }
        });
    }

    /**
     * Load user progress (calendar heatmap)
     */
    private void loadUserProgress() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (binding.calendarHeatmap == null) {
            return;
        }

        FirebaseService.getInstance().loadUserProgress(currentUser.getUid(), progress -> {
            if (progress != null && progress.getCalendar() != null) {
                binding.calendarHeatmap.setWorkoutDatesFromProgress(progress.getCalendar());
            }
        });
    }
}

