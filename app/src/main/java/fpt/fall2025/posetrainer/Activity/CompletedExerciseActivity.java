package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Adapter.CompletedExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Dialog.AchievementUnlockedDialog;
import fpt.fall2025.posetrainer.Manager.AchievementManager;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Activity hiển thị màn hình hoàn thành bài tập
 * Hiển thị thông tin tổng kết: thời gian, kcal, cấp độ, số bài tập và danh sách các bài đã tập
 */
public class CompletedExerciseActivity extends AppCompatActivity {
    private static final String TAG = "CompletedExerciseActivity";

    // UI Components
    private ImageView ivBack;
    private TextView tvHeaderTitle;
    private TextView tvDurationValue;
    private TextView tvCalories;
    private TextView tvLevel;
    private TextView tvExerciseCount;
    private RecyclerView rvCompletedExercises;
    private Button btnSave;

    // Data
    private Session currentSession;
    private List<Exercise> exercises;
    private CompletedExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_exercise);

        // Get sessionId from intent
        String sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            Log.e(TAG, "Missing sessionId from intent");
            Toast.makeText(this, "Không tìm thấy thông tin buổi tập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI
        initViews();
        setupClickListeners();
        setupRecyclerView();

        // Load session data from Firebase
        loadSessionData(sessionId);
    }

    /**
     * Khởi tạo các view từ layout
     */
    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvDurationValue = findViewById(R.id.tv_duration_value);
        tvCalories = findViewById(R.id.tv_calories);
        tvLevel = findViewById(R.id.tv_level);
        tvExerciseCount = findViewById(R.id.tv_exercise_count);
        rvCompletedExercises = findViewById(R.id.rv_completed_exercises);
        btnSave = findViewById(R.id.btn_save);

        // Initialize data lists
        exercises = new ArrayList<>();
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Save button - Update streak, achievements, and user progress
        btnSave.setOnClickListener(v -> {
            if (currentSession == null) {
                Toast.makeText(this, "Không tìm thấy thông tin buổi tập", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Disable button to prevent multiple clicks
            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");

            // Get current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để lưu kết quả", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Lưu");
                return;
            }

            String uid = currentUser.getUid();

            Log.d(TAG, "🔥 Bắt đầu cập nhật streak và achievements cho session: " + currentSession.getId());

            // Update streak
            FirebaseService.getInstance().updateStreak(uid, currentSession, streak -> {
                if (streak != null) {
                    Log.d(TAG, "✅ Streak updated: " + streak.getCurrentStreak() + " ngày");

                    // Check achievements after streak update
                    AchievementManager.getInstance().checkAchievements(uid, currentSession, newlyUnlocked -> {
                        if (newlyUnlocked != null && !newlyUnlocked.isEmpty()) {
                            Log.d(TAG, "🎉 New achievements unlocked: " + newlyUnlocked.size());

                            // Show dialog for first achievement
                            if (!newlyUnlocked.isEmpty()) {
                                String firstBadge = newlyUnlocked.get(0);
                                AchievementUnlockedDialog dialog = AchievementUnlockedDialog.newInstance(firstBadge);
                                dialog.show(getSupportFragmentManager(), "AchievementUnlockedDialog");
                            }
                        }

                        // Update user progress (calendar heatmap)
                        FirebaseService.getInstance().updateUserProgress(uid, progress -> {
                            if (progress != null) {
                                Log.d(TAG, "✅ User progress updated: " + progress.getTotalWorkoutDays() + " days");
                            }

                            // Finish activity after all updates complete
                            Toast.makeText(CompletedExerciseActivity.this, "Đã lưu kết quả thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                } else {
                    Log.e(TAG, "❌ Failed to update streak");
                    Toast.makeText(CompletedExerciseActivity.this, "Lỗi khi cập nhật streak", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu");
                }
            });
        });
    }

    /**
     * Setup RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new CompletedExerciseAdapter(this, new ArrayList<>(), exercises);
        rvCompletedExercises.setLayoutManager(new LinearLayoutManager(this));
        rvCompletedExercises.setAdapter(adapter);
    }

    /**
     * Load session từ Firebase Firestore
     */
    private void loadSessionData(String sessionId) {
        Log.d(TAG, "Loading session data for ID: " + sessionId);

        FirebaseService.getInstance().loadSessionById(sessionId, new FirebaseService.OnSessionLoadedListener() {
            @Override
            public void onSessionLoaded(Session session) {
                if (session != null) {
                    currentSession = session;
                    Log.d(TAG, "Session loaded successfully: " + session.getId());

                    // Load exercises từ session data
                    loadExercisesFromSession(session);

                    // Update UI với session data
                    updateUIWithSessionData(session);
                } else {
                    Log.e(TAG, "Failed to load session");
                    Toast.makeText(CompletedExerciseActivity.this, "Không tìm thấy thông tin buổi tập", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading session: " + error);
                Toast.makeText(CompletedExerciseActivity.this, "Lỗi khi tải thông tin buổi tập", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Load exercises từ session data
     */
    private void loadExercisesFromSession(Session session) {
        if (session.getPerExercise() == null || session.getPerExercise().isEmpty()) {
            Log.w(TAG, "No exercises in session");
            exercises.clear();
            adapter.updateExercises(new ArrayList<>(), exercises);
            return;
        }

        List<Session.PerExercise> perExercises = session.getPerExercise();
        exercises.clear();

        // Load exercises từ Firebase
        int totalExercises = perExercises.size();
        final int[] loadedCount = {0};

        for (Session.PerExercise perExercise : perExercises) {
            String exerciseId = perExercise.getExerciseId();
            if (exerciseId == null) {
                loadedCount[0]++;
                if (loadedCount[0] == totalExercises) {
                    updateAdapter(perExercises);
                }
                continue;
            }

            FirebaseService.getInstance().loadExerciseById(exerciseId, this, new FirebaseService.OnExerciseLoadedListener() {
                @Override
                public void onExerciseLoaded(Exercise exercise) {
                    if (exercise != null) {
                        exercises.add(exercise);
                    }
                    loadedCount[0]++;
                    if (loadedCount[0] == totalExercises) {
                        updateAdapter(perExercises);
                    }
                }
            });
        }
    }

    /**
     * Update adapter với exercises đã load
     */
    private void updateAdapter(List<Session.PerExercise> perExercises) {
        // Filter only completed exercises
        List<Session.PerExercise> completedExercises = new ArrayList<>();
        for (Session.PerExercise perExercise : perExercises) {
            if ("completed".equals(perExercise.getState())) {
                completedExercises.add(perExercise);
            }
        }
        adapter.updateExercises(completedExercises, exercises);
    }

    /**
     * Update UI với session data
     */
    private void updateUIWithSessionData(Session session) {
        // Update header title
        if (session.getTitle() != null && !session.getTitle().isEmpty()) {
            tvHeaderTitle.setText(session.getTitle());
        } else {
            tvHeaderTitle.setText("Hoàn thành bài tập");
        }

        // Update duration
        if (session.getSummary() != null) {
            int durationSec = session.getSummary().getDurationSec();
            int minutes = durationSec / 60;
            int seconds = durationSec % 60;
            if (minutes > 0) {
                if (seconds > 0) {
                    tvDurationValue.setText(minutes + " phút " + seconds + " giây");
                } else {
                    tvDurationValue.setText(minutes + " phút");
                }
            } else {
                tvDurationValue.setText(seconds + " giây");
            }

            // Update calories
            int calories = session.getSummary().getEstKcal();
            tvCalories.setText(calories + " kcal");
        } else {
            tvDurationValue.setText("0 phút");
            tvCalories.setText("0 kcal");
        }

        // Update level - lấy từ exercise đầu tiên hoặc tính trung bình
        String averageLevel = calculateAverageLevel(session);
        tvLevel.setText(convertLevelToVietnamese(averageLevel));

        // Update exercise count - chỉ đếm các bài đã hoàn thành
        int completedCount = 0;
        if (session.getPerExercise() != null) {
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                if ("completed".equals(perExercise.getState())) {
                    completedCount++;
                }
            }
        }
        tvExerciseCount.setText(completedCount + " bài");
    }

    /**
     * Tính toán cấp độ trung bình từ các bài tập
     */
    private String calculateAverageLevel(Session session) {
        if (session.getPerExercise() == null || session.getPerExercise().isEmpty()) {
            return "beginner";
        }

        int beginnerCount = 0;
        int intermediateCount = 0;
        int advancedCount = 0;
        int totalCompleted = 0;

        for (Session.PerExercise perExercise : session.getPerExercise()) {
            if (!"completed".equals(perExercise.getState())) {
                continue;
            }
            totalCompleted++;

            String difficulty = perExercise.getDifficultyUsed();
            if (difficulty == null) {
                // Try to get from exercise
                Exercise exercise = getExerciseById(perExercise.getExerciseId());
                if (exercise != null && exercise.getDefaultConfig() != null) {
                    difficulty = exercise.getDefaultConfig().getDifficulty();
                }
            }

            if (difficulty == null) {
                difficulty = "beginner";
            }

            String level = difficulty.toLowerCase();
            if (level.contains("beginner") || level.contains("mới")) {
                beginnerCount++;
            } else if (level.contains("intermediate") || level.contains("trung")) {
                intermediateCount++;
            } else if (level.contains("advanced") || level.contains("nâng") || level.contains("pro")) {
                advancedCount++;
            } else {
                beginnerCount++; // Default
            }
        }

        if (totalCompleted == 0) {
            return "beginner";
        }

        // Determine average level
        if (advancedCount > totalCompleted / 2) {
            return "advanced";
        } else if (intermediateCount > totalCompleted / 2) {
            return "intermediate";
        } else {
            return "beginner";
        }
    }

    /**
     * Convert English level to Vietnamese for display
     */
    private String convertLevelToVietnamese(String englishLevel) {
        if (englishLevel == null || englishLevel.isEmpty()) {
            return "Người mới bắt đầu";
        }

        String lowerLevel = englishLevel.toLowerCase();
        if (lowerLevel.contains("beginner") || lowerLevel.contains("mới")) {
            return "Người mới bắt đầu";
        } else if (lowerLevel.contains("intermediate") || lowerLevel.contains("trung")) {
            return "Trung bình";
        } else if (lowerLevel.contains("advanced") || lowerLevel.contains("nâng") || lowerLevel.contains("pro")) {
            return "Nâng cao";
        }

        return "Người mới bắt đầu"; // Default
    }

    /**
     * Lấy Exercise theo ID
     */
    private Exercise getExerciseById(String exerciseId) {
        if (exerciseId == null || exercises == null) {
            return null;
        }

        for (Exercise exercise : exercises) {
            if (exerciseId.equals(exercise.getId())) {
                return exercise;
            }
        }

        return null;
    }
}

