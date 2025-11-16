package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fpt.fall2025.posetrainer.Adapter.SessionResultExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Activity hiển thị kết quả của session đã hoàn thành
 * Chỉ hiển thị thông tin (read-only), không cho phép bắt đầu lại
 */
public class SessionResultActivity extends AppCompatActivity {
    private static final String TAG = "SessionResultActivity";

    // UI Components
    private Toolbar toolbar;
    private TextView tvWorkoutTitle;
    private TextView tvSessionDate;
    private TextView tvDuration;
    private TextView tvCalories;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyState;

    // Data
    private Session currentSession;
    private List<Exercise> exercises;
    private SessionResultExerciseAdapter adapter;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_result);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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
        setupToolbar();
        setupRecyclerView();

        // Load session data from Firebase
        loadSessionData(sessionId);
    }

    /**
     * Khởi tạo các view từ layout
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvWorkoutTitle = findViewById(R.id.tv_workout_title);
        tvSessionDate = findViewById(R.id.tv_session_date);
        tvDuration = findViewById(R.id.tv_duration);
        tvCalories = findViewById(R.id.tv_calories);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        recyclerViewExercises = findViewById(R.id.recycler_view_exercises);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Initialize data lists
        exercises = new ArrayList<>();
    }

    /**
     * Setup toolbar với nút back
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Setup RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new SessionResultExerciseAdapter(this, new ArrayList<>(), exercises);
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExercises.setAdapter(adapter);
    }

    /**
     * Load session từ Firebase Firestore
     */
    private void loadSessionData(String sessionId) {
        Log.d(TAG, "=== ĐANG TẢI SESSION ===");
        Log.d(TAG, "Đang tải session cho ID: " + sessionId);

        FirebaseService.getInstance().loadSessionById(sessionId, new FirebaseService.OnSessionLoadedListener() {
            @Override
            public void onSessionLoaded(Session session) {
                Log.d(TAG, "=== SESSION ĐÃ TẢI ===");
                
                if (session != null) {
                    currentSession = session;
                    Log.d(TAG, "Đã tải session thành công: " + session.getId());

                    // Load exercises từ session data
                    loadExercisesFromSession(session);

                    // Update UI với session data
                    updateUIWithSessionData(session);
                } else {
                    Log.e(TAG, "Không tìm thấy session");
                    Toast.makeText(SessionResultActivity.this, "Không tìm thấy thông tin buổi tập", Toast.LENGTH_SHORT).show();
                    showEmptyState("Không tìm thấy thông tin buổi tập");
                }

                Log.d(TAG, "=== KẾT THÚC TẢI SESSION ===");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Lỗi khi tải session: " + error);
                Toast.makeText(SessionResultActivity.this, "Lỗi khi tải thông tin buổi tập: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState("Lỗi khi tải thông tin buổi tập");
            }
        });
    }

    /**
     * Load exercises từ session data
     */
    private void loadExercisesFromSession(Session session) {
        Log.d(TAG, "Đang tải exercises từ session data");

        // Lấy danh sách exercise IDs từ session perExercise data
        Set<String> exerciseIds = new HashSet<>();
        if (session.getPerExercise() != null) {
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                if (perExercise.getExerciseId() != null && !perExercise.getExerciseId().isEmpty()) {
                    exerciseIds.add(perExercise.getExerciseId());
                }
            }
        }

        Log.d(TAG, "Tìm thấy " + exerciseIds.size() + " exercise IDs duy nhất");

        if (exerciseIds.isEmpty()) {
            Log.w(TAG, "Không có exercise IDs trong session");
            exercises = new ArrayList<>();
            updateExercisesList();
            return;
        }

        // Load exercises bằng IDs
        FirebaseService.getInstance().loadExercisesByIds(new ArrayList<>(exerciseIds), this, new FirebaseService.OnExercisesLoadedListener() {
            @Override
            public void onExercisesLoaded(ArrayList<Exercise> exercisesList) {
                Log.d(TAG, "=== EXERCISES ĐÃ TẢI ===");
                Log.d(TAG, "Đã tải được " + (exercisesList != null ? exercisesList.size() : 0) + " exercises");

                exercises = exercisesList != null ? exercisesList : new ArrayList<>();

                // Update exercises list trong adapter
                updateExercisesList();

                Log.d(TAG, "=== KẾT THÚC TẢI EXERCISES ===");
            }
        });
    }

    /**
     * Cập nhật danh sách exercises trong adapter
     */
    private void updateExercisesList() {
        if (currentSession == null || currentSession.getPerExercise() == null || currentSession.getPerExercise().isEmpty()) {
            showEmptyState("Không có bài tập nào");
            adapter.updateExercises(new ArrayList<>(), exercises);
            return;
        }

        showExercisesList();
        adapter.updateExercises(currentSession.getPerExercise(), exercises);
    }

    /**
     * Cập nhật UI với dữ liệu session
     */
    private void updateUIWithSessionData(Session session) {
        // Hiển thị title của workout
        if (session.getTitle() != null && !session.getTitle().isEmpty()) {
            tvWorkoutTitle.setText(session.getTitle());
        } else {
            tvWorkoutTitle.setText("Buổi tập");
        }

        // Hiển thị ngày tập
        if (session.getStartedAt() > 0) {
            Date sessionDate = new Date(session.getStartedAt() * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'lúc' HH:mm", Locale.getDefault());
            String dateString = dateFormat.format(sessionDate);
            tvSessionDate.setText(dateString);
        } else {
            tvSessionDate.setText("Không rõ ngày");
        }

        // Hiển thị thời lượng (duration)
        int durationMinutes = 0;
        if (session.getSummary() != null && session.getSummary().getDurationSec() > 0) {
            durationMinutes = session.getSummary().getDurationSec() / 60;
        } else if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
            // Tính từ startedAt và endedAt (cả hai đều là seconds)
            long durationSec = session.getEndedAt() - session.getStartedAt();
            durationMinutes = (int) (durationSec / 60);
        }
        tvDuration.setText(durationMinutes + " phút");

        // Hiển thị calories
        int calories = 0;
        if (session.getSummary() != null) {
            calories = session.getSummary().getEstKcal();
        }
        tvCalories.setText(calories + " kcal");

        // Hiển thị tiến độ
        if (session.getPerExercise() != null && !session.getPerExercise().isEmpty()) {
            int totalExercises = session.getPerExercise().size();
            int completedExercises = 0;

            for (Session.PerExercise perExercise : session.getPerExercise()) {
                if ("completed".equals(perExercise.getState())) {
                    completedExercises++;
                }
            }

            tvProgress.setText(completedExercises + "/" + totalExercises + " bài tập hoàn thành");

            // Cập nhật progress bar
            int progressPercent = totalExercises > 0 ? (completedExercises * 100) / totalExercises : 0;
            progressBar.setProgress(progressPercent);
        } else {
            tvProgress.setText("0/0 bài tập hoàn thành");
            progressBar.setProgress(0);
        }
    }

    /**
     * Hiển thị danh sách exercises
     */
    private void showExercisesList() {
        recyclerViewExercises.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Hiển thị empty state
     */
    private void showEmptyState(String message) {
        recyclerViewExercises.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText(message);
    }
}

