package fpt.fall2025.posetrainer.Dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Adapter.ScheduleAdapter;
import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Dialog to display user's scheduled workouts
 */
public class ViewScheduleDialog extends DialogFragment {
    private static final String TAG = "ViewScheduleDialog";
    private RecyclerView recyclerViewSchedules;
    private TextView tvEmptySchedule;
    private View layoutEmptySchedule;
    private ImageButton btnClose;
    private AppCompatButton btnFilter;
    private ScheduleAdapter adapter;
    private Schedule userSchedule;
    private String currentFilter = "Tất cả"; // "Tất cả", "Đã qua", "Chưa đến"
    private List<Schedule.ScheduleItem> allScheduleItems = new ArrayList<>();
    private List<String> allWorkoutNames = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_view_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewSchedules = view.findViewById(R.id.recycler_view_schedules);
        tvEmptySchedule = view.findViewById(R.id.tv_empty_schedule);
        layoutEmptySchedule = view.findViewById(R.id.layout_empty_schedule);
        btnClose = view.findViewById(R.id.btn_close);
        btnFilter = view.findViewById(R.id.btn_filter);

        // Setup RecyclerView
        adapter = new ScheduleAdapter(new ArrayList<>(), new ArrayList<>());
        recyclerViewSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSchedules.setAdapter(adapter);

        // Setup filter button
        btnFilter.setOnClickListener(v -> showFilterMenu(v));

        // Load schedule
        loadUserSchedule();

