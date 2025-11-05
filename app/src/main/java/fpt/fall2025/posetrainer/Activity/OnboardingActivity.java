package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import fpt.fall2025.posetrainer.Adapter.OnboardingPagerAdapter;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.ActivityOnboardingBinding;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private OnboardingViewModel viewModel;
    private OnboardingPagerAdapter adapter;
    private int currentPage = 0;
    private static final int TOTAL_PAGES = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        setupViewPager();
        setupButtons();
    }

    private void setupViewPager() {
        adapter = new OnboardingPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setUserInputEnabled(false); // Disable swipe

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updateUI();
            }
        });
    }

    private void setupButtons() {
        binding.btnNext.setOnClickListener(v -> {
            if (currentPage < TOTAL_PAGES - 1) {
                binding.viewPager.setCurrentItem(currentPage + 1);
            } else {
                saveDataAndFinish();
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            // Skip to main activity with default values
            goToMainActivity();
        });
    }

    private void updateUI() {
        // Update progress
        binding.progressIndicator.setProgress(currentPage + 1);

        // Update button text
        if (currentPage == TOTAL_PAGES - 1) {
            binding.btnNext.setText("BẮT ĐẦU KẾ HOẠCH CỦA TÔI");
        } else {
            binding.btnNext.setText("TIẾP THEO");
        }
    }

    private void saveDataAndFinish() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId)
                .update(
                        "gender", viewModel.getData().getGender(),
                        "bodyPart", viewModel.getData().getBodyPart(),
                        "goal", viewModel.getData().getGoal(),
                        "activityLevel", viewModel.getData().getActivityLevel(),
                        "weeklyGoal", viewModel.getData().getWeeklyGoal(),
                        "weight", viewModel.getData().getWeight(),
                        "height", viewModel.getData().getHeight(),
                        "onboardingCompleted", true
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã lưu thông tin!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}