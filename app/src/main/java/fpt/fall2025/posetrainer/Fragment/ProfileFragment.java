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
import fpt.fall2025.posetrainer.Activity.PlanPreviewActivity;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private boolean isUserDataLoaded = false;

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
        isDataLoaded = true;
    }

    private void setupClicks() {
        binding.btnPremium.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tính năng Premium đang phát triển", Toast.LENGTH_SHORT).show()
        );

        binding.menuSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );
        binding.btnGoal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );

        binding.menuSupport.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );
        View.OnClickListener openPlanPreview = v ->
                startActivity(new Intent(requireContext(), PlanPreviewActivity.class));

        // ✅ mở màn PlanPreviewActivity
        binding.menuWorkouts.setOnClickListener(openPlanPreview);   // bấm cả hàng
        binding.areaMyWorkouts.setOnClickListener(openPlanPreview); // bấm vùng chữ


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
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isUserDataLoaded) {
            Log.d(TAG, "User data đã được cache, bỏ qua reload");
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            Log.w(TAG, "Fragment không còn attached, bỏ qua load user data");
            return;
        }

        cachedUserId = uid;
        setLoading(true);

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
                    setLoading(false);
                    isUserDataLoaded = true;
                    
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
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
                    setLoading(false);
                    isUserDataLoaded = true;
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
        // Check if binding is null (fragment view might be destroyed)
        if (binding == null || !isAdded()) {
            return;
        }
        
        if (binding.profileName != null) {
            binding.profileName.setText(name);
        }
        if (binding.profileEmail != null) {
            binding.profileEmail.setText(email);
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (binding.profileImage != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .circleCrop()
                        .into(binding.profileImage);
            }

            if (binding.ivUserAvatarSmall != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .circleCrop()
                        .into(binding.ivUserAvatarSmall);
            }
        } else {
            if (binding.profileImage != null) {
                binding.profileImage.setImageResource(R.drawable.profile);
            }
            if (binding.ivUserAvatarSmall != null) {
                binding.ivUserAvatarSmall.setImageResource(R.drawable.profile);
            }
        }
    }

    /**
     * ✅ Load workout statistics từ Firestore
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    private void loadUserStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isUserDataLoaded) {
            Log.d(TAG, "User stats đã được cache, bỏ qua reload");
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            Log.w(TAG, "Fragment không còn attached, bỏ qua load user stats");
            return;
        }

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
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
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
                    Log.e(TAG, "loadUserStats error: " + e.getMessage());
                    setDefaultStats();
                });
    }

    private void setDefaultStats() {
        // Check if binding is null (fragment view might be destroyed)
        if (binding == null || !isAdded()) {
            return;
        }
        
        if (binding.tvWorkoutCount != null) {
            binding.tvWorkoutCount.setText("0");
        }
        if (binding.tvCalories != null) {
            binding.tvCalories.setText("0");
        }
        if (binding.tvDuration != null) {
            binding.tvDuration.setText("0");
        }
    }

    private void setLoading(boolean isLoading) {
        // Check if binding is null (fragment view might be destroyed)
        if (binding == null) {
            return;
        }
        
        if (binding.progressBar != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        // Disable/enable các view khi loading
        if (binding.btnPremium != null) {
            binding.btnPremium.setEnabled(!isLoading);
        }
        if (binding.menuSettings != null) {
            binding.menuSettings.setEnabled(!isLoading);
        }
        if (binding.menuSupport != null) {
            binding.menuSupport.setEnabled(!isLoading);
        }
        if (binding.menuWorkouts != null) {
            binding.menuWorkouts.setEnabled(!isLoading);
        }
        if (binding.menuSync != null) {
            binding.menuSync.setEnabled(!isLoading);
        }
        if (binding.btnLogout != null) {
            binding.btnLogout.setEnabled(!isLoading);
        }
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

    private boolean isDataLoaded = false;
    
    @Override
    public void onResume() {
        super.onResume();
        // Chỉ load data một lần ban đầu để tránh reload không cần thiết
        if (!isDataLoaded && isVisible() && isAdded()) {
            loadUserFromFirestore();
            loadUserStats();
            isDataLoaded = true;
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Không reload khi fragment trở nên visible để tối ưu hiệu năng
        // Data đã được cache, chỉ reload nếu user thay đổi hoặc cần refresh
        // User có thể pull-to-refresh hoặc quay lại fragment này để reload
        if (!hidden && isAdded() && isResumed()) {
            // Chỉ check cache, không reload
            Log.d(TAG, "Fragment visible, kiểm tra cache");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        // Không reset cachedUserId và isUserDataLoaded để cache vẫn hoạt động khi fragment bị recreate
        binding = null;
    }
}