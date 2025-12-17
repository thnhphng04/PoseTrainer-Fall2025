package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.FeedbackDAO;

/**
 * Dialog để gửi feedback về ứng dụng
 * Cho phép nhập nội dung feedback
 */
public class AppFeedbackDialog extends DialogFragment {
    private static final String TAG = "AppFeedbackDialog";
    
    private EditText etFeedbackContent;
    private Button btnSubmit;
    private Button btnCancel;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    
    private FeedbackDAO feedbackDAO;
    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_app_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        feedbackDAO = new FeedbackDAO();
        
        etFeedbackContent = view.findViewById(R.id.et_feedback_content);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutContent = view.findViewById(R.id.layout_content);
        
        // Setup click listeners
        btnSubmit.setOnClickListener(v -> submitFeedback());
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    /**
     * Gửi feedback
     */
    private void submitFeedback() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = etFeedbackContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung góp ý", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo feedback object
        String feedbackId = "feedback_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        Feedback feedback = new Feedback();
        feedback.setId(feedbackId);
        feedback.setUid(currentUser.getUid());
        feedback.setType("app");
        feedback.setExerciseId(null);
        feedback.setExerciseName(null);
        feedback.setContent(content);
        feedback.setStatus("pending");
        feedback.setCreatedAt(System.currentTimeMillis() / 1000);
        feedback.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        // Show loading
        setLoading(true);
        
        // Save to Firestore
        feedbackDAO.save(feedback, task -> {
            setLoading(false);
            
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Gửi góp ý thành công! Cảm ơn bạn đã phản hồi.", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Log.e(TAG, "Error saving feedback", task.getException());
                Toast.makeText(getContext(), "Lỗi khi gửi góp ý. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Set loading state
     */
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (layoutContent != null) {
            layoutContent.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!loading);
        }
        if (btnCancel != null) {
            btnCancel.setEnabled(!loading);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Set background đậm để không bị trong suốt
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Thêm dim background để làm nổi bật dialog
            getDialog().getWindow().setDimAmount(0.7f);
        }
    }
}

