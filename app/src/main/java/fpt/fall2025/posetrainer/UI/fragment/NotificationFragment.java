package fpt.fall2025.posetrainer.UI.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.UI.activity.MainActivity;
import fpt.fall2025.posetrainer.UI.activity.WorkoutActivity;
import fpt.fall2025.posetrainer.UI.activity.PostDetailActivity;
import fpt.fall2025.posetrainer.UI.adapter.community.NotificationAdapter;
import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.NotificationDAO;

/**
 * Fragment hiển thị danh sách thông báo
 * Hỗ trợ lọc theo loại (Tất cả, AI, Tập luyện, Xã hội)
 * Cho phép đánh dấu đã đọc, xóa thông báo
 */
public class NotificationFragment extends Fragment {
    // UI Components
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private LinearLayout emptyView;
    private TextView unreadCountBadge;
    private ImageButton markAllReadButton;
    private TabLayout notificationTabs;
    
    // Data
    private ArrayList<Notification> allNotifications; // Tất cả thông báo
    private ArrayList<Notification> filteredNotifications; // Thông báo sau khi lọc
    private NotificationAdapter adapter;
    
    // Firebase
    private AuthService authService;
    private NotificationDAO notificationDAO;
    
    // Filter type
    private String currentFilter = "all"; // "all", "ai", "workout", "social"

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        
        // Khởi tạo Firebase
        authService = new AuthService();
        notificationDAO = new NotificationDAO();
        
        // Khởi tạo views
        initViews(view);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup listeners
        setupListeners();
        
        // Load notifications
        loadNotifications();
        
