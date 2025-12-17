package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.FeedbackDAO;
import fpt.fall2025.posetrainer.UI.adapter.feedback.FeedbackHistoryAdapter;
import fpt.fall2025.posetrainer.UI.dialog.FeedbackDetailDialog;

/**
 * Dialog hiển thị lịch sử các feedback đã gửi của người dùng
 * Hiển thị trạng thái: pending, read, resolved
 */
public class FeedbackHistoryDialog extends DialogFragment {
    private static final String TAG = "FeedbackHistoryDialog";
    
    private RecyclerView rvFeedbackHistory;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageButton btnClose;
    
    private FeedbackDAO feedbackDAO;
    private FirebaseAuth mAuth;
    private FeedbackHistoryAdapter adapter;
    
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
        return inflater.inflate(R.layout.dialog_feedback_history, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        feedbackDAO = new FeedbackDAO();
        
        rvFeedbackHistory = view.findViewById(R.id.rv_feedback_history);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        btnClose = view.findViewById(R.id.btn_close);
        
        // Setup RecyclerView
        adapter = new FeedbackHistoryAdapter(getContext(), new ArrayList<>());
        adapter.setOnFeedbackClickListener(feedback -> {
            // Hiển thị dialog chi tiết feedback
            FeedbackDetailDialog detailDialog = FeedbackDetailDialog.newInstance(feedback);
            detailDialog.show(getParentFragmentManager(), "FeedbackDetailDialog");
        });
        rvFeedbackHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFeedbackHistory.setAdapter(adapter);
        
        // Setup click listeners
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
        
        // Load feedbacks
        loadFeedbacks();
    }
    
    /**
     * Load danh sách feedback của user hiện tại
     */
    private void loadFeedbacks() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        
        setLoading(true);
        
        feedbackDAO.getByUserId(currentUser.getUid(), task -> {
            setLoading(false);
            
            if (task.isSuccessful()) {
                List<Feedback> feedbacks = task.getResult();
                if (feedbacks == null) {
                    feedbacks = new ArrayList<>();
                }
                
                if (feedbacks.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.updateFeedbacks(feedbacks);
                }
            } else {
                Log.e(TAG, "Error loading feedbacks", task.getException());
                Toast.makeText(getContext(), "Lỗi khi tải lịch sử phản hồi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
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
        if (rvFeedbackHistory != null) {
            rvFeedbackHistory.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }
    
    /**
     * Hiển thị/ẩn empty state
     */
    private void showEmptyState(boolean show) {
        if (emptyState != null) {
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvFeedbackHistory != null) {
            rvFeedbackHistory.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.80);
            getDialog().getWindow().setLayout(width, height);
            // Set background đậm để không bị trong suốt
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Thêm dim background để làm nổi bật dialog
            getDialog().getWindow().setDimAmount(0.7f);
        }
    }
}
