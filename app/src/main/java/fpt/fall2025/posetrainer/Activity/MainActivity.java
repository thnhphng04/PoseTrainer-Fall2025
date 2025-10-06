package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import fpt.fall2025.posetrainer.Fragment.HomeFragment;
import fpt.fall2025.posetrainer.Fragment.FavoriteFragment;
import fpt.fall2025.posetrainer.Fragment.ProfileFragment;
import fpt.fall2025.posetrainer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Setup bottom navigation
        setupBottomNavigation();
        // Load default fragment (Home)
        loadFragment(new HomeFragment());
    }

    private void setupBottomNavigation() {
        binding.homeBtn.setOnClickListener(v -> loadFragment(new HomeFragment()));
        binding.favoriteBtn.setOnClickListener(v -> {
            loadFragment(new FavoriteFragment());
        });
        binding.cartBtn.setOnClickListener(v -> {
            // TODO: Tạo CartFragment sau  
            loadFragment(new ProfileFragment()); // Tạm thời dùng ProfileFragment
        });
        binding.profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(binding.fragmentContainer.getId(), fragment);
        transaction.commit();
    }
}