        // Close button
        btnClose.setOnClickListener(v -> dismiss());
    }

    /**
     * Show filter popup menu
     */
    private void showFilterMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 0, 0, "Tất cả");
        popupMenu.getMenu().add(0, 1, 1, "Đã qua");
        popupMenu.getMenu().add(0, 2, 2, "Chưa đến");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                currentFilter = "Tất cả";
            } else if (id == 1) {
                currentFilter = "Đã qua";
            } else if (id == 2) {
                currentFilter = "Chưa đến";
            }
            btnFilter.setText(currentFilter);
            applyFilter();
            return true;
        });
        
        popupMenu.setGravity(Gravity.START);
        popupMenu.show();
    }

    /**
     * Apply filter to schedule items
     */
    private void applyFilter() {
        if (allScheduleItems.isEmpty()) {
            return;
        }
        
        List<Schedule.ScheduleItem> filteredItems = new ArrayList<>();
        List<String> filteredNames = new ArrayList<>();
        
        if ("Tất cả".equals(currentFilter)) {
            filteredItems.addAll(allScheduleItems);
            filteredNames.addAll(allWorkoutNames);
        } else if ("Đã qua".equals(currentFilter)) {
            adapter.filterItems(allScheduleItems, allWorkoutNames, ScheduleAdapter.FilterMode.PAST, filteredItems, filteredNames);
        } else if ("Chưa đến".equals(currentFilter)) {
            adapter.filterItems(allScheduleItems, allWorkoutNames, ScheduleAdapter.FilterMode.FUTURE, filteredItems, filteredNames);
        }
        
        adapter.updateSchedules(filteredItems, filteredNames);
    }

    /**
     * Load user schedule from Firestore
     */
    private void loadUserSchedule() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in");
            showEmptyState();
            return;
        }

        Log.d(TAG, "Loading schedule for user: " + currentUser.getUid());
        FirebaseService.getInstance().loadUserSchedule(currentUser.getUid(), schedule -> {
            Log.d(TAG, "Schedule loaded: " + (schedule != null ? "not null" : "null"));
            if (schedule != null) {
                Log.d(TAG, "Schedule items count: " + 
                    (schedule.getScheduleItems() != null ? schedule.getScheduleItems().size() : 0));
            }
            
            if (schedule != null && schedule.getScheduleItems() != null && !schedule.getScheduleItems().isEmpty()) {
                userSchedule = schedule;
                allScheduleItems = new ArrayList<>(schedule.getScheduleItems());
                displaySchedules(schedule);
            } else {
                Log.d(TAG, "No schedule items found, showing empty state");
                showEmptyState();
            }
        });
    }

    /**
     * Display schedules in RecyclerView with filtering
     */
    private void displaySchedules(Schedule schedule) {
        List<Schedule.ScheduleItem> items = schedule.getScheduleItems();
        
        Log.d(TAG, "Displaying " + items.size() + " schedule items");
        
        // Show RecyclerView immediately
        recyclerViewSchedules.setVisibility(View.VISIBLE);
        if (layoutEmptySchedule != null) {
            layoutEmptySchedule.setVisibility(View.GONE);
        }
        
        // Update adapter immediately with workout IDs (will be updated with names later)
        List<String> initialNames = new ArrayList<>();
        for (Schedule.ScheduleItem item : items) {
            initialNames.add(item.getWorkoutId() != null ? item.getWorkoutId() : "Đang tải...");
        }
        
        adapter.updateSchedules(items, initialNames);
        Log.d(TAG, "Adapter updated immediately with " + items.size() + " items");
        
        // Load workout names and update adapter again
        loadWorkoutNames(items, workoutNames -> {
            Log.d(TAG, "Workout names loaded: " + workoutNames.size());
            
            // Filter out schedule items with deleted workouts
            List<Schedule.ScheduleItem> validItems = new ArrayList<>();
            List<String> validNames = new ArrayList<>();
            int deletedCount = 0;
            
            for (int i = 0; i < items.size() && i < workoutNames.size(); i++) {
                String workoutName = workoutNames.get(i);
                // Bỏ qua các schedule items có workout đã bị xóa hoặc không xác định
                if ("Bài tập đã bị xóa".equals(workoutName) || 
                    "Bài tập không xác định".equals(workoutName) ||
                    "Không xác định".equals(workoutName)) {
                    deletedCount++;
                    Log.d(TAG, "Filtering out schedule item at index " + i + " with deleted/invalid workout: " + workoutName);
                } else {
                    validItems.add(items.get(i));
                    validNames.add(workoutName);
                }
            }
            
            // Nếu có workout đã bị xóa, cập nhật schedule trong database để cleanup
            // Create final variables for use in lambda
            final int finalDeletedCount = deletedCount;
            final List<Schedule.ScheduleItem> finalValidItems = new ArrayList<>(validItems);
            final List<String> finalValidNames = new ArrayList<>(validNames);
            
            if (finalDeletedCount > 0 && userSchedule != null && finalValidItems.size() != items.size()) {
                Log.d(TAG, "Found " + finalDeletedCount + " deleted/invalid workout(s), cleaning up schedule...");
                userSchedule.setScheduleItems(finalValidItems);
                FirebaseService.getInstance().saveSchedule(userSchedule, success -> {
                    if (success) {
                        Log.d(TAG, "Schedule cleaned up successfully, removed " + finalDeletedCount + " deleted workout items");
                        // Reload schedule to reflect changes
                        loadUserSchedule();
                    } else {
                        Log.e(TAG, "Failed to clean up schedule, but still filtering display");
                        // Still update adapter with valid items only
                        allScheduleItems = finalValidItems;
                        allWorkoutNames = finalValidNames;
                        adapter.updateSchedules(finalValidItems, finalValidNames);
                        
                        // Check if we need to show empty state
                        if (finalValidItems.isEmpty()) {
                            showEmptyState();
                        }
                    }
                });
                return; // Don't update adapter here, wait for reload
            }
            
            // Update adapter with valid items only (không hiển thị bài tập đã xóa)
            allScheduleItems = validItems;
            allWorkoutNames = validNames;
            adapter.updateSchedules(validItems, validNames);
            Log.d(TAG, "Adapter updated with " + validItems.size() + " valid workout names (filtered out " + deletedCount + " deleted/invalid workouts)");
            
            // Check if we need to show empty state
            if (validItems.isEmpty()) {
                showEmptyState();
            }
        });
    }

    /**
     * Load workout names for schedule items
     */
    private void loadWorkoutNames(List<Schedule.ScheduleItem> items, OnWorkoutNamesLoadedListener listener) {
        if (items == null || items.isEmpty()) {
            listener.onWorkoutNamesLoaded(new ArrayList<>());
            return;
        }
        
        int totalCount = items.size();
        final List<String> workoutNames = new ArrayList<>(totalCount);
        // Initialize list with placeholders
        for (int i = 0; i < totalCount; i++) {
            workoutNames.add("Đang tải...");
        }
        
        final int[] loadedCount = {0};
        final Object lock = new Object();
        
        // Load workout name for each item
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            Schedule.ScheduleItem item = items.get(i);
            String workoutId = item.getWorkoutId();
            
            if (workoutId == null || workoutId.isEmpty()) {
                synchronized (lock) {
                    workoutNames.set(index, "Không xác định");
                    loadedCount[0]++;
                    if (loadedCount[0] == totalCount) {
                        Log.d(TAG, "All workout names loaded (sync), updating adapter");
                        listener.onWorkoutNamesLoaded(new ArrayList<>(workoutNames));
                    }
                }
                continue;
            }
            
            Log.d(TAG, "Loading workout name for ID: " + workoutId + " at index: " + index);
            
            // Load workout name by ID
            loadWorkoutNameById(workoutId, workoutName -> {
                synchronized (lock) {
                    String finalName = workoutName != null ? workoutName : workoutId;
                    workoutNames.set(index, finalName);
                    loadedCount[0]++;
                    Log.d(TAG, "Loaded workout name for index " + index + ": " + finalName + " (loaded: " + loadedCount[0] + "/" + totalCount + ")");
                    
                    if (loadedCount[0] == totalCount) {
                        Log.d(TAG, "All workout names loaded, updating adapter with " + workoutNames.size() + " names");
                        listener.onWorkoutNamesLoaded(new ArrayList<>(workoutNames));
                    }
                }
            });
        }
    }

    /**
     * Load workout name by ID from WorkoutTemplate or UserWorkout
     * Tries to load from workout_templates collection first, then falls back to user_workouts collection
     * Hiển thị title từ cả hai bảng: workouts_templates.title và user_workouts.title
     */
    private void loadWorkoutNameById(String workoutId, OnWorkoutNameLoadedListener listener) {
        if (workoutId == null || workoutId.isEmpty()) {
            Log.w(TAG, "WorkoutId is null or empty");
            listener.onWorkoutNameLoaded("Không xác định");
            return;
        }
        
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            Log.w(TAG, "Activity is null, returning default name");
            listener.onWorkoutNameLoaded("Không xác định");
            return;
        }
        
        androidx.appcompat.app.AppCompatActivity activity = 
            (androidx.appcompat.app.AppCompatActivity) getActivity();
        
        Log.d(TAG, "Loading workout name (title) for ID: " + workoutId + 
            " (trying workouts_templates.title first, then user_workouts.title)");
        
        // Bước 1: Thử load từ workouts_templates collection (lấy field title)
        FirebaseService.getInstance().loadWorkoutTemplateById(workoutId, activity, template -> {
            if (template != null && template.getTitle() != null && !template.getTitle().isEmpty()) {
                // Thành công: Đã tìm thấy trong workouts_templates, lấy title
                String title = template.getTitle();
                Log.d(TAG, "✓ Loaded workout title from workouts_templates: \"" + title + "\"");
                listener.onWorkoutNameLoaded(title);
            } else {
                // Không tìm thấy trong workouts_templates hoặc title rỗng
                // Bước 2: Thử load từ user_workouts collection (lấy field title)
                Log.d(TAG, "✗ WorkoutTemplate not found or title empty for ID: " + workoutId + 
                    ", trying user_workouts collection...");
                
                FirebaseService.getInstance().loadUserWorkoutById(workoutId, activity, userWorkout -> {
                    if (userWorkout != null && userWorkout.getTitle() != null && !userWorkout.getTitle().isEmpty()) {
                        // Thành công: Đã tìm thấy trong user_workouts, lấy title
                        String title = userWorkout.getTitle();
                        Log.d(TAG, "✓ Loaded workout title from user_workouts: \"" + title + "\"");
                        listener.onWorkoutNameLoaded(title);
                    } else {
                        // Không tìm thấy trong cả hai bảng - có thể workout đã bị xóa
                        // Kiểm tra xem workoutId có prefix "uw_" (UserWorkout) không để hiển thị thông báo phù hợp
                        String displayName;
                        if (workoutId != null && workoutId.startsWith("uw_")) {
                            // Đây là UserWorkout đã bị xóa
                            displayName = "Bài tập đã bị xóa";
                            Log.w(TAG, "✗ UserWorkout không tồn tại (có thể đã bị xóa): " + workoutId);
                        } else {
                            // Đây là WorkoutTemplate không tồn tại
                            displayName = "Bài tập không xác định";
                            Log.w(TAG, "✗ WorkoutTemplate không tồn tại: " + workoutId);
                        }
                        listener.onWorkoutNameLoaded(displayName);
                    }
                });
            }
        });
    }

    /**
     * Interface for workout name loading callback
     */
    private interface OnWorkoutNameLoadedListener {
        void onWorkoutNameLoaded(String workoutName);
    }

    /**
     * Interface for workout names loading callback
     */
    private interface OnWorkoutNamesLoadedListener {
        void onWorkoutNamesLoaded(List<String> workoutNames);
    }

    /**
     * Show empty state
     */
    private void showEmptyState() {
        recyclerViewSchedules.setVisibility(View.GONE);
        if (layoutEmptySchedule != null) {
            layoutEmptySchedule.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Set dialog width to 90% of screen width
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            // Set max height to prevent dialog from being too tall
            int maxHeight = (int)(getResources().getDisplayMetrics().heightPixels * 0.70);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}

