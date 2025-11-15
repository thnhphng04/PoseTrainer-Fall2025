package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter để hiển thị danh sách thông báo trong RecyclerView
 * Hỗ trợ hiển thị cả thông báo thường và thông báo AI
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    
    private Context context;
    private ArrayList<Notification> notifications;
    private OnNotificationClickListener listener;

    /**
     * Interface để xử lý sự kiện click vào notification
     */
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationLongClick(Notification notification); // Để xóa hoặc tùy chọn khác
    }

    /**
     * Constructor
     * @param context Context
     * @param notifications Danh sách thông báo
     * @param listener Listener xử lý click
     */
    public NotificationAdapter(Context context, ArrayList<Notification> notifications, 
                              OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout cho notification item
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        // Lấy notification tại vị trí hiện tại
        Notification notification = notifications.get(position);
        
        // Set title
        holder.titleTextView.setText(notification.getTitle());
        
        // Set body
        holder.bodyTextView.setText(notification.getBody());
        
        // Set thời gian (relative time: "5 phút trước", "2 giờ trước"...)
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
            notification.getSentAt(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        );
        holder.timeTextView.setText(timeAgo);
        
        // Hiển thị icon dựa theo loại thông báo
        int iconRes = getIconResourceForType(notification.getType());
        holder.iconImageView.setImageResource(iconRes);
        
        // Hiển thị badge AI nếu là thông báo từ AI
        if (notification.isFromAI()) {
            holder.aiBadge.setVisibility(View.VISIBLE);
        } else {
            holder.aiBadge.setVisibility(View.GONE);
        }
        
        // Hiển thị chấm tròn và in đậm nếu chưa đọc
        if (!notification.isRead()) {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.titleTextView.setTypeface(null, Typeface.BOLD);
            holder.bodyTextView.setTypeface(null, Typeface.BOLD);
            holder.cardView.setCardElevation(8); // Nổi bật hơn
        } else {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.titleTextView.setTypeface(null, Typeface.NORMAL);
            holder.bodyTextView.setTypeface(null, Typeface.NORMAL);
            holder.cardView.setCardElevation(2); // Bình thường
        }
        
        // Xử lý click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
        
        // Xử lý long click (giữ lâu) để hiển thị menu xóa/tùy chọn
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onNotificationLongClick(notification);
            }
            return true; // Consume event
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    /**
     * Lấy icon phù hợp theo loại thông báo
     * @param type Loại thông báo
     * @return Resource ID của icon
     */
    private int getIconResourceForType(String type) {
        if (type == null) return R.drawable.ic_notifications;
        
        switch (type) {
            case "ai_reminder_smart":
            case "workout_reminder_sent":
                return R.drawable.ic_calendar_today; // Icon lịch
                
            case "ai_feedback_posture":
            case "ai_feedback_consistency":
                return R.drawable.ic_feedback; // Icon feedback (cần tạo)
                
            case "ai_achievement":
            case "achievement":
                return R.drawable.ic_trophy; // Icon cúp (cần tạo)
                
            case "ai_plan_update":
                return R.drawable.ic_plan; // Icon kế hoạch (cần tạo)
                
            case "social":
                return R.drawable.ic_social; // Icon mạng xã hội (cần tạo)
                
            default:
                return R.drawable.ic_notifications; // Icon mặc định
        }
    }

    /**
     * Cập nhật danh sách thông báo
     * @param newNotifications Danh sách mới
     */
    public void updateNotifications(ArrayList<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    /**
     * Xóa một thông báo khỏi danh sách
     * @param position Vị trí cần xóa
     */
    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notifications.size());
        }
    }

    /**
     * Xóa thông báo theo ID
     * @param notificationId ID thông báo cần xóa
     */
    public void removeNotificationById(String notificationId) {
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).getId().equals(notificationId)) {
                removeNotification(i);
                break;
            }
        }
    }

    /**
     * ViewHolder cho notification item
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView iconImageView;
        TextView titleTextView;
        TextView bodyTextView;
        TextView timeTextView;
        View unreadIndicator; // Chấm tròn màu xanh cho thông báo chưa đọc
        TextView aiBadge; // Badge "AI" cho thông báo từ AI

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Ánh xạ các view
            cardView = itemView.findViewById(R.id.notification_card);
            iconImageView = itemView.findViewById(R.id.notification_icon);
            titleTextView = itemView.findViewById(R.id.notification_title);
            bodyTextView = itemView.findViewById(R.id.notification_body);
            timeTextView = itemView.findViewById(R.id.notification_time);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            aiBadge = itemView.findViewById(R.id.ai_badge);
        }
    }
}


