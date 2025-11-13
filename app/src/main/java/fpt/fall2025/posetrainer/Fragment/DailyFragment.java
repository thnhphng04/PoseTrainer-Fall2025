package fpt.fall2025.posetrainer.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Adapter.SessionAdapter;
import fpt.fall2025.posetrainer.Dialog.CreateScheduleDialog;
import fpt.fall2025.posetrainer.Dialog.ViewScheduleDialog;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.Helper.PermissionHelper;
import fpt.fall2025.posetrainer.Service.AlarmScheduler;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.Service.NotificationHelper;
import fpt.fall2025.posetrainer.Helper.AppStateHelper;
import fpt.fall2025.posetrainer.databinding.FragmentDailyBinding;

public class DailyFragment extends Fragment {
    private static final String TAG = "DailyFragment";
    private FragmentDailyBinding binding;
    private ArrayList<Session> sessions;
    private ArrayList<Session> filteredSessionsForSelectedDay; // Sessions for selected day
    private Schedule userSchedule; // User schedule
    private int currentDayOfWeek; // Current day of week (1-7, Sunday=1)
    private Calendar calendar;
    private Calendar selectedDayCalendar; // Calendar for selected day
    private Map<String, Boolean> weeklyWorkoutStatus; // Track workout status for each day
    private FirebaseAuth mAuth;
    private SessionAdapter sessionAdapter;
    
    // ActivityResultLauncher for notification permission (Android 13+)
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDailyBinding.inflate(inflater, container, false);
        
