package fpt.fall2025.posetrainer.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.UserDAO;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final long SAVE_DEBOUNCE_DELAY = 500; // 500ms debounce

    private ImageView imgProfile;
    private EditText etName;
    private TextView tvEmail;
    private Button btnEditProfile;

    // Notification settings views
    private Switch switchAllowNotification;
    private Switch switchEnableAiNotifications;
    private SeekBar seekBarMaxNotifications;
    private TextView tvMaxNotificationsValue;
    private Switch switchMotivationalMessages;
    
    // Notification type switches
    private Switch switchDailyAiReminder;
    private Switch switchMissedWorkoutReminder;
    private Switch switchUpcomingSchedule;
    private Switch switchSessionFeedback;
    private Switch switchStreakReminder;

    private Uri selectedImageUri;
    private FirebaseUser user;
    private AuthService authService;
    private UserDAO userDAO;
    
    // Current notification settings
    private boolean allowNotification = true;
    private boolean enableAiNotifications = true;
    private int maxNotificationsPerDay = 30;
    private String language = "vi";
    private boolean allowMotivationalMessages = true;
    
    // Notification type settings
    private boolean enableDailyAiReminder = true;
    private boolean enableMissedWorkoutReminder = true;
    private boolean enableUpcomingSchedule = true;
    private boolean enableSessionFeedback = true;
    private boolean enableStreakReminder = true;
    
    // Debounce handler cho save settings
    private Handler saveSettingsHandler = new Handler(Looper.getMainLooper());
    private Runnable saveSettingsRunnable;
    private boolean isSavingSettings = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        imgProfile = findViewById(R.id.imgProfile);
        etName = findViewById(R.id.etName);
        tvEmail = findViewById(R.id.tvEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        // Notification settings views
        switchAllowNotification = findViewById(R.id.switchAllowNotification);
        switchEnableAiNotifications = findViewById(R.id.switchEnableAiNotifications);
        seekBarMaxNotifications = findViewById(R.id.seekBarMaxNotifications);
        tvMaxNotificationsValue = findViewById(R.id.tvMaxNotificationsValue);
        switchMotivationalMessages = findViewById(R.id.switchMotivationalMessages);
        
        // Notification type switches
        switchDailyAiReminder = findViewById(R.id.switchDailyAiReminder);
        switchMissedWorkoutReminder = findViewById(R.id.switchMissedWorkoutReminder);
        switchUpcomingSchedule = findViewById(R.id.switchUpcomingSchedule);
        switchSessionFeedback = findViewById(R.id.switchSessionFeedback);
        switchStreakReminder = findViewById(R.id.switchStreakReminder);

        authService = new AuthService();
        userDAO = new UserDAO();
        user = authService.getCurrentUser();

        if (user != null) {
            etName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .into(imgProfile);
            }
        }

        setupProfileViews();
        setupNotificationViews();
        loadNotificationSettings();
    }
    
    /**
     * Setup profile views và listeners
     */
    private void setupProfileViews() {
        imgProfile.setOnClickListener(v -> openImagePicker());
        btnEditProfile.setOnClickListener(v -> saveProfileChanges());
    }
    
    /**
     * Setup notification settings views và listeners
     */
    private void setupNotificationViews() {
        // Switch: Bật/tắt thông báo
        switchAllowNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowNotification = isChecked;
            // Nếu tắt notification, thì tắt luôn AI notifications
            if (!isChecked) {
                switchEnableAiNotifications.setChecked(false);
                enableAiNotifications = false;
            }
            switchEnableAiNotifications.setEnabled(isChecked); // Chỉ enable khi allowNotification = true
            updateNotificationTypeSwitchesEnabled();
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Bật/tắt AI notifications
        switchEnableAiNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableAiNotifications = isChecked;
            // Chỉ bật được khi allowNotification = true
            if (isChecked && !allowNotification) {
                switchAllowNotification.setChecked(true);
                allowNotification = true;
            }
            updateNotificationTypeSwitchesEnabled();
            debouncedSaveNotificationSettings();
        });
        
        // SeekBar: Chọn số lượng thông báo hàng ngày (1-30)
        seekBarMaxNotifications.setMax(29); // 0-29 → 1-30
        seekBarMaxNotifications.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    maxNotificationsPerDay = progress + 1; // progress 0-29 → 1-30
                    // Đảm bảo giá trị trong khoảng 1-30
                    if (maxNotificationsPerDay < 1) maxNotificationsPerDay = 1;
                    if (maxNotificationsPerDay > 30) maxNotificationsPerDay = 30;
                    tvMaxNotificationsValue.setText(String.valueOf(maxNotificationsPerDay));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Đảm bảo giá trị hợp lệ
                int progress = seekBar.getProgress();
                maxNotificationsPerDay = progress + 1;
                if (maxNotificationsPerDay < 1) maxNotificationsPerDay = 1;
                if (maxNotificationsPerDay > 30) maxNotificationsPerDay = 30;
                seekBar.setProgress(maxNotificationsPerDay - 1);
                tvMaxNotificationsValue.setText(String.valueOf(maxNotificationsPerDay));
                debouncedSaveNotificationSettings();
            }
        });
        
        // Switch: Cho phép tin nhắn động viên
        switchMotivationalMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowMotivationalMessages = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Thông báo AI hằng ngày
        switchDailyAiReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableDailyAiReminder = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Nhắc nhở workout chưa tập
        switchMissedWorkoutReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableMissedWorkoutReminder = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Thông báo lịch tập sắp tới
        switchUpcomingSchedule.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableUpcomingSchedule = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Feedback sau khi hoàn thành session
        switchSessionFeedback.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableSessionFeedback = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Switch: Nhắc nhở giữ streak
        switchStreakReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableStreakReminder = isChecked;
            debouncedSaveNotificationSettings();
        });
        
        // Disable các switch loại thông báo nếu AI notifications bị tắt
        updateNotificationTypeSwitchesEnabled();
    }
    
    /**
     * Enable/disable các switch loại thông báo dựa trên enableAiNotifications
     */
    private void updateNotificationTypeSwitchesEnabled() {
        boolean enabled = enableAiNotifications && allowNotification;
        switchDailyAiReminder.setEnabled(enabled);
        switchMissedWorkoutReminder.setEnabled(enabled);
        switchUpcomingSchedule.setEnabled(enabled);
        switchSessionFeedback.setEnabled(enabled);
        switchStreakReminder.setEnabled(enabled);
    }
    
    /**
     * Load notification settings từ Firestore
     */
    private void loadNotificationSettings() {
        if (user == null) {
            Log.w(TAG, "User chưa đăng nhập");
            return;
        }
        
        String uid = user.getUid();
        Log.d(TAG, "Đang load notification settings cho user: " + uid);
        
        userDAO.getDocument(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Load notification settings
                    Map<String, Object> notificationMap = (Map<String, Object>) documentSnapshot.get("notification");
                    
                    if (notificationMap != null) {
                        // Load allowNotification
                        Boolean allowNotif = (Boolean) notificationMap.get("allowNotification");
                        allowNotification = allowNotif != null ? allowNotif : true;
                        
                        // Load enableAiNotifications
                        Boolean enableAi = (Boolean) notificationMap.get("enableAiNotifications");
                        enableAiNotifications = enableAi != null ? enableAi : true;
                        
                        // Load maxNotificationsPerDay
                        Object maxNotifObj = notificationMap.get("maxNotificationsPerDay");
                        if (maxNotifObj instanceof Long) {
                            maxNotificationsPerDay = ((Long) maxNotifObj).intValue();
                        } else if (maxNotifObj instanceof Integer) {
                            maxNotificationsPerDay = (Integer) maxNotifObj;
                        } else {
                            maxNotificationsPerDay = 30; // Default
                        }
                        // Đảm bảo giá trị trong khoảng 1-30
                        if (maxNotificationsPerDay < 1) maxNotificationsPerDay = 1;
                        if (maxNotificationsPerDay > 30) maxNotificationsPerDay = 30;
                        
                        // Load language
                        String lang = (String) notificationMap.get("language");
                        language = lang != null ? lang : "vi";
                        
                        // Load allowMotivationalMessages
                        Boolean allowMotiv = (Boolean) notificationMap.get("allowMotivationalMessages");
                        allowMotivationalMessages = allowMotiv != null ? allowMotiv : true;
                        
                        // Load notification type settings
                        Boolean dailyReminder = (Boolean) notificationMap.get("enableDailyAiReminder");
                        enableDailyAiReminder = dailyReminder != null ? dailyReminder : true;
                        
                        Boolean missedWorkout = (Boolean) notificationMap.get("enableMissedWorkoutReminder");
                        enableMissedWorkoutReminder = missedWorkout != null ? missedWorkout : true;
                        
                        Boolean upcomingSchedule = (Boolean) notificationMap.get("enableUpcomingSchedule");
                        enableUpcomingSchedule = upcomingSchedule != null ? upcomingSchedule : true;
                        
                        Boolean sessionFeedback = (Boolean) notificationMap.get("enableSessionFeedback");
                        enableSessionFeedback = sessionFeedback != null ? sessionFeedback : true;
                        
                        Boolean streakReminder = (Boolean) notificationMap.get("enableStreakReminder");
                        enableStreakReminder = streakReminder != null ? streakReminder : true;
                        
                        // Update UI
                        updateNotificationUI();
                        
                        Log.d(TAG, "✓ Đã load notification settings thành công");
                    } else {
                        // Không có notification settings → Tạo settings mặc định
                        Log.d(TAG, "Không có notification settings, tạo settings mặc định");
                        updateNotificationUI();
                        // Tự động lưu settings mặc định lên Firestore
                        saveNotificationSettings();
                    }
                } else {
                    Log.w(TAG, "User document không tồn tại, tạo settings mặc định");
                    updateNotificationUI(); // Vẫn update UI với giá trị mặc định
                    // Tự động lưu settings mặc định lên Firestore
                    saveNotificationSettings();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi load notification settings: " + e.getMessage(), e);
                updateNotificationUI(); // Vẫn update UI với giá trị mặc định
                Toast.makeText(this, "Không thể tải cài đặt thông báo", Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Update notification UI với giá trị hiện tại
     */
    private void updateNotificationUI() {
        switchAllowNotification.setChecked(allowNotification);
        switchEnableAiNotifications.setChecked(enableAiNotifications);
        switchEnableAiNotifications.setEnabled(allowNotification); // Chỉ enable khi allowNotification = true
        seekBarMaxNotifications.setProgress(maxNotificationsPerDay - 1); // 1-30 → 0-29
        tvMaxNotificationsValue.setText(String.valueOf(maxNotificationsPerDay));
        switchMotivationalMessages.setChecked(allowMotivationalMessages);
        
        // Update notification type switches
        switchDailyAiReminder.setChecked(enableDailyAiReminder);
        switchMissedWorkoutReminder.setChecked(enableMissedWorkoutReminder);
        switchUpcomingSchedule.setChecked(enableUpcomingSchedule);
        switchSessionFeedback.setChecked(enableSessionFeedback);
        switchStreakReminder.setChecked(enableStreakReminder);
        
        // Update enabled state
        updateNotificationTypeSwitchesEnabled();
    }
    
    /**
     * Debounced save notification settings - tránh gọi API quá nhiều lần
     */
    private void debouncedSaveNotificationSettings() {
        // Hủy callback cũ nếu có
        if (saveSettingsRunnable != null) {
            saveSettingsHandler.removeCallbacks(saveSettingsRunnable);
        }
        
        // Tạo callback mới
        saveSettingsRunnable = () -> {
            if (!isSavingSettings) {
                saveNotificationSettings();
            }
        };
        
        // Đợi 500ms trước khi save
        saveSettingsHandler.postDelayed(saveSettingsRunnable, SAVE_DEBOUNCE_DELAY);
    }
    
    /**
     * Save notification settings lên Firestore
     */
    private void saveNotificationSettings() {
        if (user == null) {
            Log.w(TAG, "User chưa đăng nhập, không thể lưu settings");
            return;
        }
        
        // Nếu đang save, bỏ qua
        if (isSavingSettings) {
            Log.d(TAG, "Đang lưu settings, bỏ qua request mới");
            return;
        }
        
        isSavingSettings = true;
        String uid = user.getUid();
        Log.d(TAG, "Đang lưu notification settings cho user: " + uid);
        
        // Đảm bảo giá trị hợp lệ
        if (maxNotificationsPerDay < 1) maxNotificationsPerDay = 1;
        if (maxNotificationsPerDay > 30) maxNotificationsPerDay = 30;
        
        // Tạo map settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("allowNotification", allowNotification);
        settings.put("enableAiNotifications", enableAiNotifications);
        settings.put("maxNotificationsPerDay", maxNotificationsPerDay);
        settings.put("language", language);
        settings.put("allowMotivationalMessages", allowMotivationalMessages);
        
        // Notification type settings
        settings.put("enableDailyAiReminder", enableDailyAiReminder);
        settings.put("enableMissedWorkoutReminder", enableMissedWorkoutReminder);
        settings.put("enableUpcomingSchedule", enableUpcomingSchedule);
        settings.put("enableSessionFeedback", enableSessionFeedback);
        settings.put("enableStreakReminder", enableStreakReminder);
        
        // Cập nhật lên Firestore
        FirebaseService.getInstance().updateAiNotificationSettings(uid, settings, success -> {
            isSavingSettings = false;
            if (success) {
                Log.d(TAG, "✓ Đã lưu notification settings thành công");
            } else {
                Log.e(TAG, "✗ Lỗi lưu notification settings");
                Toast.makeText(this, "Lỗi lưu cài đặt thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .into(imgProfile);
            }
        }
    }

    private void saveProfileChanges() {
        if (user == null) return;

        // ✅ Khai báo newName ở đầu hàm — để các listener bên trong có thể dùng được
        String newName = etName.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên hiển thị!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            // Upload ảnh sử dụng UserDAO
            userDAO.uploadAvatar(user.getUid(), selectedImageUri, task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "❌ Lỗi upload ảnh: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String photoUrl = task.getResult();
                
                UserProfileChangeRequest profileUpdates =
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .setPhotoUri(android.net.Uri.parse(photoUrl))
                                .build();

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                user.reload().addOnCompleteListener(reloadTask -> {
                                    FirebaseUser refreshedUser = authService.getCurrentUser();
                                    if (refreshedUser != null) {
                                        // Update User document trong Firestore
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("displayName", newName);
                                        updates.put("photoUrl", photoUrl);
                                        
                                        userDAO.getDocument(refreshedUser.getUid())
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "✅ Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(this, "❌ Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                });
                            } else {
                                Toast.makeText(this, "❌ Lỗi Auth: " + 
                                    (updateTask.getException() != null ? updateTask.getException().getMessage() : "Unknown error"), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        } else {
            // Không đổi ảnh, chỉ đổi tên
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.reload().addOnCompleteListener(t -> {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("displayName", newName);
                                
                                userDAO.getDocument(user.getUid())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "✅ Lưu thay đổi thành công!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "❌ Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            });
                        }
                    });
        }
    }
}
