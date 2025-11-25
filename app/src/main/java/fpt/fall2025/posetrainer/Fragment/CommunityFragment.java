package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Activity.CreatePostActivity;
import fpt.fall2025.posetrainer.Activity.ImageGalleryActivity;
import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.Activity.UserProfileActivity;
import fpt.fall2025.posetrainer.Data.CommunityRepository;
import fpt.fall2025.posetrainer.Dialog.LikeListDialog;
import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.View.CommunityViewModel;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView imgAvatar;
    private FirestoreRecyclerAdapter<Community, PostVH> adapter;
    private LinearLayoutManager layoutManager;
    private CommunityViewModel viewModel;
    
    // UI Components
    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private TabLayout tabLayout;
    private SearchView searchView;
    private ImageButton btnNotifications;
    private LinearLayout emptyState, loadingState;
    
    // Tab state
    private int currentTab = 0; // 0: Tất cả, 1: Phổ biến, 2: Mới nhất, 3: Theo dõi
    private String currentSearchQuery = "";
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private String cachedPhotoUrl = null;
    private boolean isFragmentVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        imgAvatar = v.findViewById(R.id.profile_image);

        // ViewModel để lưu vị trí scroll
        viewModel = new ViewModelProvider(requireActivity()).get(CommunityViewModel.class);

        // Initialize UI components
        rvFeed = v.findViewById(R.id.rvFeed);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        tabLayout = v.findViewById(R.id.tabLayout);
        searchView = v.findViewById(R.id.searchView);
        btnNotifications = v.findViewById(R.id.btnNotifications);
        emptyState = v.findViewById(R.id.emptyState);
        loadingState = v.findViewById(R.id.loadingState);

        // Setup tabs
        setupTabs();
        
        // Setup search
        setupSearch();
        
        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(this::refreshFeed);
        
        // Setup notifications button
        btnNotifications.setOnClickListener(view -> {
            // Mở NotificationFragment thông qua MainActivity
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openNotificationFragment();
            } else {
                // Nếu không phải MainActivity, mở MainActivity với intent
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.putExtra("openFragment", "notifications");
                startActivity(intent);
            }
        });

        // Load avatar user
        loadUserFromFirestore();

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

        // Create post button
        TextView tvCreatePost = v.findViewById(R.id.tvCreatePost);
        tvCreatePost.setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(i);
        });

        // change text color in SearchView
        try {
            SearchView.SearchAutoComplete searchAutoComplete =
                    searchView.findViewById(androidx.appcompat.R.id.search_src_text);

            if (searchAutoComplete != null) {
                searchAutoComplete.setTextColor(android.graphics.Color.parseColor("#ffffff"));

                searchAutoComplete.setHintTextColor(android.graphics.Color.parseColor("#99ffffff"));

                searchAutoComplete.setTextSize(14);
            }

            ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (searchIcon != null) {
                searchIcon.setColorFilter(android.graphics.Color.parseColor("#99ffffff"),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }

            ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeIcon != null) {
                closeIcon.setColorFilter(android.graphics.Color.parseColor("#99ffffff"),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error customizing SearchView: " + e.getMessage());
        }

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

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadFeed();
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

    private void loadFeed() {
        showLoading(true);
        
        Query baseQuery = db.collection("community");
        
        // Apply search filter
        if (!TextUtils.isEmpty(currentSearchQuery)) {
            // Note: Firestore doesn't support full-text search natively
            // This is a simple implementation - for production, consider using Algolia or similar
            baseQuery = baseQuery.whereGreaterThanOrEqualTo("content", currentSearchQuery)
                    .whereLessThanOrEqualTo("content", currentSearchQuery + "\uf8ff");
        }
        
        // Apply tab filter
        switch (currentTab) {
            case 0: // Tất cả
                baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                break;
            case 1: // Phổ biến
                // Sort by likesCount only (đơn giản hóa để tránh cần composite index)
                // Lưu ý: Nếu muốn sort chính xác hơn, cần tạo composite index hoặc dùng engagementScore field
                baseQuery = baseQuery.orderBy("likesCount", Query.Direction.DESCENDING);
                break;
            case 2: // Mới nhất
                baseQuery = baseQuery.orderBy("createdAt", Query.Direction.DESCENDING);
                break;
            case 3: // Theo dõi
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    showEmptyState("Bạn cần đăng nhập để xem bài viết từ người đang theo dõi");
                    return;
                }
                // Get following list and filter
                loadFollowingFeed(currentUser.getUid());
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
                return new PostVH(item);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                Log.d("CommunityFragment", "Binding post at position " + position + ", id: " + p.id);
                Log.d("CommunityFragment", "Post " + p.id + " - imageUrls: " + (p.imageUrls != null ? p.imageUrls.size() : "null"));
                if (p.imageUrls != null && !p.imageUrls.isEmpty()) {
                    for (int i = 0; i < p.imageUrls.size(); i++) {
                        Log.d("CommunityFragment", "  imageUrls[" + i + "]: " + p.imageUrls.get(i));
                    }
                }
                h.bind(p);
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
                if (getItemCount() == 0) {
                    showEmptyState("Chưa có bài viết nào");
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
        db.collection("users").document(currentUserId)
                .collection("following")
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
                    
                    Query query = db.collection("community")
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
                            return new PostVH(item);
                        }

                        @Override
                        protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                            h.bind(p);
                        }

                        @Override
                        public void onDataChanged() {
                            super.onDataChanged();
                            showLoading(false);
                            if (getItemCount() == 0) {
                                showEmptyState("Chưa có bài viết nào từ người bạn đang theo dõi");
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        
        if (cachedUserId != null && cachedUserId.equals(uid) && cachedPhotoUrl != null) {
            Log.d(TAG, "User avatar đã được cache, sử dụng cache");
            bindUser(cachedPhotoUrl);
            return;
        }
        
        if (!isAdded() || getView() == null || imgAvatar == null) {
            Log.w(TAG, "Fragment không còn attached, bỏ qua load user avatar");
            return;
        }

        cachedUserId = uid;
        
        db.collection("users").document(uid)
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
                    Log.e(TAG, "loadUserFromFirestore: " + e.getMessage());
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
        private final LinearLayout btnLike, btnComment;
        private final RecyclerView rvImages;
        private final LinearLayout containerMultipleImages;
        private final LinearLayout indicatorDots;
        private boolean isLiked = false;
        private String currentPostId = null;
        private long currentLikesCount = 0;
        private long currentCommentsCount = 0;

        public PostVH(@NonNull View itemView) {
            super(itemView);
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
            iconLike = itemView.findViewById(R.id.iconLike);
            iconComment = itemView.findViewById(R.id.iconComment);
            tvLike = itemView.findViewById(R.id.tvLike);
            tvComment = itemView.findViewById(R.id.tvComment);
        }

        public void bind(Community p) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

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
            tvCounts.setText("❤ " + p.likesCount + "   💬 " + p.commentsCount);
            if (tvLikesCount != null) {
                tvLikesCount.setText("❤ " + p.likesCount);
                tvLikesCount.setOnClickListener(v -> showLikeList(p.id));
            }

            if (p.createdAt != null) {
                java.util.Date d = p.createdAt.toDate();
                tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
            } else tvTime.setText("");

            // Images - hỗ trợ nhiều ảnh
            // Debug: Kiểm tra dữ liệu từ Firestore
            android.util.Log.d("PostVH", "=== BINDING POST " + p.id + " ===");
            android.util.Log.d("PostVH", "p.imageUrls: " + (p.imageUrls != null ? "size=" + p.imageUrls.size() : "NULL"));
            android.util.Log.d("PostVH", "p.imageUrl: " + (p.imageUrl != null && !p.imageUrl.isEmpty() ? p.imageUrl : "NULL/EMPTY"));
            
            if (p.imageUrls != null) {
                for (int i = 0; i < p.imageUrls.size(); i++) {
                    android.util.Log.d("PostVH", "  imageUrls[" + i + "]: " + p.imageUrls.get(i));
                }
            }
            
            // Kiểm tra cả imageUrls (mới) và imageUrl (cũ) để backward compatibility
            final List<String> imageUrls;
            
            // Ưu tiên imageUrls từ Firestore
            if (p.imageUrls != null && !p.imageUrls.isEmpty()) {
                imageUrls = p.imageUrls;
                android.util.Log.d("PostVH", "✓ Sử dụng imageUrls với " + imageUrls.size() + " ảnh");
            } else if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                // Backward compatibility: nếu chỉ có imageUrl đơn
                List<String> tempList = new ArrayList<>();
                tempList.add(p.imageUrl);
                imageUrls = tempList;
                android.util.Log.d("PostVH", "✓ Sử dụng imageUrl (backward compatibility)");
            } else {
                imageUrls = null;
                android.util.Log.d("PostVH", "✗ Không có ảnh nào");
            }
            
            if (imageUrls != null && !imageUrls.isEmpty()) {
                android.util.Log.d("PostVH", ">>> Post " + p.id + " sẽ hiển thị " + imageUrls.size() + " ảnh <<<");
                if (imageUrls.size() == 1) {
                    // Single image - dùng ImageView (backward compatibility)
                    android.util.Log.d("PostVH", "→ 1 ảnh: Dùng ImageView");
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
                    android.util.Log.d("PostVH", "→ " + imageUrls.size() + " ảnh: Dùng RecyclerView");
                    // Ẩn ImageView đơn
                    ivImage.setVisibility(View.GONE);
                    
                    // Hiển thị container nhiều ảnh
                    if (containerMultipleImages != null && rvImages != null) {
                        android.util.Log.d("PostVH", "  - Container và RecyclerView không null, bắt đầu setup");
                        
                        // QUAN TRỌNG: Set visibility TRƯỚC KHI setup
                        containerMultipleImages.setVisibility(View.VISIBLE);
                        rvImages.setVisibility(View.VISIBLE);
                        android.util.Log.d("PostVH", "  - Container visibility set to: " + containerMultipleImages.getVisibility() + " (VISIBLE=0)");
                        android.util.Log.d("PostVH", "  - RecyclerView visibility set to: " + rvImages.getVisibility() + " (VISIBLE=0)");
                        
                        setupImageRecyclerView(rvImages, imageUrls);
                        setupIndicatorDots(imageUrls.size());
                        
                        android.util.Log.d("PostVH", "  - Sau setup - Container visibility: " + containerMultipleImages.getVisibility());
                        android.util.Log.d("PostVH", "  - Sau setup - RecyclerView visibility: " + rvImages.getVisibility());
                        android.util.Log.d("PostVH", "  - RecyclerView adapter: " + (rvImages.getAdapter() != null ? "NOT NULL" : "NULL"));
                    } else {
                        android.util.Log.e("PostVH", "  - ERROR: Container=" + (containerMultipleImages != null ? "NOT NULL" : "NULL") 
                                + ", RecyclerView=" + (rvImages != null ? "NOT NULL" : "NULL"));
                    }
                }
            } else {
                android.util.Log.d("PostVH", ">>> Post " + p.id + " không có ảnh <<<");
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
            currentLikesCount = p.likesCount;
            currentCommentsCount = p.commentsCount;
            renderLike(isLiked);

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
                    tvCounts.setText("❤ " + currentLikesCount + "   💬 " + currentCommentsCount);
                    if (tvLikesCount != null) {
                        tvLikesCount.setText("❤ " + currentLikesCount);
                    }
                } else {
                    currentLikesCount = Math.max(0, currentLikesCount - 1);
                    tvCounts.setText("❤ " + currentLikesCount + "   💬 " + currentCommentsCount);
                    if (tvLikesCount != null) {
                        tvLikesCount.setText("❤ " + currentLikesCount);
                    }
                }

                new CommunityRepository()
                        .toggleLike(currentPostId)
                        .addOnFailureListener(e -> {
                            isLiked = previousLiked;
                            currentLikesCount = previousLikesCount;
                            renderLike(isLiked);
                            tvCounts.setText("❤ " + currentLikesCount + "   💬 " + currentCommentsCount);
                            if (tvLikesCount != null) {
                                tvLikesCount.setText("❤ " + currentLikesCount);
                            }
                            Log.e("LIKE", "Error toggling like: " + e.getMessage());
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
                android.util.Log.e("PostVH", "setupImageRecyclerView: imageUrls null hoặc empty!");
                return;
            }
            
            android.util.Log.d("PostVH", "=== SETUP RecyclerView với " + imageUrls.size() + " ảnh ===");
            for (int i = 0; i < imageUrls.size(); i++) {
                android.util.Log.d("PostVH", "  URL[" + i + "]: " + imageUrls.get(i));
            }
            
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
                    android.util.Log.d("PostVH", "  onCreateViewHolder called");
                    View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_gallery_image, parent, false);
                    return new ImageVH(view);
                }

                @Override
                public void onBindViewHolder(@NonNull ImageVH holder, int position) {
                    if (position < imageUrls.size()) {
                        String imageUrl = imageUrls.get(position);
                        android.util.Log.d("PostVH", "  onBindViewHolder position " + position + ": " + imageUrl);
                        holder.bind(imageUrl);
                        final int finalPosition = position;
                        holder.itemView.setOnClickListener(v -> {
                            android.util.Log.d("PostVH", "  Click ảnh position " + finalPosition);
                            openImageGallery(imageUrls, finalPosition);
                        });
                    } else {
                        android.util.Log.e("PostVH", "  ERROR: position " + position + " >= size " + imageUrls.size());
                    }
                }

                @Override
                public int getItemCount() {
                    int count = imageUrls.size();
                    android.util.Log.d("PostVH", "  getItemCount: " + count);
                    return count;
                }
            };
            
            android.util.Log.d("PostVH", "  Setting adapter to RecyclerView");
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
            
            android.util.Log.d("PostVH", "=== RecyclerView setup hoàn tất ===");
            android.util.Log.d("PostVH", "  - Visibility: " + recyclerView.getVisibility() + " (VISIBLE=0, GONE=8)");
            android.util.Log.d("PostVH", "  - Adapter: " + (recyclerView.getAdapter() != null ? "NOT NULL" : "NULL"));
            android.util.Log.d("PostVH", "  - Adapter itemCount: " + (recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : "N/A"));
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
    }
}
