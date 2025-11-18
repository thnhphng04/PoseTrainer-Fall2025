package fpt.fall2025.posetrainer.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.R;

/**
 * Helper class để tạo và hiển thị notifications
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "workout_reminder_channel";
    private static final String CHANNEL_NAME = "Workout Reminders";
    private static final String CHANNEL_ID_SOCIAL = "social_notifications_channel";
    private static final String CHANNEL_NAME_SOCIAL = "Thông báo xã hội";
    private static final int NOTIFICATION_ID_BASE = 1000;
    private static final int NOTIFICATION_ID_SOCIAL_BASE = 2000;

    /**
     * Create notification channel (required for Android 8.0+)
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            
            if (notificationManager != null) {
                // Tạo channel cho workout reminders
                NotificationChannel workoutChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                );
                workoutChannel.setDescription("Nhắc nhở tập luyện");
                workoutChannel.enableVibration(true);
                workoutChannel.enableLights(true);
                notificationManager.createNotificationChannel(workoutChannel);
                
                // Tạo channel cho social notifications
                NotificationChannel socialChannel = new NotificationChannel(
                    CHANNEL_ID_SOCIAL,
                    CHANNEL_NAME_SOCIAL,
                    NotificationManager.IMPORTANCE_HIGH
                );
                socialChannel.setDescription("Thông báo về tương tác xã hội (like, comment)");
                socialChannel.enableVibration(true);
                socialChannel.enableLights(true);
                notificationManager.createNotificationChannel(socialChannel);
                
                Log.d(TAG, "Notification channels created");
            }
        }
    }

    /**
     * Show workout reminder notification
     * @param context context
     * @param workoutId workout ID
     * @param dayOfWeek day of week (1=Monday, ..., 7=Sunday)
     * @param minutesBefore minutes before workout time (15, 10, 5, or 0 for at workout time)
     */
    public static void showWorkoutReminderNotification(Context context, String workoutId, int dayOfWeek, int minutesBefore) {
        Log.d(TAG, "Showing notification for workoutId: " + workoutId + ", minutesBefore: " + minutesBefore);
        
        // Check notification permission
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission not granted, cannot show notification");
            return;
        }
        
        // Ensure channel is created
        createNotificationChannel(context);

        // Create intent to open WorkoutActivity
        // WorkoutActivity will handle both WorkoutTemplate and UserWorkout
        Intent workoutIntent = new Intent(context, WorkoutActivity.class);
        workoutIntent.putExtra("workoutId", workoutId);
        workoutIntent.putExtra("fromSchedule", true);
        
        // Create task stack builder to ensure proper navigation
        // MainActivity will be in the back stack, so user can navigate back
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Add MainActivity as parent activity
        stackBuilder.addNextIntent(new Intent(context, MainActivity.class));
        // Add WorkoutActivity as the activity to open
        stackBuilder.addNextIntent(workoutIntent);

        // Create unique request code based on workoutId, dayOfWeek, and minutesBefore
        // This ensures each notification has a unique PendingIntent
        int requestCode = generateRequestCode(workoutId, dayOfWeek, minutesBefore);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
            requestCode,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get day name
        String dayName = getDayNameInVietnamese(dayOfWeek);

        // Build notification content based on minutesBefore
        String title;
        String contentText;
        
        switch (minutesBefore) {
            case 15:
                title = "Sắp đến giờ tập!";
                contentText = "Còn 15 phút nữa là đến giờ tập luyện vào " + dayName + ". Hãy chuẩn bị sẵn sàng!";
                break;
            case 10:
                title = "Sắp đến giờ tập!";
                contentText = "Còn 10 phút nữa là đến giờ tập luyện vào " + dayName + ". Hãy chuẩn bị sẵn sàng!";
                break;
            case 5:
                title = "Sắp đến giờ tập!";
                contentText = "Còn 5 phút nữa là đến giờ tập luyện vào " + dayName + ". Hãy chuẩn bị sẵn sàng!";
                break;
            case 0:
            default:
                title = "Đến giờ tập luyện!";
                contentText = "Bạn có lịch tập vào " + dayName + ". Nhấn để bắt đầu tập luyện.";
                break;
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar_today)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            // Use unique notification ID based on workoutId, dayOfWeek, and minutesBefore
            // This ensures each notification for different workouts has a unique ID
            int notificationId = generateNotificationId(workoutId, dayOfWeek, minutesBefore);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification shown with ID: " + notificationId + 
                " for workoutId: " + workoutId + " (" + minutesBefore + " min before)");
        }
    }

    /**
     * Check if notification permission is granted
     */
    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires runtime permission
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below: check notification enabled
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return notificationManager.areNotificationsEnabled();
            }
            return true; // Assume enabled for older versions
        }
    }

    /**
     * Get day name in Vietnamese
     * dayOfWeek: 1=Monday, 2=Tuesday, ..., 7=Sunday
     */
    private static String getDayNameInVietnamese(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return "Thứ hai";
            case 2:
                return "Thứ ba";
            case 3:
                return "Thứ tư";
            case 4:
                return "Thứ năm";
            case 5:
                return "Thứ sáu";
            case 6:
                return "Thứ bảy";
            case 7:
                return "Chủ nhật";
            default:
                return "Hôm nay";
        }
    }

    /**
     * Cancel notification
     */
    public static void cancelNotification(Context context, int dayOfWeek) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            int notificationId = NOTIFICATION_ID_BASE + dayOfWeek;
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Notification cancelled with ID: " + notificationId);
        }
    }

    /**
     * Generate unique request code for PendingIntent based on workoutId, dayOfWeek, and minutesBefore
     */
    private static int generateRequestCode(String workoutId, int dayOfWeek, int minutesBefore) {
        // Use hash of workoutId combined with dayOfWeek and minutesBefore
        // This ensures unique request codes for different workouts, days, and reminder times
        int hash = workoutId != null ? workoutId.hashCode() : 0;
        return Math.abs(hash) % 1000000 + (dayOfWeek * 1000) + minutesBefore;
    }
    
    /**
     * Generate unique notification ID based on workoutId, dayOfWeek, and minutesBefore
     */
    private static int generateNotificationId(String workoutId, int dayOfWeek, int minutesBefore) {
        // Use hash of workoutId combined with dayOfWeek and minutesBefore
        // This ensures unique notification IDs for different workouts, days, and reminder times
        int hash = workoutId != null ? workoutId.hashCode() : 0;
        // Use modulo to keep ID within reasonable range, add base offset
        return NOTIFICATION_ID_BASE + (Math.abs(hash) % 10000) + (dayOfWeek * 100) + minutesBefore;
    }

    // =============================
    // SOCIAL NOTIFICATIONS
    // =============================

    /**
     * Hiển thị notification cho thông báo xã hội (like, comment)
     * @param context Context để hiển thị notification
     * @param title Tiêu đề notification
     * @param body Nội dung notification
     * @param postId ID của bài viết (để mở khi click)
     * @param notificationType Loại notification ("social_like" hoặc "social_comment")
     */
    public static void showSocialNotification(Context context, String title, String body, 
                                               String postId, String notificationType) {
        Log.d(TAG, "=== BẮT ĐẦU HIỂN THỊ SOCIAL NOTIFICATION ===");
        Log.d(TAG, "  - Title: " + title);
        Log.d(TAG, "  - Body: " + body);
        Log.d(TAG, "  - PostId: " + postId);
        Log.d(TAG, "  - Type: " + notificationType);
        
        // Kiểm tra quyền notification
        if (!hasNotificationPermission(context)) {
            Log.e(TAG, "✗✗✗ KHÔNG CÓ QUYỀN NOTIFICATION - Không thể hiển thị thông báo");
            Log.e(TAG, "  Vui lòng cấp quyền notification trong Settings");
            return;
        }
        Log.d(TAG, "✓ Đã có quyền notification");
        
        // Đảm bảo channel đã được tạo
        createNotificationChannel(context);

        // Tạo intent để mở PostDetailActivity khi click vào notification
        Intent postIntent = new Intent(context, PostDetailActivity.class);
        postIntent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        
        // Tạo task stack builder để đảm bảo navigation đúng
        // MainActivity sẽ ở trong back stack, user có thể quay lại
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Thêm MainActivity làm parent activity
        stackBuilder.addNextIntent(new Intent(context, MainActivity.class));
        // Thêm PostDetailActivity là activity cần mở
        stackBuilder.addNextIntent(postIntent);

        // Tạo unique request code dựa trên postId và notificationType
        int requestCode = generateSocialRequestCode(postId, notificationType);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
            requestCode,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Chọn icon phù hợp
        int iconRes = R.drawable.ic_favorite_filled; // Icon heart cho social notifications
        // Nếu có icon comment thì dùng, không thì dùng icon heart cho cả comment
        // Icon comment có thể thêm sau: R.drawable.ic_comment

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Âm thanh, rung, đèn
            .setAutoCancel(true) // Tự động xóa khi tap vào
            .setContentIntent(pendingIntent);

        // Hiển thị notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            // Sử dụng unique notification ID dựa trên postId và notificationType
            int notificationId = generateSocialNotificationId(postId, notificationType);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✓✓✓ ĐÃ HIỂN THỊ THÔNG BÁO XÃ HỘI");
            Log.d(TAG, "  - Notification ID: " + notificationId);
            Log.d(TAG, "  - PostId: " + postId);
            Log.d(TAG, "  - Type: " + notificationType);
            Log.d(TAG, "=== KẾT THÚC HIỂN THỊ SOCIAL NOTIFICATION ===");
        } else {
            Log.e(TAG, "✗✗✗ NotificationManager is null - Không thể hiển thị notification");
        }
    }

    /**
     * Tạo unique request code cho PendingIntent dựa trên postId và notificationType
     */
    private static int generateSocialRequestCode(String postId, String notificationType) {
        int hash = postId != null ? postId.hashCode() : 0;
        int typeHash = notificationType != null ? notificationType.hashCode() : 0;
        // Kết hợp hash của postId và type để tạo unique request code
        return Math.abs(hash + typeHash) % 1000000;
    }

    /**
     * Tạo unique notification ID dựa trên postId và notificationType
     */
    private static int generateSocialNotificationId(String postId, String notificationType) {
        int hash = postId != null ? postId.hashCode() : 0;
        int typeHash = notificationType != null ? notificationType.hashCode() : 0;
        // Sử dụng modulo để giữ ID trong phạm vi hợp lý, thêm base offset
        return NOTIFICATION_ID_SOCIAL_BASE + (Math.abs(hash + typeHash) % 10000);
    }
}