        return view;
    }

    /**
     * Khởi tạo các views
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        loadingProgress = view.findViewById(R.id.loading_progress);
        emptyView = view.findViewById(R.id.empty_view);
        unreadCountBadge = view.findViewById(R.id.unread_count_badge);
        markAllReadButton = view.findViewById(R.id.mark_all_read_button);
        notificationTabs = view.findViewById(R.id.notification_tabs);
        
        allNotifications = new ArrayList<>();
        filteredNotifications = new ArrayList<>();
    }

    /**
     * Setup RecyclerView và Adapter
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new NotificationAdapter(getContext(), filteredNotifications, 
            new NotificationAdapter.OnNotificationClickListener() {
                @Override
                public void onNotificationClick(Notification notification) {
                    handleNotificationClick(notification);
                }

                @Override
                public void onNotificationLongClick(Notification notification) {
                    showNotificationOptionsDialog(notification);
                }
            });
        
        recyclerView.setAdapter(adapter);
    }

    /**
     * Setup các listeners cho buttons và tabs
     */
    private void setupListeners() {
        // Button đánh dấu tất cả đã đọc
        markAllReadButton.setOnClickListener(v -> {
            markAllNotificationsAsRead();
        });
        
        // Tab để lọc thông báo
        notificationTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: // Tất cả
                        currentFilter = "all";
                        break;
                    case 1: // AI
                        currentFilter = "ai";
                        break;
                    case 2: // Tập luyện
                        currentFilter = "workout";
                        break;
                    case 3: // Xã hội
                        currentFilter = "social";
                        break;
                }
                filterNotifications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Load danh sách thông báo từ Firestore
     */
    private void loadNotifications() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showEmptyView();
            return;
        }
        
        String uid = currentUser.getUid();
        
        // Hiển thị loading
        showLoading();
        
        // Load từ Firestore
        notificationDAO.loadUserNotifications(uid, notifications -> {
            if (notifications == null) {
                notifications = new ArrayList<>();
            }
            
            allNotifications = notifications;
            
            filterNotifications();
            updateUnreadCount();
            hideLoading();
            
            // Hiển thị empty view nếu không có thông báo
            if (allNotifications.isEmpty()) {
                showEmptyView();
            } else {
                hideEmptyView();
            }
        });
    }

    /**
     * Lọc thông báo theo filter hiện tại
     */
    private void filterNotifications() {
        filteredNotifications.clear();
        
        for (Notification notification : allNotifications) {
            boolean shouldInclude = false;
            
            switch (currentFilter) {
                case "all":
                    shouldInclude = true;
                    break;
                case "ai":
                    shouldInclude = notification.isFromAI();
                    break;
                case "workout":
                    shouldInclude = notification.getType() != null && 
                                  (notification.getType().contains("workout") || 
                                   notification.getType().contains("reminder") ||
                                   notification.getType().equals("ai_plan_update")); // Bao gồm thông báo lịch sắp tới
                    break;
                case "social":
                    shouldInclude = notification.getType() != null && 
                                  (notification.getType().equals("social") || 
                                   notification.getType().equals("social_like") ||
                                   notification.getType().equals("social_comment") ||
                                   notification.getType().equals("social_follow"));
                    break;
            }
            
            if (shouldInclude) {
                filteredNotifications.add(notification);
            }
        }
        
        // Cập nhật adapter
        adapter.updateNotifications(filteredNotifications);
        
        // Hiển thị empty view nếu không có thông báo sau khi lọc
        if (filteredNotifications.isEmpty()) {
            showEmptyView();
        } else {
            hideEmptyView();
        }
    }

    /**
     * Xử lý khi click vào thông báo - Hiển thị dialog chi tiết
     */
    private void handleNotificationClick(Notification notification) {
        // Đánh dấu là đã đọc
        if (!notification.isRead()) {
            markNotificationAsRead(notification);
        }
        
        // Hiển thị dialog chi tiết
        showNotificationDetailDialog(notification);
    }
    
    /**
     * Hiển thị dialog chi tiết thông báo
     */
    private void showNotificationDetailDialog(Notification notification) {
        // Tạo dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        
        // Inflate layout dialog
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_notification_detail, null);
        
        builder.setView(dialogView);
        
        // Tạo dialog
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Ánh xạ các views
        ImageView iconImageView = dialogView.findViewById(R.id.dialog_notification_icon);
        TextView senderTextView = dialogView.findViewById(R.id.dialog_notification_sender);
        TextView titleTextView = dialogView.findViewById(R.id.dialog_notification_title);
        TextView bodyTextView = dialogView.findViewById(R.id.dialog_notification_body);
        TextView timeTextView = dialogView.findViewById(R.id.dialog_notification_time);
        TextView typeTextView = dialogView.findViewById(R.id.dialog_notification_type);
        TextView aiBadge = dialogView.findViewById(R.id.dialog_ai_badge);
        com.google.android.material.button.MaterialButton viewDetailButton = 
            dialogView.findViewById(R.id.dialog_view_detail_button);
        com.google.android.material.button.MaterialButton closeButton = 
            dialogView.findViewById(R.id.dialog_close_button);
        
        // Set sender name (nhân vật tượng trưng)
        String senderName = notification.getDisplaySenderName();
        if (notification.isFromAI() && senderName != null && !senderName.isEmpty()) {
            senderTextView.setText(senderName + " 🦥");
            senderTextView.setVisibility(View.VISIBLE);
        } else {
            senderTextView.setVisibility(View.GONE);
        }
        
        // Set dữ liệu vào dialog
        titleTextView.setText(notification.getTitle());
        bodyTextView.setText(notification.getBody());
        
        // Format thời gian
        String timeAgo = formatRelativeTimeVietnamese(notification.getSentAt());
        timeTextView.setText(timeAgo);
        
        // Set icon dựa theo loại thông báo
        int iconRes = getIconResourceForType(notification.getType());
        iconImageView.setImageResource(iconRes);
        
        // Hiển thị badge AI nếu là thông báo từ AI
        if (notification.isFromAI()) {
            aiBadge.setVisibility(View.VISIBLE);
        } else {
            aiBadge.setVisibility(View.GONE);
        }
        
        // Ẩn type text view (không cần thiết)
        typeTextView.setVisibility(View.GONE);
        
        // Xử lý nút "Xem chi tiết"
        String actionType = notification.getActionType();
        String actionData = notification.getActionData();
        
        // Chỉ hiển thị nút "Xem chi tiết" nếu có actionType hợp lệ
        if (actionType != null && !actionType.isEmpty() && !"none".equals(actionType)) {
            viewDetailButton.setVisibility(View.VISIBLE);
            viewDetailButton.setOnClickListener(v -> {
                dialog.dismiss();
                navigateToAction(notification, actionType, actionData);
            });
        } else {
            viewDetailButton.setVisibility(View.GONE);
        }
        
        // Xử lý nút đóng
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        // Gửi feedback nếu là thông báo AI
        if (notification.isFromAI()) {
            notificationDAO.sendNotificationFeedback(notification.getId(), "accepted", null);
        }
        
        // Hiển thị dialog
        dialog.show();
    }
    
    /**
     * Điều hướng đến màn hình tương ứng dựa trên actionType
     */
    private void navigateToAction(Notification notification, String actionType, String actionData) {
        if ("open_workout".equals(actionType) && actionData != null) {
            Intent intent = new Intent(getContext(), WorkoutActivity.class);
            intent.putExtra("workoutId", actionData);
            intent.putExtra("fromNotification", true);
            startActivity(intent);
        } else if ("open_exercise".equals(actionType) && actionData != null) {
            Toast.makeText(getContext(), "Mở chi tiết bài tập: " + actionData, Toast.LENGTH_SHORT).show();
        } else if ("open_post".equals(actionType) && actionData != null) {
            Intent intent = new Intent(getContext(), PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, actionData);
            startActivity(intent);
        } else if ("view_progress".equals(actionType)) {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openProfileFragment();
            } else {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra("openFragment", "profile");
                startActivity(intent);
            }
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

    /**
     * Lấy icon phù hợp theo loại thông báo
     */
    private int getIconResourceForType(String type) {
        if (type == null) return R.drawable.ic_notifications;
        
        switch (type) {
            case "ai_reminder_smart":
            case "workout_reminder_sent":
                return R.drawable.ic_time;
            case "ai_feedback_posture":
            case "ai_feedback_consistency":
                return R.drawable.ic_feedback;
            case "ai_achievement":
            case "achievement":
                return R.drawable.ic_trophy;
            case "ai_plan_update":
                return R.drawable.ic_plan;
            case "ai_streak_reminder":
                return R.drawable.ic_trophy; // Icon cúp cho streak reminder
            case "social":
            case "social_like":
            case "social_comment":
            case "social_follow":
                return R.drawable.ic_social;
            default:
                return R.drawable.ic_notifications;
        }
    }

    /**
     * Hiển thị dialog tùy chọn cho thông báo (xóa, đánh dấu chưa đọc...)
     */
    private void showNotificationOptionsDialog(Notification notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(notification.getTitle());
        
        String[] options = {"Xóa thông báo", "Hủy"};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Xóa thông báo
                deleteNotification(notification);
            }
        });
        
        builder.show();
    }

    /**
     * Đánh dấu thông báo là đã đọc
     */
    private void markNotificationAsRead(Notification notification) {
        notificationDAO.markAsRead(notification.getId(), task -> {
            if (task.isSuccessful()) {
                notification.setRead(true);
                adapter.notifyDataSetChanged();
                updateUnreadCount();
            }
        });
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    private void markAllNotificationsAsRead() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        
        notificationDAO.markAllNotificationsAsRead(uid, success -> {
            if (success) {
                Toast.makeText(getContext(), "Đã đánh dấu tất cả đã đọc", Toast.LENGTH_SHORT).show();
                // Cập nhật UI
                for (Notification notification : allNotifications) {
                    notification.setRead(true);
                }
                adapter.notifyDataSetChanged();
                updateUnreadCount();
            } else {
                Toast.makeText(getContext(), "Lỗi đánh dấu thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Xóa thông báo
     */
    private void deleteNotification(Notification notification) {
        notificationDAO.delete(notification.getId(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                // Xóa khỏi danh sách
                allNotifications.remove(notification);
                filteredNotifications.remove(notification);
                adapter.notifyDataSetChanged();
                updateUnreadCount();
                
                // Hiển thị empty view nếu không còn thông báo
                if (allNotifications.isEmpty()) {
                    showEmptyView();
                }
            } else {
                Toast.makeText(getContext(), "Lỗi xóa thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cập nhật số lượng thông báo chưa đọc
     */
    private void updateUnreadCount() {
        int unreadCount = 0;
        for (Notification notification : allNotifications) {
            if (!notification.isRead()) {
                unreadCount++;
            }
        }
        
        if (unreadCount > 0) {
            unreadCountBadge.setText(String.valueOf(unreadCount));
            unreadCountBadge.setVisibility(View.VISIBLE);
        } else {
            unreadCountBadge.setVisibility(View.GONE);
        }
    }

    /**
     * Hiển thị loading
     */
    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    /**
     * Ẩn loading
     */
    private void hideLoading() {
        loadingProgress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Hiển thị empty view (khi không có thông báo)
     */
    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Ẩn empty view
     */
    private void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload notifications khi fragment hiển thị lại
        loadNotifications();
    }
}

