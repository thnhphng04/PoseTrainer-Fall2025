package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import fpt.fall2025.posetrainer.Activity.EditGoalsActivity;
import fpt.fall2025.posetrainer.Activity.EditProfileActivity;
import fpt.fall2025.posetrainer.Activity.LoginActivity;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(requireContext(), gso);

        setupClicks();
        loadUserFromFirestore();
        loadUserStats();
    }

    private void setupClicks() {
        // Premium button
        binding.btnPremium.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tính năng Premium đang phát triển", Toast.LENGTH_SHORT).show()
        );

        // Menu Settings
        binding.menuSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );
        // ➕ NÚT MỤC TIÊU
        binding.btnGoal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );

        // Menu Support
        binding.menuSupport.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Hỗ trợ và Mục tiêu", Toast.LENGTH_SHORT).show()
        );

        // Menu Workouts
        binding.menuWorkouts.setOnClickListener(v ->
                Toast.makeText(requireContext(), "My Workouts - Coming soon!", Toast.LENGTH_SHORT).show()
        );

        // Menu Sync
        binding.menuSync.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Đồng bộ dữ liệu", Toast.LENGTH_SHORT).show()
        );

        // Logout
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    /**
     * ✅ Load thông tin user từ Firestore
     * Ưu tiên photoUrl trong Firestore, fallback sang FirebaseAuth
     */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            String name = user.getDisplayName() != null ? user.getDisplayName() : "User";
                            String email = user.getEmail() != null ? user.getEmail() : currentUser.getEmail();
                            String photoUrl = doc.contains("photoUrl") ? doc.getString("photoUrl") : null;

                            // Nếu chưa có photoUrl thì fallback sang FirebaseAuth
                            if (photoUrl == null || photoUrl.isEmpty()) {
                                if (currentUser.getPhotoUrl() != null) {
                                    photoUrl = currentUser.getPhotoUrl().toString();
                                }
                            }

                            bindUser(name, email, photoUrl);
                        }
                    } else {
                        bindFromAuth(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "loadUserFromFirestore: " + e.getMessage());
                    bindFromAuth(currentUser);
                });
    }

    private void bindFromAuth(FirebaseUser user) {
        String name = user.getDisplayName() != null ? user.getDisplayName() : "User";
        String email = user.getEmail() != null ? user.getEmail() : "email@example.com";
        String photo = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
        bindUser(name, email, photo);
    }

    private void bindUser(String name, String email, String photoUrl) {
        binding.profileName.setText(name);
        binding.profileEmail.setText(email);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Load vào avatar lớn
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .circleCrop()
                    .into(binding.profileImage);

            // Load vào avatar nhỏ ở header
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .circleCrop()
                    .into(binding.ivUserAvatarSmall);
        } else {
            binding.profileImage.setImageResource(R.drawable.profile);
            binding.ivUserAvatarSmall.setImageResource(R.drawable.profile);
        }
    }

    /**
     * ✅ Load workout statistics từ Firestore
     */
    private void loadUserStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Lấy stats từ Firestore
                        long workoutCount = doc.contains("workoutCount") ? doc.getLong("workoutCount") : 0;
                        long calories = doc.contains("totalCalories") ? doc.getLong("totalCalories") : 0;
                        long duration = doc.contains("totalDuration") ? doc.getLong("totalDuration") : 0;

                        binding.tvWorkoutCount.setText(String.valueOf(workoutCount));
                        binding.tvCalories.setText(String.valueOf(calories));
                        binding.tvDuration.setText(String.valueOf(duration));
                    } else {
                        // Set giá trị mặc định
                        setDefaultStats();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUserStats error: " + e.getMessage());
                    setDefaultStats();
                });
    }

    private void setDefaultStats() {
        binding.tvWorkoutCount.setText("0");
        binding.tvCalories.setText("0");
        binding.tvDuration.setText("0");
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        // Disable/enable các view khi loading
        binding.btnPremium.setEnabled(!isLoading);
        binding.menuSettings.setEnabled(!isLoading);
        binding.menuSupport.setEnabled(!isLoading);
        binding.menuWorkouts.setEnabled(!isLoading);
        binding.menuSync.setEnabled(!isLoading);
        binding.btnLogout.setEnabled(!isLoading);
    }

    private void logout() {
        setLoading(true);
        boolean isGoogleSignedIn = GoogleSignIn.getLastSignedInAccount(requireContext()) != null;

        if (isGoogleSignedIn && googleClient != null) {
            googleClient.signOut()
                    .addOnCompleteListener(task -> {
                        safeFirebaseSignOut();
                        goLogin();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Google signOut failed: " + e.getMessage());
                        safeFirebaseSignOut();
                        goLogin();
                    });
        } else {
            safeFirebaseSignOut();
            goLogin();
        }
    }

    private void safeFirebaseSignOut() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            Log.w(TAG, "Firebase signOut error: " + e.getMessage());
        }
    }

    private void goLogin() {
        setLoading(false);
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🔄 Tải lại dữ liệu khi quay về Profile
        loadUserFromFirestore();
        loadUserStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}