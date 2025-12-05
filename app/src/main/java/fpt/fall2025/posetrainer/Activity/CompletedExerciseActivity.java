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

import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Adapter.CompletedExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Profile;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Dialog.AchievementUnlockedDialog;
import fpt.fall2025.posetrainer.Helper.CalorieCalculator;
import fpt.fall2025.posetrainer.Manager.AchievementManager;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.ProfileDAO;
import fpt.fall2025.posetrainer.DAL.SessionDAO;
import fpt.fall2025.posetrainer.DAL.StreakDAO;
import fpt.fall2025.posetrainer.DAL.UserProgressDAO;
import fpt.fall2025.posetrainer.DAL.ExerciseDAO;

import com.google.firebase.firestore.DocumentSnapshot;

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
    private AuthService authService;
    private ProfileDAO profileDAO;
    private SessionDAO sessionDAO;
    private StreakDAO streakDAO;
    private UserProgressDAO userProgressDAO;
    private ExerciseDAO exerciseDAO;

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

        // Initialize Firebase
        authService = new AuthService();
        profileDAO = new ProfileDAO();
        sessionDAO = new SessionDAO();
        streakDAO = new StreakDAO();
        userProgressDAO = new UserProgressDAO();
        exerciseDAO = new ExerciseDAO();

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
            FirebaseUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để lưu kết quả", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Lưu");
                return;
            }

            String uid = currentUser.getUid();

            Log.d(TAG, "🔥 Bắt đầu cập nhật streak và achievements cho session: " + currentSession.getId());

            // QUAN TRỌNG: Đảm bảo session đã completed trước khi update streak
            // 1. Đảm bảo tất cả PerExercise có state = "completed" (bao gồm cả khi skip)
            if (currentSession.getPerExercise() != null) {
                int totalExercises = currentSession.getPerExercise().size();
                int completedCount = 0;
                for (Session.PerExercise perExercise : currentSession.getPerExercise()) {
                    String currentState = perExercise.getState();
                    if (!"completed".equals(currentState)) {
                        Log.d(TAG, "Đang set state = 'completed' cho Exercise #" + perExercise.getExerciseNo() + 
                            " (state hiện tại: " + (currentState != null ? currentState : "null") + ")");
                        perExercise.setState("completed");
                    }
                    completedCount++;
                }
                Log.d(TAG, "Đã đảm bảo tất cả " + completedCount + "/" + totalExercises + " exercises có state = 'completed'");
            }

            // 2. Đảm bảo endedAt được set nếu chưa có
            if (currentSession.getEndedAt() == 0 || currentSession.getEndedAt() <= currentSession.getStartedAt()) {
                Log.d(TAG, "Đang set endedAt cho session (endedAt hiện tại: " + currentSession.getEndedAt() + ")");
                currentSession.setEndedAt(System.currentTimeMillis() / 1000);
                
                // Đảm bảo Summary được tạo nếu chưa có
                if (currentSession.getSummary() == null) {
                    currentSession.setSummary(new Session.SessionSummary());
                }
                
                // Update duration
                long durationSec = currentSession.getEndedAt() - currentSession.getStartedAt();
                currentSession.getSummary().setDurationSec((int) durationSec);
                
                // Load user profile để lấy weight và tính calories bằng METs formula
                loadUserProfileAndUpdateCalories(uid, () -> {
                    // Save session với endedAt và calories mới trước khi update streak
                    sessionDAO.saveSession(currentSession, success -> {
                        if (success) {
                            Log.d(TAG, "✅ Session đã được cập nhật với endedAt và calories trước khi update streak");
                            updateStreakAndAchievements(uid);
                        } else {
                            Log.e(TAG, "❌ Lỗi khi lưu session với endedAt");
                            Toast.makeText(CompletedExerciseActivity.this, "Lỗi khi lưu session", Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Lưu");
                        }
                    });
                });
            } else {
                // Session đã có endedAt, nhưng cần đảm bảo calories đã được tính đúng
                // Load profile và update calories nếu cần
                loadUserProfileAndUpdateCalories(uid, () -> {
                    // Nếu calories đã được cập nhật, lưu lại session
                    sessionDAO.saveSession(currentSession, success -> {
                        if (success) {
                            Log.d(TAG, "✅ Session đã được cập nhật với calories");
                        }
                    });
                    // Update streak ngay
                    updateStreakAndAchievements(uid);
                });
            }
        });
    }

    /**
     * Update streak và achievements sau khi đảm bảo session đã completed
     */
    private void updateStreakAndAchievements(String uid) {
        // Update streak
        streakDAO.updateStreak(uid, currentSession, streak -> {
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
                        userProgressDAO.updateUserProgress(uid, progress -> {
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

        sessionDAO.loadSessionById(sessionId, new SessionDAO.OnSessionLoadedListener() {
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

            exerciseDAO.getById(exerciseId, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    exercises.add(task.getResult());
                }
                loadedCount[0]++;
                if (loadedCount[0] == totalExercises) {
                    updateAdapter(perExercises);
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

        // Update duration - lấy từ summary.durationSec (thời gian thực tế đã tập)
        int durationSec = 0;
        if (session.getSummary() != null && session.getSummary().getDurationSec() > 0) {
            durationSec = session.getSummary().getDurationSec();
        } else if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
            // Fallback: tính từ endedAt - startedAt nếu summary không có
            durationSec = (int)(session.getEndedAt() - session.getStartedAt());
        }
        
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

        // Update calories - lấy từ summary.estKcal (đã được tính bằng METs formula)
        int calories = 0;
        if (session.getSummary() != null) {
            calories = session.getSummary().getEstKcal();
        }
        // Nếu calories = 0, có thể session chưa được tính calories, nhưng không hiển thị giá trị ước tính cũ
        tvCalories.setText(calories > 0 ? calories + " kcal" : "0 kcal");

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

    /**
     * Load user profile để lấy weight và tính calories bằng METs formula
     */
    private void loadUserProfileAndUpdateCalories(String uid, Runnable onComplete) {
        profileDAO.getDocument(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    double weightKg = 70.0; // Default weight
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        if (profile != null && profile.getWeightKg() > 0) {
                            weightKg = profile.getWeightKg();
                        }
                    }
                    
                    // Tính calories bằng METs formula
                    int estimatedKcal = calculateCaloriesForSession(weightKg);
                    if (currentSession.getSummary() != null) {
                        currentSession.getSummary().setEstKcal(estimatedKcal);
                        Log.d(TAG, "✅ Calories đã được cập nhật: " + estimatedKcal + " kcal (weight: " + weightKg + " kg)");
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi load profile, sử dụng weight mặc định", e);
                    // Sử dụng weight mặc định
                    double weightKg = 70.0;
                    int estimatedKcal = calculateCaloriesForSession(weightKg);
                    if (currentSession.getSummary() != null) {
                        currentSession.getSummary().setEstKcal(estimatedKcal);
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    /**
     * Tính calories cho session bằng METs formula
     * Ưu tiên sử dụng calculateTotalCaloriesForSession nếu có exercises, 
     * fallback sang calculateCaloriesByDuration
     */
    private int calculateCaloriesForSession(double weightKg) {
        if (currentSession == null || currentSession.getSummary() == null) {
            return 0;
        }
        
        int durationSec = currentSession.getSummary().getDurationSec();
        if (durationSec <= 0) {
            return 0;
        }
        
        double durationMinutes = durationSec / 60.0;
        
        // Nếu có exercises đã load, sử dụng calculateTotalCaloriesForSession
        if (exercises != null && !exercises.isEmpty() && currentSession.getPerExercise() != null) {
            Exercise[] exerciseArray = exercises.toArray(new Exercise[0]);
            int totalCalories = CalorieCalculator.calculateTotalCaloriesForSession(currentSession, weightKg, exerciseArray);
            if (totalCalories > 0) {
                return totalCalories;
            }
        }
        
        // Fallback: tính bằng average METs
        double averageMets = calculateAverageMets();
        return CalorieCalculator.calculateCaloriesByDuration(averageMets, weightKg, durationMinutes);
    }

    /**
     * Tính average METs từ các exercises trong session
     */
    private double calculateAverageMets() {
        if (currentSession == null || currentSession.getPerExercise() == null || currentSession.getPerExercise().isEmpty()) {
            return 5.0; // Default METs for calisthenics
        }
        
        double totalMets = 0.0;
        int count = 0;
        
        for (Session.PerExercise perExercise : currentSession.getPerExercise()) {
            if (!"completed".equals(perExercise.getState())) {
                continue;
            }
            
            Exercise exercise = getExerciseById(perExercise.getExerciseId());
            if (exercise != null && exercise.getDefaultConfig() != null) {
                double mets = exercise.getDefaultConfig().getMets();
                if (mets > 0) {
                    totalMets += mets;
                    count++;
                }
            }
        }
        
        if (count > 0) {
            return totalMets / count;
        }
        
        return 5.0; // Default METs
    }
}

