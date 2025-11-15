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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;

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

    private Uri selectedImageUri;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    
    // Current notification settings
    private boolean allowNotification = true;
    private boolean enableAiNotifications = true;
    private int maxNotificationsPerDay = 30;
    private String language = "vi";
    private boolean allowMotivationalMessages = true;

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

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

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
            saveNotificationSettings();
        });
        
        // Switch: Bật/tắt AI notifications
        switchEnableAiNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableAiNotifications = isChecked;
            // Chỉ bật được khi allowNotification = true
            if (isChecked && !allowNotification) {
                switchAllowNotification.setChecked(true);
                allowNotification = true;
            }
            saveNotificationSettings();
        });
        
        // SeekBar: Chọn số lượng thông báo hàng ngày (1-30)
        seekBarMaxNotifications.setMax(29); // 0-29 → 1-30
        seekBarMaxNotifications.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxNotificationsPerDay = progress + 1; // progress 0-29 → 1-30
                tvMaxNotificationsValue.setText(String.valueOf(maxNotificationsPerDay));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveNotificationSettings();
            }
        });
        
        // Switch: Cho phép tin nhắn động viên
        switchMotivationalMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allowMotivationalMessages = isChecked;
            saveNotificationSettings();
        });
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
        
        db.collection("users")
            .document(uid)
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
                        
                        // Load language
                        String lang = (String) notificationMap.get("language");
                        language = lang != null ? lang : "vi";
                        
                        // Load allowMotivationalMessages
                        Boolean allowMotiv = (Boolean) notificationMap.get("allowMotivationalMessages");
                        allowMotivationalMessages = allowMotiv != null ? allowMotiv : true;
                        
                        // Update UI
                        updateNotificationUI();
                        
                        Log.d(TAG, "✓ Đã load notification settings thành công");
                    } else {
                        // Không có notification settings → Dùng giá trị mặc định
                        Log.d(TAG, "Không có notification settings, dùng giá trị mặc định");
                        updateNotificationUI();
                    }
                } else {
                    Log.w(TAG, "User document không tồn tại");
                    updateNotificationUI(); // Vẫn update UI với giá trị mặc định
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi load notification settings: " + e.getMessage(), e);
                updateNotificationUI(); // Vẫn update UI với giá trị mặc định
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
    }
    
    /**
     * Save notification settings lên Firestore
     */
    private void saveNotificationSettings() {
        if (user == null) {
            Log.w(TAG, "User chưa đăng nhập, không thể lưu settings");
            return;
        }
        
        String uid = user.getUid();
        Log.d(TAG, "Đang lưu notification settings cho user: " + uid);
        
        // Tạo map settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("allowNotification", allowNotification);
        settings.put("enableAiNotifications", enableAiNotifications);
        settings.put("maxNotificationsPerDay", maxNotificationsPerDay);
        settings.put("language", language);
        settings.put("allowMotivationalMessages", allowMotivationalMessages);
        
        // Cập nhật lên Firestore
        FirebaseService.getInstance().updateAiNotificationSettings(uid, settings, success -> {
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
            // ✅ Upload ảnh đúng theo Storage Rules (/avatars/{uid}/{filename})
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String fileName = "avatar_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference()
                    .child("avatars/" + user.getUid() + "/" + fileName);

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String photoUrl = uri.toString();

                                UserProfileChangeRequest profileUpdates =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(newName)
                                                .setPhotoUri(uri)
                                                .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                user.reload().addOnCompleteListener(t -> {
                                                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                                                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                                                    db.collection("users")
                                                            .document(refreshedUser.getUid())
                                                            .update("displayName", newName,
                                                                    "photoUrl", photoUrl)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(this, "✅ Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                                                                finish();
                                                            })
                                                            .addOnFailureListener(e ->
                                                                    Toast.makeText(this, "❌ Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                });
                                            } else {
                                                Toast.makeText(this, "❌ Lỗi Auth: " + task.getException(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Không đổi ảnh, chỉ đổi tên
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.reload().addOnCompleteListener(t -> {
                                db.collection("users")
                                        .document(user.getUid())
                                        .update("displayName", newName)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "✅ Lưu thay đổi thành công!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            });
                        }
                    });
        }
    }
}
