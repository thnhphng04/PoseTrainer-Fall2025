package fpt.fall2025.posetrainer.Fragment;

import android.os.Bundle;
import android.util.Log;
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
    private static final String TAG = "MyWorkoutFragment";
    private FragmentMyworkoutBinding binding;
    private ArrayList<UserWorkout> userWorkouts;
    private UserWorkoutCardAdapter userWorkoutAdapter;
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

        // Setup RecyclerView with vertical layout for card view
        binding.userWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        // Set adapter immediately to avoid "No adapter attached" warning
        userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
        binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
        
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
            Log.w(TAG, "Cảnh báo: Chưa có người dùng đăng nhập");
            // Show empty state with login message
            showEmptyState("Vui lòng đăng nhập để xem bài tập của bạn");
            return;
        }

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isWorkoutsLoaded) {
            Log.d(TAG, "User data và workouts đã được cache, bỏ qua reload");
            // Vẫn cần load workouts để đảm bảo data mới nhất
            loadUserWorkouts(uid);
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            Log.w(TAG, "Fragment không còn attached, bỏ qua load user info");
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
                    Log.e(TAG, "Lỗi: Không thể tải thông tin người dùng", e);
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
     * Load user workouts from Firebase
     * Đã tối ưu với cache và reuse adapter để tránh lag
     */
    private void loadUserWorkouts(String userId) {
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(userId) && isWorkoutsLoaded && 
            userWorkouts != null && !userWorkouts.isEmpty()) {
            Log.d(TAG, "Workouts đã được cache, bỏ qua reload");
            return;
        }
        
        // Kiểm tra activity có tồn tại không
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            Log.w(TAG, "Activity không tồn tại, không thể load workouts");
            return;
        }
        
        Log.d(TAG, "========== ĐANG TẢI BÀI TẬP CỦA NGƯỜI DÙNG ==========");
        Log.d(TAG, "Đang tải bài tập của người dùng với ID: " + userId);
        
        FirebaseService.getInstance().loadUserWorkouts(userId, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnUserWorkoutsLoadedListener() {
            @Override
            public void onUserWorkoutsLoaded(ArrayList<UserWorkout> workouts) {
                Log.d(TAG, "========== CALLBACK TẢI BÀI TẬP ==========");
                Log.d(TAG, "Đã nhận được " + (workouts != null ? workouts.size() : 0) + " bài tập");
                
                userWorkouts = workouts != null ? workouts : new ArrayList<>();
                isWorkoutsLoaded = true;
                
                // Kiểm tra fragment view có còn attached không
                if (!isAdded() || binding == null) {
                    return;
                }
                
                if (userWorkouts.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy bài tập nào, hiển thị trạng thái trống");
                    showEmptyState("Chưa có bài tập đã lưu.\nTạo hoặc chỉnh sửa bài tập để lưu ở đây!");
                } else {
                    Log.d(TAG, "Tìm thấy " + userWorkouts.size() + " bài tập, đang cập nhật adapter");
                    showWorkoutsList();
                    
                    // Reuse adapter thay vì tạo mới mỗi lần
                    if (userWorkoutAdapter == null) {
                        userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
                        userWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                            // Refresh the list when an item is deleted
                            isWorkoutsLoaded = false; // Reset flag để reload
                            loadUserWorkouts(userId);
                        });
                        binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
                    } else {
                        // Update adapter với data mới
                        // Giả sử UserWorkoutCardAdapter có method updateList, nếu không thì tạo adapter mới
                        userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
                        userWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                            // Refresh the list when an item is deleted
                            isWorkoutsLoaded = false; // Reset flag để reload
                            loadUserWorkouts(userId);
                        });
                        binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
                    }
                    
                    Log.d(TAG, "Adapter đã được cập nhật với " + userWorkouts.size() + " mục");
                }
                
                Log.d(TAG, "========== KẾT THÚC CALLBACK TẢI BÀI TẬP ==========");
            }
        });
    }

    private void showEmptyState(String message) {
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.userWorkoutsRecyclerView.setVisibility(View.GONE);
        binding.emptyStateText.setText(message);
    }

    private void showWorkoutsList() {
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.userWorkoutsRecyclerView.setVisibility(View.VISIBLE);
    }

    private boolean isDataLoaded = false;
    
    /**
     * Refresh the list when returning to this fragment
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    @Override
    public void onResume() {
        super.onResume();
        // Chỉ load data lần đầu để tránh reload không cần thiết
        if (isVisible() && isAdded()) {
            if (!isDataLoaded) {
                // Lần đầu load
                loadUserFromFirestore();
                isDataLoaded = true;
            } else {
                // Chỉ load workouts nếu cần, user info đã được cache
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    loadUserWorkouts(currentUser.getUid());
                }
            }
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Không reload khi fragment trở nên visible để tối ưu hiệu năng
        // Data đã được cache, chỉ reload nếu cần
        if (!hidden && isAdded() && isResumed()) {
            Log.d(TAG, "Fragment visible, kiểm tra cache");
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
