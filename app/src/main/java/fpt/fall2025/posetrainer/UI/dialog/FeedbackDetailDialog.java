package fpt.fall2025.posetrainer.UI.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.R;

/**
 * Dialog hiển thị chi tiết feedback
 */
public class FeedbackDetailDialog extends DialogFragment {
    private static final String TAG = "FeedbackDetailDialog";
    private static final String ARG_FEEDBACK = "feedback";
    
    private TextView tvFeedbackTypeIcon;
    private TextView tvFeedbackType;
    private TextView tvFeedbackStatus;
    private TextView tvFeedbackTime;
    private LinearLayout layoutExerciseName;
    private TextView tvExerciseName;
    private TextView tvFeedbackContent;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private View layoutUpdatedAt;
    private ImageButton btnClose;
    private com.google.android.material.button.MaterialButton btnCloseBottom;
    
    private Feedback feedback;
    
    /**
     * Tạo instance mới của dialog với feedback
     */
    public static FeedbackDetailDialog newInstance(Feedback feedback) {
        FeedbackDetailDialog dialog = new FeedbackDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FEEDBACK, feedback);
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
        return inflater.inflate(R.layout.dialog_feedback_detail, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Lấy feedback từ arguments
        if (getArguments() != null) {
            feedback = (Feedback) getArguments().getSerializable(ARG_FEEDBACK);
        }
        
        if (feedback == null) {
            dismiss();
            return;
        }
        
        initViews(view);
        setupClickListeners();
        bindData();
    }
    
    private void initViews(View view) {
        tvFeedbackTypeIcon = view.findViewById(R.id.tv_feedback_type_icon);
        tvFeedbackType = view.findViewById(R.id.tv_feedback_type);
        tvFeedbackStatus = view.findViewById(R.id.tv_feedback_status);
        tvFeedbackTime = view.findViewById(R.id.tv_feedback_time);
        layoutExerciseName = view.findViewById(R.id.layout_exercise_name);
        tvExerciseName = view.findViewById(R.id.tv_exercise_name);
        tvFeedbackContent = view.findViewById(R.id.tv_feedback_content);
        tvCreatedAt = view.findViewById(R.id.tv_created_at);
        tvUpdatedAt = view.findViewById(R.id.tv_updated_at);
        layoutUpdatedAt = view.findViewById(R.id.layout_updated_at);
        btnClose = view.findViewById(R.id.btn_close);
        btnCloseBottom = view.findViewById(R.id.btn_close_bottom);
    }
    
    private void setupClickListeners() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
        if (btnCloseBottom != null) {
            btnCloseBottom.setOnClickListener(v -> dismiss());
        }
    }
    
    /**
     * Format timestamp (milliseconds) to relative time in Vietnamese
     * @param timestampMillis Timestamp in milliseconds
     * @return Formatted string like "5 phút trước", "2 giờ trước"
     */
    private String formatRelativeTimeVietnamese(long timestampMillis) {
        if (timestampMillis == 0) return "Không xác định";
        
        long now = System.currentTimeMillis();
        long diffMillis = now - timestampMillis;
        
        if (diffMillis < 60000) { // Less than 1 minute
            return "Vừa xong";
        } else if (diffMillis < 3600000) { // Less than 1 hour
            long minutes = diffMillis / 60000;
            return minutes + " phút trước";
        } else if (diffMillis < 86400000) { // Less than 1 day
            long hours = diffMillis / 3600000;
            return hours + " giờ trước";
        } else if (diffMillis < 2592000000L) { // Less than 30 days
            long days = diffMillis / 86400000;
            return days + " ngày trước";
        } else if (diffMillis < 31104000000L) { // Less than 1 year
            long months = diffMillis / 2592000000L;
            return months + " tháng trước";
        } else {
            long years = diffMillis / 31104000000L;
            return years + " năm trước";
        }
    }

    private void bindData() {
        // Set icon và loại feedback
        String typeIcon = "📱";
        String typeText = "Góp ý về ứng dụng";
        
        if ("exercise".equals(feedback.getType())) {
            typeIcon = "💪";
            typeText = "Góp ý về bài tập";
        } else if ("post".equals(feedback.getType())) {
            typeIcon = "📝";
            typeText = "Góp ý về bài viết";
        }
        
        tvFeedbackTypeIcon.setText(typeIcon);
        tvFeedbackType.setText(typeText);
        
        // Set trạng thái
        String status = feedback.getStatus();
        if (status == null) {
            status = "pending";
        }
        
        String statusText = "Đang chờ";
        int statusBgRes = R.drawable.status_background_pending;
        
        switch (status.toLowerCase()) {
            case "accepted":
                statusText = "Đã chấp nhận";
                statusBgRes = R.drawable.status_background_accepted;
                break;
            case "rejected":
                statusText = "Đã từ chối";
                statusBgRes = R.drawable.status_background_rejected;
                break;
            case "resolved":
                statusText = "Đã xử lý";
                statusBgRes = R.drawable.status_background_resolved;
                break;
            case "pending":
            default:
                statusText = "Đang chờ";
                statusBgRes = R.drawable.status_background_pending;
                break;
        }
        
        tvFeedbackStatus.setText(statusText);
        tvFeedbackStatus.setBackgroundResource(statusBgRes);
        
        // Set tên bài tập (nếu có)
        if ("exercise".equals(feedback.getType()) && feedback.getExerciseName() != null && !feedback.getExerciseName().isEmpty()) {
            tvExerciseName.setText(feedback.getExerciseName());
            layoutExerciseName.setVisibility(View.VISIBLE);
        } else {
            layoutExerciseName.setVisibility(View.GONE);
        }
        
        // Set nội dung feedback
        if (feedback.getContent() != null) {
            tvFeedbackContent.setText(feedback.getContent());
        } else {
            tvFeedbackContent.setText("Không có nội dung");
        }
        
        // Set thời gian tương đối
        if (feedback.getCreatedAt() > 0) {
            long createdAtMillis = feedback.getCreatedAt() * 1000;
            String timeAgo = formatRelativeTimeVietnamese(createdAtMillis);
            tvFeedbackTime.setText(timeAgo);
        } else {
            tvFeedbackTime.setText("");
        }
        
        // Set thời gian tạo chi tiết
        if (feedback.getCreatedAt() > 0) {
            long createdAtMillis = feedback.getCreatedAt() * 1000;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(createdAtMillis));
            tvCreatedAt.setText(formattedDate);
        } else {
            tvCreatedAt.setText("Không xác định");
        }
        
        // Set thời gian cập nhật (nếu có và khác với thời gian tạo)
        if (feedback.getUpdatedAt() > 0 && feedback.getUpdatedAt() != feedback.getCreatedAt()) {
            long updatedAtMillis = feedback.getUpdatedAt() * 1000;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(updatedAtMillis));
            tvUpdatedAt.setText(formattedDate);
            if (layoutUpdatedAt != null) {
                layoutUpdatedAt.setVisibility(View.VISIBLE);
            }
        } else {
            if (layoutUpdatedAt != null) {
                layoutUpdatedAt.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.85);
            getDialog().getWindow().setLayout(width, height);
            // Set background đậm để không bị trong suốt
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Thêm dim background để làm nổi bật dialog
            getDialog().getWindow().setDimAmount(0.7f);
        }
    }
}
