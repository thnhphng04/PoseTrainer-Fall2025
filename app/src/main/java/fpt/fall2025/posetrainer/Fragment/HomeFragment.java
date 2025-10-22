package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import fpt.fall2025.posetrainer.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ArrayList<WorkoutTemplate> workoutTemplates;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initBodyPartsListeners();

        // Initialize data
        workoutTemplates = new ArrayList<>();

        // Setup RecyclerView
        binding.view1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Setup search listeners
        setupSearchListeners();

        // Load current user info
        loadCurrentUserInfo();

        // Load workout templates from Firestore
        loadWorkoutTemplates();
    }


    /**
     * Setup search bar and search option buttons
     */
    private void setupSearchListeners() {
        // Main search bar
        binding.searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

    }

    /**
     * Load workout templates from Firebase Firestore
     */
    private void loadWorkoutTemplates() {
        Log.d(TAG, "Loading workout templates from Firestore...");

        FirebaseService.getInstance().loadWorkoutTemplates((androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnWorkoutTemplatesLoadedListener() {
            @Override
            public void onWorkoutTemplatesLoaded(ArrayList<WorkoutTemplate> templates) {
                workoutTemplates = templates;
                binding.view1.setAdapter(new WorkoutTemplateAdapter(workoutTemplates));
                Log.d(TAG, "Loaded " + workoutTemplates.size() + " workout templates");
            }
        });
    }

    /**
     * Load current user information from Firestore
     */
    private void loadCurrentUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user found");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Loading user info for UID: " + uid);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "HomeFragment: Firestore query completed");
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "HomeFragment: User document exists");
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            Log.d(TAG, "HomeFragment: User object created successfully");
                            updateUserUI(user);
                        } else {
                            Log.e(TAG, "HomeFragment: Failed to convert document to User object");
                        }
                    } else {
                        Log.w(TAG, "HomeFragment: User document not found in Firestore for UID: " + uid);
                        // Fallback: use Firebase Auth user data directly
                        updateUserUIFromFirebaseAuth(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "HomeFragment: Failed to load user info", e);
                });
    }

    /**
     * Update UI with user information
     */
    private void updateUserUI(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            binding.tvUserName.setText(user.getDisplayName());
        } else {
            binding.tvUserName.setText("User");
        }

        if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoURL())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.ivUserAvatar);
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.profile);
        }
    }

    /**
     * Fallback method to update UI with Firebase Auth user data when Firestore document is not found
     */
    private void updateUserUIFromFirebaseAuth(FirebaseUser firebaseUser) {
        Log.d(TAG, "HomeFragment: Using Firebase Auth data as fallback");

        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            binding.tvUserName.setText(firebaseUser.getDisplayName());
        } else {
            binding.tvUserName.setText("User");
        }

        if (firebaseUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(firebaseUser.getPhotoUrl())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.ivUserAvatar);
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.profile);
        }
    }
    private void initBodyPartsListeners() {
        // Full Body
        binding.bodyPartFullBody.setOnClickListener(v -> {
            handleBodyPartClick("Full Body");
        });

        // Core
        binding.bodyPartCore.setOnClickListener(v -> {
            handleBodyPartClick("Core");
        });

        // Arm
        binding.bodyPartArm.setOnClickListener(v -> {
            handleBodyPartClick("Arm");
        });

        // Chest
        binding.bodyPartChest.setOnClickListener(v -> {
            handleBodyPartClick("Chest");
        });

        // Butt & Leg
        binding.bodyPartButtLeg.setOnClickListener(v -> {
            handleBodyPartClick("Butt & Leg");
        });

        // Back
        binding.bodyPartBack.setOnClickListener(v -> {
            handleBodyPartClick("Back");
        });

        // Shoulder
        binding.bodyPartShoulder.setOnClickListener(v -> {
            handleBodyPartClick("Shoulder");
        });

        // Custom
        binding.bodyPartCustom.setOnClickListener(v -> {
            handleBodyPartClick("Custom");
        });
    }

    private void handleBodyPartClick(String bodyPart) {
        Toast.makeText(getActivity(), "Selected: " + bodyPart, Toast.LENGTH_SHORT).show();
        Toast.makeText(getActivity(), "Selected: " + bodyPart, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), fpt.fall2025.posetrainer.Activity.ChallengeDetailActivity.class);
        intent.putExtra("body_part", bodyPart);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

