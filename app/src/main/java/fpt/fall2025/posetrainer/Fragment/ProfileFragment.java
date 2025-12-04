package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import fpt.fall2025.posetrainer.Activity.AchievementsActivity;
import fpt.fall2025.posetrainer.Activity.EditGoalsActivity;
import fpt.fall2025.posetrainer.Activity.EditProfileActivity;
import fpt.fall2025.posetrainer.Activity.LoginActivity;
import fpt.fall2025.posetrainer.Activity.WorkoutHistoryActivity;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.DAL.SessionDAO;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private AuthService authService;
    private UserDAO userDAO;
    private SessionDAO sessionDAO;
    private GoogleSignInClient googleClient;
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private boolean isUserDataLoaded = false;
    private boolean isStatsLoaded = false; // Cache riêng cho stats

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthService();
        userDAO = new UserDAO();
        sessionDAO = new SessionDAO();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(requireContext(), gso);

        setupClicks();
        loadUserFromFirestore();
        loadUserStats();
        loadUserStreak();
        isDataLoaded = true;
    }

    private void setupClicks() {
        binding.btnPremium.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Tính năng Premium đang phát triển", Toast.LENGTH_SHORT).show()
        );

        binding.menuSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class))
        );
        binding.menuAchievements.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AchievementsActivity.class))
        );
        binding.btnGoal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );

        binding.menuSupport.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditGoalsActivity.class))
        );
        View.OnClickListener openWorkoutHistory = v ->
                startActivity(new Intent(requireContext(), WorkoutHistoryActivity.class));

        // ✅ mở màn WorkoutHistoryActivity
        binding.menuWorkouts.setOnClickListener(openWorkoutHistory);   // bấm cả hàng
        binding.areaMyWorkouts.setOnClickListener(openWorkoutHistory); // bấm vùng chữ


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
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isUserDataLoaded) {
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            return;
        }

        cachedUserId = uid;
        setLoading(true);

        userDAO.getById(uid, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                if (binding == null || !isAdded()) {
                    return;
                }
                setLoading(false);
                Toast.makeText(requireContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                return;
            }
            
            User user = task.getResult();
            if (binding == null || !isAdded()) {
                return;
            }
            
            setLoading(false);
            isUserDataLoaded = true;
            
            String name = user.getDisplayName() != null ? user.getDisplayName() : "User";
            String email = user.getEmail() != null ? user.getEmail() : currentUser.getEmail();
            String photoUrl = user.getPhotoURL() != null ? user.getPhotoURL() : null;

            // Nếu chưa có photoUrl thì fallback sang FirebaseAuth
            if (photoUrl == null || photoUrl.isEmpty()) {
                String authPhotoUrl = authService.getCurrentUserPhotoUrl();
                if (authPhotoUrl != null && !authPhotoUrl.isEmpty()) {
                    photoUrl = authPhotoUrl;
                }
            }

            if (binding.profileName != null) {
                binding.profileName.setText(name);
            }
            if (binding.profileEmail != null) {
                binding.profileEmail.setText(email != null ? email : "");
            }

            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(photoUrl)
                        .circleCrop()
                        .into(binding.profileImage);
            } else {
                if (binding.profileImage != null) {
                    binding.profileImage.setImageResource(R.drawable.profile);
                }
            }
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
        } else {
            if (binding.profileImage != null) {
                binding.profileImage.setImageResource(R.drawable.profile);
            }
        }
    }

    /**
     * ✅ Load workout statistics từ bảng sessions theo uid người dùng
     * Tính toán từ các sessions đã hoàn thành (endedAt > 0)
     * Đã tối ưu với cache để tránh reload không cần thiết
     */
    private void loadUserStats() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        
        // Kiểm tra cache: Chỉ reload nếu user thay đổi hoặc chưa load stats lần nào
        if (cachedUserId != null && cachedUserId.equals(uid) && isStatsLoaded) {
            return;
        }
        
        // Kiểm tra fragment view có còn attached không
        if (!isAdded() || binding == null) {
            return;
        }

        // Query sessions của user đã hoàn thành (endedAt > 0)
        sessionDAO.getCollection()
                .whereEqualTo("uid", uid)
                .whereGreaterThan("endedAt", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
                    int workoutCount = 0;
                    int totalCalories = 0;
                    int totalDurationSec = 0;
                    
                    // Tính toán từ các sessions
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Session session = document.toObject(Session.class);
                            if (session == null) continue;
                            
                            // Chỉ tính sessions đã hoàn thành (endedAt > 0)
                            if (session.getEndedAt() <= 0) continue;
                            
                            workoutCount++;
                            
                            // Cộng dồn calories (ưu tiên từ summary, fallback = 0)
                            if (session.getSummary() != null) {
                                int estKcal = session.getSummary().getEstKcal();
                                if (estKcal > 0) {
                                    totalCalories += estKcal;
                                }
                            }
                            
                            // Cộng dồn duration (giây)
                            // Ưu tiên từ summary, fallback tính từ endedAt - startedAt
                            int durationSec = 0;
                            if (session.getSummary() != null && session.getSummary().getDurationSec() > 0) {
                                durationSec = session.getSummary().getDurationSec();
                            } else if (session.getEndedAt() > 0 && session.getStartedAt() > 0) {
                                // Fallback: tính từ endedAt - startedAt (cả hai đều là seconds)
                                durationSec = (int) (session.getEndedAt() - session.getStartedAt());
                            }
                            
                            if (durationSec > 0) {
                                totalDurationSec += durationSec;
                            }
                        } catch (Exception e) {
                            // Error parsing session, skip
                        }
                    }
                    
                    // Chuyển duration từ giây sang phút
                    int totalDurationMin = totalDurationSec / 60;
                    
                    // Hiển thị kết quả
                    binding.tvWorkoutCount.setText(String.valueOf(workoutCount));
                    binding.tvCalories.setText(String.valueOf(totalCalories));
                    binding.tvDuration.setText(String.valueOf(totalDurationMin));
                    
                    // Đánh dấu đã load stats
                    isStatsLoaded = true;
                })
                .addOnFailureListener(e -> {
                    // Check if fragment view is still attached
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    
                    setDefaultStats();
                    isStatsLoaded = true; // Vẫn đánh dấu đã load để tránh retry liên tục
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

    /**
     * Load user streak and display
     */
    private void loadUserStreak() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        FirebaseService.getInstance().loadUserStreak(currentUser.getUid(), streak -> {
            if (getActivity() == null || binding == null || !isAdded()) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (streak != null) {
                    if (binding.tvStreakCurrent != null) {
                        binding.tvStreakCurrent.setText(String.valueOf(streak.getCurrentStreak()));
                    }
                    if (binding.tvStreakLongest != null) {
                        binding.tvStreakLongest.setText("Tối đa: " + streak.getLongestStreak());
                    }
                } else {
                    if (binding.tvStreakCurrent != null) {
                        binding.tvStreakCurrent.setText("0");
                    }
                    if (binding.tvStreakLongest != null) {
                        binding.tvStreakLongest.setText("Tối đa: 0");
                    }
                }
            });
        });
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
            authService.signOut();
        } catch (Exception e) {
            // Error during sign out
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset flag khi view bị destroy
        isDataLoaded = false;
        isStatsLoaded = false; // Reset stats cache khi view bị destroy
        // Không reset cachedUserId và isUserDataLoaded để cache vẫn hoạt động khi fragment bị recreate
        binding = null;
    }
}