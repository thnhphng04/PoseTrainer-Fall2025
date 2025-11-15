package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.Activity.SearchActivity;
import fpt.fall2025.posetrainer.Adapter.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.Activity.MainActivity;
import fpt.fall2025.posetrainer.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ArrayList<WorkoutTemplate> workoutTemplates;
    private ArrayList<WorkoutTemplate> filteredWorkoutTemplates;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Filter states
    private String selectedCategory = "Latest Updates";
    private String selectedDuration = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initBodyPartsListeners();
        workoutTemplates = new ArrayList<>();
        filteredWorkoutTemplates = new ArrayList<>();

        binding.view1.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        // Set adapter immediately to avoid "No adapter attached" warning
        binding.view1.setAdapter(new WorkoutTemplateAdapter(filteredWorkoutTemplates));

        setupSearchListeners();
        setupFilterListeners();
        setupNotificationButton();
        loadCurrentUserInfo();
        loadWorkoutTemplates();
        loadUnreadNotificationCount();
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
            } else {
                Log.w(TAG, "Activity không phải MainActivity, không thể mở NotificationFragment");
            }
        });
    }
    
    /**
     * Load và hiển thị số lượng thông báo chưa đọc
     * Được gọi khi fragment hiển thị và định kỳ để cập nhật
     */
    private void loadUnreadNotificationCount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User chưa đăng nhập, không thể load notification count");
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Đang load số lượng thông báo chưa đọc cho user: " + uid);
        
        // Gọi FirebaseService để đếm thông báo chưa đọc
        FirebaseService.getInstance().countUnreadNotifications(uid, count -> {
            Log.d(TAG, "Số thông báo chưa đọc: " + count);
            updateNotificationBadge(count);
        });
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
            
            Log.d(TAG, "✓ Hiển thị badge với số lượng: " + count);
        } else {
            // Không có thông báo chưa đọc → Ẩn badge
            badgeTextView.setVisibility(android.view.View.GONE);
            Log.d(TAG, "✓ Ẩn badge (không có thông báo chưa đọc)");
        }
    }

    private void loadWorkoutTemplates() {
        FirebaseService.getInstance().loadWorkoutTemplates(
                (androidx.appcompat.app.AppCompatActivity) getActivity(),
                templates -> {
                    workoutTemplates = templates;
                    applyFilters();
                }
        );
    }

    /**
     * 🔄 Load current user info from Firestore or Auth (like ProfileFragment)
     */
    private void loadCurrentUserInfo() {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            Log.w(TAG, "Không tìm thấy người dùng hiện tại");
            return;
        }

        String uid = current.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
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
                    Log.e(TAG, "Lỗi: Không thể tải thông tin người dùng", e);
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
            isDataLoaded = true;
        }
        
        // Luôn refresh notification count khi fragment hiển thị lại
        // Để badge cập nhật khi có thông báo mới
        loadUnreadNotificationCount();
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Khi fragment trở nên visible lại, refresh user info (phòng trường hợp user đã chỉnh sửa profile)
        // Chỉ refresh nếu fragment đã resumed và added
        if (!hidden && isAdded() && isResumed()) {
            loadCurrentUserInfo();
            // Refresh notification count khi fragment hiển thị lại
            loadUnreadNotificationCount();
        }
    }

    private void initBodyPartsListeners() {
        binding.bodyPartFullBody.setOnClickListener(v -> handleBodyPartClick("Full Body"));
        binding.bodyPartCore.setOnClickListener(v -> handleBodyPartClick("Core"));
        binding.bodyPartArm.setOnClickListener(v -> handleBodyPartClick("Arm"));
        binding.bodyPartChest.setOnClickListener(v -> handleBodyPartClick("Chest"));
        binding.bodyPartButtLeg.setOnClickListener(v -> handleBodyPartClick("Butt & Leg"));
        binding.bodyPartBack.setOnClickListener(v -> handleBodyPartClick("Back"));
        binding.bodyPartShoulder.setOnClickListener(v -> handleBodyPartClick("Shoulder"));
        binding.bodyPartCustom.setOnClickListener(v -> handleBodyPartClick("Custom"));
    }

    private void handleBodyPartClick(String bodyPart) {
        Toast.makeText(getActivity(), "Selected: " + bodyPart, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), fpt.fall2025.posetrainer.Activity.ChallengeDetailActivity.class);
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

        // Update RecyclerView
        binding.view1.setAdapter(new WorkoutTemplateAdapter(filteredWorkoutTemplates));
        
        Log.d(TAG, "Đã lọc bài tập: " + filteredWorkoutTemplates.size() + " / " + workoutTemplates.size());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        binding = null;
    }
}
