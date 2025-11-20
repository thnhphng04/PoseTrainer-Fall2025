package fpt.fall2025.posetrainer.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.stream.Collectors;

import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.Adapter.NotificationAdapter;
import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Fragment hiển thị danh sách thông báo
 * Hỗ trợ lọc theo loại (Tất cả, AI, Tập luyện, Xã hội)
 * Cho phép đánh dấu đã đọc, xóa thông báo
 */
public class NotificationFragment extends Fragment {
    
    private static final String TAG = "NotificationFragment";
    
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
    private FirebaseAuth mAuth;
    private FirebaseService firebaseService;
    
    // Filter type
    private String currentFilter = "all"; // "all", "ai", "workout", "social"

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        
        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        firebaseService = FirebaseService.getInstance();
        
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "⚠ User chưa đăng nhập, không thể load notifications");
            showEmptyView();
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "=== NOTIFICATION FRAGMENT: LOADING NOTIFICATIONS ===");
        Log.d(TAG, "User UID: " + uid);
        
        // Hiển thị loading
        showLoading();
        
        // Load từ Firestore
        firebaseService.loadUserNotifications(uid, notifications -> {
            Log.d(TAG, "=== NOTIFICATION FRAGMENT: CALLBACK RECEIVED ===");
            Log.d(TAG, "Số lượng notifications nhận được: " + notifications.size());
            
            if (notifications == null) {
                Log.w(TAG, "⚠ Notifications list is null!");
                notifications = new ArrayList<>();
            }
            
            allNotifications = notifications;
            Log.d(TAG, "allNotifications.size() = " + allNotifications.size());
            
            filterNotifications();
            updateUnreadCount();
            hideLoading();
            
            // Hiển thị empty view nếu không có thông báo
            if (allNotifications.isEmpty()) {
                Log.d(TAG, "⚠ Không có thông báo nào, hiển thị empty view");
                showEmptyView();
            } else {
                Log.d(TAG, "✓ Có " + allNotifications.size() + " thông báo, hiển thị danh sách");
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
        
        Log.d(TAG, "Đã lọc: " + filteredNotifications.size() + " thông báo (filter: " + currentFilter + ")");
    }

    /**
     * Xử lý khi click vào thông báo
     */
    private void handleNotificationClick(Notification notification) {
        Log.d(TAG, "Click vào thông báo: " + notification.getTitle());
        
        // Đánh dấu là đã đọc
        if (!notification.isRead()) {
            markNotificationAsRead(notification);
        }
        
        // Xử lý action dựa trên actionType
        String actionType = notification.getActionType();
        String actionData = notification.getActionData();
        
        if ("open_workout".equals(actionType) && actionData != null) {
            // Mở WorkoutActivity với workoutId
            Intent intent = new Intent(getContext(), WorkoutActivity.class);
            intent.putExtra("workoutId", actionData);
            intent.putExtra("fromNotification", true);
            startActivity(intent);
        } else if ("open_exercise".equals(actionType) && actionData != null) {
            // Mở ExerciseDetailActivity (nếu có)
            // TODO: Implement khi có ExerciseDetailActivity
            Toast.makeText(getContext(), "Mở chi tiết bài tập: " + actionData, Toast.LENGTH_SHORT).show();
        } else if ("open_post".equals(actionType) && actionData != null) {
            // Mở PostDetailActivity với postId (cho thông báo xã hội)
            Intent intent = new Intent(getContext(), PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, actionData);
            startActivity(intent);
        } else if ("view_progress".equals(actionType)) {
            // Chuyển sang ProfileFragment
            // Lấy MainActivity và mở ProfileFragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openProfileFragment();
            } else {
                // Nếu không phải MainActivity, mở MainActivity với intent
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra("openFragment", "profile");
                startActivity(intent);
            }
        } else {
            // Không có action cụ thể, chỉ hiển thị thông báo đã đọc
            Toast.makeText(getContext(), "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
        }
        
        // Gửi feedback nếu là thông báo AI
        if (notification.isFromAI()) {
            firebaseService.sendNotificationFeedback(notification.getId(), "accepted", null);
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
        firebaseService.markNotificationAsRead(notification.getId(), success -> {
            if (success) {
                Log.d(TAG, "Đã đánh dấu thông báo đã đọc");
                notification.setRead(true);
                adapter.notifyDataSetChanged();
                updateUnreadCount();
            } else {
                Log.e(TAG, "Lỗi đánh dấu thông báo");
            }
        });
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    private void markAllNotificationsAsRead() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String uid = currentUser.getUid();
        
        firebaseService.markAllNotificationsAsRead(uid, success -> {
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
        firebaseService.deleteNotification(notification.getId(), success -> {
            if (success) {
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
        
        Log.d(TAG, "Số thông báo chưa đọc: " + unreadCount);
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

