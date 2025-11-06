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
import androidx.core.content.ContextCompat;

import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.R;

/**
 * Helper class để tạo và hiển thị notifications
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "workout_reminder_channel";
    private static final String CHANNEL_NAME = "Workout Reminders";
    private static final int NOTIFICATION_ID_BASE = 1000;

    /**
     * Create notification channel (required for Android 8.0+)
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Nhắc nhở tập luyện");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
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
        Intent intent = new Intent(context, WorkoutActivity.class);
        intent.putExtra("workoutId", workoutId);
        intent.putExtra("fromSchedule", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            (int) System.currentTimeMillis(),
            intent,
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
            // Use unique notification ID based on dayOfWeek and minutesBefore
            int notificationId = NOTIFICATION_ID_BASE + (dayOfWeek * 10) + minutesBefore;
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification shown with ID: " + notificationId + " (" + minutesBefore + " min before)");
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
}

