package fpt.fall2025.posetrainer.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.BroadcastReceiver.WorkoutReminderReceiver;
import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Helper.PermissionHelper;

/**
 * Service để schedule alarms cho workout reminders
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private static AlarmScheduler instance;
    private Context context;
    private AlarmManager alarmManager;

    private AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static synchronized AlarmScheduler getInstance(Context context) {
        if (instance == null) {
            instance = new AlarmScheduler(context);
        }
        return instance;
    }

    /**
     * Schedule alarms từ Schedule
     */
    public void scheduleAlarmsFromSchedule(Schedule schedule) {
        if (schedule == null || schedule.getScheduleItems() == null) {
            Log.w(TAG, "Schedule or scheduleItems is null");
            return;
        }

        // Cancel all existing alarms first
        cancelAllAlarms();

        if (schedule.getNotification() == null || !schedule.getNotification().isEnabled()) {
            Log.d(TAG, "Notifications are disabled in schedule");
            return;
        }

        // Check if we can schedule exact alarms
        boolean canScheduleExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExact = PermissionHelper.canScheduleExactAlarms(context);
            if (!canScheduleExact) {
                Log.w(TAG, "Cannot schedule exact alarms: permission not granted. Using inexact alarms instead.");
            }
        }

        int remindBeforeMin = schedule.getNotification().getRemindBeforeMin();

        for (Schedule.ScheduleItem item : schedule.getScheduleItems()) {
            // Ưu tiên sử dụng exactDate, nếu không có thì fallback về dayOfWeek (backward compatibility)
            if (item.getTimeLocal() != null) {
                if (item.getExactDate() != null && !item.getExactDate().isEmpty()) {
                    // Có exactDate - schedule alarm cho ngày chính xác
                    scheduleAlarmForScheduleItem(item, remindBeforeMin, canScheduleExact);
                } else if (item.getDayOfWeek() != null && !item.getDayOfWeek().isEmpty()) {
                    // Không có exactDate nhưng có dayOfWeek - fallback (backward compatibility)
                    // Note: Logic này có thể cần được xử lý khác nếu muốn hỗ trợ recurring schedules
                    Log.w(TAG, "Schedule item có dayOfWeek nhưng không có exactDate - bỏ qua (cần exactDate để schedule)");
                }
            }
        }

        Log.d(TAG, "Scheduled alarms for " + schedule.getScheduleItems().size() + " schedule items " +
                   "(exact: " + canScheduleExact + ")");
    }

    /**
     * Schedule alarm cho một ScheduleItem
     * Ưu tiên sử dụng exactDate nếu có để tránh lặp lại tuần sau
     */
    private void scheduleAlarmForScheduleItem(Schedule.ScheduleItem item, int remindBeforeMin, boolean useExact) {
        String[] timeParts = item.getTimeLocal().split(":");
        if (timeParts.length != 2) {
            Log.e(TAG, "Invalid time format: " + item.getTimeLocal());
            return;
        }

        try {
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Chỉ schedule nếu có exactDate
            if (item.getExactDate() == null || item.getExactDate().isEmpty()) {
                Log.w(TAG, "Không thể schedule alarm: schedule item thiếu exactDate");
                return;
            }
            
            scheduleAlarmForExactDate(item.getExactDate(), hour, minute, remindBeforeMin, item.getWorkoutId(), useExact);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing time: " + item.getTimeLocal(), e);
        }
    }

    /**
     * Schedule alarm cho một ngày chính xác
     * exactDate format: "yyyy-MM-dd"
     * Schedules 4 alarms: 15 min before, 10 min before, 5 min before, and at workout time
     */
    private void scheduleAlarmForExactDate(String exactDate, int hour, int minute, int remindBeforeMin, String workoutId, boolean useExact) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date date = dateFormat.parse(exactDate);
            
            Calendar workoutTime = Calendar.getInstance();
            workoutTime.setTime(date);
            workoutTime.set(Calendar.HOUR_OF_DAY, hour);
            workoutTime.set(Calendar.MINUTE, minute);
            workoutTime.set(Calendar.SECOND, 0);
            workoutTime.set(Calendar.MILLISECOND, 0);
            
            // Chỉ schedule nếu thời gian trong tương lai
            if (workoutTime.getTimeInMillis() <= System.currentTimeMillis()) {
                Log.d(TAG, "Bỏ qua alarm cho ngày " + exactDate + " vì đã qua");
                return;
            }
            
            // Define reminder times: 15, 10, 5 minutes before, and 0 (at workout time)
            int[] reminderMinutes = {15, 10, 5, 0};
            
            // Schedule alarms for each reminder time
            for (int minutesBefore : reminderMinutes) {
                Calendar alarmTime = (Calendar) workoutTime.clone();
                alarmTime.add(Calendar.MINUTE, -minutesBefore);
                
                // Only schedule if alarm time is in the future
                if (alarmTime.getTimeInMillis() > System.currentTimeMillis()) {
                    // Use exactDate + minutesBefore as unique identifier
                    String uniqueId = exactDate + "_" + minutesBefore + "_" + (workoutId != null ? workoutId : "");
                    scheduleAlarmForExactDateSingle(alarmTime.getTimeInMillis(), workoutId, exactDate, minutesBefore, useExact, uniqueId);
                    Log.d(TAG, "Scheduled " + (useExact ? "exact" : "inexact") + " alarm for date " + exactDate + 
                        " at " + String.format("%02d:%02d", hour, minute) + 
                        " (remind " + minutesBefore + " min before)");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing exactDate: " + exactDate, e);
        }
    }


    /**
     * Schedule a single alarm for exact date
     */
    private void scheduleAlarmForExactDateSingle(long triggerAtMillis, String workoutId, String exactDate, int minutesBefore, boolean useExact, String uniqueId) {
        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        intent.putExtra("workoutId", workoutId);
        intent.putExtra("exactDate", exactDate);
        intent.putExtra("minutesBefore", minutesBefore);
        
        // Use unique request code based on uniqueId hash
        int requestCode = uniqueId.hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        scheduleAlarmInternal(triggerAtMillis, pendingIntent, useExact);
    }


    /**
     * Internal method to schedule alarm
     */
    private void scheduleAlarmInternal(long triggerAtMillis, PendingIntent pendingIntent, boolean useExact) {
        try {
            if (useExact) {
                // Try to use exact alarm methods
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+ (API 23+): Use setExactAndAllowWhileIdle for better reliability
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Android 4.4+ (API 19+): Use setExact
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                } else {
                    // Android 4.3 and below: Use set (less precise but works)
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                }
            } else {
                // Use inexact alarm (fallback when exact permission not available)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+: Use setAndAllowWhileIdle for better reliability
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Android 4.4+: Use setWindow for better batching
                    long windowMillis = 60 * 60 * 1000; // 1 hour window
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        windowMillis,
                        pendingIntent
                    );
                } else {
                    // Android 4.3 and below: Use set
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
            // Fallback to inexact alarm if exact fails
            if (useExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                    Log.d(TAG, "Fallback to inexact alarm due to permission issue");
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to schedule fallback alarm", ex);
                }
            }
        }
    }

    /**
     * Cancel all workout reminder alarms
     */
    public void cancelAllAlarms() {
        Log.d(TAG, "Cancelling all workout reminder alarms");
        
        // Cancel alarms for all possible combinations
        // This is a simplified approach - in production, you might want to track alarm IDs
        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        
        // Cancel alarms for each day of week (1-7) and each reminder time (15, 10, 5, 0)
        int[] reminderMinutes = {15, 10, 5, 0};
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            for (int minutesBefore : reminderMinutes) {
                for (int i = 0; i < 100; i++) { // Cancel up to 100 alarms per day/reminder combination
                    int requestCode = (dayOfWeek * 10000) + (minutesBefore * 1000) + i;
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent);
                        pendingIntent.cancel();
                    }
                }
            }
        }
        
        Log.d(TAG, "All alarms cancelled");
    }

    /**
     * Cancel alarm for specific schedule item
     */
    public void cancelAlarmForScheduleItem(Schedule.ScheduleItem item, int dayOfWeek) {
        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        int[] reminderMinutes = {15, 10, 5, 0};
        for (int minutesBefore : reminderMinutes) {
            int requestCode = (dayOfWeek * 10000) + (minutesBefore * 1000) + (item.getWorkoutId() != null ? Math.abs(item.getWorkoutId().hashCode() % 1000) : 0);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Cancelled alarm for day " + dayOfWeek + " workout " + item.getWorkoutId() + " (" + minutesBefore + " min before)");
            }
        }
    }
}

