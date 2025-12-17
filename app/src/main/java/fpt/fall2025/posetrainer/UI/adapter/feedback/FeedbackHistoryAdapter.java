package fpt.fall2025.posetrainer.UI.adapter.feedback;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter để hiển thị danh sách feedback trong dialog lịch sử
 */
public class FeedbackHistoryAdapter extends RecyclerView.Adapter<FeedbackHistoryAdapter.FeedbackViewHolder> {
    
    private Context context;
    private List<Feedback> feedbacks;
    private OnFeedbackClickListener listener;
    
    /**
     * Interface để xử lý sự kiện click vào feedback
     */
    public interface OnFeedbackClickListener {
        void onFeedbackClick(Feedback feedback);
    }
    
    public FeedbackHistoryAdapter(Context context, List<Feedback> feedbacks) {
        this.context = context;
        this.feedbacks = feedbacks != null ? feedbacks : new ArrayList<>();
    }
    
    public void setOnFeedbackClickListener(OnFeedbackClickListener listener) {
        this.listener = listener;
    }
    
    public void updateFeedbacks(List<Feedback> newFeedbacks) {
        this.feedbacks = newFeedbacks != null ? newFeedbacks : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_feedback_history, parent, false);
        return new FeedbackViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        Feedback feedback = feedbacks.get(position);
        holder.bind(feedback);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFeedbackClick(feedback);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return feedbacks.size();
    }
    
    /**
     * Format timestamp (milliseconds) to relative time in Vietnamese
     * @param timestampMillis Timestamp in milliseconds
     * @return Formatted string like "5 phút trước", "2 giờ trước"
     */
    private static String formatRelativeTimeVietnamese(long timestampMillis) {
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

    /**
     * ViewHolder class
     */
    public static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        private TextView tvFeedbackTypeIcon;
        private TextView tvFeedbackType;
        private TextView tvFeedbackStatus;
        private View layoutExerciseName;
        private TextView tvExerciseName;
        private TextView tvFeedbackContent;
        private TextView tvFeedbackTime;
        
        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFeedbackTypeIcon = itemView.findViewById(R.id.tv_feedback_type_icon);
            tvFeedbackType = itemView.findViewById(R.id.tv_feedback_type);
            tvFeedbackStatus = itemView.findViewById(R.id.tv_feedback_status);
            layoutExerciseName = itemView.findViewById(R.id.layout_exercise_name);
            tvExerciseName = itemView.findViewById(R.id.tv_exercise_name);
            tvFeedbackContent = itemView.findViewById(R.id.tv_feedback_content);
            tvFeedbackTime = itemView.findViewById(R.id.tv_feedback_time);
        }
        
        public void bind(Feedback feedback) {
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
                case "read":
                    statusText = "Đã đọc";
                    statusBgRes = R.drawable.status_background_read;
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
                tvFeedbackContent.setText("");
            }
            
            // Set thời gian
            if (feedback.getCreatedAt() > 0) {
                long createdAtMillis = feedback.getCreatedAt() * 1000; // Convert từ seconds sang milliseconds
                String timeAgo = formatRelativeTimeVietnamese(createdAtMillis);
                tvFeedbackTime.setText(timeAgo);
            } else {
                tvFeedbackTime.setText("");
            }
        }
    }
}
