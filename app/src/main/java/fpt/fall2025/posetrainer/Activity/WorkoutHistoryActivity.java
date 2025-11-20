package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.Adapter.WorkoutHistoryAdapter;
import fpt.fall2025.posetrainer.Adapter.FavoriteWorkoutAdapter;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Domain.FavoriteWorkoutItem;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

/**
 * Activity hiển thị lịch sử tập luyện với tabs: Lịch sử, Gần đây, Yêu thích
 * Giống như màn hình trong ảnh người dùng gửi
 */
public class WorkoutHistoryActivity extends AppCompatActivity {
    private static final String TAG = "WorkoutHistoryActivity";
    
    // UI Components
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private Spinner spinnerFilter;
    private LinearLayout layoutFilterSection;
    private RecyclerView recyclerViewSessions;
    private TextView tvDateRange;
    private TextView tvSummary;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyState;
    private FloatingActionButton fabAdd;
    
    // Data
    private ArrayList<Session> allSessions;
    private ArrayList<Session> filteredSessions;
    private ArrayList<FavoriteWorkoutItem> favoriteWorkoutItems; // Chứa cả WorkoutTemplate và UserWorkout
    private WorkoutHistoryAdapter sessionAdapter;
    private FavoriteWorkoutAdapter favoriteWorkoutAdapter;
    private RecyclerView.Adapter currentAdapter; // Adapter hiện tại đang được sử dụng
    
    // Tab và Filter
    private int currentTab = 0; // 0: Lịch sử, 1: Gần đây, 2: Yêu thích
    private String currentFilter = "Tất cả"; // "Tất cả", "Tuần này", "Tháng này", "Năm nay"
    
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize UI
        initViews();
        setupToolbar();
        setupTabs();
        setupFilter();
        setupRecyclerView();
        
