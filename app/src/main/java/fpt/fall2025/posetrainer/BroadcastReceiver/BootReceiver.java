package fpt.fall2025.posetrainer.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Service.AlarmScheduler;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * BroadcastReceiver để restart alarms sau khi thiết bị reboot
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted, restarting alarms");
            
            // Wait a bit for Firebase to initialize
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                restartAlarms(context);
            }, 5000); // Wait 5 seconds
        }
    }

    /**
     * Restart alarms for current user
     */
    private void restartAlarms(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping alarm restart");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Restarting alarms for user: " + userId);

        FirebaseService.getInstance().loadUserSchedule(userId, new FirebaseService.OnScheduleLoadedListener() {
            @Override
            public void onScheduleLoaded(Schedule schedule) {
                if (schedule != null) {
                    Log.d(TAG, "Reloaded schedule, scheduling alarms");
                    AlarmScheduler.getInstance(context).scheduleAlarmsFromSchedule(schedule);
                } else {
                    Log.d(TAG, "No schedule found for user");
                }
            }
        });
    }
}

