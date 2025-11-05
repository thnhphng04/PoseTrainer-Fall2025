package fpt.fall2025.posetrainer.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

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
            if (item.getDayOfWeek() != null && item.getTimeLocal() != null) {
                scheduleAlarmForScheduleItem(item, remindBeforeMin, canScheduleExact);
            }
        }

        Log.d(TAG, "Scheduled alarms for " + schedule.getScheduleItems().size() + " schedule items " +
                   "(exact: " + canScheduleExact + ")");
    }

    /**
     * Schedule alarm cho một ScheduleItem
     */
    private void scheduleAlarmForScheduleItem(Schedule.ScheduleItem item, int remindBeforeMin, boolean useExact) {
        if (item.getDayOfWeek() == null || item.getDayOfWeek().isEmpty()) {
            return;
        }

        String[] timeParts = item.getTimeLocal().split(":");
        if (timeParts.length != 2) {
            Log.e(TAG, "Invalid time format: " + item.getTimeLocal());
            return;
        }

        try {
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Schedule alarm for each day of week
            for (Integer dayOfWeek : item.getDayOfWeek()) {
                scheduleAlarmForDay(dayOfWeek, hour, minute, remindBeforeMin, item.getWorkoutId(), useExact);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing time: " + item.getTimeLocal(), e);
        }
    }

    /**
     * Schedule alarm cho một ngày cụ thể trong tuần
     * dayOfWeek: 1=Monday, 2=Tuesday, ..., 7=Sunday
     * Schedules 4 alarms: 15 min before, 10 min before, 5 min before, and at workout time
     */
    private void scheduleAlarmForDay(int dayOfWeek, int hour, int minute, int remindBeforeMin, String workoutId, boolean useExact) {
        // Define reminder times: 15, 10, 5 minutes before, and 0 (at workout time)
        int[] reminderMinutes = {15, 10, 5, 0};
        
        Calendar workoutTime = getNextAlarmTime(dayOfWeek, hour, minute);
        
        // Schedule alarms for each reminder time
        for (int minutesBefore : reminderMinutes) {
            Calendar alarmTime = (Calendar) workoutTime.clone();
            alarmTime.add(Calendar.MINUTE, -minutesBefore);
            
            // Only schedule if alarm time is in the future
            if (alarmTime.getTimeInMillis() > System.currentTimeMillis()) {
                scheduleAlarm(alarmTime.getTimeInMillis(), workoutId, dayOfWeek, minutesBefore, useExact);
                Log.d(TAG, "Scheduled " + (useExact ? "exact" : "inexact") + " alarm for day " + dayOfWeek + 
                    " at " + String.format("%02d:%02d", hour, minute) + 
                    " (remind " + minutesBefore + " min before)");
            }
        }
    }

    /**
     * Get next alarm time for a specific day of week
     */
    private Calendar getNextAlarmTime(int dayOfWeek, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        
        // Convert Schedule day (Monday=1, ..., Sunday=7) to Calendar day
        int calendarDayOfWeek = convertScheduleDayToCalendarDay(dayOfWeek);
        
        // Set to next occurrence of this day
        int currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        int daysUntilNext = (calendarDayOfWeek - currentDayOfWeek + 7) % 7;
        
        if (daysUntilNext == 0) {
            // Today is the target day, check if time has passed
            Calendar todayTime = Calendar.getInstance();
            todayTime.set(Calendar.HOUR_OF_DAY, hour);
            todayTime.set(Calendar.MINUTE, minute);
            todayTime.set(Calendar.SECOND, 0);
            todayTime.set(Calendar.MILLISECOND, 0);
            
            if (todayTime.getTimeInMillis() <= System.currentTimeMillis()) {
                // Time has passed today, schedule for next week
                daysUntilNext = 7;
            }
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, daysUntilNext);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar;
    }

    /**
     * Convert Schedule day to Calendar day
     * Schedule: Monday=1, Tuesday=2, ..., Sunday=7
     * Calendar: Sunday=1, Monday=2, ..., Saturday=7
     */
    private int convertScheduleDayToCalendarDay(int scheduleDay) {
        switch (scheduleDay) {
            case 1: // Monday
                return Calendar.MONDAY;
            case 2: // Tuesday
                return Calendar.TUESDAY;
            case 3: // Wednesday
                return Calendar.WEDNESDAY;
            case 4: // Thursday
                return Calendar.THURSDAY;
            case 5: // Friday
                return Calendar.FRIDAY;
            case 6: // Saturday
                return Calendar.SATURDAY;
            case 7: // Sunday
                return Calendar.SUNDAY;
            default:
                return Calendar.MONDAY;
        }
    }

    /**
     * Schedule a single alarm
     * @param triggerAtMillis time when alarm should trigger
     * @param workoutId workout ID
     * @param dayOfWeek day of week (1=Monday, ..., 7=Sunday)
     * @param minutesBefore minutes before workout time (15, 10, 5, or 0)
     * @param useExact true for exact alarm, false for inexact (may have small delay)
     */
    private void scheduleAlarm(long triggerAtMillis, String workoutId, int dayOfWeek, int minutesBefore, boolean useExact) {
        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        intent.putExtra("workoutId", workoutId);
        intent.putExtra("dayOfWeek", dayOfWeek);
        intent.putExtra("minutesBefore", minutesBefore);
        
        // Use unique request code based on dayOfWeek, workoutId, and minutesBefore
        int requestCode = (dayOfWeek * 10000) + (minutesBefore * 1000) + (workoutId != null ? Math.abs(workoutId.hashCode() % 1000) : 0);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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

