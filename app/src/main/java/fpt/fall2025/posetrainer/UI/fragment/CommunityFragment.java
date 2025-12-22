package fpt.fall2025.posetrainer.UI.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.UI.activity.CreatePostActivity;
import fpt.fall2025.posetrainer.UI.activity.ImageGalleryActivity;
import fpt.fall2025.posetrainer.UI.activity.MainActivity;
import fpt.fall2025.posetrainer.UI.activity.PostDetailActivity;
import fpt.fall2025.posetrainer.UI.activity.UserProfileActivity;
import fpt.fall2025.posetrainer.UI.dialog.LikeListDialog;
import fpt.fall2025.posetrainer.UI.dialog.PostFeedbackDialog;
import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.CommunityDAO;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.DAL.NotificationDAO;
import fpt.fall2025.posetrainer.ViewModel.CommunityViewModel;

public class CommunityFragment extends Fragment {
    private AuthService authService;
    private CommunityDAO communityDAO;
    private UserDAO userDAO;
    private NotificationDAO notificationDAO;
    private ImageView imgAvatar;
    private FirestoreRecyclerAdapter<Community, PostVH> adapter;
    private LinearLayoutManager layoutManager;
    private CommunityViewModel viewModel;
    
    // UI Components
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private TabLayout tabLayout;
    private SearchView searchView;
    private View btnNotifications;
    private TextView tvNotificationBadge;
    private LinearLayout emptyState, loadingState;
    
    // Tab state
    private int currentTab = 0; // 0: Tất cả, 1: Phổ biến, 2: Mới nhất, 3: Theo dõi, 4: Cá nhân
    private String currentSearchQuery = "";
    private boolean isAutoSwitchingTab = false; // Flag để tránh clear search khi tự động chuyển tab
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private String cachedPhotoUrl = null;
    private boolean isFragmentVisible = false;
    
    // Notification badge
    private long lastNotificationCountUpdate = 0;
    private static final long NOTIFICATION_COUNT_UPDATE_INTERVAL = 5000; // 5 giây
    
    /**
     * Chuyển đổi tiếng Việt có dấu sang không dấu để hỗ trợ tìm kiếm
     * Ví dụ: "Nguyễn Văn A" -> "nguyen van a"
     */
    private String removeVietnameseDiacritics(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Normalize và chuyển sang lowercase
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        
        // Loại bỏ các ký tự dấu (diacritics)
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // Chuyển sang lowercase
        return normalized.toLowerCase();
    }
    
