package fpt.fall2025.posetrainer.UI.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.UI.activity.EditWorkoutActivity;
import fpt.fall2025.posetrainer.UI.activity.PlanPreviewActivity;
import fpt.fall2025.posetrainer.UI.adapter.workout.UserWorkoutCardAdapter;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.DAL.UserWorkoutDAO;
import fpt.fall2025.posetrainer.DAL.SessionDAO;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.databinding.FragmentMyworkoutBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private AuthService authService;
    private UserDAO userDAO;
    private UserWorkoutDAO userWorkoutDAO;
    private SessionDAO sessionDAO;
    
    // Cache để tránh reload không cần thiết
    private String cachedUserId = null;
    private boolean isWorkoutsLoaded = false;
    private java.util.Set<String> completedWorkoutIds = new java.util.HashSet<>(); // Set các workout ID đã hoàn thành

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyworkoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthService();
        userDAO = new UserDAO();
        userWorkoutDAO = new UserWorkoutDAO();
        sessionDAO = new SessionDAO();

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
        FirebaseUser currentUser = authService.getCurrentUser();
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
        
        userDAO.getDocument(uid).get()
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
                FirebaseUser currentUser = authService.getCurrentUser();
                if (currentUser != null) {
                    loadUserWorkouts(currentUser.getUid());
                }
            });
        }
        
        if (aiWorkoutAdapter != null) {
            aiWorkoutAdapter.setOnUserWorkoutDeletedListener(() -> {
                // Refresh the list when an item is deleted
                isWorkoutsLoaded = false;
                FirebaseUser currentUser = authService.getCurrentUser();
                if (currentUser != null) {
                    loadUserWorkouts(currentUser.getUid());
                }
            });
        }
    }

    /**
     * Filter workouts và hiển thị vào 2 RecyclerView riêng biệt
     * Ẩn các AI workouts đã hoàn thành (có session với endedAt > 0)
     */
    private void filterAndDisplayWorkouts(ArrayList<UserWorkout> allWorkouts) {
        if (allWorkouts == null || allWorkouts.isEmpty()) {
            userWorkouts.clear();
            aiWorkouts.clear();
            updateWorkoutCounts();
            showAIWorkoutEmptyState();
            return;
        }

        // Load completed sessions để xác định workouts đã hoàn thành
        // Pass allWorkouts vào để tránh load lại
        loadCompletedSessions(allWorkouts, () -> {
            // Filter workouts sau khi đã load completed sessions
            userWorkouts.clear();
            aiWorkouts.clear();
            
            for (UserWorkout workout : allWorkouts) {
                String source = workout.getSource();
                if (source != null && source.equals("ai")) {
                    // Chỉ thêm AI workout nếu chưa hoàn thành
                    if (!completedWorkoutIds.contains(workout.getId())) {
                        aiWorkouts.add(workout);
                        android.util.Log.d("MyWorkoutFragment", "➕ Thêm AI workout vào danh sách: " + workout.getTitle());
                    } else {
                        android.util.Log.d("MyWorkoutFragment", "➖ Bỏ qua AI workout đã hoàn thành: " + workout.getTitle());
                    }
                } else {
                    userWorkouts.add(workout);
                }
            }
            
            android.util.Log.d("MyWorkoutFragment", "📝 Sau khi filter: " + aiWorkouts.size() + " AI workouts sẽ hiển thị");
            
            // Sắp xếp và hiển thị
            sortAndDisplayWorkouts();
        });
    }
    
    /**
     * Load các sessions đã hoàn thành để xác định workouts nào đã hoàn thành
     * @param allWorkouts Danh sách tất cả workouts để so sánh (tránh load lại)
     */
    private void loadCompletedSessions(ArrayList<UserWorkout> allWorkouts, Runnable onComplete) {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            completedWorkoutIds.clear();
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        String uid = currentUser.getUid();
        completedWorkoutIds.clear();
        
        android.util.Log.d("MyWorkoutFragment", "🔍 Đang load completed sessions cho " + (allWorkouts != null ? allWorkouts.size() : 0) + " workouts");
        
        // Query trực tiếp từ Firestore để lấy sessions đã hoàn thành
        // So sánh title của session với title của workout để xác định workout nào đã hoàn thành
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("sessions")
            .whereEqualTo("uid", uid)
            .whereGreaterThan("endedAt", 0)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                // Lưu danh sách sessions đã hoàn thành với title và startedAt
                // Map: title -> startedAt (để so sánh với workout createdAt)
                java.util.Map<String, Long> completedSessions = new java.util.HashMap<>();
                android.util.Log.d("MyWorkoutFragment", "📋 Tìm thấy " + queryDocumentSnapshots.size() + " sessions đã hoàn thành");
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String sessionTitle = doc.getString("title");
                    Long sessionStartedAt = doc.getLong("startedAt");
                    if (sessionTitle != null && !sessionTitle.isEmpty() && sessionStartedAt != null) {
                        // Lưu startedAt lớn nhất cho mỗi title (session mới nhất)
                        Long existingStartedAt = completedSessions.get(sessionTitle);
                        if (existingStartedAt == null || sessionStartedAt > existingStartedAt) {
                            completedSessions.put(sessionTitle, sessionStartedAt);
                        }
                        android.util.Log.d("MyWorkoutFragment", "  ✓ Session completed: " + sessionTitle + " (startedAt: " + sessionStartedAt + ")");
                    }
                }
                
                // So sánh với workouts đã có (không cần load lại)
                if (allWorkouts != null) {
                    int aiWorkoutCount = 0;
                    int completedCount = 0;
                    for (UserWorkout workout : allWorkouts) {
                        if (workout.getSource() != null && workout.getSource().equals("ai")) {
                            aiWorkoutCount++;
                            String workoutTitle = workout.getTitle();
                            long workoutCreatedAt = workout.getCreatedAt();
                            android.util.Log.d("MyWorkoutFragment", "🤖 AI Workout: " + workoutTitle + " (ID: " + workout.getId() + ", createdAt: " + workoutCreatedAt + ")");
                            
                            // So sánh title VÀ kiểm tra thời gian: session phải được tạo SAU KHI workout được tạo
                            // Điều này đảm bảo chỉ match đúng workout (tránh match workout cũ với workout mới có cùng title)
                            Long sessionStartedAt = completedSessions.get(workoutTitle);
                            if (sessionStartedAt != null && sessionStartedAt >= workoutCreatedAt) {
                                // Session được tạo sau hoặc cùng lúc với workout → đây là session của workout này
                                completedWorkoutIds.add(workout.getId());
                                completedCount++;
                                android.util.Log.d("MyWorkoutFragment", "  ❌ Workout đã hoàn thành (session startedAt: " + sessionStartedAt + " >= workout createdAt: " + workoutCreatedAt + "), sẽ ẩn: " + workoutTitle);
                            } else {
                                android.util.Log.d("MyWorkoutFragment", "  ✅ Workout chưa hoàn thành (session startedAt: " + sessionStartedAt + " < workout createdAt: " + workoutCreatedAt + "), sẽ hiển thị: " + workoutTitle);
                            }
                        }
                    }
                    android.util.Log.d("MyWorkoutFragment", "📊 Tổng kết: " + aiWorkoutCount + " AI workouts, " + completedCount + " đã hoàn thành, " + (aiWorkoutCount - completedCount) + " sẽ hiển thị");
                }
                
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("MyWorkoutFragment", "❌ Error loading completed sessions", e);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
    }
    
    /**
     * Sắp xếp và hiển thị workouts
     */
    private void sortAndDisplayWorkouts() {

        // Sắp xếp AI workouts theo số ngày (numeric sort) thay vì string sort
        // Để tránh sắp xếp: "Ngày 1", "Ngày 10", "Ngày 11", "Ngày 2"...
        // Thành: "Ngày 1", "Ngày 2", ..., "Ngày 10", "Ngày 11"...
        Collections.sort(aiWorkouts, new Comparator<UserWorkout>() {
            @Override
            public int compare(UserWorkout w1, UserWorkout w2) {
                int day1 = extractDayNumber(w1.getTitle());
                int day2 = extractDayNumber(w2.getTitle());
                return Integer.compare(day1, day2);
            }
        });

        // Sắp xếp theo thời gian tạo từ mới nhất đến cũ nhất (descending order)
        Collections.sort(userWorkouts, new Comparator<UserWorkout>() {
            @Override
            public int compare(UserWorkout w1, UserWorkout w2) {
                // So sánh createdAt: workout mới hơn (createdAt lớn hơn) sẽ đứng trước
                return Long.compare(w2.getCreatedAt(), w1.getCreatedAt());
            }
        });

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
        
        // Hiển thị thông báo nếu tất cả AI workouts đã hoàn thành
        showAIWorkoutEmptyState();
    }
    
    /**
     * Hiển thị thông báo khi tất cả AI workouts đã hoàn thành
     * Chỉ hiển thị khi: có AI workouts trong database VÀ tất cả đều đã hoàn thành (không có trong aiWorkouts)
     */
    private void showAIWorkoutEmptyState() {
        android.widget.TextView emptyTextView = binding.getRoot().findViewById(R.id.ai_empty_message);
        if (emptyTextView == null) {
            android.util.Log.w("MyWorkoutFragment", "⚠️ Không tìm thấy ai_empty_message TextView");
            return;
        }
        
        android.util.Log.d("MyWorkoutFragment", "🔔 showAIWorkoutEmptyState: aiWorkouts.size() = " + (aiWorkouts != null ? aiWorkouts.size() : 0));
        
        // Nếu còn workouts chưa hoàn thành, ẩn thông báo
        if (aiWorkouts != null && !aiWorkouts.isEmpty()) {
            android.util.Log.d("MyWorkoutFragment", "✅ Còn AI workouts, ẩn thông báo");
            emptyTextView.setVisibility(android.view.View.GONE);
            return;
        }
        
        // Nếu không còn workouts hiển thị, kiểm tra xem có AI workouts trong database không
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userWorkoutDAO.getByUserId(uid, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    int totalAIWorkouts = 0;
                    for (UserWorkout workout : task.getResult()) {
                        if (workout.getSource() != null && workout.getSource().equals("ai")) {
                            totalAIWorkouts++;
                        }
                    }
                    android.util.Log.d("MyWorkoutFragment", "📊 Tổng AI workouts trong DB: " + totalAIWorkouts + ", đang hiển thị: " + (aiWorkouts != null ? aiWorkouts.size() : 0));
                    
                    // Chỉ hiển thị thông báo nếu có AI workouts trong database VÀ tất cả đã hoàn thành
                    if (totalAIWorkouts > 0) {
                        android.util.Log.d("MyWorkoutFragment", "💬 Hiển thị thông báo: Tất cả AI workouts đã hoàn thành");
                        emptyTextView.setVisibility(android.view.View.VISIBLE);
                    } else {
                        // Không có AI workouts nào, ẩn thông báo
                        android.util.Log.d("MyWorkoutFragment", "🚫 Không có AI workouts, ẩn thông báo");
                        emptyTextView.setVisibility(android.view.View.GONE);
                    }
                } else {
                    // Lỗi load, ẩn thông báo để an toàn
                    android.util.Log.e("MyWorkoutFragment", "❌ Lỗi load workouts trong showAIWorkoutEmptyState");
                    emptyTextView.setVisibility(android.view.View.GONE);
                }
            });
        } else {
            android.util.Log.w("MyWorkoutFragment", "⚠️ Không có user, ẩn thông báo");
            emptyTextView.setVisibility(android.view.View.GONE);
        }
    }

    /**
     * Extract số ngày từ title (ví dụ: "Ngày 1: Fullbody" -> 1)
     * Nếu không tìm thấy số, trả về Integer.MAX_VALUE để đẩy về cuối danh sách
     */
    private int extractDayNumber(String title) {
        if (title == null || title.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        // Pattern để tìm số sau "Ngày "
        Pattern pattern = Pattern.compile("Ngày\\s+(\\d+)");
        Matcher matcher = pattern.matcher(title);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }

        // Nếu không match pattern, thử tìm số đầu tiên trong title
        pattern = Pattern.compile("(\\d+)");
        matcher = pattern.matcher(title);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }

        return Integer.MAX_VALUE;
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
        // KHÔNG cache nữa - luôn reload để đảm bảo data mới nhất (đặc biệt sau khi tạo plan mới)
        // Cache có thể gây ra vấn đề khi có workouts mới
        
        // Kiểm tra activity có tồn tại không
        if (getActivity() == null || !(getActivity() instanceof androidx.appcompat.app.AppCompatActivity)) {
            return;
        }
        
        // Clear completedWorkoutIds trước khi load lại để đảm bảo data mới nhất
        completedWorkoutIds.clear();
        
        userWorkoutDAO.getByUserId(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<UserWorkout> workouts = new ArrayList<>(task.getResult());
                isWorkoutsLoaded = true;
                cachedUserId = userId; // Update cached user ID
                
                // Kiểm tra fragment view có còn attached không
                if (!isAdded() || binding == null) {
                    return;
                }
                
                // Filter và hiển thị workouts vào 2 RecyclerView riêng biệt
                filterAndDisplayWorkouts(workouts != null ? workouts : new ArrayList<>());
            } else {
                isWorkoutsLoaded = true;
                cachedUserId = userId; // Update cached user ID
                if (!isAdded() || binding == null) {
                    return;
                }
                filterAndDisplayWorkouts(new ArrayList<>());
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
                FirebaseUser currentUser = authService.getCurrentUser();
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
            FirebaseUser currentUser = authService.getCurrentUser();
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
            FirebaseUser currentUser = authService.getCurrentUser();
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