        // Register for activity result for notification permission
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Quyền thông báo đã được cấp");
                    // Re-schedule alarms now that permission is granted
                    scheduleAlarms();
                } else {
                    Log.w(TAG, "Quyền thông báo bị từ chối");
                    Toast.makeText(getContext(), 
                        "Cần quyền thông báo để nhắc nhở tập luyện. Vui lòng cấp quyền trong cài đặt.", 
                        Toast.LENGTH_LONG).show();
                }
            }
        );
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize data
        sessions = new ArrayList<>();
        filteredSessionsForSelectedDay = new ArrayList<>();
        userSchedule = null;
        calendar = Calendar.getInstance();
        selectedDayCalendar = Calendar.getInstance();
        currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        weeklyWorkoutStatus = new HashMap<>();

        // Setup UI
        setupHeader();
        setupWeeklyGoal();
        setupActivities();
        setupRecyclerView();
        
        // Load sessions from Firestore
        loadSessions();
        
        // Load user schedule from Firestore
        loadUserSchedule();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Track DailyFragment visibility: đang visible
        AppStateHelper.setDailyFragmentVisible(true);
        Log.d(TAG, "DailyFragment onResume: Fragment đang hiển thị - thông báo sẽ bị ẩn");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Track DailyFragment visibility: không còn visible
        AppStateHelper.setDailyFragmentVisible(false);
        Log.d(TAG, "DailyFragment onPause: Fragment đã ẩn - thông báo sẽ được hiển thị");
    }

    /**
     * Setup RecyclerView for displaying sessions
     */
    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(filteredSessionsForSelectedDay);
        binding.recyclerViewSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewSessions.setAdapter(sessionAdapter);
    }

    /**
     * Setup header with back arrow and edit button
     */
    private void setupHeader() {
        // Set current date
        updateCurrentDate();
        
        binding.ivBackArrow.setOnClickListener(v -> {
            // Navigate back
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.tvEdit.setOnClickListener(v -> {
            // Handle edit functionality
            Toast.makeText(getContext(), "Chỉnh sửa", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Update the title with current date
     */
    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        
        // Get day name in Vietnamese
        String dayName = getDayNameInVietnamese(currentDayOfWeek);
        
        binding.tvTitle.setText(dayName + " " + currentDate);
    }

    /**
     * Get day name in Vietnamese
     */
    private String getDayNameInVietnamese(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Chủ nhật";
            case Calendar.MONDAY:
                return "Thứ hai";
            case Calendar.TUESDAY:
                return "Thứ ba";
            case Calendar.WEDNESDAY:
                return "Thứ tư";
            case Calendar.THURSDAY:
                return "Thứ năm";
            case Calendar.FRIDAY:
                return "Thứ sáu";
            case Calendar.SATURDAY:
                return "Thứ bảy";
            default:
                return "Ngày";
        }
    }

    /**
     * Setup weekly goal circles
     */
    private void setupWeeklyGoal() {
        // Update weekly goal title with current week
        updateWeeklyGoalTitle();
        
        // Update day labels with actual dates
        updateDayLabels();
        
        // Set up click listeners for each day circle
        View[] circles = {
            binding.circle1, binding.circle2, binding.circle3, binding.circle4,
            binding.circle5, binding.circle6, binding.circle7
        };

        // Calendar day constants
        int[] calendarDays = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        };

        for (int i = 0; i < circles.length; i++) {
            final int dayOfWeek = calendarDays[i];
            circles[i].setOnClickListener(v -> {
                // Handle day selection
                selectDay(dayOfWeek);
            });
        }

        // Set initial day to current day of week
        selectDay(currentDayOfWeek);
    }

    /**
     * Update day labels with actual dates of current week
     */
    private void updateDayLabels() {
        // Get start of week (Monday)
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        
        // Day labels
        String[] dayLabels = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        
        // Update each day label with date
        for (int i = 0; i < 7; i++) {
            Calendar dayCalendar = (Calendar) startOfWeek.clone();
            dayCalendar.add(Calendar.DAY_OF_MONTH, i);
            
            int dayOfMonth = dayCalendar.get(Calendar.DAY_OF_MONTH);
            
            // Update the TextView for this day
            switch (i) {
                case 0:
                    binding.tvDay1.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 1:
                    binding.tvDay2.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 2:
                    binding.tvDay3.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 3:
                    binding.tvDay4.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 4:
                    binding.tvDay5.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 5:
                    binding.tvDay6.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
                case 6:
                    binding.tvDay7.setText(dayOfMonth + "\n" + dayLabels[i]);
                    break;
            }
        }
    }

    /**
     * Update weekly goal title with current week information
     */
    private void updateWeeklyGoalTitle() {
        // Get week number and year
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        
        // Get start and end dates of current week
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.add(Calendar.DAY_OF_MONTH, -1); // Adjust for Vietnamese week start
        
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String weekRange = dateFormat.format(startOfWeek.getTime()) + " - " + dateFormat.format(endOfWeek.getTime());
        
        binding.tvWeeklyGoalTitle.setText("Mục tiêu tuần " + weekOfYear + " (" + weekRange + ")");
    }

    /**
     * Select a specific day and update UI
     */
    private void selectDay(int day) {
        currentDayOfWeek = day;
        
        // Update selected day calendar
        updateSelectedDayCalendar(day);
        
        // Filter sessions for selected day
        filterSessionsForSelectedDay();
        
        // Update schedule UI for selected day
        updateScheduleUI();
        
        // Update activity info for selected day
        updateActivityInfo();
        
        // Refresh weekly status to show current selection
        updateWeeklyStatus();
        
        // Highlight selected day
        highlightSelectedDay();
    }

    /**
     * Update selected day calendar based on selected day of week
     */
    private void updateSelectedDayCalendar(int dayOfWeek) {
        Calendar today = Calendar.getInstance();
        int todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        
        // Calculate offset from today
        int offset = dayOfWeek - todayDayOfWeek;
        
        selectedDayCalendar = (Calendar) today.clone();
        selectedDayCalendar.add(Calendar.DAY_OF_MONTH, offset);
        selectedDayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        selectedDayCalendar.set(Calendar.MINUTE, 0);
        selectedDayCalendar.set(Calendar.SECOND, 0);
        selectedDayCalendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Filter sessions for the selected day
     */
    private void filterSessionsForSelectedDay() {
        filteredSessionsForSelectedDay.clear();
        
        if (sessions == null || sessions.isEmpty()) {
            if (sessionAdapter != null) {
                sessionAdapter.updateSessions(filteredSessionsForSelectedDay);
            }
            // Show empty state
            if (binding != null) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewSessions.setVisibility(View.GONE);
            }
            return;
        }
        
        Calendar selectedDayStart = (Calendar) selectedDayCalendar.clone();
        Calendar selectedDayEnd = (Calendar) selectedDayCalendar.clone();
        selectedDayEnd.set(Calendar.HOUR_OF_DAY, 23);
        selectedDayEnd.set(Calendar.MINUTE, 59);
        selectedDayEnd.set(Calendar.SECOND, 59);
        selectedDayEnd.set(Calendar.MILLISECOND, 999);
        
        for (Session session : sessions) {
            if (session != null && session.getStartedAt() > 0) {
                Date sessionDate = new Date(session.getStartedAt() * 1000);
                Calendar sessionCalendar = Calendar.getInstance();
                sessionCalendar.setTime(sessionDate);
                
                // Check if session is on selected day
                if (sessionCalendar.after(selectedDayStart) && sessionCalendar.before(selectedDayEnd) ||
                    sessionCalendar.get(Calendar.DAY_OF_YEAR) == selectedDayStart.get(Calendar.DAY_OF_YEAR) &&
                    sessionCalendar.get(Calendar.YEAR) == selectedDayStart.get(Calendar.YEAR)) {
                    filteredSessionsForSelectedDay.add(session);
                }
            }
        }
        
        // Update adapter
        if (sessionAdapter != null) {
            sessionAdapter.updateSessions(filteredSessionsForSelectedDay);
        }
        
        // Show/hide empty state
        if (filteredSessionsForSelectedDay.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewSessions.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.recyclerViewSessions.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Convert Calendar day of week to array index
     * Calendar: Sunday=1, Monday=2, ..., Saturday=7
     * Our array: Monday=0, Tuesday=1, ..., Sunday=6
     */
    private int convertDayOfWeekToIndex(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            case Calendar.SATURDAY:
                return 5;
            case Calendar.SUNDAY:
                return 6;
            default:
                return 0;
        }
    }

    /**
     * Setup activities section
     */
    private void setupActivities() {
        binding.tvHistory.setOnClickListener(v -> {
            // Navigate to history - could navigate to a history fragment or activity
            Toast.makeText(getContext(), "Lịch sử", Toast.LENGTH_SHORT).show();
            // TODO: Implement navigation to history screen
        });

        // View Schedule Button
        binding.btnViewSchedule.setOnClickListener(v -> {
            // Show dialog to view scheduled workouts
            showViewScheduleDialog();
        });

        binding.btnAddActivity.setOnClickListener(v -> {
            // Show dialog to create new workout schedule
            showCreateScheduleDialog();
        });
    }

    /**
     * Show dialog to view scheduled workouts
     */
    private void showViewScheduleDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        ViewScheduleDialog dialog = new ViewScheduleDialog();
        dialog.show(getParentFragmentManager(), "ViewScheduleDialog");
    }

    /**
     * Show dialog to create new workout schedule
     */
    private void showCreateScheduleDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check permissions before showing dialog
        checkAndRequestPermissions();

        CreateScheduleDialog dialog = new CreateScheduleDialog();
        dialog.setOnScheduleCreatedListener(scheduleItem -> {
            // Add schedule item to user's schedule
            addScheduleItemToSchedule(scheduleItem);
        });
        dialog.show(getParentFragmentManager(), "CreateScheduleDialog");
    }

    /**
     * Add schedule item to user's schedule
     */
    private void addScheduleItemToSchedule(Schedule.ScheduleItem newItem) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (userSchedule == null) {
            // Create new schedule
            createNewSchedule(newItem);
        } else {
            // Add to existing schedule
            updateExistingSchedule(newItem);
        }
    }

    /**
     * Create new schedule with first item
     */
    private void createNewSchedule(Schedule.ScheduleItem item) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        List<Schedule.ScheduleItem> items = new ArrayList<>();
        items.add(item);

        Schedule.NotificationSettings notificationSettings = new Schedule.NotificationSettings(
            true, // enabled
            15, // remindBeforeMin (15 minutes)
            "default" // sound
        );

        Schedule newSchedule = new Schedule(
            null, // id - will be generated by Firestore
            currentUser.getUid(),
            "Lịch tập của tôi",
            java.util.TimeZone.getDefault().getID(),
            items,
            notificationSettings
        );

        FirebaseService.getInstance().saveSchedule(newSchedule, success -> {
            if (success) {
                Toast.makeText(getContext(), "Đã thêm lịch tập thành công", Toast.LENGTH_SHORT).show();
                // Reload schedule
                loadUserSchedule();
                // Schedule alarms
                scheduleAlarms();
                // Create notification records for this schedule item
                createNotificationsForScheduleItem(item, currentUser.getUid());
            } else {
                Toast.makeText(getContext(), "Lỗi khi thêm lịch tập", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Update existing schedule with new item
     */
    private void updateExistingSchedule(Schedule.ScheduleItem newItem) {
        if (userSchedule == null || userSchedule.getScheduleItems() == null) {
            userSchedule = new Schedule();
            userSchedule.setScheduleItems(new ArrayList<>());
        }

        List<Schedule.ScheduleItem> items = userSchedule.getScheduleItems();
        items.add(newItem);

        // Ensure notification settings exist
        if (userSchedule.getNotification() == null) {
            userSchedule.setNotification(new Schedule.NotificationSettings(true, 15, "default"));
        }

        FirebaseService.getInstance().saveSchedule(userSchedule, success -> {
            if (success) {
                Toast.makeText(getContext(), "Đã thêm lịch tập thành công", Toast.LENGTH_SHORT).show();
                // Reload schedule
                loadUserSchedule();
                // Schedule alarms
                scheduleAlarms();
                // Create notification records for this schedule item
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    createNotificationsForScheduleItem(newItem, currentUser.getUid());
                }
            } else {
                Toast.makeText(getContext(), "Lỗi khi cập nhật lịch tập", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Create notification records for a schedule item
     * This creates notifications for each day of week in the schedule
     */
    private void createNotificationsForScheduleItem(Schedule.ScheduleItem item, String uid) {
        if (item.getDayOfWeek() == null || item.getDayOfWeek().isEmpty() || 
            item.getTimeLocal() == null || item.getWorkoutId() == null) {
            Log.w(TAG, "Không thể tạo thông báo: schedule item thiếu các trường bắt buộc");
            return;
        }

        // Get workout name
        loadWorkoutName(item.getWorkoutId(), workoutName -> {
            String[] dayNames = {"", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy", "Chủ nhật"};
            
            // Create notification for each day in the schedule
            for (Integer dayOfWeek : item.getDayOfWeek()) {
                if (dayOfWeek >= 1 && dayOfWeek <= 7) {
                    String dayName = dayNames[dayOfWeek];
                    
                    // Calculate notification time (scheduled time)
                    long notificationTime = calculateNotificationTime(dayOfWeek, item.getTimeLocal());
                    
                    Notification notification = new Notification(
                        null, // id - will be generated by Firestore
                        uid,
                        "workout_reminder", // type
                        "Nhắc nhở tập luyện",
                        "Bạn có lịch tập \"" + workoutName + "\" vào " + dayName + " lúc " + item.getTimeLocal(),
                        notificationTime, // scheduled time
                        false // sent - false means not sent yet
                    );
                    
                    // Save notification to Firestore
                    FirebaseService.getInstance().saveNotification(notification, success -> {
                        if (success) {
                            Log.d(TAG, "Đã tạo thông báo cho ngày " + dayOfWeek + " lúc " + item.getTimeLocal());
                        } else {
                            Log.w(TAG, "Không thể tạo thông báo cho ngày " + dayOfWeek);
                        }
                    });
                }
            }
        });
    }

    /**
     * Load workout name by ID from WorkoutTemplate or UserWorkout
     * Tries to load from workout_templates collection first, then falls back to user_workouts collection
     */
    private void loadWorkoutName(String workoutId, OnWorkoutNameLoadedListener listener) {
        if (workoutId == null || workoutId.isEmpty()) {
            Log.w(TAG, "WorkoutId là null hoặc rỗng");
            listener.onWorkoutNameLoaded("Bài tập");
            return;
        }
        
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            Log.w(TAG, "Activity là null, trả về tên mặc định");
            listener.onWorkoutNameLoaded("Bài tập");
            return;
        }
        
        Log.d(TAG, "Đang tải tên workout cho ID: " + workoutId + " (thử WorkoutTemplate trước, sau đó UserWorkout)");
        
        // Try to load from WorkoutTemplate first (workout_templates collection)
        FirebaseService.getInstance().loadWorkoutTemplateById(workoutId, (androidx.appcompat.app.AppCompatActivity) getActivity(), template -> {
            if (template != null && template.getTitle() != null && !template.getTitle().isEmpty()) {
                // Successfully loaded from WorkoutTemplate
                Log.d(TAG, "✓ Đã tải tên workout từ WorkoutTemplate: " + template.getTitle());
                listener.onWorkoutNameLoaded(template.getTitle());
            } else {
                // WorkoutTemplate not found or invalid - try UserWorkout (user_workouts collection)
                Log.d(TAG, "✗ Không tìm thấy WorkoutTemplate cho ID: " + workoutId + ", đang thử UserWorkout...");
                FirebaseService.getInstance().loadUserWorkoutById(workoutId, (androidx.appcompat.app.AppCompatActivity) getActivity(), userWorkout -> {
                    if (userWorkout != null && userWorkout.getTitle() != null && !userWorkout.getTitle().isEmpty()) {
                        // Successfully loaded from UserWorkout
                        Log.d(TAG, "✓ Đã tải tên workout từ UserWorkout: " + userWorkout.getTitle());
                        listener.onWorkoutNameLoaded(userWorkout.getTitle());
                    } else {
                        // Neither WorkoutTemplate nor UserWorkout found
                        Log.w(TAG, "✗ Không thể tải tên workout từ WorkoutTemplate hoặc UserWorkout cho ID: " + workoutId);
                        // Fallback to default name
                        listener.onWorkoutNameLoaded("Bài tập");
                    }
                });
            }
        });
    }

    /**
     * Calculate notification time based on day of week and time
     */
    private long calculateNotificationTime(int dayOfWeek, String timeLocal) {
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        
        // Convert Schedule day (Monday=1, ..., Sunday=7) to Calendar day
        int calendarDayOfWeek = convertScheduleDayToCalendarDay(dayOfWeek);
        
        // Parse time
        String[] timeParts = timeLocal.split(":");
        if (timeParts.length != 2) {
            return System.currentTimeMillis();
        }
        
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
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
                daysUntilNext = 7;
            }
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, daysUntilNext);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTimeInMillis();
    }

    /**
     * Interface for workout name loading callback
     */
    private interface OnWorkoutNameLoadedListener {
        void onWorkoutNameLoaded(String workoutName);
    }
    private void scheduleAlarms() {
        if (userSchedule == null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            return;
        }

        // Check notification permission (Android 13+)
        // Note: We still schedule alarms even without permission, 
        // but notifications won't show until permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(context)) {
                Log.w(TAG, "Quyền thông báo chưa được cấp. Alarms sẽ được lên lịch nhưng thông báo sẽ không hiển thị.");
            }
        }

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(context);

        // Schedule alarms (will use inexact alarms if exact permission not available)
        AlarmScheduler.getInstance(context).scheduleAlarmsFromSchedule(userSchedule);
        
        Log.d(TAG, "Đã lên lịch alarms cho schedule: " + userSchedule.getTitle());
    }

    /**
     * Check and request required permissions for notifications
     * Returns true if all permissions are granted or can use fallback, false otherwise
     */
    private boolean checkAndRequestPermissions() {
        if (getContext() == null) {
            return false;
        }

        boolean needsNotificationPermission = false;

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(getContext())) {
                needsNotificationPermission = true;
                // Use ActivityResultLauncher instead of deprecated requestPermissions
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // For exact alarm permission (Android 12+): 
        // We can use inexact alarms as fallback, so we don't block scheduling
        // Just show a warning if exact permission is not available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionHelper.canScheduleExactAlarms(getContext())) {
                Log.w(TAG, "Quyền exact alarm chưa được cấp. Sẽ sử dụng inexact alarms (có thể có độ trễ nhỏ).");
                // Show informational toast (not blocking)
                Toast.makeText(getContext(), 
                    "Lưu ý: Thông báo có thể đến muộn vài phút. Để chính xác hơn, vui lòng cấp quyền trong cài đặt.", 
                    Toast.LENGTH_LONG).show();
            }
        }

        // Return true if notification permission is granted (or not needed for older Android)
        // Note: Even if exact alarm permission is not granted, we can still schedule inexact alarms
        return !needsNotificationPermission;
    }

    /**
     * Load user schedule from Firestore
     */
    private void loadUserSchedule() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng hiện tại, không thể tải schedule");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "=== ĐANG TẢI USER SCHEDULE ===");
        Log.d(TAG, "Đang tải schedule cho userId: " + userId);

        FirebaseService.getInstance().loadUserSchedule(userId, new FirebaseService.OnScheduleLoadedListener() {
            @Override
            public void onScheduleLoaded(Schedule schedule) {
                Log.d(TAG, "=== ĐÃ TẢI SCHEDULE ===");
                
                if (schedule != null) {
                    Log.d(TAG, "Đã tải schedule: " + schedule.getTitle() + 
                            " với " + (schedule.getScheduleItems() != null ? schedule.getScheduleItems().size() : 0) + " items");
                    
                    // Validate and cleanup schedule items (remove deleted workouts)
                    validateAndCleanupSchedule(schedule, cleanedSchedule -> {
                        userSchedule = cleanedSchedule;
                        
                        // Update UI with cleaned schedule
                        updateScheduleUI();
                        
                        // Schedule alarms for notifications (only for valid workouts)
                        scheduleAlarms();
                    });
                } else {
                    Log.d(TAG, "Không tìm thấy schedule cho người dùng");
                    userSchedule = null;
                }
                
                Log.d(TAG, "=== KẾT THÚC TẢI SCHEDULE ===");
            }
        });
    }

    /**
     * Validate and cleanup schedule items - remove items with deleted workouts
     */
    private void validateAndCleanupSchedule(Schedule schedule, OnScheduleValidatedListener listener) {
        if (schedule == null || schedule.getScheduleItems() == null || schedule.getScheduleItems().isEmpty()) {
            listener.onScheduleValidated(schedule);
            return;
        }
        
        List<Schedule.ScheduleItem> items = schedule.getScheduleItems();
        final List<Schedule.ScheduleItem> validItems = new ArrayList<>();
        final int[] checkedCount = {0};
        final int totalCount = items.size();
        
        if (totalCount == 0) {
            listener.onScheduleValidated(schedule);
            return;
        }
        
        // Check each schedule item's workout
        for (Schedule.ScheduleItem item : items) {
            String workoutId = item.getWorkoutId();
            if (workoutId == null || workoutId.isEmpty()) {
                checkedCount[0]++;
                if (checkedCount[0] == totalCount) {
                    // All items checked, cleanup if needed
                    cleanupScheduleIfNeeded(schedule, validItems, listener);
                }
                continue;
            }
            
            // Check if workout exists
            checkWorkoutExists(workoutId, exists -> {
                synchronized (validItems) {
                    if (exists) {
                        validItems.add(item);
                        Log.d(TAG, "Workout tồn tại: " + workoutId);
                    } else {
                        Log.d(TAG, "Workout không tồn tại (đã bị xóa): " + workoutId);
                    }
                    
                    checkedCount[0]++;
                    if (checkedCount[0] == totalCount) {
                        // All items checked, cleanup if needed
                        cleanupScheduleIfNeeded(schedule, validItems, listener);
                    }
                }
            });
        }
    }
    
    /**
     * Check if workout exists in either workouts_templates or user_workouts
     */
    private void checkWorkoutExists(String workoutId, OnWorkoutExistsListener listener) {
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            listener.onWorkoutExists(false);
            return;
        }
        
        androidx.appcompat.app.AppCompatActivity activity = 
            (androidx.appcompat.app.AppCompatActivity) getActivity();
        
        // Try WorkoutTemplate first
        FirebaseService.getInstance().loadWorkoutTemplateById(workoutId, activity, template -> {
            if (template != null && template.getTitle() != null && !template.getTitle().isEmpty()) {
                listener.onWorkoutExists(true);
            } else {
                // Try UserWorkout
                FirebaseService.getInstance().loadUserWorkoutById(workoutId, activity, userWorkout -> {
                    listener.onWorkoutExists(userWorkout != null && 
                                           userWorkout.getTitle() != null && 
                                           !userWorkout.getTitle().isEmpty());
                });
            }
        });
    }
    
    /**
     * Cleanup schedule if there are deleted workouts
     */
    private void cleanupScheduleIfNeeded(Schedule schedule, List<Schedule.ScheduleItem> validItems, 
                                        OnScheduleValidatedListener listener) {
        if (validItems.size() == schedule.getScheduleItems().size()) {
            // No items were removed, schedule is valid
            listener.onScheduleValidated(schedule);
        } else {
            // Some items were removed, update schedule in database
            Log.d(TAG, "Đang dọn dẹp schedule: xóa " + 
                (schedule.getScheduleItems().size() - validItems.size()) + " workout items đã bị xóa");
            
            Schedule cleanedSchedule = new Schedule();
            cleanedSchedule.setId(schedule.getId());
            cleanedSchedule.setUid(schedule.getUid());
            cleanedSchedule.setTitle(schedule.getTitle());
            cleanedSchedule.setTimezone(schedule.getTimezone());
            cleanedSchedule.setScheduleItems(validItems);
            cleanedSchedule.setNotification(schedule.getNotification());
            
            FirebaseService.getInstance().saveSchedule(cleanedSchedule, success -> {
                if (success) {
                    Log.d(TAG, "Đã dọn dẹp schedule thành công");
                    listener.onScheduleValidated(cleanedSchedule);
                } else {
                    Log.e(TAG, "Không thể dọn dẹp schedule, sử dụng schedule gốc");
                    listener.onScheduleValidated(schedule);
                }
            });
        }
    }
    
    /**
     * Interface for workout exists check callback
     */
    private interface OnWorkoutExistsListener {
        void onWorkoutExists(boolean exists);
    }
    
    /**
     * Interface for schedule validated callback
     */
    private interface OnScheduleValidatedListener {
        void onScheduleValidated(Schedule schedule);
    }

    /**
     * Update UI with schedule information for selected day
     */
    private void updateScheduleUI() {
        if (userSchedule == null || userSchedule.getScheduleItems() == null) {
            return;
        }

        int selectedDay = selectedDayCalendar.get(Calendar.DAY_OF_WEEK);
        // Convert Calendar day (Sunday=1, Monday=2, ...) to Schedule format (Monday=1, Tuesday=2, ..., Sunday=7)
        int scheduleDayOfWeek = convertCalendarDayToScheduleDay(selectedDay);

        List<Schedule.ScheduleItem> itemsForSelectedDay = new ArrayList<>();
        
        // Only show valid schedule items (deleted workouts already filtered out)
        for (Schedule.ScheduleItem item : userSchedule.getScheduleItems()) {
            if (item.getDayOfWeek() != null && item.getDayOfWeek().contains(scheduleDayOfWeek)) {
                itemsForSelectedDay.add(item);
            }
        }

        // You can display scheduled workouts here
        if (binding != null && !itemsForSelectedDay.isEmpty()) {
            Log.d(TAG, "Tìm thấy " + itemsForSelectedDay.size() + " workout đã lên lịch cho ngày được chọn");
            // TODO: Display scheduled workouts in UI
            // For example, show them in a separate RecyclerView or add indicators
        }
    }

    /**
     * Convert Calendar day of week to Schedule day of week format
     * Calendar: Sunday=1, Monday=2, ..., Saturday=7
     * Schedule: Monday=1, Tuesday=2, ..., Sunday=7
     */
    private int convertCalendarDayToScheduleDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
            default:
                return 1;
        }
    }

    /**
     * Convert Schedule day of week to Calendar day of week format
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
     * Update activity information display with real data from sessions
     */
    private void updateActivityInfo() {
        // Calculate total time and calories for the selected day
        int totalSeconds = 0;
        int totalCalories = 0;

        if (filteredSessionsForSelectedDay != null && !filteredSessionsForSelectedDay.isEmpty()) {
            for (Session session : filteredSessionsForSelectedDay) {
                if (session != null) {
                    // Get duration from summary
                    if (session.getSummary() != null) {
                        totalSeconds += session.getSummary().getDurationSec();
                        totalCalories += session.getSummary().getEstKcal();
                    } else {
                        // Fallback: calculate from startedAt and endedAt
                        if (session.getStartedAt() > 0 && session.getEndedAt() > 0) {
                            totalSeconds += (int)(session.getEndedAt() - session.getStartedAt());
                        }
                    }
                }
            }
        }

        // Convert seconds to minutes
        int totalMinutes = totalSeconds / 60;
        
        // Update UI
        if (binding != null) {
            binding.tvTime.setText("Thời gian: " + totalMinutes + " phút");
            binding.tvCalories.setText("Calo: " + totalCalories + " kcal");
            
            // Update weekly progress
            updateWeeklyProgress();
        }
    }

    /**
     * Update weekly progress bar
     */
    private void updateWeeklyProgress() {
        if (weeklyWorkoutStatus == null) {
            return;
        }
        
        int completedDays = 0;
        int totalPastDays = 0;
        
        // Get current date for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get start of current week (Monday)
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        int[] calendarDays = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        };
        
        // Count completed days and total past days using actual dates
        for (int i = 0; i < dayKeys.length; i++) {
            int dayOfWeek = calendarDays[i];
            
            // Calculate the actual date for this day in the current week
            Calendar dayDate = (Calendar) startOfWeek.clone();
            int daysToAdd = (dayOfWeek - Calendar.MONDAY + 7) % 7;
            dayDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
            
            // Compare actual dates, not just day of week numbers
            int dateComparison = dayDate.compareTo(today);
            
            // Only count past days and today
            if (dateComparison <= 0) {
                totalPastDays++;
                
                // Check if this day has a workout
                Boolean workoutStatus = weeklyWorkoutStatus.get(dayKeys[i]);
                if (workoutStatus != null && workoutStatus) {
                    completedDays++;
                }
            }
        }
        
        // Update progress bar
        if (totalPastDays > 0 && binding != null) {
            int progress = (completedDays * 100) / totalPastDays;
            binding.progressBarWeekly.setProgress(progress);
            binding.tvWeeklyProgress.setText("Tiến độ: " + completedDays + "/" + totalPastDays + " ngày");
        } else if (binding != null) {
            binding.progressBarWeekly.setProgress(0);
            binding.tvWeeklyProgress.setText("Tiến độ: 0/0 ngày");
        }
    }

    /**
     * Load sessions from Firebase Firestore for current user
     */
    private void loadSessions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user, cannot load sessions");
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem lịch sử tập luyện", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "=== LOADING SESSIONS ===");
        Log.d(TAG, "Loading sessions for uid: " + uid + " from Firestore...");

        FirebaseService.getInstance().loadUserSessions(uid, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnSessionsLoadedListener() {
            @Override
            public void onSessionsLoaded(ArrayList<Session> loadedSessions) {
                Log.d(TAG, "=== SESSIONS LOADED ===");
                Log.d(TAG, "Received " + (loadedSessions != null ? loadedSessions.size() : "null") + " sessions");

                if (loadedSessions != null && !loadedSessions.isEmpty()) {
                    for (int i = 0; i < loadedSessions.size(); i++) {
                        Session session = loadedSessions.get(i);
                        Log.d(TAG, "Session " + i + ": ID=" + session.getId() +
                                ", UID=" + session.getUid() +
                                ", StartedAt=" + session.getStartedAt());
                    }
                }

                sessions = loadedSessions != null ? loadedSessions : new ArrayList<>();
                
                // Analyze sessions and update weekly status
                analyzeWeeklySessions();
                
                // Filter sessions for selected day
                filterSessionsForSelectedDay();
                
                // Update activity info after loading sessions
                updateActivityInfo();
                
                Log.d(TAG, "Sessions loaded: " + sessions.size() + " sessions");
                Log.d(TAG, "=== END LOADING SESSIONS ===");
            }
        });
    }

    /**
     * Analyze sessions for the current week and update workout status
     */
    private void analyzeWeeklySessions() {
        // Safety check: ensure weeklyWorkoutStatus is initialized
        if (weeklyWorkoutStatus == null) {
            Log.w(TAG, "weeklyWorkoutStatus is null in analyzeWeeklySessions, initializing...");
            weeklyWorkoutStatus = new HashMap<>();
        }
        
        // Clear previous status
        weeklyWorkoutStatus.clear();
        
        // Initialize all days as no workout
        initializeWeeklyStatus();
        
        // Check each session with additional safety checks
        if (sessions != null) {
            for (Session session : sessions) {
                if (session != null && session.getStartedAt() > 0) {
                    try {
                        long sessionTimestamp = session.getStartedAt();
                        // Convert seconds to milliseconds for Date constructor
                        Date sessionDate = new Date(sessionTimestamp * 1000);
                        Calendar sessionCalendar = Calendar.getInstance();
                        sessionCalendar.setTime(sessionDate);
                        
                        // Check if session is in current week
                        if (isDateInCurrentWeek(sessionCalendar)) {
                            int dayOfWeek = sessionCalendar.get(Calendar.DAY_OF_WEEK);
                            String dayKey = getDayKeyFromCalendarDay(dayOfWeek);
                            if (dayKey != null && weeklyWorkoutStatus != null) {
                                weeklyWorkoutStatus.put(dayKey, true);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing session: " + e.getMessage());
                    }
                }
            }
        }
        
        // Update UI with workout status
        updateWeeklyStatus();
    }

    /**
     * Check if a date is in the current week
     */
    private boolean isDateInCurrentWeek(Calendar dateCalendar) {
        Calendar startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
        endOfWeek.set(Calendar.MINUTE, 59);
        endOfWeek.set(Calendar.SECOND, 59);
        endOfWeek.set(Calendar.MILLISECOND, 999);
        
        return (dateCalendar.after(startOfWeek) || dateCalendar.equals(startOfWeek)) && 
               (dateCalendar.before(endOfWeek) || dateCalendar.equals(endOfWeek));
    }

    /**
     * Get day key from Calendar day of week
     */
    private String getDayKeyFromCalendarDay(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "monday";
            case Calendar.TUESDAY:
                return "tuesday";
            case Calendar.WEDNESDAY:
                return "wednesday";
            case Calendar.THURSDAY:
                return "thursday";
            case Calendar.FRIDAY:
                return "friday";
            case Calendar.SATURDAY:
                return "saturday";
            case Calendar.SUNDAY:
                return "sunday";
            default:
                return "monday";
        }
    }

    /**
     * Update weekly status UI based on workout data
     */
    private void updateWeeklyStatus() {
        // Safety check: ensure weeklyWorkoutStatus is initialized
        if (weeklyWorkoutStatus == null) {
            Log.w(TAG, "weeklyWorkoutStatus is null, initializing...");
            weeklyWorkoutStatus = new HashMap<>();
            initializeWeeklyStatus();
        }
        
        // Get UI elements
        View[] circles = {
            binding.circle1, binding.circle2, binding.circle3, binding.circle4,
            binding.circle5, binding.circle6, binding.circle7
        };
        
        android.widget.ImageView[] statusIcons = {
            binding.ivStatus1, binding.ivStatus2, binding.ivStatus3, binding.ivStatus4,
            binding.ivStatus5, binding.ivStatus6, binding.ivStatus7
        };
        
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        
        // Get current date for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get start of current week (Monday)
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        
        // Update each day's status with date logic
        for (int i = 0; i < circles.length; i++) {
            // Convert array index to Calendar day of week
            int[] calendarDays = {
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            };
            int dayOfWeek = calendarDays[i];
            
            // Calculate the actual date for this day in the current week
            Calendar dayDate = (Calendar) startOfWeek.clone();
            int daysToAdd = (dayOfWeek - Calendar.MONDAY + 7) % 7;
            dayDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
            
            // Safe null check: if key doesn't exist or value is null, default to false
            Boolean workoutStatus = weeklyWorkoutStatus.get(dayKeys[i]);
            boolean hasWorkout = (workoutStatus != null) ? workoutStatus : false;
            
            // Compare actual dates, not just day of week numbers
            int dateComparison = dayDate.compareTo(today);
            
            if (dateComparison < 0) {
                // Past day - show workout status
                if (hasWorkout) {
                    // Had workout - green circle with check mark
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_done);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_check);
                    statusIcons[i].setVisibility(View.VISIBLE);
                } else {
                    // No workout - red circle with X mark
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_missed);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_close);
                    statusIcons[i].setVisibility(View.VISIBLE);
                }
            } else if (dateComparison == 0) {
                // Today - show workout status
                if (hasWorkout) {
                    // Has workout today - green circle with check mark
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_done);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_check);
                    statusIcons[i].setVisibility(View.VISIBLE);
                } else {
                    // No workout today - red circle with X mark
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_missed);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_close);
                    statusIcons[i].setVisibility(View.VISIBLE);
                }
            } else {
                // Future day - show empty circle (no status)
                circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_empty);
                statusIcons[i].setVisibility(View.GONE);
            }
        }
        
    }

    /**
     * Initialize weekly status with default values
     */
    private void initializeWeeklyStatus() {
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : dayKeys) {
            weeklyWorkoutStatus.put(day, false);
        }
    }

    /**
     * Highlight the selected day with bold text
     */
    private void highlightSelectedDay() {
        if (binding == null) {
            return;
        }
        
        // Get all day TextViews
        TextView[] dayTextViews = {
            binding.tvDay1, binding.tvDay2, binding.tvDay3, binding.tvDay4,
            binding.tvDay5, binding.tvDay6, binding.tvDay7
        };
        
        // Calendar day constants matching the order in days_container
        int[] calendarDays = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        };
        
        // Find index of selected day
        int selectedIndex = -1;
        for (int i = 0; i < calendarDays.length; i++) {
            if (calendarDays[i] == currentDayOfWeek) {
                selectedIndex = i;
                break;
            }
        }
        
        // Update text style for all days
        for (int i = 0; i < dayTextViews.length; i++) {
            if (dayTextViews[i] != null) {
                if (i == selectedIndex) {
                    // Selected day - make it bold
                    dayTextViews[i].setTypeface(null, android.graphics.Typeface.BOLD);
                    dayTextViews[i].setTextColor(getResources().getColor(fpt.fall2025.posetrainer.R.color.hw_primary, null));
                } else {
                    // Other days - normal weight
                    dayTextViews[i].setTypeface(null, android.graphics.Typeface.NORMAL);
                    dayTextViews[i].setTextColor(getResources().getColor(fpt.fall2025.posetrainer.R.color.hw_text_secondary, null));
                }
            }
        }
    }
}
