package fpt.fall2025.posetrainer.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.Adapter.UserWorkoutCardAdapter;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentFavoriteBinding;

import java.util.ArrayList;

/**
 * FavoriteFragment - Fragment hiển thị danh sách user workouts đã lưu của user
 * Cho phép xem và xóa các workout đã tạo
 */
public class FavoriteFragment extends Fragment {
    private static final String TAG = "FavoriteFragment";
    private FragmentFavoriteBinding binding;
    private ArrayList<UserWorkout> userWorkouts;
    private UserWorkoutCardAdapter userWorkoutAdapter;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize data
        userWorkouts = new ArrayList<>();

        // Setup RecyclerView with vertical layout for card view
        binding.userWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        
        // Get current user ID and load avatar
        getCurrentUserId();
        loadUserAvatar();
    }

    /**
     * Load user avatar and name from Firebase Auth
     */
    private void loadUserAvatar() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Load user's profile photo
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(binding.userAvatar);
                Log.d(TAG, "Loaded user avatar: " + currentUser.getPhotoUrl());
            } else {
                // Use default profile image
                binding.userAvatar.setImageResource(R.drawable.profile);
                Log.d(TAG, "Using default profile image");
            }
            
            // Set user name
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                binding.userNameText.setText(displayName + "'s Workouts");
            } else {
                binding.userNameText.setText("My Workouts");
            }
        } else {
            // Use default profile image and text
            binding.userAvatar.setImageResource(R.drawable.profile);
            binding.userNameText.setText("My Workouts");
            Log.d(TAG, "No user logged in, using defaults");
        }
    }

    /**
     * Get current user ID from Firebase Auth
     */
    private void getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + userId);
            // Load user workouts after getting user ID
            loadUserWorkouts();
        } else {
            Log.w(TAG, "No user logged in");
            // Show empty state with login message
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.userWorkoutsRecyclerView.setVisibility(View.GONE);
            binding.emptyStateText.setText("Please log in to view your workouts");
        }
    }

    /**
     * Load user workouts from Firebase
     */
    private void loadUserWorkouts() {
        Log.d(TAG, "=== FAVORITE FRAGMENT LOADING ===");
        Log.d(TAG, "Loading user workouts for userId: " + userId);
        
        FirebaseService.getInstance().loadUserWorkouts(userId, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnUserWorkoutsLoadedListener() {
            @Override
            public void onUserWorkoutsLoaded(ArrayList<UserWorkout> workouts) {
                Log.d(TAG, "=== FAVORITE FRAGMENT CALLBACK ===");
                Log.d(TAG, "Received " + (workouts != null ? workouts.size() : "null") + " workouts");
                
                userWorkouts = workouts;
                
                if (userWorkouts == null || userWorkouts.isEmpty()) {
                    Log.d(TAG, "No workouts found, showing empty state");
                    // Show empty state
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    binding.userWorkoutsRecyclerView.setVisibility(View.GONE);
                    binding.emptyStateText.setText("No saved workouts yet.\nCreate or edit a workout to save it here!");
                } else {
                    Log.d(TAG, "Found " + userWorkouts.size() + " workouts, setting up adapter");
                    // Show workouts
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.userWorkoutsRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Setup adapter with delete listener
                    userWorkoutAdapter = new UserWorkoutCardAdapter(userWorkouts);
                    userWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                        // Refresh the list when an item is deleted
                        loadUserWorkouts();
                    });
                    binding.userWorkoutsRecyclerView.setAdapter(userWorkoutAdapter);
                    
                    Log.d(TAG, "Adapter set with " + userWorkouts.size() + " items");
                }
                
                Log.d(TAG, "=== END FAVORITE FRAGMENT CALLBACK ===");
            }
        });
    }

    /**
     * Refresh the list when returning to this fragment
     */
    @Override
    public void onResume() {
        super.onResume();
        // Check if user is still logged in and reload data
        getCurrentUserId();
        loadUserAvatar();
    }
}
