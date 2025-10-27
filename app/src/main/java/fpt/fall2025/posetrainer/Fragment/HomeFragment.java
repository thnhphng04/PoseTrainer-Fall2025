package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
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
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        binding.view1.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        setupSearchListeners();
        loadCurrentUserInfo();
        loadWorkoutTemplates();
    }

    private void setupSearchListeners() {
        binding.searchBar.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SearchActivity.class));
        });
    }

    private void loadWorkoutTemplates() {
        FirebaseService.getInstance().loadWorkoutTemplates(
                (androidx.appcompat.app.AppCompatActivity) getActivity(),
                templates -> {
                    workoutTemplates = templates;
                    binding.view1.setAdapter(new WorkoutTemplateAdapter(workoutTemplates));
                }
        );
    }

    /**
     * 🔄 Load current user info from Firestore or Auth (like ProfileFragment)
     */
    private void loadCurrentUserInfo() {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            Log.w(TAG, "No current user found");
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
                    Log.e(TAG, "Failed to load user info", e);
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

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại khi người dùng quay lại Home sau khi chỉnh sửa
        loadCurrentUserInfo();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
