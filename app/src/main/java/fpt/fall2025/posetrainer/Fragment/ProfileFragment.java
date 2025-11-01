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
    }

    private void setupClicks() {
        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );
        // ➕ NÚT MỤC TIÊU
        binding.btnGoal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );

        binding.btnLogout.setOnClickListener(v -> logout());
    }

    /**
     * ✅ Load thông tin user giống FavoriteFragment:
     *   - Lấy dữ liệu Firestore ("users" collection)
     *   - Ưu tiên dùng photoUrl trong Firestore (Storage link)
     */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show();
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
                            String photoUrl = doc.contains("photoUrl") ? doc.getString("photoUrl") : null;

                            // Nếu chưa có photoUrl thì fallback sang FirebaseAuth
                            if (photoUrl == null || photoUrl.isEmpty()) {
                                if (currentUser.getPhotoUrl() != null) {
                                    photoUrl = currentUser.getPhotoUrl().toString();
                                }
                            }

                            bindUser(name, photoUrl);
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
        String photo = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
        bindUser(name, photo);
    }

    private void bindUser(String name, String photoUrl) {
        binding.profileName.setText(name);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.profileImage);

            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.ivUserAvatarSmall);
        } else {
            binding.profileImage.setImageResource(R.drawable.profile);
            binding.ivUserAvatarSmall.setImageResource(R.drawable.profile);
        }
    }

    private void setLoading(boolean on) {
        binding.progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.btnEditProfile.setEnabled(!on);
        binding.btnLogout.setEnabled(!on);
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
        try { FirebaseAuth.getInstance().signOut(); }
        catch (Exception e) { Log.w(TAG, "Firebase signOut error: " + e.getMessage()); }
    }

    private void goLogin() {
        setLoading(false);
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserFromFirestore(); // 🔁 Luôn tải lại khi quay lại Profile
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