        // Load sessions từ Firebase
        loadSessions();
    }

    /**
     * Khởi tạo các view từ layout
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        spinnerFilter = findViewById(R.id.spinner_filter);
        layoutFilterSection = findViewById(R.id.layout_filter_section);
        recyclerViewSessions = findViewById(R.id.recycler_view_sessions);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvSummary = findViewById(R.id.tv_summary);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        fabAdd = findViewById(R.id.fab_add);
        
        // Initialize data lists
        allSessions = new ArrayList<>();
        filteredSessions = new ArrayList<>();
        favoriteWorkoutItems = new ArrayList<>();
        
        // Setup FAB (tạm thời ẩn vì chưa có chức năng cụ thể)
        if (fabAdd != null) {
            fabAdd.setVisibility(View.GONE);
        }
    }

    /**
     * Setup toolbar với nút back
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Setup tabs: Lịch sử, Gần đây, Yêu thích
     */
    private void setupTabs() {
        // Tab đầu tiên (Lịch sử) được chọn mặc định
        tabLayout.selectTab(tabLayout.getTabAt(0));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                Log.d(TAG, "Tab được chọn: " + currentTab);
                
                // Xử lý theo tab được chọn
                if (currentTab == 2) {
                    // Tab Yêu thích: hiển thị favorite workout templates
                    loadFavoriteWorkoutTemplates();
                    // Ẩn filter section và date range/summary cho tab Yêu thích
                    layoutFilterSection.setVisibility(View.GONE);
                    findViewById(R.id.layout_date_summary).setVisibility(View.GONE);
                } else {
                    // Tab Lịch sử hoặc Gần đây: hiển thị sessions
                    filterSessionsByTab();
                    // Hiển thị filter section và date range/summary
                    layoutFilterSection.setVisibility(View.VISIBLE);
                    findViewById(R.id.layout_date_summary).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }
        });
    }

    /**
     * Setup filter spinner
     */
    private void setupFilter() {
        // Tạo danh sách filter options
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("Tất cả");
        filterOptions.add("Tuần này");
        filterOptions.add("Tháng này");
        filterOptions.add("Năm nay");
        
        // Tạo adapter cho spinner
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            filterOptions
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        
        // Xử lý khi chọn filter
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filterOptions.get(position);
                Log.d(TAG, "Filter được chọn: " + currentFilter);
                // Filter sessions theo filter được chọn
                filterSessionsByTab();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Không cần xử lý
            }
        });
    }

    /**
     * Setup RecyclerView
     */
    private void setupRecyclerView() {
        // Khởi tạo adapter cho sessions (mặc định)
        sessionAdapter = new WorkoutHistoryAdapter(filteredSessions);
        recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSessions.setAdapter(sessionAdapter);
        currentAdapter = sessionAdapter;
    }

    /**
     * Load sessions từ Firebase Firestore
     */
    private void loadSessions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng đăng nhập, không thể tải sessions");
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch sử tập luyện", Toast.LENGTH_SHORT).show();
            showEmptyState("Vui lòng đăng nhập để xem lịch sử");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "=== ĐANG TẢI SESSIONS ===");
        Log.d(TAG, "Đang tải sessions cho uid: " + uid);

        FirebaseService.getInstance().loadUserSessions(uid, this, new FirebaseService.OnSessionsLoadedListener() {
            @Override
            public void onSessionsLoaded(ArrayList<Session> loadedSessions) {
                Log.d(TAG, "=== SESSIONS ĐÃ TẢI ===");
                Log.d(TAG, "Nhận được " + (loadedSessions != null ? loadedSessions.size() : 0) + " sessions");

                allSessions = loadedSessions != null ? loadedSessions : new ArrayList<>();
                
                // Filter sessions theo tab và filter hiện tại
                filterSessionsByTab();
                
                Log.d(TAG, "=== KẾT THÚC TẢI SESSIONS ===");
            }
        });
    }

    /**
     * Filter sessions theo tab được chọn (Lịch sử, Gần đây, Yêu thích)
     */
    private void filterSessionsByTab() {
        filteredSessions.clear();
        
        if (allSessions == null || allSessions.isEmpty()) {
            showEmptyState("Không có buổi tập nào");
            if (currentTab != 2 && sessionAdapter != null) {
                sessionAdapter.updateSessions(filteredSessions);
            }
            updateDateRangeAndSummary();
            return;
        }
        
        Calendar now = Calendar.getInstance();
        Calendar filterStart = Calendar.getInstance();
        filterStart.set(Calendar.HOUR_OF_DAY, 0);
        filterStart.set(Calendar.MINUTE, 0);
        filterStart.set(Calendar.SECOND, 0);
        filterStart.set(Calendar.MILLISECOND, 0);
        
        // Áp dụng filter theo thời gian (Tất cả, Tuần này, Tháng này, Năm nay)
        switch (currentFilter) {
            case "Tuần này":
                // Lấy thứ 2 đầu tuần
                int dayOfWeek = filterStart.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY);
                filterStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
                break;
            case "Tháng này":
                filterStart.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case "Năm nay":
                filterStart.set(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Tất cả":
            default:
                filterStart.set(Calendar.YEAR, 2000); // Lấy tất cả từ năm 2000
                break;
        }
        
        long filterStartTime = filterStart.getTimeInMillis() / 1000; // Convert to seconds
        
        // Filter sessions theo tab (chỉ cho tab Lịch sử và Gần đây)
        for (Session session : allSessions) {
            if (session == null || session.getStartedAt() <= 0) {
                continue;
            }
            
            // Kiểm tra filter theo thời gian
            if (session.getStartedAt() < filterStartTime) {
                continue;
            }
            
            // Filter theo tab (chỉ cho tab Lịch sử và Gần đây)
            switch (currentTab) {
                case 0: // Lịch sử - hiển thị tất cả
                    filteredSessions.add(session);
                    break;
                case 1: // Gần đây - chỉ hiển thị 7 ngày gần nhất
                    long sevenDaysAgo = (System.currentTimeMillis() / 1000) - (7 * 24 * 60 * 60);
                    if (session.getStartedAt() >= sevenDaysAgo) {
                        filteredSessions.add(session);
                    }
                    break;
            }
        }
        
        // Cập nhật UI
        if (filteredSessions.isEmpty()) {
            showEmptyState("Không có buổi tập nào");
        } else {
            showSessionsList();
        }
        
        // Chuyển về session adapter nếu đang ở tab Lịch sử hoặc Gần đây
        if (currentTab != 2) {
            if (currentAdapter != sessionAdapter) {
                recyclerViewSessions.setAdapter(sessionAdapter);
                currentAdapter = sessionAdapter;
            }
            sessionAdapter.updateSessions(filteredSessions);
        }
        
        updateDateRangeAndSummary();
    }

    /**
     * Cập nhật date range và summary
     */
    private void updateDateRangeAndSummary() {
        if (filteredSessions == null || filteredSessions.isEmpty()) {
            tvDateRange.setText("");
            tvSummary.setText("0 bản ghi/0 phút");
            return;
        }
        
        // Tìm ngày đầu và ngày cuối
        long minDate = Long.MAX_VALUE;
        long maxDate = Long.MIN_VALUE;
        int totalMinutes = 0;
        
        for (Session session : filteredSessions) {
            if (session.getStartedAt() > 0) {
                if (session.getStartedAt() < minDate) {
                    minDate = session.getStartedAt();
                }
                if (session.getStartedAt() > maxDate) {
                    maxDate = session.getStartedAt();
                }
                
                // Tính tổng thời gian
                if (session.getSummary() != null && session.getSummary().getDurationSec() > 0) {
                    totalMinutes += session.getSummary().getDurationSec() / 60;
                } else if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
                    long durationSec = session.getEndedAt() - session.getStartedAt();
                    totalMinutes += (int) (durationSec / 60);
                }
            }
        }
        
        // Format date range
        if (minDate != Long.MAX_VALUE && maxDate != Long.MIN_VALUE) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d 'Th'MM", Locale.getDefault());
            Date startDate = new Date(minDate * 1000);
            Date endDate = new Date(maxDate * 1000);
            
            String startStr = dateFormat.format(startDate);
            String endStr = dateFormat.format(endDate);
            
            tvDateRange.setText(startStr + " - " + endStr);
        } else {
            tvDateRange.setText("");
        }
        
        // Update summary
        tvSummary.setText(filteredSessions.size() + " bản ghi/" + totalMinutes + " phút");
    }

    /**
     * Hiển thị danh sách sessions
     */
    private void showSessionsList() {
        recyclerViewSessions.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Hiển thị empty state
     */
    private void showEmptyState(String message) {
        recyclerViewSessions.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText(message);
    }

    /**
     * Load favorite workout templates và user workouts cho tab Yêu thích
     */
    private void loadFavoriteWorkoutTemplates() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng đăng nhập, không thể tải favorite workouts");
            Toast.makeText(this, "Vui lòng đăng nhập để xem workout yêu thích", Toast.LENGTH_SHORT).show();
            showEmptyState("Vui lòng đăng nhập để xem workout yêu thích");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "=== ĐANG TẢI FAVORITE WORKOUTS ===");
        Log.d(TAG, "Đang tải favorite workout templates và user workouts cho uid: " + uid);

        // Bước 1: Load favorite workout template IDs
        FirebaseService.getInstance().loadFavoriteWorkoutTemplateIds(uid, favoriteIds -> {
            Log.d(TAG, "=== FAVORITE IDs ĐÃ TẢI ===");
            Log.d(TAG, "Nhận được " + (favoriteIds != null ? favoriteIds.size() : 0) + " favorite IDs");

            if (favoriteIds == null || favoriteIds.isEmpty()) {
                favoriteWorkoutItems = new ArrayList<>();
                showEmptyState("Chưa có workout nào được yêu thích");
                return;
            }

            // Bước 2: Load từng favorite ID - thử load như WorkoutTemplate trước, nếu không có thì load như UserWorkout
            favoriteWorkoutItems = new ArrayList<>();
            final int[] loadedCount = {0};
            final int totalCount = favoriteIds.size();

            for (String favoriteId : favoriteIds) {
                // Thử load như WorkoutTemplate trước
                FirebaseService.getInstance().loadWorkoutTemplateById(favoriteId, this, template -> {
                    if (template != null) {
                        // Là WorkoutTemplate
                        favoriteWorkoutItems.add(new FavoriteWorkoutItem(template));
                        Log.d(TAG, "✓ Loaded WorkoutTemplate: " + template.getTitle());
                        
                        // Tăng counter và kiểm tra xem đã load xong chưa
                        loadedCount[0]++;
                        if (loadedCount[0] == totalCount) {
                            // Đã load xong tất cả
                            updateFavoriteWorkoutUI();
                        }
                    } else {
                        // Không phải WorkoutTemplate, thử load như UserWorkout
                        FirebaseService.getInstance().loadUserWorkoutById(favoriteId, this, userWorkout -> {
                            if (userWorkout != null) {
                                // Là UserWorkout
                                favoriteWorkoutItems.add(new FavoriteWorkoutItem(userWorkout));
                                Log.d(TAG, "✓ Loaded UserWorkout: " + userWorkout.getTitle());
                            } else {
                                Log.w(TAG, "✗ Không tìm thấy workout với ID: " + favoriteId);
                            }
                            
                            // Tăng counter và kiểm tra xem đã load xong chưa
                            loadedCount[0]++;
                            if (loadedCount[0] == totalCount) {
                                // Đã load xong tất cả
                                updateFavoriteWorkoutUI();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Cập nhật UI sau khi load xong favorite workouts
     */
    private void updateFavoriteWorkoutUI() {
        Log.d(TAG, "=== CẬP NHẬT UI FAVORITE WORKOUTS ===");
        Log.d(TAG, "Tổng số favorite workouts: " + favoriteWorkoutItems.size());

        if (favoriteWorkoutItems.isEmpty()) {
            showEmptyState("Chưa có workout nào được yêu thích");
        } else {
            showSessionsList();
            // Chuyển sang favorite workout adapter với layout item riêng
            recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            favoriteWorkoutAdapter = new FavoriteWorkoutAdapter(favoriteWorkoutItems);
            recyclerViewSessions.setAdapter(favoriteWorkoutAdapter);
            currentAdapter = favoriteWorkoutAdapter;
        }

        Log.d(TAG, "=== KẾT THÚC TẢI FAVORITE WORKOUTS ===");
    }

}

