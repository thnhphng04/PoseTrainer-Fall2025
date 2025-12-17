package fpt.fall2025.posetrainer.UI.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.UI.dialog.ExerciseFeedbackDialog;
import fpt.fall2025.posetrainer.UI.dialog.AppFeedbackDialog;
import fpt.fall2025.posetrainer.UI.dialog.FeedbackHistoryDialog;

/**
 * FeedbackActivity - Màn hình gửi feedback
 * Hiển thị 2 nút: Góp ý về bài tập và Góp ý về ứng dụng
 */
public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";

    private Toolbar toolbar;
    private LinearLayout btnExerciseFeedback;
    private LinearLayout btnAppFeedback;
    private LinearLayout btnFeedbackHistory;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupClickListeners();
        
        // Check if should auto-show dialog with exercise
        checkAutoShowDialog();
    }
    
    /**
     * Kiểm tra và tự động hiển thị dialog nếu có exercise từ Intent
     */
    private void checkAutoShowDialog() {
        Exercise exercise = (Exercise) getIntent().getSerializableExtra("exercise");
        boolean autoShow = getIntent().getBooleanExtra("auto_show_dialog", false);
        
        if (autoShow && exercise != null) {
            // Tự động hiển thị dialog với exercise đã chọn
            ExerciseFeedbackDialog dialog = ExerciseFeedbackDialog.newInstance(exercise);
            dialog.show(getSupportFragmentManager(), "ExerciseFeedbackDialog");
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        btnExerciseFeedback = findViewById(R.id.btn_exercise_feedback);
        btnAppFeedback = findViewById(R.id.btn_app_feedback);
        btnFeedbackHistory = findViewById(R.id.btn_feedback_history);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Gửi góp ý");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        // Nút góp ý về bài tập
        btnExerciseFeedback.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ExerciseFeedbackDialog dialog = new ExerciseFeedbackDialog();
            dialog.show(getSupportFragmentManager(), "ExerciseFeedbackDialog");
        });

        // Nút góp ý về ứng dụng
        btnAppFeedback.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }
            
            AppFeedbackDialog dialog = new AppFeedbackDialog();
            dialog.show(getSupportFragmentManager(), "AppFeedbackDialog");
        });

        // Nút lịch sử phản hồi
        btnFeedbackHistory.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }
            
            FeedbackHistoryDialog dialog = new FeedbackHistoryDialog();
            dialog.show(getSupportFragmentManager(), "FeedbackHistoryDialog");
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

