package fpt.fall2025.posetrainer.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentDailyBinding;

public class DailyFragment extends Fragment {
    private static final String TAG = "DailyFragment";
    private FragmentDailyBinding binding;
    private ArrayList<Session> sessions;
    private int currentDayOfWeek; // Current day of week (1-7, Sunday=1)
    private Calendar calendar;
    private Map<String, Boolean> weeklyWorkoutStatus; // Track workout status for each day

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDailyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize data
        sessions = new ArrayList<>();
        calendar = Calendar.getInstance();
        currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        weeklyWorkoutStatus = new HashMap<>();

        // Setup UI
        setupHeader();
        setupWeeklyGoal();
        setupActivities();
        
        // Load sessions from Firestore
        loadSessions();
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
        
        // Update activity info for selected day
        updateActivityInfo();
        
        // Refresh weekly status to show current selection
        updateWeeklyStatus();
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
            // Navigate to history
            Toast.makeText(getContext(), "Lịch sử", Toast.LENGTH_SHORT).show();
        });

        binding.btnStartWorkout.setOnClickListener(v -> {
            // Start workout
            Toast.makeText(getContext(), "Bắt đầu tập luyện", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddActivity.setOnClickListener(v -> {
            // Add activity
            Toast.makeText(getContext(), "Thêm hoạt động", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Update activity information display
     */
    private void updateActivityInfo() {
        // Calculate total time and calories for the selected day
        int totalMinutes = 0;
        int totalCalories = 0;

        for (Session session : sessions) {
            // Filter sessions for the selected day
            // This is a simplified example - you might want to filter by actual date
            if (session != null) {
                totalMinutes += 30; // Example: 30 minutes per session
                totalCalories += 150; // Example: 150 calories per session
            }
        }

        // Update UI
        binding.tvTime.setText("Thời gian: " + totalMinutes + " phút");
        binding.tvCalories.setText("Calo: " + totalCalories + " kcal");
    }

    /**
     * Load sessions from Firebase Firestore
     */
    private void loadSessions() {
        Log.d(TAG, "=== LOADING SESSIONS ===");
        Log.d(TAG, "Loading sessions for uid_1 from Firestore...");

        FirebaseService.getInstance().loadUserSessions("uid_1", (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnSessionsLoadedListener() {
            @Override
            public void onSessionsLoaded(ArrayList<Session> loadedSessions) {
                Log.d(TAG, "=== SESSIONS LOADED ===");
                Log.d(TAG, "Received " + (loadedSessions != null ? loadedSessions.size() : "null") + " sessions");

                if (loadedSessions != null) {
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
        
        return dateCalendar.after(startOfWeek) && dateCalendar.before(endOfWeek);
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
        int todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        
        // Update each day's status with date logic
        for (int i = 0; i < circles.length; i++) {
            // Convert array index to Calendar day of week
            int[] calendarDays = {
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            };
            int dayOfWeek = calendarDays[i];
            
            // Safe null check: if key doesn't exist or value is null, default to false
            Boolean workoutStatus = weeklyWorkoutStatus.get(dayKeys[i]);
            boolean hasWorkout = (workoutStatus != null) ? workoutStatus : false;
            
            // Determine day status based on date
            if (dayOfWeek < todayDayOfWeek) {
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
            } else if (dayOfWeek == todayDayOfWeek) {
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
        
        // Highlight current day
        highlightCurrentDay();
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
     * Highlight the current day with a different style
     */
    private void highlightCurrentDay() {
        // Safety check: ensure weeklyWorkoutStatus is initialized
        if (weeklyWorkoutStatus == null) {
            Log.w(TAG, "weeklyWorkoutStatus is null in highlightCurrentDay, initializing...");
            weeklyWorkoutStatus = new HashMap<>();
            initializeWeeklyStatus();
        }
        
        View[] circles = {
            binding.circle1, binding.circle2, binding.circle3, binding.circle4,
            binding.circle5, binding.circle6, binding.circle7
        };
        
        android.widget.ImageView[] statusIcons = {
            binding.ivStatus1, binding.ivStatus2, binding.ivStatus3, binding.ivStatus4,
            binding.ivStatus5, binding.ivStatus6, binding.ivStatus7
        };
        
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        
        // Convert current day to index
        int currentIndex = convertDayOfWeekToIndex(currentDayOfWeek);
        
        // Highlight current day with safe null check
        for (int i = 0; i < circles.length; i++) {
            if (i == currentIndex) {
                // Safe null check: if key doesn't exist or value is null, default to false
                Boolean workoutStatus = weeklyWorkoutStatus.get(dayKeys[i]);
                boolean hasWorkout = (workoutStatus != null) ? workoutStatus : false;
                
                // Current day gets special highlighting with blue border
                if (hasWorkout) {
                    // Current day with workout - green circle with blue border effect
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_done);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_check);
                } else {
                    // Current day without workout - red circle with blue border effect  
                    circles[i].setBackgroundResource(fpt.fall2025.posetrainer.R.drawable.circle_workout_missed);
                    statusIcons[i].setImageResource(fpt.fall2025.posetrainer.R.drawable.ic_close);
                }
                statusIcons[i].setVisibility(View.VISIBLE);
                
                // Add special border or highlight for current day
                // You can add additional styling here if needed
            }
        }
    }
}
