package fpt.fall2025.posetrainer.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Service để nhận và xử lý thông báo push từ Firebase Cloud Messaging (FCM)
 * Hỗ trợ cả thông báo thường và thông báo AI cá nhân hóa
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    
    // Channel ID cho các loại thông báo khác nhau
    private static final String CHANNEL_ID_AI = "ai_notifications"; // Thông báo từ AI
    private static final String CHANNEL_ID_WORKOUT = "workout_reminders"; // Nhắc tập luyện
    private static final String CHANNEL_ID_SOCIAL = "social_notifications"; // Thông báo mạng xã hội
    private static final String CHANNEL_ID_DEFAULT = "default_notifications"; // Thông báo mặc định

    /**
     * Được gọi khi FCM token của thiết bị thay đổi
     * Cần cập nhật token mới lên Firestore để server có thể gửi thông báo
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token mới: " + token);
        
        // Cập nhật token lên Firestore cho user hiện tại
        updateTokenToFirestore(token);
    }

    /**
     * Được gọi khi nhận được thông báo từ FCM
     * Xử lý và hiển thị thông báo cho người dùng
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "=== NHẬN THÔNG BÁO TỪ FCM ===");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Kiểm tra xem message có data payload không
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
        
        // Kiểm tra xem message có notification payload không
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage);
        }
    }

    /**
     * Xử lý thông báo có data payload (được gửi từ Cloud Functions)
     * Data payload chứa thông tin chi tiết về thông báo AI
     */
    private void handleDataMessage(Map<String, String> data) {
        // Lấy các trường dữ liệu từ payload
        String notificationId = data.get("notificationId");
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        String actionType = data.get("actionType");
        String actionData = data.get("actionData");
        String isAiGenerated = data.get("isAiGenerated");
        String sentAtStr = data.get("sentAt"); // Timestamp từ Cloud Functions
        
        Log.d(TAG, "Xử lý data message - Type: " + type + ", Title: " + title);
        
        // Lưu notification vào Firestore (nếu chưa có trong Firestore)
        // Cloud Functions có thể đã lưu rồi, nhưng để chắc chắn ta sẽ lưu lại
        saveNotificationToFirestore(notificationId, type, title, body, actionType, actionData, 
                                   "true".equals(isAiGenerated), sentAtStr);
        
        // Tạo và hiển thị notification
        showNotification(notificationId, type, title, body, actionType, actionData, 
                        "true".equals(isAiGenerated));
    }

    /**
     * Xử lý thông báo có notification payload (thông báo đơn giản)
     */
    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        String title = notification.getTitle();
        String body = notification.getBody();
        
        // Lấy thêm data nếu có
        Map<String, String> data = remoteMessage.getData();
        String notificationId = data.get("notificationId");
        String type = data.getOrDefault("type", "default");
        String actionType = data.get("actionType");
        String actionData = data.get("actionData");
        String isAiGenerated = data.get("isAiGenerated");
        String sentAtStr = data.get("sentAt");
        
        Log.d(TAG, "Xử lý notification message - Title: " + title);
        
        // Lưu notification vào Firestore
        saveNotificationToFirestore(notificationId, type, title, body, actionType, actionData, 
                                   "true".equals(isAiGenerated), sentAtStr);
        
        // Hiển thị thông báo
        showNotification(notificationId, type, title, body, actionType, actionData, 
                        "true".equals(isAiGenerated));
    }

    /**
     * Hiển thị notification trên thiết bị
     * @param notificationId ID của notification trong Firestore
     * @param type Loại thông báo
     * @param title Tiêu đề
     * @param body Nội dung
     * @param actionType Loại hành động khi tap (open_workout, open_exercise, etc.)
     * @param actionData Dữ liệu cho action (workoutId, exerciseId, etc.)
     * @param isAiGenerated Có phải thông báo do AI tạo không
     */
    private void showNotification(String notificationId, String type, String title, 
                                  String body, String actionType, String actionData, 
                                  boolean isAiGenerated) {
        
        // Tạo notification channels (nếu chưa có)
        createNotificationChannels();
        
        // Chọn channel phù hợp theo type
        String channelId = getChannelIdForType(type);
        
        // Tạo intent để mở khi tap vào notification
        Intent intent = createIntentForAction(actionType, actionData);
        
        // Tạo PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            generateNotificationId(notificationId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Chọn icon phù hợp
        int iconRes = getIconForType(type);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Tự động xóa khi tap
            .setContentIntent(pendingIntent);
        
        // Thêm badge cho AI notifications
        if (isAiGenerated) {
            builder.setSubText("✨ AI"); // Thêm dấu hiệu đây là thông báo AI
        }
        
        // Hiển thị notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            int notifId = generateNotificationId(notificationId, type);
            notificationManager.notify(notifId, builder.build());
            Log.d(TAG, "Đã hiển thị thông báo với ID: " + notifId);
        }
    }

    /**
     * Tạo Intent phù hợp dựa trên actionType
     * @param actionType Loại hành động: "open_workout", "open_exercise", "view_progress", "none"
     * @param actionData Dữ liệu cho action
     * @return Intent để mở Activity tương ứng
     */
    private Intent createIntentForAction(String actionType, String actionData) {
        Intent intent;
        
        if (actionType == null || "none".equals(actionType)) {
            // Mở MainActivity (mặc định)
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("openFragment", "notifications"); // Mở NotificationFragment
        } else if ("open_workout".equals(actionType) && actionData != null) {
            // Mở WorkoutActivity với workoutId
            intent = new Intent(this, WorkoutActivity.class);
            intent.putExtra("workoutId", actionData);
            intent.putExtra("fromNotification", true);
        } else if ("open_exercise".equals(actionType) && actionData != null) {
            // Mở ExerciseDetailActivity với exerciseId
            // (Cần tạo Activity này nếu chưa có)
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("openFragment", "exercise");
            intent.putExtra("exerciseId", actionData);
        } else if ("open_post".equals(actionType) && actionData != null) {
            // Mở PostDetailActivity với postId (cho thông báo xã hội)
            intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, actionData);
        } else if ("view_progress".equals(actionType)) {
            // Mở ProfileFragment để xem progress
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("openFragment", "profile");
        } else {
            // Mặc định mở MainActivity
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("openFragment", "notifications");
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /**
     * Tạo các Notification Channels cho Android 8.0+
     * Mỗi loại thông báo sẽ có channel riêng để người dùng có thể tùy chỉnh
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                // Channel cho AI notifications
                NotificationChannel aiChannel = new NotificationChannel(
                    CHANNEL_ID_AI,
                    "Thông báo từ AI",
                    NotificationManager.IMPORTANCE_HIGH
                );
                aiChannel.setDescription("Thông báo cá nhân hóa từ AI về tập luyện của bạn");
                aiChannel.enableVibration(true);
                aiChannel.enableLights(true);
                notificationManager.createNotificationChannel(aiChannel);
                
                // Channel cho workout reminders
                NotificationChannel workoutChannel = new NotificationChannel(
                    CHANNEL_ID_WORKOUT,
                    "Nhắc nhở tập luyện",
                    NotificationManager.IMPORTANCE_HIGH
                );
                workoutChannel.setDescription("Nhắc nhở khi đến giờ tập luyện");
                workoutChannel.enableVibration(true);
                workoutChannel.enableLights(true);
                notificationManager.createNotificationChannel(workoutChannel);
                
                // Channel cho social notifications
                NotificationChannel socialChannel = new NotificationChannel(
                    CHANNEL_ID_SOCIAL,
                    "Mạng xã hội",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                socialChannel.setDescription("Thông báo về like, comment, follow");
                notificationManager.createNotificationChannel(socialChannel);
                
                // Channel mặc định
                NotificationChannel defaultChannel = new NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    "Thông báo chung",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                defaultChannel.setDescription("Các thông báo khác");
                notificationManager.createNotificationChannel(defaultChannel);
                
                Log.d(TAG, "Đã tạo các notification channels");
            }
        }
    }

    /**
     * Lấy Channel ID phù hợp theo loại thông báo
     */
    private String getChannelIdForType(String type) {
        if (type == null) return CHANNEL_ID_DEFAULT;
        
        if (type.startsWith("ai_")) {
            return CHANNEL_ID_AI;
        } else if (type.contains("reminder") || type.contains("workout")) {
            return CHANNEL_ID_WORKOUT;
        } else if (type.contains("social")) {
            return CHANNEL_ID_SOCIAL;
        } else {
            return CHANNEL_ID_DEFAULT;
        }
    }

    /**
     * Lấy icon phù hợp theo loại thông báo
     */
    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_notifications;
        
        if (type.startsWith("ai_")) {
            return R.drawable.ic_ai_sparkle; // Icon AI (cần tạo)
        } else if (type.contains("reminder")) {
            return R.drawable.ic_calendar_today;
        } else if (type.contains("achievement")) {
            return R.drawable.ic_trophy; // Icon cúp (cần tạo)
        } else if (type.contains("social")) {
            return R.drawable.ic_social; // Icon social (cần tạo)
        } else {
            return R.drawable.ic_notifications;
        }
    }

    /**
     * Generate unique notification ID
     */
    private int generateNotificationId(String notificationId, String type) {
        if (notificationId != null) {
            return Math.abs(notificationId.hashCode());
        }
        // Fallback: dùng type + timestamp
        return Math.abs((type + System.currentTimeMillis()).hashCode());
    }

    /**
     * Lưu notification vào Firestore khi nhận được từ FCM
     * Để user có thể xem lại trong NotificationFragment
     */
    private void saveNotificationToFirestore(String notificationId, String type, String title, 
                                             String body, String actionType, String actionData, 
                                             boolean isAiGenerated, String sentAtStr) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có user đăng nhập, không thể lưu notification");
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Đang lưu notification vào Firestore - Type: " + type + ", Title: " + title);
        
        // Parse sentAt timestamp (nếu có từ Cloud Functions, dùng nó; không thì dùng thời gian hiện tại)
        long sentAt;
        if (sentAtStr != null && !sentAtStr.isEmpty()) {
            try {
                sentAt = Long.parseLong(sentAtStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Không thể parse sentAt: " + sentAtStr + ", dùng thời gian hiện tại");
                sentAt = System.currentTimeMillis();
            }
        } else {
            sentAt = System.currentTimeMillis();
        }
        
        // Tạo Notification object
        Notification notification = new Notification(
            notificationId, // ID từ Cloud Functions (nếu có)
            uid,
            type != null ? type : "default",
            title != null ? title : "Thông báo",
            body != null ? body : "",
            sentAt,
            false // read = false (chưa đọc)
        );
        
        // Set các field cho AI notifications
        if (isAiGenerated) {
            notification.setAiGenerated(true);
        }
        if (actionType != null) {
            notification.setActionType(actionType);
        }
        if (actionData != null) {
            notification.setActionData(actionData);
        }
        
        // Lưu vào Firestore
        // Nếu notificationId đã có (từ Cloud Functions), thì kiểm tra xem document đã tồn tại chưa
        // Nếu chưa có hoặc không tồn tại, thì tạo mới
        if (notificationId != null && !notificationId.isEmpty()) {
            // Kiểm tra xem notification đã tồn tại chưa
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Document chưa tồn tại → Tạo mới
                        saveNotificationDocument(notificationId, notification);
                    } else {
                        Log.d(TAG, "Notification đã tồn tại trong Firestore: " + notificationId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lỗi kiểm tra notification tồn tại, tạo mới: " + e.getMessage());
                    // Nếu lỗi kiểm tra, vẫn tạo mới
                    saveNotificationDocument(null, notification);
                });
        } else {
            // Không có notificationId → Tạo mới với auto-generated ID
            saveNotificationDocument(null, notification);
        }
    }
    
    /**
     * Lưu notification document vào Firestore
     */
    private void saveNotificationDocument(String documentId, Notification notification) {
        if (documentId != null && !documentId.isEmpty()) {
            // Lưu với document ID cụ thể
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(documentId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Đã lưu notification vào Firestore với ID: " + documentId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Lỗi lưu notification vào Firestore: " + e.getMessage(), e);
                });
        } else {
            // Tạo mới với auto-generated ID
            FirebaseService.getInstance().saveNotification(notification, success -> {
                if (success) {
                    Log.d(TAG, "✓ Đã lưu notification vào Firestore thành công");
                } else {
                    Log.e(TAG, "✗ Lỗi lưu notification vào Firestore");
                }
            });
        }
    }

    /**
     * Cập nhật FCM token lên Firestore
     */
    private void updateTokenToFirestore(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có user đăng nhập, không thể cập nhật token");
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Đang cập nhật FCM token cho user: " + uid);
        
        // Cập nhật token trong Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("notification.fcmToken", token)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ Đã cập nhật FCM token thành công");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi cập nhật FCM token: " + e.getMessage(), e);
            });
    }
}

