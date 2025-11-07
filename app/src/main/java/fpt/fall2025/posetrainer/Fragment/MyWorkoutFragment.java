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
        
        // Load user info and workouts
        loadUserFromFirestore();
        isDataLoaded = true;
    }

    /**
     * 🔄 Load user info from Firestore or Auth (like ProfileFragment and HomeFragment)
     */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in");
            // Show empty state with login message
            showEmptyState("Please log in to view your workouts");
            return;
        }

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
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
                    Log.e(TAG, "Failed to load user info", e);
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
     */
    private void loadUserWorkouts(String userId) {
        Log.d(TAG, "=== MY WORKOUT FRAGMENT LOADING ===");
        Log.d(TAG, "Loading user workouts for userId: " + userId);
        
        FirebaseService.getInstance().loadUserWorkouts(userId, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnUserWorkoutsLoadedListener() {
            @Override
            public void onUserWorkoutsLoaded(ArrayList<UserWorkout> workouts) {
                Log.d(TAG, "=== MY WORKOUT FRAGMENT CALLBACK ===");
                Log.d(TAG, "Received " + (workouts != null ? workouts.size() : "null") + " workouts");
                
                userWorkouts = workouts;
                
                if (userWorkouts == null || userWorkouts.isEmpty()) {
                    Log.d(TAG, "No workouts found, showing empty state");
                    showEmptyState("No saved workouts yet.\nCreate or edit a workout to save it here!");
                } else {
                    Log.d(TAG, "Found " + userWorkouts.size() + " workouts, setting up adapter");
                    showWorkoutsList();
                    
                    // Setup adapter with delete listener
                    userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
                    userWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                        // Refresh the list when an item is deleted
                        loadUserWorkouts(userId);
                    });
                    binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
                    
                    Log.d(TAG, "Adapter set with " + userWorkouts.size() + " items");
                }
                
                Log.d(TAG, "=== END MY WORKOUT FRAGMENT CALLBACK ===");
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
     */
    @Override
    public void onResume() {
        super.onResume();
        // Chỉ load data một lần ban đầu để tránh reload không cần thiết
        if (!isDataLoaded && isVisible() && isAdded()) {
            loadUserFromFirestore();
            isDataLoaded = true;
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Reload khi fragment trở nên visible (phòng trường hợp data đã thay đổi)
        // Chỉ refresh nếu fragment đã resumed và added
        if (!hidden && isAdded() && isResumed()) {
            loadUserFromFirestore();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        binding = null;
    }
}
