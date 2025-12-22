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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
    private ListenerRegistration feedbacksListener; // Listener cho real-time updates
    
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
        
        // Load feedbacks với real-time listener
        startFeedbacksListener();
    }
    
    /**
     * Bắt đầu lắng nghe real-time updates cho danh sách feedback
     */
    private void startFeedbacksListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        
        // Dừng listener cũ nếu có
        stopFeedbacksListener();
        
        setLoading(true);
        
        // Sử dụng real-time listener thay vì chỉ load một lần
        feedbacksListener = feedbackDAO.listenByUserId(currentUser.getUid(), (querySnapshot, error) -> {
            setLoading(false);
            
            if (error != null) {
                Log.e(TAG, "Error listening to feedbacks", error);
                // Nếu lỗi do thiếu index, thử dùng query đơn giản hơn
                if (error.getMessage() != null && (error.getMessage().contains("index") || error.getMessage().contains("requires an index"))) {
                    Log.w(TAG, "Thử dùng query đơn giản (không có orderBy)");
                    // Fallback: dùng query đơn giản và sort client-side
                    startSimpleFeedbacksListener(currentUser.getUid());
                    return;
                }
                Toast.makeText(getContext(), "Lỗi khi tải lịch sử phản hồi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
                return;
            }
            
            if (querySnapshot != null) {
                List<Feedback> feedbacks = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Feedback feedback = doc.toObject(Feedback.class);
                    if (feedback != null) {
                        feedback.setId(doc.getId());
                        feedbacks.add(feedback);
                    }
                }
                
                // Sort client-side nếu cần (trong trường hợp query không có orderBy)
                feedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                
                if (feedbacks.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.updateFeedbacks(feedbacks);
                }
                
                Log.d(TAG, "✅ Cập nhật " + feedbacks.size() + " feedback (real-time)");
            }
        });
    }
    
    /**
     * Fallback: Dùng query đơn giản (không có orderBy) và sort client-side
     */
    private void startSimpleFeedbacksListener(String uid) {
        stopFeedbacksListener();
        
        feedbacksListener = feedbackDAO.getCollection()
            .whereEqualTo("uid", uid)
            .addSnapshotListener((querySnapshot, error) -> {
                setLoading(false);
                
                if (error != null) {
                    Log.e(TAG, "Error listening to feedbacks (simple query)", error);
                    Toast.makeText(getContext(), "Lỗi khi tải lịch sử phản hồi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                    return;
                }
                
                if (querySnapshot != null) {
                    List<Feedback> feedbacks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Feedback feedback = doc.toObject(Feedback.class);
                        if (feedback != null) {
                            feedback.setId(doc.getId());
                            feedbacks.add(feedback);
                        }
                    }
                    
                    // Sort client-side theo createdAt giảm dần
                    feedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    
                    if (feedbacks.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        adapter.updateFeedbacks(feedbacks);
                    }
                    
                    Log.d(TAG, "✅ Cập nhật " + feedbacks.size() + " feedback (simple query, real-time)");
                }
            });
    }
    
    /**
     * Dừng lắng nghe real-time updates
     */
    private void stopFeedbacksListener() {
        if (feedbacksListener != null) {
            feedbacksListener.remove();
            feedbacksListener = null;
            Log.d(TAG, "Đã dừng listener feedback");
        }
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
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Dừng listener khi dialog bị dismiss
        stopFeedbacksListener();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dừng listener khi dialog bị đóng để tránh memory leak
        stopFeedbacksListener();
    }
}
