package fpt.fall2025.posetrainer.Dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
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
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmptySchedule;
    private View layoutEmptySchedule;
    private View layoutStatistics;
    private TextView tvTotalCount, tvPastCount, tvFutureCount;
    private ImageButton btnClose;
    private AppCompatButton btnFilter;
    private Button btnAddNew;
    private EditText etSearch;
    private ScheduleAdapter adapter;
    private Schedule userSchedule;
    private String currentFilter = "Tất cả"; // "Tất cả", "Đã qua", "Chưa đến"
    private String currentSearchQuery = "";
    private String currentSort = "Thời gian ↑"; // "Thời gian ↑", "Thời gian ↓", "Tên ↑", "Tên ↓", "Ngày ↑", "Ngày ↓"
    private AppCompatButton btnSort;
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
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmptySchedule = view.findViewById(R.id.tv_empty_schedule);
        layoutEmptySchedule = view.findViewById(R.id.layout_empty_schedule);
        layoutStatistics = view.findViewById(R.id.layout_statistics);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvPastCount = view.findViewById(R.id.tv_past_count);
        tvFutureCount = view.findViewById(R.id.tv_future_count);
        btnClose = view.findViewById(R.id.btn_close);
        btnFilter = view.findViewById(R.id.btn_filter);
        btnSort = view.findViewById(R.id.btn_sort);
        btnAddNew = view.findViewById(R.id.btn_add_new);
        etSearch = view.findViewById(R.id.et_search);
        
        // Setup swipe refresh
        swipeRefresh.setColorSchemeColors(0xFF4d9df2);
        swipeRefresh.setOnRefreshListener(() -> {
            loadUserSchedule();
            swipeRefresh.setRefreshing(false);
        });

        // Setup RecyclerView
        adapter = new ScheduleAdapter(new ArrayList<>(), new ArrayList<>());
        recyclerViewSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSchedules.setAdapter(adapter);
        
        // Setup click listeners
        adapter.setOnScheduleItemClickListener((item, position) -> {
            // Navigate to day in DailyFragment
            navigateToDay(item);
        });
        
        adapter.setOnScheduleItemLongClickListener((item, position, view1) -> {
            showItemMenu(item, position, view1);
            return true;
        });

        // Setup search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup filter button
        btnFilter.setOnClickListener(v -> showFilterMenu(v));
        
        // Setup sort button
        btnSort.setOnClickListener(v -> showSortMenu(v));
        
        // Setup add new button
        btnAddNew.setOnClickListener(v -> showCreateScheduleDialog());

        // Load schedule
        loadUserSchedule();

        // Close button
        btnClose.setOnClickListener(v -> dismiss());
    }

    /**
     * Show sort popup menu
     */
    private void showSortMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 0, 0, "Thời gian ↑");
        popupMenu.getMenu().add(0, 1, 1, "Thời gian ↓");
        popupMenu.getMenu().add(0, 2, 2, "Tên ↑");
        popupMenu.getMenu().add(0, 3, 3, "Tên ↓");
        popupMenu.getMenu().add(0, 4, 4, "Ngày ↑");
        popupMenu.getMenu().add(0, 5, 5, "Ngày ↓");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            String[] sortOptions = {"Thời gian ↑", "Thời gian ↓", "Tên ↑", "Tên ↓", "Ngày ↑", "Ngày ↓"};
            if (id >= 0 && id < sortOptions.length) {
                currentSort = sortOptions[id];
                btnSort.setText(currentSort);
                applyFilterAndSearch();
            }
            return true;
        });
        
        popupMenu.setGravity(Gravity.START);
        popupMenu.show();
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
     * Apply filter and search to schedule items
     */
    private void applyFilterAndSearch() {
        if (allScheduleItems.isEmpty()) {
            return;
        }
        
        List<Schedule.ScheduleItem> filteredItems = new ArrayList<>();
        List<String> filteredNames = new ArrayList<>();
        
        // Apply filter first
        if ("Tất cả".equals(currentFilter)) {
            filteredItems.addAll(allScheduleItems);
            filteredNames.addAll(allWorkoutNames);
        } else if ("Đã qua".equals(currentFilter)) {
            adapter.filterItems(allScheduleItems, allWorkoutNames, ScheduleAdapter.FilterMode.PAST, filteredItems, filteredNames);
        } else if ("Chưa đến".equals(currentFilter)) {
            adapter.filterItems(allScheduleItems, allWorkoutNames, ScheduleAdapter.FilterMode.FUTURE, filteredItems, filteredNames);
        }
        
        // Apply search if query exists
        if (!currentSearchQuery.isEmpty()) {
            List<Schedule.ScheduleItem> searchedItems = new ArrayList<>();
            List<String> searchedNames = new ArrayList<>();
            
            for (int i = 0; i < filteredItems.size(); i++) {
                String workoutName = (i < filteredNames.size()) ? filteredNames.get(i) : "";
                if (workoutName.toLowerCase().contains(currentSearchQuery)) {
                    searchedItems.add(filteredItems.get(i));
                    searchedNames.add(workoutName);
                }
            }
            
            filteredItems = searchedItems;
            filteredNames = searchedNames;
        }
        
        // Apply sort
        sortItems(filteredItems, filteredNames);
        
        adapter.updateSchedules(filteredItems, filteredNames);
        updateStatistics();
    }
    
    /**
     * Sort schedule items
     */
    private void sortItems(List<Schedule.ScheduleItem> items, List<String> names) {
        if (items.isEmpty() || "Thời gian ↑".equals(currentSort)) {
            // Default: sort by time ascending
            return;
        }
        
        // Create list of indices for sorting
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            indices.add(i);
        }
        
        // Sort indices based on current sort option
        indices.sort((i1, i2) -> {
            Schedule.ScheduleItem item1 = items.get(i1);
            Schedule.ScheduleItem item2 = items.get(i2);
            
            if ("Thời gian ↓".equals(currentSort)) {
                // Sort by time descending
                String time1 = item1.getTimeLocal() != null ? item1.getTimeLocal() : "00:00";
                String time2 = item2.getTimeLocal() != null ? item2.getTimeLocal() : "00:00";
                return time2.compareTo(time1);
            } else if ("Tên ↑".equals(currentSort)) {
                // Sort by name ascending
                String name1 = (i1 < names.size()) ? names.get(i1) : "";
                String name2 = (i2 < names.size()) ? names.get(i2) : "";
                return name1.compareToIgnoreCase(name2);
            } else if ("Tên ↓".equals(currentSort)) {
                // Sort by name descending
                String name1 = (i1 < names.size()) ? names.get(i1) : "";
                String name2 = (i2 < names.size()) ? names.get(i2) : "";
                return name2.compareToIgnoreCase(name1);
            } else if ("Ngày ↑".equals(currentSort)) {
                // Sort by first day ascending
                int day1 = item1.getDayOfWeek() != null && !item1.getDayOfWeek().isEmpty() 
                    ? item1.getDayOfWeek().get(0) : 0;
                int day2 = item2.getDayOfWeek() != null && !item2.getDayOfWeek().isEmpty() 
                    ? item2.getDayOfWeek().get(0) : 0;
                return Integer.compare(day1, day2);
            } else if ("Ngày ↓".equals(currentSort)) {
                // Sort by first day descending
                int day1 = item1.getDayOfWeek() != null && !item1.getDayOfWeek().isEmpty() 
                    ? item1.getDayOfWeek().get(0) : 0;
                int day2 = item2.getDayOfWeek() != null && !item2.getDayOfWeek().isEmpty() 
                    ? item2.getDayOfWeek().get(0) : 0;
                return Integer.compare(day2, day1);
            }
            
            return 0;
        });
        
        // Reorder items and names based on sorted indices
        List<Schedule.ScheduleItem> sortedItems = new ArrayList<>();
        List<String> sortedNames = new ArrayList<>();
        for (int index : indices) {
            sortedItems.add(items.get(index));
            if (index < names.size()) {
                sortedNames.add(names.get(index));
            }
        }
        
        items.clear();
        names.clear();
        items.addAll(sortedItems);
        names.addAll(sortedNames);
    }
    
    /**
     * Apply filter to schedule items (backward compatibility)
     */
    private void applyFilter() {
        applyFilterAndSearch();
    }
    
    /**
     * Show long press menu for schedule item
     */
    private void showItemMenu(Schedule.ScheduleItem item, int position, View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 0, 0, "✏️ Chỉnh sửa");
        popupMenu.getMenu().add(0, 2, 2, "🗑️ Xóa");
        popupMenu.getMenu().add(0, 3, 3, "🗓️ Xem ngày");
        
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == 0) {
                // Edit
                showEditScheduleDialog(item, position);
            } else if (id == 2) {
                // Delete
                deleteScheduleItem(position);
            } else if (id == 3) {
                // Navigate to day
                navigateToDay(item);
            }
            return true;
        });
        
        popupMenu.setGravity(Gravity.END);
        popupMenu.show();
    }
    
    /**
     * Show edit schedule dialog
     */
    private void showEditScheduleDialog(Schedule.ScheduleItem item, int position) {
        EditScheduleDialog dialog = EditScheduleDialog.newInstance(item, position);
        dialog.setOnScheduleUpdatedListener(new EditScheduleDialog.OnScheduleUpdatedListener() {
            @Override
            public void onScheduleUpdated(Schedule.ScheduleItem updatedItem, int index) {
                updateScheduleItem(updatedItem, index);
            }
            
            @Override
            public void onScheduleDeleted(int index) {
                deleteScheduleItem(index);
            }
        });
        dialog.show(getParentFragmentManager(), "EditScheduleDialog");
    }
    
    /**
     * Update schedule item
     */
    private void updateScheduleItem(Schedule.ScheduleItem updatedItem, int index) {
        if (userSchedule == null || index < 0 || index >= allScheduleItems.size()) {
            return;
        }
        
        // Update in local list
        allScheduleItems.set(index, updatedItem);
        
        // Update in schedule
        List<Schedule.ScheduleItem> items = userSchedule.getScheduleItems();
        if (items != null && index < items.size()) {
            items.set(index, updatedItem);
            userSchedule.setScheduleItems(items);
        }
        
        // Save to Firestore
        FirebaseService.getInstance().saveSchedule(userSchedule, success -> {
            if (success) {
                Toast.makeText(getContext(), "Đã cập nhật lịch tập", Toast.LENGTH_SHORT).show();
                // Reload to refresh UI
                loadUserSchedule();
            } else {
                Toast.makeText(getContext(), "Lỗi khi cập nhật lịch tập", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Delete schedule item
     */
    private void deleteScheduleItem(int index) {
        if (userSchedule == null || index < 0 || index >= allScheduleItems.size()) {
            return;
        }
        
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa lịch tập này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                // Tạo list mới để tránh reference issue
                List<Schedule.ScheduleItem> items = new ArrayList<>(userSchedule.getScheduleItems());
                if (items != null && index < items.size()) {
                    items.remove(index);
                    userSchedule.setScheduleItems(items);
                    
                    // Đảm bảo có ID để update thay vì tạo mới
                    if (userSchedule.getId() == null || userSchedule.getId().isEmpty()) {
                        Log.e(TAG, "Schedule ID is null, cannot update");
                        Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID lịch tập", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Save to Firestore với force update
                    FirebaseService.getInstance().saveSchedule(userSchedule, success -> {
                        if (success) {
                            Log.d(TAG, "Schedule item deleted successfully, reloading...");
                            Toast.makeText(getContext(), "Đã xóa lịch tập", Toast.LENGTH_SHORT).show();
                            
                            // Clear local cache trước khi reload
                            allScheduleItems.clear();
                            allWorkoutNames.clear();
                            
                            // Reload sau một chút để đảm bảo Firestore đã update
                            recyclerViewSchedules.postDelayed(() -> {
                                loadUserSchedule();
                            }, 500);
                        } else {
                            Log.e(TAG, "Failed to save schedule after deletion");
                            Toast.makeText(getContext(), "Lỗi khi xóa lịch tập", Toast.LENGTH_SHORT).show();
                            // Reload anyway để sync lại
                            loadUserSchedule();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Lỗi: Không tìm thấy lịch tập để xóa", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    
    /**
     * Navigate to day in DailyFragment
     */
    private void navigateToDay(Schedule.ScheduleItem item) {
        if (item.getDayOfWeek() == null || item.getDayOfWeek().isEmpty()) {
            return;
        }
        
        // Get first day of week from schedule item
        int scheduleDay = item.getDayOfWeek().get(0);
        
        // Convert Schedule day (Monday=1, ..., Sunday=7) to Calendar day (Sunday=1, Monday=2, ...)
        int calendarDay = convertScheduleDayToCalendarDay(scheduleDay);
        
        // Dismiss dialog first
        dismiss();
        
        // Navigate to DailyFragment and select day
        if (getActivity() != null && getActivity() instanceof fpt.fall2025.posetrainer.Activity.MainActivity) {
            fpt.fall2025.posetrainer.Activity.MainActivity mainActivity = 
                (fpt.fall2025.posetrainer.Activity.MainActivity) getActivity();
            mainActivity.navigateToDailyFragmentWithDay(calendarDay);
        }
    }
    
    /**
     * Convert Schedule day to Calendar day
     */
    private int convertScheduleDayToCalendarDay(int scheduleDay) {
        switch (scheduleDay) {
            case 1: return Calendar.MONDAY;
            case 2: return Calendar.TUESDAY;
            case 3: return Calendar.WEDNESDAY;
            case 4: return Calendar.THURSDAY;
            case 5: return Calendar.FRIDAY;
            case 6: return Calendar.SATURDAY;
            case 7: return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }
    
    /**
     * Show create schedule dialog
     */
    private void showCreateScheduleDialog() {
        CreateScheduleDialog dialog = new CreateScheduleDialog();
        dialog.setOnScheduleCreatedListener(scheduleItem -> {
            // Luôn load schedule từ Firestore trước để đảm bảo có dữ liệu mới nhất
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return;
            }
            
            FirebaseService.getInstance().loadUserSchedule(currentUser.getUid(), schedule -> {
                if (schedule != null && schedule.getScheduleItems() != null && !schedule.getScheduleItems().isEmpty()) {
                    // Có schedule cũ, thêm vào schedule đó
                    userSchedule = schedule;
                    List<Schedule.ScheduleItem> items = userSchedule.getScheduleItems();
                    if (items == null) {
                        items = new ArrayList<>();
                    }
                    items.add(scheduleItem);
                    userSchedule.setScheduleItems(items);
                    
                    FirebaseService.getInstance().saveSchedule(userSchedule, success -> {
                        if (success) {
                            Toast.makeText(getContext(), "Đã thêm lịch tập", Toast.LENGTH_SHORT).show();
                            loadUserSchedule();
                        } else {
                            Toast.makeText(getContext(), "Lỗi khi thêm lịch tập", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Không có schedule hoặc schedule rỗng
                    List<Schedule.ScheduleItem> items = new ArrayList<>();
                    items.add(scheduleItem);
                    
                    Schedule.NotificationSettings notificationSettings = new Schedule.NotificationSettings(
                        true, 15, "default"
                    );
                    
                    // Nếu có schedule document (dù rỗng), sử dụng ID của nó để update
                    Schedule newSchedule;
                    if (schedule != null && schedule.getId() != null && !schedule.getId().isEmpty()) {
                        // Update schedule cũ
                        newSchedule = new Schedule(
                            schedule.getId(), // Sử dụng ID cũ
                            currentUser.getUid(),
                            "Lịch tập của tôi",
                            java.util.TimeZone.getDefault().getID(),
                            items,
                            notificationSettings
                        );
                    } else {
                        // Tạo schedule mới
                        newSchedule = new Schedule(
                            null,
                            currentUser.getUid(),
                            "Lịch tập của tôi",
                            java.util.TimeZone.getDefault().getID(),
                            items,
                            notificationSettings
                        );
                    }
                    
                    FirebaseService.getInstance().saveSchedule(newSchedule, success -> {
                        if (success) {
                            Toast.makeText(getContext(), "Đã thêm lịch tập", Toast.LENGTH_SHORT).show();
                            loadUserSchedule();
                        } else {
                            Toast.makeText(getContext(), "Lỗi khi thêm lịch tập", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
        dialog.show(getParentFragmentManager(), "CreateScheduleDialog");
    }
    
    /**
     * Update statistics
     */
    private void updateStatistics() {
        if (layoutStatistics == null) {
            return;
        }
        
        int total = allScheduleItems.size();
        int past = 0;
        int future = 0;
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        
        for (Schedule.ScheduleItem item : allScheduleItems) {
            boolean isPast = isScheduleItemPast(item, today, startOfWeek);
            if (isPast) {
                past++;
            } else {
                future++;
            }
        }
        
        if (total > 0) {
            layoutStatistics.setVisibility(View.VISIBLE);
            tvTotalCount.setText("Tổng: " + total);
            tvPastCount.setText("Đã qua: " + past);
            tvFutureCount.setText("Sắp tới: " + future);
        } else {
            layoutStatistics.setVisibility(View.GONE);
        }
    }
    
    /**
     * Check if schedule item is past
     */
    private boolean isScheduleItemPast(Schedule.ScheduleItem item, Calendar today, Calendar startOfWeek) {
        if (item.getDayOfWeek() == null || item.getDayOfWeek().isEmpty()) {
            return false;
        }
        
        for (Integer dayOfWeek : item.getDayOfWeek()) {
            Calendar dayDate = (Calendar) startOfWeek.clone();
            int daysToAdd = (dayOfWeek - 1);
            dayDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
            
            if (dayDate.compareTo(today) <= 0) {
                return true;
            }
        }
        
        return false;
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
        
        // Clear local cache trước khi load
        allScheduleItems.clear();
        allWorkoutNames.clear();
        
        FirebaseService.getInstance().loadUserSchedule(currentUser.getUid(), schedule -> {
            Log.d(TAG, "Schedule loaded: " + (schedule != null ? "not null" : "null"));
            if (schedule != null) {
                Log.d(TAG, "Schedule ID: " + schedule.getId());
                Log.d(TAG, "Schedule items count: " + 
                    (schedule.getScheduleItems() != null ? schedule.getScheduleItems().size() : 0));
            }
            
            if (schedule != null && schedule.getScheduleItems() != null && !schedule.getScheduleItems().isEmpty()) {
                userSchedule = schedule;
                // Tạo list mới để tránh reference issue
                allScheduleItems = new ArrayList<>(schedule.getScheduleItems());
                displaySchedules(schedule);
            } else {
                Log.d(TAG, "No schedule items found, showing empty state");
                userSchedule = null;
                allScheduleItems.clear();
                allWorkoutNames.clear();
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
            
            // Apply filter and search
            applyFilterAndSearch();
            
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

