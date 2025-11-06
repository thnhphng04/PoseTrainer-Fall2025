package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Setup bottom navigation
        setupBottomNavigation();
        // Load default fragment (Home)
        loadFragment(new HomeFragment());
    }

    private void setupBottomNavigation() {
        binding.homeBtn.setOnClickListener(v -> loadFragment(new HomeFragment()));
        binding.personalBtn.setOnClickListener(v -> {
            loadFragment(new MyWorkoutFragment());
        });
        binding.dailyBtn.setOnClickListener(v -> {
            loadFragment(new DailyFragment());
        });
        binding.profileBtn.setOnClickListener(v -> loadFragment(new ProfileFragment()));
        binding.Community.setOnClickListener(v -> loadFragment(new CommunityFragment()));
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(binding.fragmentContainer.getId(), fragment);
        transaction.commit();
    }
}