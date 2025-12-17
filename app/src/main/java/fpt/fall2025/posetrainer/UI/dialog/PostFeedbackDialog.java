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
 * Dialog để báo cáo/góp ý về bài viết
 * Cho phép nhập nội dung báo cáo
 */
public class PostFeedbackDialog extends DialogFragment {
    private static final String TAG = "PostFeedbackDialog";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_POST_CONTENT = "post_content";
    
    private EditText etFeedbackContent;
    private Button btnSubmit;
    private Button btnCancel;
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    
    private String postId;
    private String postContent;
    private FeedbackDAO feedbackDAO;
    private FirebaseAuth mAuth;

    /**
     * Tạo instance mới của dialog với post ID và content
     */
    public static PostFeedbackDialog newInstance(String postId, String postContent) {
        PostFeedbackDialog dialog = new PostFeedbackDialog();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        args.putString(ARG_POST_CONTENT, postContent != null ? postContent : "");
        dialog.setArguments(args);
        return dialog;
    }

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
        return inflater.inflate(R.layout.dialog_post_feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        feedbackDAO = new FeedbackDAO();
        
        // Load post data from arguments
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
            postContent = getArguments().getString(ARG_POST_CONTENT);
        }
        
        etFeedbackContent = view.findViewById(R.id.et_feedback_content);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnCancel = view.findViewById(R.id.btn_cancel);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutContent = view.findViewById(R.id.layout_content);
        
        // Hiển thị preview nội dung bài viết nếu có
        TextView tvPostPreview = view.findViewById(R.id.tv_post_preview);
        if (tvPostPreview != null && postContent != null && !postContent.isEmpty()) {
            // Hiển thị tối đa 100 ký tự
            String preview = postContent.length() > 100 ? postContent.substring(0, 100) + "..." : postContent;
            tvPostPreview.setText("Bài viết: " + preview);
            tvPostPreview.setVisibility(View.VISIBLE);
        } else if (tvPostPreview != null) {
            tvPostPreview.setVisibility(View.GONE);
        }
        
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
        
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = etFeedbackContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo feedback object
        String feedbackId = "feedback_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        Feedback feedback = new Feedback();
        feedback.setId(feedbackId);
        feedback.setUid(currentUser.getUid());
        feedback.setType("post");
        feedback.setExerciseId(null);
        feedback.setExerciseName(null);
        feedback.setPostId(postId);
        feedback.setPostContent(postContent != null ? postContent : "");
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
                Toast.makeText(getContext(), "Báo cáo đã được gửi! Cảm ơn bạn đã phản hồi.", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Log.e(TAG, "Error saving feedback", task.getException());
                Toast.makeText(getContext(), "Lỗi khi gửi báo cáo. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
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
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setDimAmount(0.7f);
        }
    }
}

