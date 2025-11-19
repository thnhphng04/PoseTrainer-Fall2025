package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import fpt.fall2025.posetrainer.Fragment.HomeFragment;
import fpt.fall2025.posetrainer.Fragment.MyWorkoutFragment;
import fpt.fall2025.posetrainer.Fragment.DailyFragment;
import fpt.fall2025.posetrainer.Fragment.ProfileFragment;
import fpt.fall2025.posetrainer.Fragment.CommunityFragment;
import fpt.fall2025.posetrainer.Fragment.NotificationFragment;
import fpt.fall2025.posetrainer.Helper.AppStateHelper;
import fpt.fall2025.posetrainer.databinding.ActivityMainBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.Service.NotificationHelper;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ActivityMainBinding binding;
    
    // Cache fragments để tránh tạo lại mỗi lần chuyển tab
    private HomeFragment homeFragment;
    private CommunityFragment communityFragment;
    private MyWorkoutFragment myWorkoutFragment;
    private DailyFragment dailyFragment;
    private ProfileFragment profileFragment;
    private NotificationFragment notificationFragment; // Fragment thông báo
    
    private Fragment currentFragment;
    private ListenerRegistration socialNotificationListener; // Listener cho social notifications

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Khởi tạo và cache fragments
        initializeFragments();
        
        // Setup bottom navigation
        setupBottomNavigation();
        
        // Kiểm tra xem có intent để mở fragment cụ thể không (từ notification)
        handleIntent(getIntent());
        
        // Setup FCM token và notification settings cho user
        setupUserNotificationSettings();
        
        // Setup listener cho social notifications (sau khi user đã đăng nhập)
        // Delay một chút để đảm bảo user đã được setup xong
        binding.getRoot().postDelayed(() -> {
            setupSocialNotificationListener();
        }, 1000); // Delay 1 giây
    }
    
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    /**
     * Xử lý Intent để mở fragment phù hợp
     * Được gọi từ onCreate và onNewIntent (khi app đang chạy và nhận intent mới)
     */
    private void handleIntent(android.content.Intent intent) {
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentToOpen = intent.getStringExtra("openFragment");
            Log.d(TAG, "Intent yêu cầu mở fragment: " + fragmentToOpen);
            
            if ("notifications".equals(fragmentToOpen)) {
                // Mở NotificationFragment
                showFragment(notificationFragment);
            } else if ("profile".equals(fragmentToOpen)) {
                showFragment(profileFragment);
            } else if ("exercise".equals(fragmentToOpen)) {
                // TODO: Handle exercise fragment
                showFragment(homeFragment);
            } else {
                // Mặc định mở Home
                showFragment(homeFragment);
            }
        } else {
            // Không có intent đặc biệt, load fragment mặc định (Home)
            if (currentFragment == null) {
                showFragment(homeFragment);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Track app state: app đang ở foreground
        AppStateHelper.setAppInForeground(true);
        Log.d(TAG, "MainActivity onResume: Ứng dụng ở foreground");
        
        // Refresh MyWorkoutFragment nếu đang visible (ví dụ khi quay lại từ PlanPreviewActivity)
        if (currentFragment instanceof MyWorkoutFragment && currentFragment.isAdded() && currentFragment.isResumed()) {
            binding.getRoot().postDelayed(() -> {
                if (currentFragment instanceof MyWorkoutFragment && currentFragment.isAdded()) {
                    ((MyWorkoutFragment) currentFragment).refreshWorkouts();
                }
            }, 500); // Delay 500ms để đảm bảo activity đã resume xong
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Track app state: app có thể đang ở background
        // Note: Không set false ngay vì có thể đang chuyển sang Activity khác trong cùng app
        Log.d(TAG, "MainActivity onPause");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Track app state: app đang ở background
        AppStateHelper.setAppInForeground(false);
        Log.d(TAG, "MainActivity onStop: Ứng dụng ở background");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy listener khi activity bị destroy
        if (socialNotificationListener != null) {
            socialNotificationListener.remove();
            socialNotificationListener = null;
            Log.d(TAG, "Đã hủy social notification listener");
        }
    }

    /**
     * Khởi tạo tất cả fragments một lần và cache chúng
     */
    private void initializeFragments() {
        // Tìm fragments đã tồn tại (trong trường hợp activity bị recreate)
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
        communityFragment = (CommunityFragment) getSupportFragmentManager().findFragmentByTag("community");
        myWorkoutFragment = (MyWorkoutFragment) getSupportFragmentManager().findFragmentByTag("myWorkout");
        dailyFragment = (DailyFragment) getSupportFragmentManager().findFragmentByTag("daily");
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        notificationFragment = (NotificationFragment) getSupportFragmentManager().findFragmentByTag("notifications");
        
        // Tạo fragments mới nếu chưa tồn tại
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (communityFragment == null) {
            communityFragment = new CommunityFragment();
        }
        if (myWorkoutFragment == null) {
            myWorkoutFragment = new MyWorkoutFragment();
        }
        if (dailyFragment == null) {
            dailyFragment = new DailyFragment();
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }
        if (notificationFragment == null) {
            notificationFragment = new NotificationFragment();
        }
    }

    private void setupBottomNavigation() {
        binding.homeBtn.setOnClickListener(v -> showFragment(homeFragment));
        binding.personalBtn.setOnClickListener(v -> showFragment(myWorkoutFragment));
        binding.dailyBtn.setOnClickListener(v -> showFragment(dailyFragment));
        binding.profileBtn.setOnClickListener(v -> showFragment(profileFragment));
        binding.Community.setOnClickListener(v -> showFragment(communityFragment));
    }

    /**
     * Hiển thị fragment sử dụng show/hide thay vì replace để tăng hiệu năng
     */
    private void showFragment(Fragment fragment) {
        if (fragment == null || fragment == currentFragment) {
            return;
        }
        
        // Kiểm tra activity có đang trong trạng thái hợp lệ không
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            
            // Ẩn fragment hiện tại nếu có
            if (currentFragment != null && currentFragment.isAdded()) {
                transaction.hide(currentFragment);
            }
            
            // Hiển thị fragment được chọn
            if (fragment.isAdded()) {
                // Fragment đã được add, chỉ cần show
                transaction.show(fragment);
            } else {
                // Fragment chưa được add, add nó vào
                String tag = getFragmentTag(fragment);
                transaction.add(binding.fragmentContainer.getId(), fragment, tag);
            }
            
            // Sử dụng commit() thay vì commitNow() để tránh IllegalStateException
            transaction.commit();
            
            currentFragment = fragment;
            
            // Track DailyFragment visibility để suppress notification khi user đang xem schedule
            if (fragment instanceof DailyFragment) {
                AppStateHelper.setDailyFragmentVisible(true);
                Log.d(TAG, "DailyFragment hiện đang HIỂN THỊ - thông báo sẽ bị ẩn");
            } else {
                AppStateHelper.setDailyFragmentVisible(false);
            }
            
            // Refresh MyWorkoutFragment khi được show để đảm bảo data mới nhất
            if (fragment instanceof MyWorkoutFragment) {
                // Delay một chút để đảm bảo fragment đã được show xong
                binding.getRoot().postDelayed(() -> {
                    if (fragment.isAdded() && fragment.isResumed()) {
                        ((MyWorkoutFragment) fragment).refreshWorkouts();
                    }
                }, 300); // Delay 300ms
            }
            
            Log.d(TAG, "Đã chuyển sang fragment: " + fragment.getClass().getSimpleName());
        } catch (IllegalStateException e) {
            // Xử lý trường hợp activity đang save state
            Log.w(TAG, "Không thể commit fragment transaction: " + e.getMessage());
            // Fallback: sử dụng commitAllowingStateLoss() như giải pháp cuối cùng
            try {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (currentFragment != null && currentFragment.isAdded()) {
                    transaction.hide(currentFragment);
                }
                if (fragment.isAdded()) {
                    transaction.show(fragment);
                } else {
                    String tag = getFragmentTag(fragment);
                    transaction.add(binding.fragmentContainer.getId(), fragment, tag);
                }
                transaction.commitAllowingStateLoss();
                currentFragment = fragment;
            } catch (Exception ex) {
                Log.e(TAG, "Không thể commit fragment transaction: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Lấy tag cho fragment caching
     */
    private String getFragmentTag(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return "home";
        } else if (fragment instanceof CommunityFragment) {
            return "community";
        } else if (fragment instanceof MyWorkoutFragment) {
            return "myWorkout";
        } else if (fragment instanceof DailyFragment) {
            return "daily";
        } else if (fragment instanceof ProfileFragment) {
            return "profile";
        } else if (fragment instanceof NotificationFragment) {
            return "notifications";
        }
        return fragment.getClass().getSimpleName();
    }
    
    /**
     * Method public để các fragment khác có thể gọi mở NotificationFragment
     * Ví dụ: từ ProfileFragment có thể thêm button để xem thông báo
     */
    public void openNotificationFragment() {
        showFragment(notificationFragment);
    }
    
    /**
     * Method public để các fragment khác có thể gọi mở ProfileFragment
     * Ví dụ: từ NotificationFragment khi click vào thông báo "view_progress"
     */
    public void openProfileFragment() {
        showFragment(profileFragment);
    }
    
    /**
     * Method public để các fragment khác có thể gọi navigate về HomeFragment
     * Ví dụ: từ DailyFragment có thể click back arrow để về Home
     */
    public void navigateToHomeFragment() {
        showFragment(homeFragment);
    }
    
    /**
     * Method public để các fragment khác có thể gọi navigate đến MyWorkoutFragment
     * Ví dụ: từ DailyFragment có thể click tv_history để xem lịch sử
     */
    public void navigateToMyWorkoutFragment() {
        showFragment(myWorkoutFragment);
    }
    
    /**
     * Setup FCM token và notification settings cho user
     * Được gọi khi app khởi động để đảm bảo user có token và settings đúng
     */
    private void setupUserNotificationSettings() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User chưa đăng nhập, không thể setup notification settings");
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Đang setup notification settings cho user: " + uid);
        
        // Lấy FCM token và lưu lên Firestore
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    Log.d(TAG, "✓ FCM Token: " + token);
                    
                    // Lưu token lên Firestore
                    FirebaseService.getInstance().updateFcmToken(
                        uid, 
                        token, 
                        success -> {
                            if (success) {
                                Log.d(TAG, "✓ Đã lưu FCM token lên Firestore");
                            } else {
                                Log.e(TAG, "✗ Lỗi lưu FCM token");
                            }
                        }
                    );
                    
                    // Sau khi có token, setup notification settings
                    setupNotificationSettings(uid);
                } else {
                    Log.e(TAG, "✗ Lỗi lấy FCM token", task.getException());
                    // Vẫn thử setup settings dù không có token
                    setupNotificationSettings(uid);
                }
            });
    }
    
    /**
     * Setup notification settings cho user trong Firestore
     * Đảm bảo user có đầy đủ settings để nhận AI notifications
     * Tự động cập nhật maxNotificationsPerDay = 30 cho user cũ
     */
    private void setupNotificationSettings(String uid) {
        // Kiểm tra xem user đã có notification settings chưa
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Object> notificationSettings = new HashMap<>();
                
                // Kiểm tra xem có notification settings cũ không
                Map<String, Object> oldNotification = (Map<String, Object>) documentSnapshot.get("notification");
                
                if (oldNotification != null) {
                    // User cũ → Chỉ cập nhật maxNotificationsPerDay nếu chưa có hoặc khác 30
                    Object maxNotifObj = oldNotification.get("maxNotificationsPerDay");
                    int currentMax = 30;
                    if (maxNotifObj instanceof Long) {
                        currentMax = ((Long) maxNotifObj).intValue();
                    } else if (maxNotifObj instanceof Integer) {
                        currentMax = (Integer) maxNotifObj;
                    }
                    
                    // Nếu user cũ có maxNotificationsPerDay < 30 hoặc không có → Cập nhật lên 30
                    if (currentMax < 30 || maxNotifObj == null) {
                        notificationSettings.put("maxNotificationsPerDay", 30);
                        Log.d(TAG, "Đang cập nhật maxNotificationsPerDay cho user cũ: " + currentMax + " → 30");
                    }
                    
                    // Giữ các settings cũ khác
                    if (oldNotification.get("allowNotification") != null) {
                        notificationSettings.put("allowNotification", oldNotification.get("allowNotification"));
                    } else {
                        notificationSettings.put("allowNotification", true);
                    }
                    
                    if (oldNotification.get("enableAiNotifications") != null) {
                        notificationSettings.put("enableAiNotifications", oldNotification.get("enableAiNotifications"));
                    } else {
                        notificationSettings.put("enableAiNotifications", true);
                    }
                    
                    if (oldNotification.get("language") != null) {
                        notificationSettings.put("language", oldNotification.get("language"));
                    } else {
                        notificationSettings.put("language", "vi");
                    }
                    
                    if (oldNotification.get("allowMotivationalMessages") != null) {
                        notificationSettings.put("allowMotivationalMessages", oldNotification.get("allowMotivationalMessages"));
                    } else {
                        notificationSettings.put("allowMotivationalMessages", true);
                    }
                } else {
                    // User mới → Tạo settings mới với giá trị mặc định
                    notificationSettings.put("allowNotification", true);
                    notificationSettings.put("enableAiNotifications", true);
                    notificationSettings.put("language", "vi");
                    notificationSettings.put("allowMotivationalMessages", true);
                    notificationSettings.put("maxNotificationsPerDay", 30);
                }
                
                // Cập nhật settings lên Firestore
                if (!notificationSettings.isEmpty()) {
                    FirebaseService.getInstance().updateAiNotificationSettings(
                        uid, 
                        notificationSettings, 
                        success -> {
                            if (success) {
                                Log.d(TAG, "✓ Đã cập nhật notification settings lên Firestore");
                                Log.d(TAG, "User đã sẵn sàng nhận AI notifications!");
                            } else {
                                Log.e(TAG, "✗ Lỗi cập nhật notification settings");
                            }
                        }
                    );
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi kiểm tra notification settings: " + e.getMessage(), e);
                // Nếu lỗi, vẫn tạo settings mới với giá trị mặc định
                Map<String, Object> defaultSettings = new HashMap<>();
                defaultSettings.put("allowNotification", true);
                defaultSettings.put("enableAiNotifications", true);
                defaultSettings.put("language", "vi");
                defaultSettings.put("allowMotivationalMessages", true);
                defaultSettings.put("maxNotificationsPerDay", 30);
                
                FirebaseService.getInstance().updateAiNotificationSettings(
                    uid, 
                    defaultSettings, 
                    success -> {
                        if (success) {
                            Log.d(TAG, "✓ Đã tạo notification settings mới");
                        } else {
                            Log.e(TAG, "✗ Lỗi tạo notification settings");
                        }
                    }
                );
            });
    }
    
    /**
     * Setup listener để lắng nghe social notifications từ Firestore
     * Khi có notification mới (social_like hoặc social_comment) cho user hiện tại,
     * sẽ hiển thị notification trên thiết bị giống như thông báo lịch tập
     */
    private void setupSocialNotificationListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User chưa đăng nhập, không thể setup social notification listener");
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Đang setup listener cho social notifications của user: " + uid);
        
        // Lắng nghe notification mới cho user hiện tại
        // Query tất cả notification của user, filter type trong code
        Query query = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("uid", uid)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(10); // Lắng nghe 10 notification mới nhất
        
        // Lưu thời gian setup listener để chỉ hiển thị notification mới hơn thời gian này
        final long listenerSetupTime = System.currentTimeMillis() - 10000; // Trừ 10 giây để đảm bảo bắt được notification vừa tạo
        Log.d(TAG, "Listener setup time: " + listenerSetupTime + " (" + new java.util.Date(listenerSetupTime) + ")");
        
        socialNotificationListener = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "✗ Lỗi lắng nghe social notifications: " + error.getMessage(), error);
                // Nếu lỗi do thiếu index, log thông báo hướng dẫn
                if (error.getMessage() != null && error.getMessage().contains("index")) {
                    Log.e(TAG, "⚠️ Cần tạo Firestore index cho query: notifications(uid, sentAt)");
                    Log.e(TAG, "⚠️ Vui lòng click vào link trong log để tạo index tự động");
                }
                return;
            }
            
            if (snapshot == null) {
                Log.w(TAG, "Snapshot is null");
                return;
            }
            
            Log.d(TAG, "=== SNAPSHOT LISTENER TRIGGERED ===");
            Log.d(TAG, "Số lượng documents: " + snapshot.size());
            Log.d(TAG, "Số lượng document changes: " + snapshot.getDocumentChanges().size());
            Log.d(TAG, "Listener setup time: " + listenerSetupTime + " (" + new java.util.Date(listenerSetupTime) + ")");
            
            // Kiểm tra xem có notification mới được thêm vào không
            for (DocumentChange change : snapshot.getDocumentChanges()) {
                Log.d(TAG, "DocumentChange type: " + change.getType() + ", Document ID: " + change.getDocument().getId());
                
                // Chỉ xử lý notification mới được thêm vào (ADDED)
                if (change.getType() == DocumentChange.Type.ADDED) {
                    // Có notification mới được thêm vào
                    Map<String, Object> data = change.getDocument().getData();
                    if (data != null) {
                        String type = (String) data.get("type");
                        String title = (String) data.get("title");
                        String body = (String) data.get("body");
                        String actionType = (String) data.get("actionType");
                        String actionData = (String) data.get("actionData");
                        Long sentAt = data.get("sentAt") != null ? 
                                ((Number) data.get("sentAt")).longValue() : 0L;
                        
                        Log.d(TAG, "=== NOTIFICATION MỚI ĐƯỢC THÊM ===");
                        Log.d(TAG, "  - Type: " + type);
                        Log.d(TAG, "  - Title: " + title);
                        Log.d(TAG, "  - Body: " + body);
                        Log.d(TAG, "  - SentAt: " + sentAt + " (" + new java.util.Date(sentAt) + ")");
                        Log.d(TAG, "  - ListenerSetupTime: " + listenerSetupTime + " (" + new java.util.Date(listenerSetupTime) + ")");
                        Log.d(TAG, "  - ActionType: " + actionType + ", ActionData: " + actionData);
                        
                        // Chỉ xử lý notification có type là social_like hoặc social_comment
                        if ("social_like".equals(type) || "social_comment".equals(type)) {
                            // Chỉ hiển thị notification mới hơn thời gian setup listener
                            // (trừ 5 giây để đảm bảo bắt được notification vừa tạo)
                            if (sentAt >= listenerSetupTime) {
                                // Luôn hiển thị notification cho social
                                // Vì user cần biết ngay khi có người tương tác với bài viết
                                if (actionData != null && "open_post".equals(actionType)) {
                                    // Hiển thị notification giống như thông báo lịch tập
                                    Log.d(TAG, "✓✓✓ HIỂN THỊ SOCIAL NOTIFICATION: " + title);
                                    NotificationHelper.showSocialNotification(
                                            this,
                                            title != null ? title : "Thông báo xã hội",
                                            body != null ? body : "",
                                            actionData,
                                            type
                                    );
                                } else {
                                    Log.w(TAG, "⚠️ ActionData hoặc ActionType không hợp lệ");
                                    Log.w(TAG, "  - ActionData: " + actionData);
                                    Log.w(TAG, "  - ActionType: " + actionType);
                                }
                            } else {
                                Log.d(TAG, "⚠️ Notification cũ, bỏ qua (sentAt: " + sentAt + " < listenerSetupTime: " + listenerSetupTime + ")");
                            }
                        } else {
                            Log.d(TAG, "⚠️ Notification không phải social type: " + type);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Notification data is null");
                    }
                } else {
                    Log.d(TAG, "⚠️ DocumentChange type không phải ADDED: " + change.getType());
                }
            }
            
            // Fallback: Nếu không có document changes nhưng có documents mới trong snapshot
            // (Có thể xảy ra khi listener được setup sau khi notification đã được tạo)
            if (snapshot.getDocumentChanges().isEmpty() && !snapshot.isEmpty()) {
                Log.d(TAG, "⚠️ Không có document changes, nhưng có documents trong snapshot");
                Log.d(TAG, "⚠️ Có thể listener được setup sau khi notification đã được tạo");
                Log.d(TAG, "⚠️ Kiểm tra documents trong snapshot...");
                
                // Kiểm tra document đầu tiên (mới nhất) xem có phải notification mới không
                if (snapshot.getDocuments().size() > 0) {
                    Map<String, Object> firstData = snapshot.getDocuments().get(0).getData();
                    if (firstData != null) {
                        String type = (String) firstData.get("type");
                        Long sentAt = firstData.get("sentAt") != null ? 
                                ((Number) firstData.get("sentAt")).longValue() : 0L;
                        
                        Log.d(TAG, "Document đầu tiên - Type: " + type + ", SentAt: " + sentAt);
                        
                        // Nếu là social notification và mới hơn listener setup time
                        if (("social_like".equals(type) || "social_comment".equals(type)) && 
                            sentAt >= listenerSetupTime) {
                            String title = (String) firstData.get("title");
                            String body = (String) firstData.get("body");
                            String actionType = (String) firstData.get("actionType");
                            String actionData = (String) firstData.get("actionData");
                            
                            if (actionData != null && "open_post".equals(actionType)) {
                                Log.d(TAG, "✓✓✓ FALLBACK: HIỂN THỊ SOCIAL NOTIFICATION: " + title);
                                NotificationHelper.showSocialNotification(
                                        this,
                                        title != null ? title : "Thông báo xã hội",
                                        body != null ? body : "",
                                        actionData,
                                        type
                                );
                            }
                        }
                    }
                }
            }
        });
        
        Log.d(TAG, "✓ Đã setup listener cho social notifications");
    }
}