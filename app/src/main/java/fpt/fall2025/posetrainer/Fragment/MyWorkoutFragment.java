package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.Activity.EditWorkoutActivity;
import fpt.fall2025.posetrainer.Activity.PlanPreviewActivity;
import fpt.fall2025.posetrainer.Adapter.UserWorkoutCardAdapter;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentMyworkoutBinding;

import java.util.ArrayList;

/**
 * MyWorkoutFragment - Fragment hiển thị danh sách user workouts đã lưu của user
 * Cho phép xem và xóa các workout đã tạo
 */
public class MyWorkoutFragment extends Fragment {
    private FragmentMyworkoutBinding binding;
    private ArrayList<UserWorkout> userWorkouts; // Workouts của user (không phải AI)
    private ArrayList<UserWorkout> aiWorkouts; // Workouts từ AI
    private UserWorkoutCardAdapter userWorkoutAdapter; // Adapter cho user workouts
    private UserWorkoutCardAdapter aiWorkoutAdapter; // Adapter cho AI workouts
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private boolean isWorkoutsLoaded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyworkoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize data
        userWorkouts = new ArrayList<>();
        aiWorkouts = new ArrayList<>();

        // Setup RecyclerView cho user workouts
        binding.userWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
        binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
        
        // Setup RecyclerView cho AI workouts
        binding.aiWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        aiWorkoutAdapter = new UserWorkoutCardAdapter(aiWorkouts);
        binding.aiWorkoutsRecyclerView.setAdapter(aiWorkoutAdapter);
        
        // Setup delete listeners
        setupDeleteListeners();
        
        // Setup click listener for create AI workout button
        setupCreateAIWorkoutButton();
        
        // Setup click listener for create new workout FAB
        setupCreateNewWorkoutButton();
        
