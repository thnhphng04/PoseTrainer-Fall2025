package fpt.fall2025.posetrainer.UI.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.UUID;

import fpt.fall2025.posetrainer.UI.adapter.exercise.CustomExerciseAdapter;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.ExerciseUser;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.SessionDAO;

/**
 * Activity hiển thị danh sách custom exercises của user
 */
public class CustomExercisesActivity extends AppCompatActivity {
    private static final String TAG = "CustomExercisesActivity";

    private Toolbar toolbar;
    private RecyclerView recyclerViewExercises;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    private CustomExerciseAdapter adapter;
    private ArrayList<Exercise> customExercises;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionDAO sessionDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_exercises);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionDAO = new SessionDAO();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadCustomExercises();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewExercises = findViewById(R.id.recycler_view_exercises);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressBar = findViewById(R.id.progress_bar);

        customExercises = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CustomExerciseAdapter(customExercises, exercise -> {
            // Click vào exercise - tạo session và bắt đầu tập
            createQuickWorkoutSession(exercise);
        });
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExercises.setAdapter(adapter);
    }

    /**
     * Tạo quick workout session với một exercise và bắt đầu tập
     */
    private void createQuickWorkoutSession(Exercise exercise) {
        Log.d(TAG, "Creating quick workout session for exercise: " + exercise.getName());
        Log.d(TAG, "Exercise ID: " + exercise.getId());
        Log.d(TAG, "Exercise analyzerType: " + (exercise.getMediapipe() != null ? exercise.getMediapipe().getAnalyzerType() : "null"));

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate exercise ID
        if (exercise.getId() == null || exercise.getId().isEmpty()) {
            Log.e(TAG, "Exercise ID is null or empty");
            Toast.makeText(this, "Lỗi: Exercise ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate unique session ID
        String sessionId = "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Create session object
        Session session = new Session();
        session.setId(sessionId);
        session.setUid(currentUser.getUid());
        session.setTitle(exercise.getName());
        session.setDescription("Quick workout với " + exercise.getName());
        session.setStartedAt(System.currentTimeMillis() / 1000);
        session.setEndedAt(0);

        // Initialize session summary
        Session.SessionSummary summary = new Session.SessionSummary();
        summary.setDurationSec(0);
        summary.setEstKcal(0);
        session.setSummary(summary);

        // Initialize per-exercise data (chỉ có 1 exercise)
        ArrayList<Session.PerExercise> perExercises = new ArrayList<>();
        Session.PerExercise perExercise = new Session.PerExercise();
        perExercise.setExerciseNo(1);
        perExercise.setExerciseId(exercise.getId()); // Đảm bảo exercise ID được set đúng
        perExercise.setDifficultyUsed(exercise.getLevel() != null ? exercise.getLevel() : "beginner");
        perExercise.setState("not_started");

        // Tạo SetData ban đầu
        ArrayList<Session.SetData> initialSets = new ArrayList<>();
        int sets = exercise.getDefaultConfig() != null ? exercise.getDefaultConfig().getSets() : 3;
        int reps = exercise.getDefaultConfig() != null ? exercise.getDefaultConfig().getReps() : 12;
        
        Log.d(TAG, "Exercise sets: " + sets + ", reps: " + reps);
        
        for (int setNo = 1; setNo <= sets; setNo++) {
            Session.SetData setData = new Session.SetData();
            setData.setSetNo(setNo);
            setData.setTargetReps(reps);
            setData.setCorrectReps(0);
            setData.setState("incomplete");
            initialSets.add(setData);
        }
        perExercise.setSets(initialSets);
        perExercise.setMedia(new Session.ExerciseMedia());
        perExercises.add(perExercise);
        session.setPerExercise(perExercises);

        // Initialize session flags
        Session.SessionFlags flags = new Session.SessionFlags();
        flags.setUploaded(false);
        flags.setExportable(true);
        session.setFlags(flags);

        // Initialize device info
        Session.DeviceInfo deviceInfo = new Session.DeviceInfo();
        deviceInfo.setModel(android.os.Build.MODEL);
        deviceInfo.setOs("Android " + android.os.Build.VERSION.RELEASE);
        session.setDeviceInfo(deviceInfo);

        session.setAppVersion("1.0.0");

        Log.d(TAG, "Session created with ID: " + sessionId + ", exerciseId: " + perExercise.getExerciseId());

        // Save session to Firebase
        sessionDAO.saveSession(session, new SessionDAO.OnSessionSavedListener() {
            @Override
            public void onSessionSaved(boolean success) {
                if (success) {
                    Log.d(TAG, "Session saved successfully, starting workout");
                    // Start workout with session
                    Intent intent = new Intent(CustomExercisesActivity.this, SessionActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    startActivityForResult(intent, 1001);
                } else {
                    Log.e(TAG, "Failed to save session");
                    Toast.makeText(CustomExercisesActivity.this, "Lỗi tạo phiên tập", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Load custom exercises của user hiện tại từ Firebase
     */
    private void loadCustomExercises() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Loading custom exercises for user: " + uid);

        setLoading(true);

        // Load từ collection exerciseUser, chỉ lấy exercises của user hiện tại
        db.collection("exerciseUser")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        customExercises.clear();
                        Log.d(TAG, "Found " + task.getResult().size() + " custom exercises");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Load ExerciseUser từ Firestore
                                ExerciseUser exerciseUser = document.toObject(ExerciseUser.class);
                                if (exerciseUser != null) {
                                    exerciseUser.setId(document.getId());
                                    // Convert ExerciseUser to Exercise để tương thích với adapter và các phần khác
                                    Exercise exercise = exerciseUser.toExercise();
                                    customExercises.add(exercise);
                                    Log.d(TAG, "Loaded ExerciseUser: " + exerciseUser.getName() + " (ID: " + document.getId() + ")");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing ExerciseUser: " + e.getMessage());
                            }
                        }

                        updateUI();
                    } else {
                        Log.e(TAG, "Error loading custom exercises: ", task.getException());
                        Toast.makeText(this, "Lỗi tải danh sách bài tập", Toast.LENGTH_SHORT).show();
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (customExercises.isEmpty()) {
            recyclerViewExercises.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewExercises.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerViewExercises.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            // Workout completed or cancelled
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Workout completed successfully");
            } else {
                Log.d(TAG, "Workout cancelled or failed");
            }
        }
    }
}

