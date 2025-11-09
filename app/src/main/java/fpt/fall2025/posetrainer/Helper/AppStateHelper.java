package fpt.fall2025.posetrainer.Helper;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Helper class để kiểm tra trạng thái app (foreground/background)
 */
public class AppStateHelper {
    private static final String TAG = "AppStateHelper";
    private static boolean isAppInForeground = false;
    private static boolean isDailyFragmentVisible = false;
    
    /**
     * Kiểm tra xem app có đang ở foreground không
     */
    public static boolean isAppInForeground(Context context) {
        if (context == null) {
            return false;
        }
        
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return isAppInForeground; // Fallback to cached value
        }
        
        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                boolean wasInForeground = isAppInForeground;
                isAppInForeground = true;
                if (!wasInForeground) {
                    Log.d(TAG, "Ứng dụng chuyển sang TRẠNG THÁI FOREGROUND");
                }
                return true;
            }
        }
        
        boolean wasInForeground = isAppInForeground;
        isAppInForeground = false;
        if (wasInForeground) {
            Log.d(TAG, "Ứng dụng chuyển sang TRẠNG THÁI BACKGROUND");
        }
        return false;
    }
    
    /**
     * Set trạng thái app (foreground/background)
     * Được gọi từ Activity lifecycle callbacks
     */
    public static void setAppInForeground(boolean inForeground) {
        boolean wasInForeground = isAppInForeground;
        isAppInForeground = inForeground;
        if (wasInForeground != inForeground) {
            Log.d(TAG, "Trạng thái ứng dụng thay đổi: " + (inForeground ? "FOREGROUND" : "BACKGROUND"));
        }
    }
    
    /**
     * Set trạng thái DailyFragment (visible/hidden)
     * Được gọi từ DailyFragment lifecycle callbacks
     */
    public static void setDailyFragmentVisible(boolean visible) {
        boolean wasVisible = isDailyFragmentVisible;
        isDailyFragmentVisible = visible;
        if (wasVisible != visible) {
            Log.d(TAG, "Trạng thái hiển thị DailyFragment thay đổi: " + (visible ? "HIỂN THỊ" : "ẨN"));
        }
    }
    
    /**
     * Kiểm tra xem DailyFragment có đang visible không
     */
    public static boolean isDailyFragmentVisible() {
        return isDailyFragmentVisible;
    }
    
    /**
     * Kiểm tra xem có nên hiển thị notification không
     * Không hiển thị notification nếu:
     * 1. App đang ở foreground VÀ
     * 2. DailyFragment đang visible
     */
    public static boolean shouldShowNotification(Context context) {
        boolean appForeground = isAppInForeground(context);
        boolean dailyVisible = isDailyFragmentVisible();
        
        if (appForeground && dailyVisible) {
            Log.d(TAG, "Thông báo bị ẩn: Ứng dụng ở foreground VÀ DailyFragment đang hiển thị");
            return false;
        }
        
        // Có thể mở rộng: không hiển thị notification nếu app đang ở foreground (bất kỳ fragment nào)
        // Uncomment dòng dưới nếu muốn suppress notification khi app ở foreground
        // if (appForeground) {
        //     Log.d(TAG, "Thông báo bị ẩn: Ứng dụng ở foreground");
        //     return false;
        // }
        
        return true;
    }
}

