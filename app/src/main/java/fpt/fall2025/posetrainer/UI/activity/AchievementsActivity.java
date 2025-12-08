package fpt.fall2025.posetrainer.UI.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.UI.adapter.community.AchievementAdapter;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.AchievementDAO;
import fpt.fall2025.posetrainer.DAL.UserProgressDAO;
import fpt.fall2025.posetrainer.databinding.ActivityAchievementsBinding;

public class AchievementsActivity extends AppCompatActivity {
    private static final String TAG = "AchievementsActivity";
    private ActivityAchievementsBinding binding;
    private AuthService authService;
    private AchievementDAO achievementDAO;
    private UserProgressDAO userProgressDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAchievementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = new AuthService();
        achievementDAO = new AchievementDAO();
        userProgressDAO = new UserProgressDAO();

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
        FirebaseUser currentUser = authService.getCurrentUser();
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
        achievementDAO.loadUserAchievements(currentUser.getUid(), achievement -> {
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
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (binding.calendarHeatmap == null) {
            return;
        }

        userProgressDAO.loadUserProgress(currentUser.getUid(), progress -> {
            if (progress != null && progress.getCalendar() != null) {
                binding.calendarHeatmap.setWorkoutDatesFromProgress(progress.getCalendar());
            }
        });
    }
}

