package fpt.fall2025.posetrainer.Helper;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class để kiểm tra và request các permissions cần thiết cho notifications
 */
public class PermissionHelper {
    private static final String TAG = "PermissionHelper";

    /**
     * Kiểm tra xem app có quyền hiển thị notifications không
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires runtime permission
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 và thấp hơn: check notification enabled
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            return notificationManager.areNotificationsEnabled();
        }
    }

    /**
     * Request notification permission (cho Android 13+)
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    requestCode
                );
            }
        }
    }

    /**
     * Kiểm tra xem app có quyền schedule exact alarms không
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires checking AlarmManager.canScheduleExactAlarms()
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                return alarmManager.canScheduleExactAlarms();
            }
        }
        // Android 11 và thấp hơn: permission được grant tự động
        return true;
    }

    /**
     * Mở settings để user cấp quyền schedule exact alarms (cho Android 12+)
     */
    public static void openExactAlarmPermissionSettings(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                );
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening exact alarm settings", e);
                // Fallback: open general app settings
                openAppSettings(activity);
            }
        }
    }

    /**
     * Mở app settings
     */
    public static void openAppSettings(Activity activity) {
        try {
            android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            );
            android.net.Uri uri = android.net.Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening app settings", e);
        }
    }

    /**
     * Kiểm tra tất cả permissions cần thiết cho notifications
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        boolean hasNotification = hasNotificationPermission(context);
        boolean canScheduleExact = canScheduleExactAlarms(context);
        
        Log.d(TAG, "Permission check - Notification: " + hasNotification + 
                   ", Exact Alarm: " + canScheduleExact);
        
        return hasNotification && canScheduleExact;
    }
}