    /**
     * Kiểm tra xem text có chứa search query không (hỗ trợ cả có dấu và không dấu)
     */
    private boolean matchesSearch(String text, String searchQuery) {
        if (text == null || searchQuery == null || searchQuery.isEmpty()) {
            return true; // Nếu không có search query thì match tất cả
        }
        
        String textLower = text.toLowerCase();
        String searchLower = searchQuery.toLowerCase();
        
        // Kiểm tra match trực tiếp (có dấu)
        if (textLower.contains(searchLower)) {
            return true;
        }
        
        // Kiểm tra match không dấu
        String textNoDiacritics = removeVietnameseDiacritics(text);
        String searchNoDiacritics = removeVietnameseDiacritics(searchQuery);
        
        return textNoDiacritics.contains(searchNoDiacritics);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        authService = new AuthService();
        communityDAO = new CommunityDAO();
        userDAO = new UserDAO();
        notificationDAO = new NotificationDAO();
        imgAvatar = v.findViewById(R.id.profile_image);

        // ViewModel để lưu vị trí scroll
        viewModel = new ViewModelProvider(requireActivity()).get(CommunityViewModel.class);

        // Initialize UI components
        rvFeed = v.findViewById(R.id.rvFeed);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        tabLayout = v.findViewById(R.id.tabLayout);
        searchView = v.findViewById(R.id.searchView);  // ← PHẢI FIND VIEW TRƯỚC
        btnNotifications = v.findViewById(R.id.btnNotifications);
        tvNotificationBadge = v.findViewById(R.id.tv_notification_badge);
        emptyState = v.findViewById(R.id.emptyState);
        loadingState = v.findViewById(R.id.loadingState);

        // ===== SAU ĐÓ MỚI SETUP SEARCHVIEW =====
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.clearFocus();

        // Setup tabs
        setupTabs();

        // Setup search
        setupSearch();

        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(this::refreshFeed);

        // Setup notifications button
        btnNotifications.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openNotificationFragment();
            } else {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra("openFragment", "notifications");
                startActivity(intent);
            }
        });

        // Load avatar user
        loadUserFromFirestore();
        
        // Load notification count
        loadUnreadNotificationCount();

        // Setup RecyclerView
        layoutManager = new LinearLayoutManager(getContext());
        rvFeed.setLayoutManager(layoutManager);
        rvFeed.setItemAnimator(null);

        // Load initial feed
        loadFeed();

        // Scroll position handling
        rvFeed.post(() -> {
            if (viewModel.lastScrollPosition > 0) {
                layoutManager.scrollToPositionWithOffset(viewModel.lastScrollPosition, 0);
            }
        });

        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    viewModel.lastScrollPosition = layoutManager.findFirstVisibleItemPosition();
                }
            }
        });

        // Create post button - setup click cho cả layout và textview
        LinearLayout layoutCreatePost = v.findViewById(R.id.layoutCreatePost);
        TextView tvCreatePost = v.findViewById(R.id.tvCreatePost);
        
        // Setup click cho cả layout và textview để tránh khoảng hở không bắt được click
        View.OnClickListener createPostClickListener = view -> {
            Intent i = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(i);
        };
        
        if (layoutCreatePost != null) {
            layoutCreatePost.setOnClickListener(createPostClickListener);
        }
        if (tvCreatePost != null) {
            tvCreatePost.setOnClickListener(createPostClickListener);
        }

        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        try {
            SearchView.SearchAutoComplete searchAutoComplete =
                    searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchAutoComplete != null) {
                searchAutoComplete.setHint("Tìm kiếm bài viết, người dùng…");
                searchAutoComplete.setText("");
                searchAutoComplete.setBackgroundColor(Color.TRANSPARENT);
                searchAutoComplete.setTextColor(Color.WHITE);
                searchAutoComplete.setHintTextColor(Color.parseColor("#99FFFFFF"));
                searchAutoComplete.setTextSize(14);
                searchAutoComplete.setFocusable(true);
                searchAutoComplete.setFocusableInTouchMode(true);
            }

            ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (searchIcon != null) {
                searchIcon.setColorFilter(Color.parseColor("#99ffffff"), PorterDuff.Mode.SRC_IN);
            }

            ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeIcon != null) {
                closeIcon.setColorFilter(Color.parseColor("#99ffffff"), PorterDuff.Mode.SRC_IN);
            }
        } catch (Exception e) {
            // Error customizing SearchView
        }

        searchView.setQuery("", false);
        searchView.clearFocus();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setSubmitButtonEnabled(false);


        // Back button
        ImageButton btnBack = v.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Phổ biến"));
        tabLayout.addTab(tabLayout.newTab().setText("Mới nhất"));
        tabLayout.addTab(tabLayout.newTab().setText("Theo dõi"));
        tabLayout.addTab(tabLayout.newTab().setText("Cá nhân"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                // Chỉ clear search khi user chuyển tab thủ công, không clear khi auto switch
                if (!isAutoSwitchingTab) {
                    currentSearchQuery = "";
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                loadFeed();
                }
                isAutoSwitchingTab = false;
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                loadFeed();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    currentSearchQuery = "";
                    loadFeed();
                }
                return true;
            }
        });
    }

    private void refreshFeed() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        loadFeed();
        swipeRefresh.setRefreshing(false);
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
     * Cập nhật badge hiển thị số thông báo chưa đọc
     * @param count Số lượng thông báo chưa đọc
     */
    private void updateNotificationBadge(int count) {
        if (tvNotificationBadge == null) {
            return;
        }
        
        if (count > 0) {
            // Có thông báo chưa đọc → Hiển thị badge
            tvNotificationBadge.setVisibility(View.VISIBLE);
            
            // Hiển thị số lượng, nếu > 99 thì hiển thị "99+"
            if (count > 99) {
                tvNotificationBadge.setText("99+");
            } else {
                tvNotificationBadge.setText(String.valueOf(count));
            }
        } else {
            // Không có thông báo chưa đọc → Ẩn badge
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void loadFeed() {
        showLoading(true);
        
        Query baseQuery = communityDAO.getCollection();
        
        // Filter chỉ lấy các post có isVisible = true ở query level để tránh IndexOutOfBoundsException
        baseQuery = baseQuery.whereEqualTo("isVisible", true);
        
        // Apply search filter - chỉ áp dụng cho tab Tất cả và Mới nhất
        // Tab Phổ biến không hỗ trợ search vì Firestore cần composite index
        // Lưu ý: Không filter ở query level nữa vì cần tìm cả trong content và author.displayName
        // Filter sẽ được thực hiện ở client side trong onBindViewHolder
        boolean hasSearch = !TextUtils.isEmpty(currentSearchQuery);
        
        // Apply tab filter
        switch (currentTab) {
            case 0: // Tất cả
                baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                break;
            case 1: // Phổ biến
                // Sort by likesCount only - không hỗ trợ search, chuyển về tab Tất cả
                if (hasSearch) {
                    currentTab = 0;
                    isAutoSwitchingTab = true;
                    tabLayout.getTabAt(0).select();
                    // Search filter sẽ được thực hiện ở client side
                    baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                } else {
                baseQuery = baseQuery.orderBy("likesCount", Query.Direction.DESCENDING);
                }
                break;
            case 2: // Mới nhất
                baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                break;
            case 3: // Theo dõi
                // Không hỗ trợ search, chuyển về tab Tất cả
                if (hasSearch) {
                    currentTab = 0;
                    isAutoSwitchingTab = true;
                    tabLayout.getTabAt(0).select();
                    // Search filter sẽ được thực hiện ở client side
                    baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                    break;
                }
                FirebaseUser currentUser = authService.getCurrentUser();
                if (currentUser == null) {
                    showEmptyState("Bạn cần đăng nhập để xem bài viết từ người đang theo dõi");
                    return;
                }
                loadFollowingFeed(currentUser.getUid());
                return;
            case 4: // Cá nhân
                // Không hỗ trợ search, chuyển về tab Tất cả
                if (hasSearch) {
                    currentTab = 0;
                    isAutoSwitchingTab = true;
                    tabLayout.getTabAt(0).select();
                    // Search filter sẽ được thực hiện ở client side
                    baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                    break;
                }
                FirebaseUser currentUserForMyPosts = authService.getCurrentUser();
                if (currentUserForMyPosts == null) {
                    showEmptyState("Bạn cần đăng nhập để xem bài viết của mình");
                    return;
                }
                loadMyPostsFeed(currentUserForMyPosts.getUid());
                return;
        }
        
        baseQuery = baseQuery.limit(50);

        FirestoreRecyclerOptions<Community> opts = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(baseQuery, Community.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirestoreRecyclerAdapter<Community, PostVH>(opts) {
            @NonNull
            @Override
            public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_post, parent, false);
                return new PostVH(item, CommunityFragment.this);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                // Filter search query: kiểm tra cả content và author.displayName (hỗ trợ tìm không dấu)
                boolean matchesSearch = true;
                if (!TextUtils.isEmpty(currentSearchQuery)) {
                    boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                    boolean authorMatch = p.author != null && p.author.displayName != null && 
                        matchesSearch(p.author.displayName, currentSearchQuery);
                    matchesSearch = contentMatch || authorMatch;
                }
                
                if (!matchesSearch) {
                    // Ẩn item nếu không match search query
                    h.itemView.setVisibility(View.GONE);
                    h.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
                
                // Hiển thị item nếu match
                h.itemView.setVisibility(View.VISIBLE);
                h.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                h.bind(p, currentTab);
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                showLoading(false);
                
                // Đếm số item thực sự hiển thị sau khi filter search
                int visibleCount = 0;
                if (!TextUtils.isEmpty(currentSearchQuery)) {
                    for (int i = 0; i < getItemCount(); i++) {
                        Community p = getItem(i);
                        if (p != null) {
                            boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                            boolean authorMatch = p.author != null && p.author.displayName != null && 
                                matchesSearch(p.author.displayName, currentSearchQuery);
                            if (contentMatch || authorMatch) {
                                visibleCount++;
                            }
                        }
                    }
                } else {
                    visibleCount = getItemCount();
                }
                
                if (visibleCount == 0) {
                    String message = !TextUtils.isEmpty(currentSearchQuery) 
                        ? "Không tìm thấy kết quả cho \"" + currentSearchQuery + "\""
                        : "Chưa có bài viết nào";
                    showEmptyState(message);
                } else {
                    hideEmptyState();
                }
            }
        };

        rvFeed.setAdapter(adapter);
        if (isFragmentVisible && !isHidden()) {
            adapter.startListening();
        }
    }

    private void loadFollowingFeed(String currentUserId) {
        userDAO.getFollowingCollection(currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        followingIds.add(doc.getId());
                    }
                    
                    if (followingIds.isEmpty()) {
                        showEmptyState("Bạn chưa theo dõi ai. Hãy theo dõi người dùng để xem bài viết của họ!");
                        return;
                    }
                    
                    Query query = communityDAO.getCollection()
                            .whereEqualTo("isVisible", true) // Filter chỉ lấy các post visible
                            .whereIn("uid", followingIds)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(50);
                    
                    FirestoreRecyclerOptions<Community> opts = new FirestoreRecyclerOptions.Builder<Community>()
                            .setQuery(query, Community.class)
                            .setLifecycleOwner(getViewLifecycleOwner())
                            .build();

                    if (adapter != null) {
                        adapter.stopListening();
                    }

                    adapter = new FirestoreRecyclerAdapter<Community, PostVH>(opts) {
                        @NonNull
                        @Override
                        public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View item = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.item_post, parent, false);
                            return new PostVH(item, CommunityFragment.this);
                        }

                        @Override
                        protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                            // Filter search query: kiểm tra cả content và author.displayName (hỗ trợ tìm không dấu)
                            boolean matchesSearchResult = true;
                            if (!TextUtils.isEmpty(currentSearchQuery)) {
                                boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                                boolean authorMatch = p.author != null && p.author.displayName != null && 
                                    matchesSearch(p.author.displayName, currentSearchQuery);
                                matchesSearchResult = contentMatch || authorMatch;
                            }
                            
                            if (!matchesSearchResult) {
                                // Ẩn item nếu không match search query
                                h.itemView.setVisibility(View.GONE);
                                h.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                                return;
                            }
                            
                            // Hiển thị item nếu match
                            h.itemView.setVisibility(View.VISIBLE);
                            h.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                            h.bind(p, currentTab);
                        }

                        @Override
                        public void onDataChanged() {
                            super.onDataChanged();
                            showLoading(false);
                            
                            // Đếm số item thực sự hiển thị sau khi filter search
                            int visibleCount = 0;
                            if (!TextUtils.isEmpty(currentSearchQuery)) {
                                for (int i = 0; i < getItemCount(); i++) {
                                    Community p = getItem(i);
                                    if (p != null) {
                                        boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                                        boolean authorMatch = p.author != null && p.author.displayName != null && 
                                            matchesSearch(p.author.displayName, currentSearchQuery);
                                        if (contentMatch || authorMatch) {
                                            visibleCount++;
                                        }
                                    }
                                }
                            } else {
                                visibleCount = getItemCount();
                            }
                            
                            if (visibleCount == 0) {
                                String message = !TextUtils.isEmpty(currentSearchQuery) 
                                    ? "Không tìm thấy kết quả cho \"" + currentSearchQuery + "\""
                                    : "Chưa có bài viết nào từ người bạn đang theo dõi";
                                showEmptyState(message);
                            } else {
                                hideEmptyState();
                            }
                        }
                    };

                    rvFeed.setAdapter(adapter);
                    if (isFragmentVisible && !isHidden()) {
                        adapter.startListening();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Lỗi tải danh sách theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMyPostsFeed(String currentUserId) {
        Query query = communityDAO.getCollection()
                .whereEqualTo("uid", currentUserId)
                .whereEqualTo("isVisible", true) // Chỉ lấy các post visible
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50);
        
        FirestoreRecyclerOptions<Community> opts = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(query, Community.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirestoreRecyclerAdapter<Community, PostVH>(opts) {
            @NonNull
            @Override
            public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_post, parent, false);
                return new PostVH(item, CommunityFragment.this);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                // Filter search query: kiểm tra cả content và author.displayName (hỗ trợ tìm không dấu)
                boolean matchesSearchResult = true;
                if (!TextUtils.isEmpty(currentSearchQuery)) {
                    boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                    boolean authorMatch = p.author != null && p.author.displayName != null && 
                        matchesSearch(p.author.displayName, currentSearchQuery);
                    matchesSearchResult = contentMatch || authorMatch;
                }
                
                if (!matchesSearchResult) {
                    // Ẩn item nếu không match search query
                    h.itemView.setVisibility(View.GONE);
                    h.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
                
                // Hiển thị item nếu match
                h.itemView.setVisibility(View.VISIBLE);
                h.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                h.bind(p, currentTab);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                showLoading(false);
                
                // Đếm số item thực sự hiển thị sau khi filter search
                int visibleCount = 0;
                if (!TextUtils.isEmpty(currentSearchQuery)) {
                    for (int i = 0; i < getItemCount(); i++) {
                        Community p = getItem(i);
                        if (p != null) {
                            boolean contentMatch = matchesSearch(p.content, currentSearchQuery);
                            boolean authorMatch = p.author != null && p.author.displayName != null && 
                                matchesSearch(p.author.displayName, currentSearchQuery);
                            if (contentMatch || authorMatch) {
                                visibleCount++;
                            }
                        }
                    }
                } else {
                    visibleCount = getItemCount();
                }
                
                if (visibleCount == 0) {
                    String message = !TextUtils.isEmpty(currentSearchQuery) 
                        ? "Không tìm thấy kết quả cho \"" + currentSearchQuery + "\""
                        : "Bạn chưa có bài viết nào";
                    showEmptyState(message);
                } else {
                    hideEmptyState();
                }
            }
        };

        rvFeed.setAdapter(adapter);
        if (isFragmentVisible && !isHidden()) {
            adapter.startListening();
        }
    }

    private void showLoading(boolean show) {
        if (loadingState != null) {
            loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (rvFeed != null) {
            rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyState(String message) {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
            TextView tv = emptyState.findViewById(R.id.emptyText);
            if (tv != null) {
                tv.setText(message);
            }
        }
        if (rvFeed != null) {
            rvFeed.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
        if (rvFeed != null) {
            rvFeed.setVisibility(View.VISIBLE);
        }
    }

    // ===================== LOAD USER AVATAR =====================
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        
        if (cachedUserId != null && cachedUserId.equals(uid) && cachedPhotoUrl != null) {
            bindUser(cachedPhotoUrl);
            return;
        }
        
        if (!isAdded() || getView() == null || imgAvatar == null) {
            return;
        }

        cachedUserId = uid;
        
        userDAO.getDocument(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || getView() == null || imgAvatar == null) {
                        return;
                    }
                    
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            String photoUrl = doc.contains("photoUrl") ? doc.getString("photoUrl") : null;
                            if (photoUrl == null || photoUrl.isEmpty()) {
                                if (currentUser.getPhotoUrl() != null)
                                    photoUrl = currentUser.getPhotoUrl().toString();
                            }
                            cachedPhotoUrl = photoUrl;
                            bindUser(photoUrl);
                        }
                    } else {
                        String photoUrl = currentUser.getPhotoUrl() != null ? 
                                currentUser.getPhotoUrl().toString() : null;
                        cachedPhotoUrl = photoUrl;
                        bindFromAuth(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getView() == null || imgAvatar == null) {
                        return;
                    }
                    String photoUrl = currentUser.getPhotoUrl() != null ? 
                            currentUser.getPhotoUrl().toString() : null;
                    cachedPhotoUrl = photoUrl;
                    bindFromAuth(currentUser);
                });
    }

    private void bindUser(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void bindFromAuth(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null && !isHidden()) {
            adapter.startListening();
            isFragmentVisible = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
        isFragmentVisible = false;
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (adapter != null && isResumed()) {
            if (hidden) {
                if (isFragmentVisible) {
                    adapter.stopListening();
                    isFragmentVisible = false;
                }
            } else {
                if (!isFragmentVisible) {
                    adapter.startListening();
                    isFragmentVisible = true;
                }
            }
        }
    }

    // ---------------- ViewHolder ---------------- 
    public static class PostVH extends RecyclerView.ViewHolder {
        private final TextView tvAuthor, tvContent, tvCounts, tvTime, tvLike, tvComment, tvLikesCount;
        private final ImageView ivAuthorAvatar, ivImage, iconLike, iconComment;
        private final ImageButton btnMoreOptions;
        private final LinearLayout btnLike, btnComment;
        private final RecyclerView rvImages;
        private final LinearLayout containerMultipleImages;
        private final LinearLayout indicatorDots;
        private boolean isLiked = false;
        private String currentPostId = null;
        private String currentPostContent = null;
        private String currentPostUid = null;
        private long currentLikesCount = 0;
        private long currentCommentsCount = 0;
        private CommunityDAO communityDAO;
        private Fragment fragment;

        public PostVH(@NonNull View itemView, Fragment fragment) {
            super(itemView);
            this.fragment = fragment;
            communityDAO = new CommunityDAO();
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCounts = itemView.findViewById(R.id.tvCounts);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            rvImages = itemView.findViewById(R.id.rvImages);
            containerMultipleImages = itemView.findViewById(R.id.containerMultipleImages);
            indicatorDots = itemView.findViewById(R.id.indicatorDots);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);

            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnMoreOptions = itemView.findViewById(R.id.btn_more_options);
            iconLike = itemView.findViewById(R.id.iconLike);
            iconComment = itemView.findViewById(R.id.iconComment);
            tvLike = itemView.findViewById(R.id.tvLike);
            tvComment = itemView.findViewById(R.id.tvComment);
        }

        public void bind(Community p, int currentTab) {
            AuthService authService = new AuthService();
            FirebaseUser currentUser = authService.getCurrentUser();

            // Author info
            String authorName = (p.author != null && p.author.displayName != null) ? p.author.displayName : "User";
            tvAuthor.setText(authorName);
            
            // Author avatar
            if (ivAuthorAvatar != null) {
                String photoUrl = p.getPhotoURL();
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(ivAuthorAvatar.getContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivAuthorAvatar);
                } else {
                    ivAuthorAvatar.setImageResource(R.drawable.ic_person);
                }
                
                // Click avatar/name to open profile
                ivAuthorAvatar.setOnClickListener(v -> openUserProfile(p.uid));
                tvAuthor.setOnClickListener(v -> openUserProfile(p.uid));
            }

            tvContent.setText(p.content != null ? p.content : "");
            tvCounts.setText("💬 " + p.commentsCount);
            if (tvLikesCount != null) {
                tvLikesCount.setText("❤ " + p.likesCount);
                tvLikesCount.setOnClickListener(v -> showLikeList(p.id));
            }

            if (p.createdAt != null) {
                java.util.Date d = p.createdAt.toDate();
                tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
            } else tvTime.setText("");

            // Images - hỗ trợ nhiều ảnh
            // Kiểm tra cả imageUrls (mới) và imageUrl (cũ) để backward compatibility
            final List<String> imageUrls;
            
            // Ưu tiên imageUrls từ Firestore
            if (p.imageUrls != null && !p.imageUrls.isEmpty()) {
                imageUrls = p.imageUrls;
            } else if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                // Backward compatibility: nếu chỉ có imageUrl đơn
                List<String> tempList = new ArrayList<>();
                tempList.add(p.imageUrl);
                imageUrls = tempList;
            } else {
                imageUrls = null;
            }
            
            if (imageUrls != null && !imageUrls.isEmpty()) {
                if (imageUrls.size() == 1) {
                    // Single image - dùng ImageView (backward compatibility)
                    // Ẩn container nhiều ảnh
                    if (containerMultipleImages != null) {
                        containerMultipleImages.setVisibility(View.GONE);
                    }
                    if (rvImages != null) {
                        rvImages.setAdapter(null);
                    }
                    // Hiển thị ImageView đơn
                    ivImage.setVisibility(View.VISIBLE);
                    Glide.with(ivImage.getContext()).load(imageUrls.get(0)).into(ivImage);
                    final String singleImageUrl = imageUrls.get(0);
                    ivImage.setOnClickListener(v -> {
                        List<String> singleImageList = new ArrayList<>();
                        singleImageList.add(singleImageUrl);
                        openImageGallery(singleImageList, 0);
                    });
                } else {
                    // Multiple images - dùng RecyclerView horizontal
                    // Ẩn ImageView đơn
                    ivImage.setVisibility(View.GONE);
                    
                    // Hiển thị container nhiều ảnh
                    if (containerMultipleImages != null && rvImages != null) {
                        // QUAN TRỌNG: Set visibility TRƯỚC KHI setup
                        containerMultipleImages.setVisibility(View.VISIBLE);
                        rvImages.setVisibility(View.VISIBLE);
                        
                        setupImageRecyclerView(rvImages, imageUrls);
                        setupIndicatorDots(imageUrls.size());
                    }
                }
            } else {
                ivImage.setVisibility(View.GONE);
                if (containerMultipleImages != null) {
                    containerMultipleImages.setVisibility(View.GONE);
                }
                if (rvImages != null) {
                    rvImages.setAdapter(null);
                }
            }

            // Like state
            isLiked = currentUser != null && p.likedBy != null && p.likedBy.contains(currentUser.getUid());
            currentPostId = p.id;
            currentPostContent = p.content != null ? p.content : "";
            currentPostUid = p.uid;
            currentLikesCount = p.likesCount;
            currentCommentsCount = p.commentsCount;
            renderLike(isLiked);
            
            // More options button (3 dots) - show popup menu
            if (btnMoreOptions != null) {
                btnMoreOptions.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(itemView.getContext(), v);
                    popupMenu.getMenuInflater().inflate(R.menu.post_options_menu, popupMenu.getMenu());
                    
                    // Ẩn/hiện menu items dựa trên tab và quyền sở hữu
                    boolean isMyPost = currentUser != null && currentPostUid != null && currentPostUid.equals(currentUser.getUid());
                    boolean isMyPostsTab = currentTab == 4; // Tab Cá nhân
                    
                    // Ẩn "Báo cáo" nếu là bài viết của mình
                    if (isMyPost) {
                        popupMenu.getMenu().findItem(R.id.menu_report_post).setVisible(false);
                    }
                    
                    // Chỉ hiển thị "Xóa" khi ở tab Cá nhân và là bài viết của mình
                    if (isMyPostsTab && isMyPost) {
                        popupMenu.getMenu().findItem(R.id.menu_delete_post).setVisible(true);
                    } else {
                        popupMenu.getMenu().findItem(R.id.menu_delete_post).setVisible(false);
                    }
                    
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.menu_report_post) {
                            // Show PostFeedbackDialog
                            if (currentPostId != null && fragment != null) {
                                PostFeedbackDialog dialog = PostFeedbackDialog.newInstance(currentPostId, currentPostContent);
                                dialog.show(fragment.getParentFragmentManager(), "PostFeedbackDialog");
                            }
                            return true;
                        } else if (item.getItemId() == R.id.menu_delete_post) {
                            // Xóa bài viết
                            if (currentPostId != null && fragment != null) {
                                showDeletePostDialog(currentPostId);
                            }
                            return true;
                        }
                        return false;
                    });
                    
                    popupMenu.show();
                });
            }

            // Like button
            btnLike.setOnClickListener(v -> {
                if (currentUser == null) {
                    Toast.makeText(itemView.getContext(), "Bạn cần đăng nhập để thích bài viết", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean previousLiked = isLiked;
                long previousLikesCount = currentLikesCount;

                isLiked = !isLiked;
                renderLike(isLiked);
                
                if (isLiked) {
                    currentLikesCount++;
                    tvCounts.setText("💬 " + currentCommentsCount);
                    if (tvLikesCount != null) {
                        tvLikesCount.setText("❤ " + currentLikesCount);
                    }
                } else {
                    currentLikesCount = Math.max(0, currentLikesCount - 1);
                    tvCounts.setText("💬 " + currentCommentsCount);
                    if (tvLikesCount != null) {
                        tvLikesCount.setText("❤ " + currentLikesCount);
                    }
                }

                communityDAO.toggleLike(currentPostId)
                        .addOnFailureListener(e -> {
                            isLiked = previousLiked;
                            currentLikesCount = previousLikesCount;
                            renderLike(isLiked);
                            tvCounts.setText("💬 " + currentCommentsCount);
                            if (tvLikesCount != null) {
                                tvLikesCount.setText("❤ " + currentLikesCount);
                            }
                            // Error toggling like
                        });
            });

            // Comment button
            btnComment.setOnClickListener(v -> {
                Intent i = new Intent(itemView.getContext(), PostDetailActivity.class);
                i.putExtra(PostDetailActivity.EXTRA_POST_ID, p.id);
                itemView.getContext().startActivity(i);
            });
        }

        private void openUserProfile(String userId) {
            Intent i = new Intent(itemView.getContext(), UserProfileActivity.class);
            i.putExtra(UserProfileActivity.EXTRA_USER_ID, userId);
            itemView.getContext().startActivity(i);
        }

        private void showLikeList(String postId) {
            LikeListDialog dialog = new LikeListDialog(itemView.getContext(), postId);
            dialog.show();
        }

        private void openImageGallery(List<String> imageUrls, int position) {
            ArrayList<String> images = new ArrayList<>(imageUrls);
            Intent i = new Intent(itemView.getContext(), ImageGalleryActivity.class);
            i.putStringArrayListExtra(ImageGalleryActivity.EXTRA_IMAGES, images);
            i.putExtra(ImageGalleryActivity.EXTRA_POSITION, position);
            itemView.getContext().startActivity(i);
        }

        private void setupIndicatorDots(int count) {
            if (indicatorDots == null) return;
            
            indicatorDots.removeAllViews();
            
            if (count <= 1) {
                indicatorDots.setVisibility(View.GONE);
                return;
            }
            
            indicatorDots.setVisibility(View.VISIBLE);
            
            for (int i = 0; i < count; i++) {
                View dot = new View(itemView.getContext());
                int size = 8;
                int margin = 4;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(margin, 0, margin, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(android.R.drawable.presence_invisible);
                dot.getBackground().setColorFilter(android.graphics.Color.parseColor("#CCCCCC"), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
                indicatorDots.addView(dot);
            }
            
            // Highlight dot đầu tiên
            if (indicatorDots.getChildCount() > 0) {
                View firstDot = indicatorDots.getChildAt(0);
                firstDot.getBackground().setColorFilter(android.graphics.Color.parseColor("#666666"), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        
        private void updateIndicatorDots(int selectedPosition) {
            if (indicatorDots == null) return;
            
            for (int i = 0; i < indicatorDots.getChildCount(); i++) {
                View dot = indicatorDots.getChildAt(i);
                if (i == selectedPosition) {
                    dot.getBackground().setColorFilter(android.graphics.Color.parseColor("#666666"), 
                        android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    dot.getBackground().setColorFilter(android.graphics.Color.parseColor("#CCCCCC"), 
                        android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
        }
        
        private void setupImageRecyclerView(RecyclerView recyclerView, final List<String> imageUrls) {
            if (imageUrls == null || imageUrls.isEmpty()) {
                return;
            }
            
            // Clear old decorations and listeners to avoid inconsistency
            while (recyclerView.getItemDecorationCount() > 0) {
                recyclerView.removeItemDecorationAt(0);
            }
            recyclerView.clearOnScrollListeners();
            
            // Setup LinearLayoutManager horizontal
            LinearLayoutManager layoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);
            
            // Thêm khoảng cách giữa các ảnh
            recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    int position = parent.getChildAdapterPosition(view);
                    int spacing = 8; // 8dp spacing
                    
                    // Chuyển dp sang px
                    float density = itemView.getContext().getResources().getDisplayMetrics().density;
                    int spacingPx = (int) (spacing * density);
                    
                    if (position == 0) {
                        // Ảnh đầu tiên: margin left = 0, margin right = spacing/2
                        outRect.left = 0;
                        outRect.right = spacingPx / 2;
                    } else if (position == imageUrls.size() - 1) {
                        // Ảnh cuối cùng: margin left = spacing/2, margin right = 0
                        outRect.left = spacingPx / 2;
                        outRect.right = 0;
                    } else {
                        // Ảnh ở giữa: margin left và right = spacing/2
                        outRect.left = spacingPx / 2;
                        outRect.right = spacingPx / 2;
                    }
                }
            });
            
            // Tạo adapter cho RecyclerView
            androidx.recyclerview.widget.RecyclerView.Adapter<ImageVH> adapter = 
                new androidx.recyclerview.widget.RecyclerView.Adapter<ImageVH>() {
                @NonNull
                @Override
                public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_gallery_image, parent, false);
                    return new ImageVH(view);
                }

                @Override
                public void onBindViewHolder(@NonNull ImageVH holder, int position) {
                    if (position < imageUrls.size()) {
                        String imageUrl = imageUrls.get(position);
                        holder.bind(imageUrl);
                        final int finalPosition = position;
                        holder.itemView.setOnClickListener(v -> {
                            openImageGallery(imageUrls, finalPosition);
                        });
                    }
                }

                @Override
                public int getItemCount() {
                    return imageUrls.size();
                }
            };
            
            recyclerView.setAdapter(adapter);
            
            // Thêm scroll listener để update indicator dots
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                            updateIndicatorDots(firstVisiblePosition);
                        }
                    }
                }
            });
        }

        private static class ImageVH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final ImageView ivImage;
            private final ProgressBar progressBar;

            public ImageVH(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivImage);
                progressBar = itemView.findViewById(R.id.progressBar);
            }

            public void bind(String imageUrl) {
                if (imageUrl == null || imageUrl.isEmpty()) {
                    ivImage.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                
                progressBar.setVisibility(View.VISIBLE);
                ivImage.setVisibility(View.VISIBLE);
                
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(android.R.color.darker_gray)
                        .error(android.R.drawable.ic_menu_report_image)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivImage);
            }
        }

        private void renderLike(boolean liked) {
            if (liked) {
                iconLike.setImageResource(R.drawable.ic_favorite_filled);
                iconLike.setColorFilter(android.graphics.Color.parseColor("#E0245E"));
                tvLike.setTextColor(android.graphics.Color.parseColor("#E0245E"));
                tvLike.setText("Đã thích");
            } else {
                iconLike.setImageResource(R.drawable.ic_favorite_border);
                iconLike.setColorFilter(android.graphics.Color.parseColor("#606770"));
                tvLike.setTextColor(android.graphics.Color.parseColor("#606770"));
                tvLike.setText("Thích");
            }
        }
        
        private void showDeletePostDialog(String postId) {
            new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                    .setTitle("Xóa bài viết")
                    .setMessage("Bạn có chắc chắn muốn xóa bài viết này? Hành động này không thể hoàn tác.")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        deletePost(postId);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
        
        private void deletePost(String postId) {
            communityDAO.delete(postId, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(itemView.getContext(), "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(itemView.getContext(), "Lỗi xóa bài viết: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh notification count khi fragment hiển thị lại
        // Để badge cập nhật khi có thông báo mới
        loadUnreadNotificationCount();
    }
}