        // Load user info and workouts
        loadUserFromFirestore();
        isDataLoaded = true;
    }

    /**
     * 🔄 Load user info from Firestore or Auth (like ProfileFragment and HomeFragment)
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Show empty state with login message
            showEmptyState("Vui lòng đăng nhập để xem bài tập của bạn");
            return;
        }

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isWorkoutsLoaded) {
            // Vẫn cần load workouts để đảm bảo data mới nhất
            loadUserWorkouts(uid);
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            return;
        }
        
        cachedUserId = uid;
        
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    // Kiểm tra lại fragment view trước khi update UI
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    
                    if (doc.exists()) {
                        // Try to get user data from Firestore
                        String name = doc.getString("displayName");
                        String photoUrl = null;
                        
                        // Ưu tiên field "photoUrl" (mới từ Storage)
                        if (doc.contains("photoUrl")) {
                            photoUrl = doc.getString("photoUrl");
                        } else if (doc.contains("photourl")) { // trường cũ
                            photoUrl = doc.getString("photourl");
                        }

                        if (photoUrl == null || photoUrl.isEmpty()) {
                            photoUrl = currentUser.getPhotoUrl() != null 
                                ? currentUser.getPhotoUrl().toString() 
                                : null;
                        }

                        updateUserUI(name, photoUrl);
                        loadUserWorkouts(uid);
                    } else {
                        updateUserUIFromAuth(currentUser);
                        loadUserWorkouts(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    // Kiểm tra lại fragment view trước khi update UI
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    updateUserUIFromAuth(currentUser);
                    loadUserWorkouts(uid);
                });
    }

    private void updateUserUI(String name, String photoUrl) {
        if (name != null && !name.isEmpty()) {
            binding.userNameText.setText("Không gian của " + name);
        } else {
            binding.userNameText.setText("My Workouts");
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .circleCrop()
                    .into(binding.userAvatar);
        } else {
            binding.userAvatar.setImageResource(R.drawable.profile);
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

    /**
     * Setup click listener cho nút tạo buổi tập với AI
     */
    private void setupCreateAIWorkoutButton() {
        binding.btnCreateAiWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PlanPreviewActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Setup click listener cho nút + tạo bài tập mới
     */
    private void setupCreateNewWorkoutButton() {
        binding.fabCreateWorkout.setOnClickListener(v -> {
            // Mở EditWorkoutActivity với mode tạo mới
            Intent intent = new Intent(requireContext(), EditWorkoutActivity.class);
            intent.putExtra("createNew", true);
            intent.putExtra("workoutTemplateId", "new"); // Dummy ID để tránh null
            startActivity(intent);
        });
    }

    /**
     * Setup delete listeners cho cả 2 adapter
     */
    private void setupDeleteListeners() {
        if (userWorkoutAdapter != null) {
            userWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                // Refresh the list when an item is deleted
                isWorkoutsLoaded = false;
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    loadUserWorkouts(currentUser.getUid());
                }
            });
        }
        
        if (aiWorkoutAdapter != null) {
            aiWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                // Refresh the list when an item is deleted
                isWorkoutsLoaded = false;
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    loadUserWorkouts(currentUser.getUid());
                }
            });
        }
    }

    /**
     * Filter workouts và hiển thị vào 2 RecyclerView riêng biệt
     */
    private void filterAndDisplayWorkouts(ArrayList<UserWorkout> allWorkouts) {
        if (allWorkouts == null || allWorkouts.isEmpty()) {
            userWorkouts.clear();
            aiWorkouts.clear();
            updateWorkoutCounts();
            return;
        }

        // Filter workouts
        userWorkouts.clear();
        aiWorkouts.clear();
        
        for (UserWorkout workout : allWorkouts) {
            String source = workout.getSource();
            if (source != null && source.equals("ai")) {
                aiWorkouts.add(workout);
            } else {
                userWorkouts.add(workout);
            }
        }

        // Cập nhật adapters
        userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
        aiWorkoutAdapter = new UserWorkoutCardAdapter(aiWorkouts);
        
        // Setup delete listeners lại
        setupDeleteListeners();
        
        // Update RecyclerViews
        binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
        binding.aiWorkoutsRecyclerView.setAdapter(aiWorkoutAdapter);
        
        // Cập nhật count texts
        updateWorkoutCounts();
    }

    /**
     * Cập nhật số lượng workouts cho cả 2 section
     */
    private void updateWorkoutCounts() {
        if (binding.userWorkoutCountText != null) {
            binding.userWorkoutCountText.setText(userWorkouts.size() + " bài tập");
        }
        if (binding.aiWorkoutCountText != null) {
            binding.aiWorkoutCountText.setText(aiWorkouts.size() + " bài tập");
        }
    }

    /**
     * Load user workouts from Firebase
     * Đã tối ưu với cache và reuse adapter để tránh lag
     */
    private void loadUserWorkouts(String userId) {
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(userId) && isWorkoutsLoaded && 
            userWorkouts != null && !userWorkouts.isEmpty()) {
            return;
        }
        
        // Kiểm tra activity có tồn tại không
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }
        
        FirebaseService.getInstance().loadUserWorkouts(userId, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnUserWorkoutsLoadedListener() {
            @Override
            public void onUserWorkoutsLoaded(ArrayList<UserWorkout> workouts) {
                isWorkoutsLoaded = true;
                
                // Kiểm tra fragment view có còn attached không
                if (!isAdded() || binding == null) {
                    return;
                }
                
                // Filter và hiển thị workouts vào 2 RecyclerView riêng biệt
                filterAndDisplayWorkouts(workouts != null ? workouts : new ArrayList<>());
            }
        });
    }

    private void showEmptyState(String message) {
        // Empty state chỉ hiển thị khi cả 2 section đều trống
        if ((userWorkouts == null || userWorkouts.isEmpty()) && 
            (aiWorkouts == null || aiWorkouts.isEmpty())) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.emptyStateText.setText(message);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private boolean isDataLoaded = false;
    
    /**
     * Refresh the list when returning to this fragment
     * Luôn reload workouts để đảm bảo data mới nhất
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isVisible() && isAdded()) {
            if (!isDataLoaded) {
                // Lần đầu load
                loadUserFromFirestore();
                isDataLoaded = true;
            } else {
                // Reload workouts mỗi khi fragment resume để đảm bảo data mới nhất
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    isWorkoutsLoaded = false; // Reset flag để force reload
                    loadUserWorkouts(currentUser.getUid());
                }
            }
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Reload workouts khi fragment được show lại từ hidden state
        if (!hidden && isAdded() && isResumed()) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                isWorkoutsLoaded = false; // Reset flag để force reload
                loadUserWorkouts(currentUser.getUid());
            }
        }
    }
    
    /**
     * Public method để refresh workouts từ bên ngoài (ví dụ từ Activity)
     */
    public void refreshWorkouts() {
        if (isAdded() && isResumed()) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                isWorkoutsLoaded = false; // Reset flag để force reload
                loadUserWorkouts(currentUser.getUid());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        // Không reset cachedUserId và isWorkoutsLoaded để cache vẫn hoạt động khi fragment bị recreate
        binding = null;
        userWorkoutAdapter = null; // Clear adapter reference
    }
}
