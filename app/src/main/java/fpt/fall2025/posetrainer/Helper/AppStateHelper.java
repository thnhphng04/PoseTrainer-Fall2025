package fpt.fall2025.posetrainer.Helper;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Helper class để kiểm tra trạng thái app (foreground/background)
 */
public class AppStateHelper {
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
                isAppInForeground = true;
                return true;
            }
        }
        
        isAppInForeground = false;
        return false;
    }
    
    /**
     * Set trạng thái app (foreground/background)
     * Được gọi từ Activity lifecycle callbacks
     */
    public static void setAppInForeground(boolean inForeground) {
        isAppInForeground = inForeground;
    }
    
    /**
     * Set trạng thái DailyFragment (visible/hidden)
     * Được gọi từ DailyFragment lifecycle callbacks
     */
    public static void setDailyFragmentVisible(boolean visible) {
        isDailyFragmentVisible = visible;
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
            return false;
        }
        
        return true;
    }
}

