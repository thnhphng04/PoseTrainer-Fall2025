package fpt.fall2025.posetrainer.UI.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.UI.activity.SearchActivity;
import fpt.fall2025.posetrainer.UI.adapter.workout.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.UI.adapter.session.SessionAdapter;
import fpt.fall2025.posetrainer.UI.adapter.community.CollectionAdapter;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.Collection;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.SessionDAO;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.DAL.NotificationDAO;
import fpt.fall2025.posetrainer.DAL.StreakDAO;
import fpt.fall2025.posetrainer.DAL.CollectionDAO;
import fpt.fall2025.posetrainer.DAL.WorkoutTemplateDAO;
import fpt.fall2025.posetrainer.UI.activity.MainActivity;
import fpt.fall2025.posetrainer.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ArrayList<WorkoutTemplate> workoutTemplates;
    private ArrayList<WorkoutTemplate> filteredWorkoutTemplates;
    private ArrayList<WorkoutTemplate> featuredWorkouts; // Featured/Recommended workouts
    private ArrayList<WorkoutTemplate> personalizedWorkouts; // Personalized workouts
    private AuthService authService;
    private SessionDAO sessionDAO;
    private UserDAO userDAO;
    private NotificationDAO notificationDAO;
    private StreakDAO streakDAO;
    private CollectionDAO collectionDAO;
    private WorkoutTemplateDAO workoutTemplateDAO;
    private WorkoutTemplateAdapter adapter; // Reuse adapter thay vì tạo mới mỗi lần
    private WorkoutTemplateAdapter featuredAdapter; // Adapter cho featured workouts
    private WorkoutTemplateAdapter personalizedAdapter; // Adapter cho personalized workouts
    private SessionAdapter recentActivityAdapter; // Adapter cho recent activity
    private CollectionAdapter collectionAdapter; // Adapter cho collections
    private ArrayList<Session> recentSessions; // Recent sessions list
    private ArrayList<Collection> collections; // Collections list
    
    // Filter states
    private String selectedCategory = "Latest Updates";
    private String selectedDuration = null;
    
    // Cache để tránh reload không cần thiết
    private boolean isWorkoutTemplatesLoaded = false;
    private String cachedUserId = null;
    private long lastNotificationCountUpdate = 0;
    private static final long NOTIFICATION_COUNT_UPDATE_INTERVAL = 5000; // 5 giây

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authService = new AuthService();
        sessionDAO = new SessionDAO();
        userDAO = new UserDAO();
        notificationDAO = new NotificationDAO();
        streakDAO = new StreakDAO();
        collectionDAO = new CollectionDAO();
        workoutTemplateDAO = new WorkoutTemplateDAO();

        workoutTemplates = new ArrayList<>();
        filteredWorkoutTemplates = new ArrayList<>();
        featuredWorkouts = new ArrayList<>();
        personalizedWorkouts = new ArrayList<>();
        recentSessions = new ArrayList<>();
        collections = new ArrayList<>();

        // Setup RecyclerView cho filtered workouts (view1)
        binding.view1.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        // Khởi tạo adapter một lần và reuse thay vì tạo mới mỗi lần
        if (adapter == null) {
            adapter = new WorkoutTemplateAdapter(filteredWorkoutTemplates);
            binding.view1.setAdapter(adapter);
        } else {
            // Adapter đã tồn tại, chỉ cần update data
            adapter.updateList(filteredWorkoutTemplates);
            if (binding.view1.getAdapter() == null) {
                binding.view1.setAdapter(adapter);
            }
        }

        // Setup RecyclerView cho Featured Workouts (rv_featured_workouts)
        binding.rvFeaturedWorkouts.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        if (featuredAdapter == null) {
            featuredAdapter = new WorkoutTemplateAdapter(featuredWorkouts);
            binding.rvFeaturedWorkouts.setAdapter(featuredAdapter);
        } else {
            featuredAdapter.updateList(featuredWorkouts);
            if (binding.rvFeaturedWorkouts.getAdapter() == null) {
                binding.rvFeaturedWorkouts.setAdapter(featuredAdapter);
            }
        }

        // Setup RecyclerView cho Personalized Workouts (rv_workouts)
        binding.rvWorkouts.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        if (personalizedAdapter == null) {
            personalizedAdapter = new WorkoutTemplateAdapter(personalizedWorkouts);
            binding.rvWorkouts.setAdapter(personalizedAdapter);
        } else {
            personalizedAdapter.updateList(personalizedWorkouts);
            if (binding.rvWorkouts.getAdapter() == null) {
                binding.rvWorkouts.setAdapter(personalizedAdapter);
            }
        }

        // Setup RecyclerView cho Collections
        binding.rvCollections.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        if (collectionAdapter == null) {
            collectionAdapter = new CollectionAdapter(collections);
            binding.rvCollections.setAdapter(collectionAdapter);
        } else {
            collectionAdapter.updateList(collections);
            if (binding.rvCollections.getAdapter() == null) {
                binding.rvCollections.setAdapter(collectionAdapter);
            }
        }

        // Setup RecyclerView cho Recent Activity
        binding.rvRecentActivity.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );
        if (recentActivityAdapter == null) {
            recentActivityAdapter = new SessionAdapter(recentSessions);
            binding.rvRecentActivity.setAdapter(recentActivityAdapter);
        } else {
            recentActivityAdapter.updateSessions(recentSessions);
            if (binding.rvRecentActivity.getAdapter() == null) {
                binding.rvRecentActivity.setAdapter(recentActivityAdapter);
            }
        }

        setupSearchListeners();
        setupFilterListeners();
        setupNotificationButton();
        setupSeeAllButton();
        setupPullToRefresh();
        setupQuickActionFAB();
        loadCurrentUserInfo();
        loadWorkoutTemplates();
        loadUnreadNotificationCount();
        loadUserStreak();
        loadCollections();
        loadRecentActivity();
        isDataLoaded = true;
        
        // Initialize chip states
        updateCategoryChipStates();
        updateDurationChipStates();
    }

    private void setupSearchListeners() {
        binding.searchBar.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SearchActivity.class));
        });
    }
    
    /**
     * Setup button notification để mở NotificationFragment
     * Và hiển thị badge số lượng thông báo chưa đọc
     */
    private void setupNotificationButton() {
        // Click vào button notification → Mở NotificationFragment
        binding.notificationButtonContainer.setOnClickListener(v -> {
            // Lấy MainActivity và gọi method mở NotificationFragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openNotificationFragment();
            }
        });
    }
    
    /**
     * Load và hiển thị số lượng thông báo chưa đọc
     * Được gọi khi fragment hiển thị và định kỳ để cập nhật
     * Đã tối ưu với debounce để tránh load quá nhiều lần
     */
    private void loadUnreadNotificationCount() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        
        String uid = currentUser.getUid();
        
        // Debounce: Chỉ load nếu đã qua 5 giây từ lần update cuối
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationCountUpdate < NOTIFICATION_COUNT_UPDATE_INTERVAL) {
            return;
        }
        
        lastNotificationCountUpdate = currentTime;
        
        // Gọi FirebaseService để đếm thông báo chưa đọc
        notificationDAO.countUnreadNotifications(uid, count -> {
            updateNotificationBadge(count);
        });
    }
    
    /**
     * Load user streak and display
     */
    private void loadUserStreak() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        streakDAO.loadUserStreak(currentUser.getUid(), streak -> {
            if (getActivity() == null || binding == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (streak != null && streak.getCurrentStreak() > 0) {
                    if (binding.tvStreakCount != null) {
                        binding.tvStreakCount.setText(streak.getCurrentStreak() + " ngày");
                    }
                    if (binding.llStreakSection != null) {
                        binding.llStreakSection.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (binding.llStreakSection != null) {
                        binding.llStreakSection.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    /**
     * Load và hiển thị Collections
     */
    private void loadCollections() {
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }

        collectionDAO.getPublicCollections(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<Collection> loadedCollections = new ArrayList<>(task.getResult());
                if (!isAdded() || binding == null) {
                    return;
                }

                if (loadedCollections != null) {
                        collections = loadedCollections;

                        // Update adapter
                        if (collectionAdapter != null) {
                            collectionAdapter.updateList(collections);
                        } else {
                            collectionAdapter = new CollectionAdapter(collections);
                            binding.rvCollections.setAdapter(collectionAdapter);
                        }
                } else {
                    collections = new ArrayList<>();
                    if (collectionAdapter != null) {
                        collectionAdapter.updateList(collections);
                    }
                }
            }
        });
    }

    /**
     * Load và hiển thị Recent Activity (sessions gần đây)
     */
    private void loadRecentActivity() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            recentSessions.clear();
            if (recentActivityAdapter != null) {
                recentActivityAdapter.updateSessions(recentSessions);
            }
            return;
        }

        String uid = currentUser.getUid();

        // Query 4 sessions gần đây nhất
        sessionDAO.getCollection()
                .whereEqualTo("uid", uid)
                .orderBy("startedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(4)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }

                    recentSessions.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Session session = document.toObject(Session.class);
                        if (session != null) {
                            session.setId(document.getId());
                            recentSessions.add(session);
                        }
                    }

                    // Update adapter
                    if (recentActivityAdapter != null) {
                        recentActivityAdapter.updateSessions(recentSessions);
                    } else {
                        recentActivityAdapter = new SessionAdapter(recentSessions);
                        binding.rvRecentActivity.setAdapter(recentActivityAdapter);
                    }

                    // Show empty state nếu không có data
                    if (recentSessions.isEmpty()) {
                        showEmptyStateForRecentActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    recentSessions.clear();
                    if (recentActivityAdapter != null) {
                        recentActivityAdapter.updateSessions(recentSessions);
                    }
                    showErrorState("Không thể tải hoạt động gần đây. Vui lòng thử lại.");
                });
    }

    /**
     * Setup Quick Action FAB
     */
    private void setupQuickActionFAB() {
        if (binding.fabQuickAction != null) {
            binding.fabQuickAction.setOnClickListener(v -> {
                // Mở SearchActivity để chọn workout
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Show empty state cho Featured Workouts
     */
    private void showEmptyStateForFeatured() {
        // Có thể thêm empty state view nếu cần
    }

    /**
     * Show empty state cho Personalized Workouts
     */
    private void showEmptyStateForPersonalized() {
        // Có thể thêm empty state view nếu cần
    }

    /**
     * Show empty state cho Recent Activity
     */
    private void showEmptyStateForRecentActivity() {
        // Có thể thêm empty state view nếu cần
    }

    /**
     * Show error state với message
     */
    private void showErrorState(String message) {
        if (getActivity() != null && isAdded()) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cập nhật badge hiển thị số thông báo chưa đọc
     * @param count Số lượng thông báo chưa đọc
     */
    private void updateNotificationBadge(int count) {
        TextView badgeTextView = binding.tvNotificationBadge;
        
        if (count > 0) {
            // Có thông báo chưa đọc → Hiển thị badge
            badgeTextView.setVisibility(android.view.View.VISIBLE);
            
            // Hiển thị số lượng, nếu > 99 thì hiển thị "99+"
            if (count > 99) {
                badgeTextView.setText("99+");
            } else {
                badgeTextView.setText(String.valueOf(count));
            }
        } else {
            // Không có thông báo chưa đọc → Ẩn badge
            badgeTextView.setVisibility(android.view.View.GONE);
        }
    }

    /**
     * Load workout templates từ Firebase
     * Chỉ load một lần và cache để tránh reload không cần thiết
     */
    private void loadWorkoutTemplates() {
        // Kiểm tra xem đã load chưa để tránh reload không cần thiết
        if (isWorkoutTemplatesLoaded && workoutTemplates != null && !workoutTemplates.isEmpty()) {
            applyFilters();
            loadRecommendedWorkouts();
            loadPersonalizedWorkouts();
            return;
        }
        
        // Kiểm tra activity có tồn tại không
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }
        
        workoutTemplateDAO.getPublicTemplates(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<WorkoutTemplate> templates = new ArrayList<>(task.getResult());
                workoutTemplates = templates;
                isWorkoutTemplatesLoaded = true;
            } else {
                workoutTemplates = new ArrayList<>();
                isWorkoutTemplatesLoaded = true;
            }
            applyFilters();
            // Load recommended và personalized workouts sau khi có data
            loadRecommendedWorkouts();
            loadPersonalizedWorkouts();
        });
    }

    /**
     * Load và hiển thị Featured/Recommended Workouts
     * Logic: Hiển thị các workouts mới nhất, phổ biến nhất, hoặc theo level của user
     */
    private void loadRecommendedWorkouts() {
        if (workoutTemplates == null || workoutTemplates.isEmpty()) {
            featuredWorkouts.clear();
            if (featuredAdapter != null) {
                featuredAdapter.updateList(featuredWorkouts);
            }
            showEmptyStateForFeatured();
            return;
        }

        featuredWorkouts.clear();
        
        // Strategy 1: Lấy 5 workouts mới nhất (dựa trên updatedAt)
        ArrayList<WorkoutTemplate> sortedByDate = new ArrayList<>(workoutTemplates);
        sortedByDate.sort((a, b) -> Long.compare(
            b.getUpdatedAt() != 0 ? b.getUpdatedAt() : 0,
            a.getUpdatedAt() != 0 ? a.getUpdatedAt() : 0
        ));
        
        // Lấy 5 workouts đầu tiên
        int count = Math.min(5, sortedByDate.size());
        for (int i = 0; i < count; i++) {
            featuredWorkouts.add(sortedByDate.get(i));
        }
        
        // Update adapter
        if (featuredAdapter != null) {
            featuredAdapter.updateList(featuredWorkouts);
        } else {
            featuredAdapter = new WorkoutTemplateAdapter(featuredWorkouts);
            binding.rvFeaturedWorkouts.setAdapter(featuredAdapter);
        }

        // Hide empty state nếu có data
        if (featuredWorkouts.isEmpty()) {
            showEmptyStateForFeatured();
        }
    }

    /**
     * Load và hiển thị Personalized Workouts (Bài tập dành cho bạn)
     * Logic: Dựa trên level của user, hoặc workouts phù hợp với user
     */
    private void loadPersonalizedWorkouts() {
        if (workoutTemplates == null || workoutTemplates.isEmpty()) {
            personalizedWorkouts.clear();
            if (personalizedAdapter != null) {
                personalizedAdapter.updateList(personalizedWorkouts);
            }
            showEmptyStateForPersonalized();
            return;
        }

        personalizedWorkouts.clear();
        FirebaseUser currentUser = authService.getCurrentUser();
        
        // Strategy: Lấy workouts phù hợp với level của user
        // Nếu chưa có user level, lấy beginner workouts
        String userLevel = "Beginner"; // Default
        
        if (currentUser != null) {
            // Có thể load user level từ Firestore nếu có
            // Tạm thời dùng default
        }
        
        // Filter workouts theo level
        ArrayList<WorkoutTemplate> levelFiltered = new ArrayList<>();
        for (WorkoutTemplate template : workoutTemplates) {
            if (template.getLevel() != null && 
                template.getLevel().equalsIgnoreCase(userLevel)) {
                levelFiltered.add(template);
            }
        }
        
        // Nếu không có đủ workouts theo level, thêm các workouts khác
        if (levelFiltered.size() < 5) {
            for (WorkoutTemplate template : workoutTemplates) {
                if (!levelFiltered.contains(template)) {
                    levelFiltered.add(template);
                    if (levelFiltered.size() >= 10) break; // Tối đa 10 workouts
                }
            }
        }
        
        // Lấy 5-8 workouts đầu tiên
        int count = Math.min(8, levelFiltered.size());
        for (int i = 0; i < count; i++) {
            personalizedWorkouts.add(levelFiltered.get(i));
        }
        
        // Update adapter
        if (personalizedAdapter != null) {
            personalizedAdapter.updateList(personalizedWorkouts);
        } else {
            personalizedAdapter = new WorkoutTemplateAdapter(personalizedWorkouts);
            binding.rvWorkouts.setAdapter(personalizedAdapter);
        }

        // Hide empty state nếu có data
        if (personalizedWorkouts.isEmpty()) {
            showEmptyStateForPersonalized();
        }
    }

    /**
     * Setup click listener cho button "Xem tất cả"
     */
    private void setupSeeAllButton() {
        binding.btnSeeAll.setOnClickListener(v -> {
            // Navigate đến SearchActivity để xem tất cả workouts
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            // Có thể pass thêm filter nếu cần (ví dụ: filter theo goal "Tăng cơ bắp")
            startActivity(intent);
        });
    }

    /**
     * Setup Pull to Refresh
     */
    private void setupPullToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright, null),
                getResources().getColor(android.R.color.holo_green_light, null),
                getResources().getColor(android.R.color.holo_orange_light, null),
                getResources().getColor(android.R.color.holo_red_light, null)
        );

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshAllData();
        });
    }

    /**
     * Refresh tất cả data khi user pull to refresh
     */
    private void refreshAllData() {
        // Reset cache flags để force reload
        isWorkoutTemplatesLoaded = false;
        lastNotificationCountUpdate = 0;

        // Reload tất cả data
        loadCurrentUserInfo();
        loadWorkoutTemplates();
        loadUnreadNotificationCount();
        loadUserStreak();
        loadCollections();
        loadRecentActivity();

        // Stop refresh indicator sau khi load xong (với delay nhỏ)
        binding.swipeRefreshLayout.postDelayed(() -> {
            if (binding != null && binding.swipeRefreshLayout != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000); // 1 giây delay
    }

    /**
     * 🔄 Load current user info from Firestore or Auth (like ProfileFragment)
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    private void loadCurrentUserInfo() {
        FirebaseUser current = authService.getCurrentUser();
        if (current == null) {
            return;
        }

        String uid = current.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && binding.ivUserAvatar.getDrawable() != null) {
            return;
        }
        
        cachedUserId = uid;
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || getView() == null) {
            return;
        }
        
        userDAO.getDocument(uid).get()
                .addOnSuccessListener(doc -> {
                    // Kiểm tra lại fragment view trước khi update UI
                    if (!isAdded() || getView() == null || binding == null) {
                        return;
                    }
                    
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            String photoUrl = null;
                            // Ưu tiên field "photoUrl" (mới từ Storage)
                            if (doc.contains("photoUrl")) {
                                photoUrl = doc.getString("photoUrl");
                            } else if (doc.contains("photourl")) { // trường cũ
                                photoUrl = doc.getString("photourl");
                            }

                            if (photoUrl == null || photoUrl.isEmpty()) {
                                photoUrl = user.getPhotoURL(); // fallback Google
                            }

                            updateUserUI(user.getDisplayName(), photoUrl);
                        } else {
                            updateUserUIFromAuth(current);
                        }
                    } else {
                        updateUserUIFromAuth(current);
                    }
                })
                .addOnFailureListener(e -> {
                    // Kiểm tra lại fragment view trước khi update UI
                    if (!isAdded() || getView() == null || binding == null) {
                        return;
                    }
                    updateUserUIFromAuth(current);
                });
    }

    private void updateUserUI(String name, String photoUrl) {
        binding.tvUserName.setText(name != null && !name.isEmpty() ? name : "User");

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.ivUserAvatar);
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.profile);
        }
    }

    private void updateUserUIFromAuth(FirebaseUser firebaseUser) {
        String name = firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()
                ? firebaseUser.getDisplayName() : "User";
        String photoUrl = firebaseUser.getPhotoUrl() != null
                ? firebaseUser.getPhotoUrl().toString()
                : null;

        updateUserUI(name, photoUrl);
    }

    private boolean isDataLoaded = false;
    
    @Override
    public void onResume() {
        super.onResume();
        // Chỉ load data một lần ban đầu để tránh reload không cần thiết
        if (!isDataLoaded && isVisible() && isAdded()) {
            loadCurrentUserInfo();
            loadWorkoutTemplates(); // Chỉ load một lần
            isDataLoaded = true;
        }
        
        // Refresh notification count khi fragment hiển thị lại (với debounce)
        // Để badge cập nhật khi có thông báo mới
        loadUnreadNotificationCount();
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Khi fragment trở nên visible lại, chỉ refresh notification count
        // User info và workout templates đã được cache, không cần reload
        if (!hidden && isAdded() && isResumed()) {
            // Chỉ refresh notification count khi fragment hiển thị lại
            loadUnreadNotificationCount();
        }
    }

    private void handleBodyPartClick(String bodyPart) {
        Toast.makeText(getActivity(), "Selected: " + bodyPart, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), fpt.fall2025.posetrainer.UI.activity.ChallengeDetailActivity.class);
        intent.putExtra("body_part", bodyPart);
        startActivity(intent);
    }

    /**
     * Setup click listeners for category and duration filter chips
     */
    private void setupFilterListeners() {
        // Category chips
        binding.chipLatest.setOnClickListener(v -> selectCategory("Latest Updates"));
        binding.chipBeginner.setOnClickListener(v -> selectCategory("Beginner"));
        binding.chipIntermediate.setOnClickListener(v -> selectCategory("Intermediate"));
        binding.chipAdvanced.setOnClickListener(v -> selectCategory("Advanced"));

        // Duration chips
        binding.chip17min.setOnClickListener(v -> selectDuration("1-7 min"));
        binding.chip815min.setOnClickListener(v -> selectDuration("8-15 min"));
        binding.chip15minPlus.setOnClickListener(v -> selectDuration(">15 min"));
        binding.chipStretching.setOnClickListener(v -> selectDuration("Stretching & Warm-up"));
    }

    /**
     * Handle category chip selection
     */
    private void selectCategory(String category) {
        selectedCategory = category;
        updateCategoryChipStates();
        applyFilters();
    }

    /**
     * Handle duration chip selection
     */
    private void selectDuration(String duration) {
        if (selectedDuration != null && selectedDuration.equals(duration)) {
            // Deselect if same duration is clicked again
            selectedDuration = null;
        } else {
            selectedDuration = duration;
        }
        updateDurationChipStates();
        applyFilters();
    }

    /**
     * Update visual states of category chips
     */
    private void updateCategoryChipStates() {
        // Reset all chips to unselected state
        binding.chipLatest.setBackgroundResource(R.drawable.chip_bg);
        binding.chipBeginner.setBackgroundResource(R.drawable.chip_bg);
        binding.chipIntermediate.setBackgroundResource(R.drawable.chip_bg);
        binding.chipAdvanced.setBackgroundResource(R.drawable.chip_bg);

        // Set selected chip
        switch (selectedCategory) {
            case "Latest Updates":
                binding.chipLatest.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Beginner":
                binding.chipBeginner.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Intermediate":
                binding.chipIntermediate.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
            case "Advanced":
                binding.chipAdvanced.setBackgroundResource(R.drawable.chip_selected_bg);
                break;
        }
    }

    /**
     * Update visual states of duration chips
     */
    private void updateDurationChipStates() {
        // Reset all chips to unselected state
        binding.chip17min.setBackgroundResource(R.drawable.chip_bg);
        binding.chip815min.setBackgroundResource(R.drawable.chip_bg);
        binding.chip15minPlus.setBackgroundResource(R.drawable.chip_bg);
        binding.chipStretching.setBackgroundResource(R.drawable.chip_bg);

        // Set selected chip if any
        if (selectedDuration != null) {
            switch (selectedDuration) {
                case "1-7 min":
                    binding.chip17min.setBackgroundResource(R.drawable.chip_selected_bg);
                    break;
                case "8-15 min":
                    binding.chip815min.setBackgroundResource(R.drawable.chip_selected_bg);
                    break;
                case ">15 min":
                    binding.chip15minPlus.setBackgroundResource(R.drawable.chip_selected_bg);
                    break;
                case "Stretching & Warm-up":
                    binding.chipStretching.setBackgroundResource(R.drawable.chip_selected_bg);
                    break;
            }
        }
    }

    /**
     * Apply filters based on selected category and duration
     */
    private void applyFilters() {
        filteredWorkoutTemplates.clear();

        for (WorkoutTemplate template : workoutTemplates) {
            boolean matchesCategory = false;
            boolean matchesDuration = false;

            // Check category filter
            if (selectedCategory.equals("Latest Updates")) {
                matchesCategory = true; // Show all for latest updates
            } else {
                matchesCategory = template.getLevel() != null && 
                    template.getLevel().equalsIgnoreCase(selectedCategory);
            }

            // Check duration filter
            if (selectedDuration == null) {
                matchesDuration = true; // No duration filter
            } else {
                int duration = template.getEstDurationMin();
                switch (selectedDuration) {
                    case "1-7 min":
                        matchesDuration = duration >= 1 && duration <= 7;
                        break;
                    case "8-15 min":
                        matchesDuration = duration >= 8 && duration <= 15;
                        break;
                    case ">15 min":
                        matchesDuration = duration > 15;
                        break;
                    case "Stretching & Warm-up":
                        // For stretching, we might want to check focus or title
                        matchesDuration = template.getFocus() != null && 
                            template.getFocus().contains("Stretching") ||
                            template.getTitle() != null && 
                            template.getTitle().toLowerCase().contains("stretch");
                        break;
                }
            }

            if (matchesCategory && matchesDuration) {
                filteredWorkoutTemplates.add(template);
            }
        }

        // Update RecyclerView bằng cách update adapter thay vì tạo mới
        if (adapter != null) {
            adapter.updateList(filteredWorkoutTemplates);
        } else {
            // Nếu adapter chưa tồn tại, tạo mới
            adapter = new WorkoutTemplateAdapter(filteredWorkoutTemplates);
            binding.view1.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        // Không reset isWorkoutTemplatesLoaded và cachedUserId để cache vẫn hoạt động khi fragment bị recreate
        binding = null;
        adapter = null; // Clear adapter reference
        featuredAdapter = null;
        personalizedAdapter = null;
        recentActivityAdapter = null;
    }
}
