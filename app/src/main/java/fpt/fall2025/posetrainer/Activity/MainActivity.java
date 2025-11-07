package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import fpt.fall2025.posetrainer.Fragment.HomeFragment;
import fpt.fall2025.posetrainer.Fragment.MyWorkoutFragment;
import fpt.fall2025.posetrainer.Fragment.DailyFragment;
import fpt.fall2025.posetrainer.Fragment.ProfileFragment;
import fpt.fall2025.posetrainer.Fragment.CommunityFragment;
import fpt.fall2025.posetrainer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ActivityMainBinding binding;
    
    // Cache fragments để tránh tạo lại mỗi lần chuyển tab
    private HomeFragment homeFragment;
    private CommunityFragment communityFragment;
    private MyWorkoutFragment myWorkoutFragment;
    private DailyFragment dailyFragment;
    private ProfileFragment profileFragment;
    
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Khởi tạo và cache fragments
        initializeFragments();
        
        // Setup bottom navigation
        setupBottomNavigation();
        
        // Load fragment mặc định (Home)
        showFragment(homeFragment);
    }

    /**
     * Khởi tạo tất cả fragments một lần và cache chúng
     */
    private void initializeFragments() {
        // Tìm fragments đã tồn tại (trong trường hợp activity bị recreate)
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
        communityFragment = (CommunityFragment) getSupportFragmentManager().findFragmentByTag("community");
        myWorkoutFragment = (MyWorkoutFragment) getSupportFragmentManager().findFragmentByTag("myWorkout");
        dailyFragment = (DailyFragment) getSupportFragmentManager().findFragmentByTag("daily");
        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        
        // Tạo fragments mới nếu chưa tồn tại
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (communityFragment == null) {
            communityFragment = new CommunityFragment();
        }
        if (myWorkoutFragment == null) {
            myWorkoutFragment = new MyWorkoutFragment();
        }
        if (dailyFragment == null) {
            dailyFragment = new DailyFragment();
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }
    }

    private void setupBottomNavigation() {
        binding.homeBtn.setOnClickListener(v -> showFragment(homeFragment));
        binding.personalBtn.setOnClickListener(v -> showFragment(myWorkoutFragment));
        binding.dailyBtn.setOnClickListener(v -> showFragment(dailyFragment));
        binding.profileBtn.setOnClickListener(v -> showFragment(profileFragment));
        binding.Community.setOnClickListener(v -> showFragment(communityFragment));
    }

    /**
     * Hiển thị fragment sử dụng show/hide thay vì replace để tăng hiệu năng
     */
    private void showFragment(Fragment fragment) {
        if (fragment == null || fragment == currentFragment) {
            return;
        }
        
        // Kiểm tra activity có đang trong trạng thái hợp lệ không
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            
            // Ẩn fragment hiện tại nếu có
            if (currentFragment != null && currentFragment.isAdded()) {
                transaction.hide(currentFragment);
            }
            
            // Hiển thị fragment được chọn
            if (fragment.isAdded()) {
                // Fragment đã được add, chỉ cần show
                transaction.show(fragment);
            } else {
                // Fragment chưa được add, add nó vào
                String tag = getFragmentTag(fragment);
                transaction.add(binding.fragmentContainer.getId(), fragment, tag);
            }
            
            // Sử dụng commit() thay vì commitNow() để tránh IllegalStateException
            transaction.commit();
            
            currentFragment = fragment;
            Log.d(TAG, "Switched to fragment: " + fragment.getClass().getSimpleName());
        } catch (IllegalStateException e) {
            // Xử lý trường hợp activity đang save state
            Log.w(TAG, "Cannot commit fragment transaction: " + e.getMessage());
            // Fallback: sử dụng commitAllowingStateLoss() như giải pháp cuối cùng
            try {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (currentFragment != null && currentFragment.isAdded()) {
                    transaction.hide(currentFragment);
                }
                if (fragment.isAdded()) {
                    transaction.show(fragment);
                } else {
                    String tag = getFragmentTag(fragment);
                    transaction.add(binding.fragmentContainer.getId(), fragment, tag);
                }
                transaction.commitAllowingStateLoss();
                currentFragment = fragment;
            } catch (Exception ex) {
                Log.e(TAG, "Failed to commit fragment transaction: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Lấy tag cho fragment caching
     */
    private String getFragmentTag(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return "home";
        } else if (fragment instanceof CommunityFragment) {
            return "community";
        } else if (fragment instanceof MyWorkoutFragment) {
            return "myWorkout";
        } else if (fragment instanceof DailyFragment) {
            return "daily";
        } else if (fragment instanceof ProfileFragment) {
            return "profile";
        }
        return fragment.getClass().getSimpleName();
    }
}