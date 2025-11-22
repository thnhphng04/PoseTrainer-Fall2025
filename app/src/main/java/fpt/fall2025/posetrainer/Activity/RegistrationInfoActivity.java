package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import fpt.fall2025.posetrainer.Adapter.RegistrationViewPagerAdapter;
import fpt.fall2025.posetrainer.Domain.Profile;
import fpt.fall2025.posetrainer.Fragment.Registration.BasicInfoFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.CurrentBodyFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.ExperienceFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.TargetBodyFragment;
import fpt.fall2025.posetrainer.R;

public class RegistrationInfoActivity extends AppCompatActivity implements
        BasicInfoFragment.BasicInfoListener,
        ExperienceFragment.ExperienceListener,
        CurrentBodyFragment.CurrentBodyListener,
        TargetBodyFragment.TargetBodyListener {

    private ViewPager2 viewPager;
    private MaterialButton btnBack, btnNext;
    private TextView tvStepIndicator;
    private ProgressBar progressBar, progressLoading;

    private RegistrationViewPagerAdapter adapter;
    private BasicInfoFragment basicInfoFragment;
    private ExperienceFragment experienceFragment;
    private CurrentBodyFragment currentBodyFragment;
    private TargetBodyFragment targetBodyFragment;

    // Data storage
    private String birthday, gender, height, weight, dailyMinutes, weeklyGoal;
    private String trainingStartTime, trainingEndTime;
    private String selectedExperienceLevel;
    private String currentBodyType;
    private String targetBodyType, targetWeight;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        uid = firebaseUser.getUid();

        // Default gender
        gender = "female";

        bindViews();
        setupViewPager();
        setupButtons();
        updateStepIndicator(0);
    }

    private void bindViews() {
        viewPager = findViewById(R.id.viewPager);
        btnBack = findViewById(R.id.btn_back);
        btnNext = findViewById(R.id.btn_next);
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        progressBar = findViewById(R.id.progress_bar);
        progressLoading = findViewById(R.id.progress_loading);
    }

    private void setupViewPager() {
        adapter = new RegistrationViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swipe

        // Setup fragments after adapter is created
        viewPager.post(() -> {
            setupFragments();
            updateButtonVisibility(viewPager.getCurrentItem());
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateStepIndicator(position);
                updateButtonVisibility(position);

                // Setup fragments when page changes
                setupFragments();
            }
        });
    }

    private void setupFragments() {
        // Get current fragment from adapter
        int currentPosition = viewPager.getCurrentItem();
        Fragment currentFragment = adapter.getFragment(currentPosition);

        if (currentFragment == null) {
            // Fragment chưa được tạo, sẽ được tạo sau
            viewPager.postDelayed(() -> setupFragments(), 100);
            return;
        }

        // Setup based on current position
        switch (currentPosition) {
            case 0:
                if (currentFragment instanceof BasicInfoFragment && basicInfoFragment == null) {
                    basicInfoFragment = (BasicInfoFragment) currentFragment;
                    basicInfoFragment.setListener(this);
                }
                break;
            case 1:
                if (currentFragment instanceof ExperienceFragment && experienceFragment == null) {
                    experienceFragment = (ExperienceFragment) currentFragment;
                    experienceFragment.setListener(this);
                }
                break;
            case 2:
                if (currentFragment instanceof CurrentBodyFragment && currentBodyFragment == null) {
                    currentBodyFragment = (CurrentBodyFragment) currentFragment;
                    currentBodyFragment.setListener(this);
                    currentBodyFragment.updateBodyImages();
                }
                break;
            case 3:
                if (currentFragment instanceof TargetBodyFragment && targetBodyFragment == null) {
                    targetBodyFragment = (TargetBodyFragment) currentFragment;
                    targetBodyFragment.setListener(this);
                    targetBodyFragment.updateBodyImages();
                }
                break;
        }

        // Setup all fragments that are already created
        for (int i = 0; i < adapter.getItemCount(); i++) {
            Fragment fragment = adapter.getFragment(i);
            if (fragment == null) continue;

            switch (i) {
                case 0:
                    if (fragment instanceof BasicInfoFragment && basicInfoFragment == null) {
                        basicInfoFragment = (BasicInfoFragment) fragment;
                        basicInfoFragment.setListener(this);
                    }
                    break;
                case 1:
                    if (fragment instanceof ExperienceFragment && experienceFragment == null) {
                        experienceFragment = (ExperienceFragment) fragment;
                        experienceFragment.setListener(this);
                    }
                    break;
                case 2:
                    if (fragment instanceof CurrentBodyFragment && currentBodyFragment == null) {
                        currentBodyFragment = (CurrentBodyFragment) fragment;
                        currentBodyFragment.setListener(this);
                        currentBodyFragment.updateBodyImages();
                    }
                    break;
                case 3:
                    if (fragment instanceof TargetBodyFragment && targetBodyFragment == null) {
                        targetBodyFragment = (TargetBodyFragment) fragment;
                        targetBodyFragment.setListener(this);
                        targetBodyFragment.updateBodyImages();
                    }
                    break;
            }
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1, true);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (validateCurrentFragment()) {
                int current = viewPager.getCurrentItem();
                if (current < adapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(current + 1, true);
                } else {
                    // Last fragment - save data
                    saveProfile();
                }
            }
        });
    }

    private boolean validateCurrentFragment() {
        int current = viewPager.getCurrentItem();
        switch (current) {
            case 0:
                if (basicInfoFragment != null) {
                    return basicInfoFragment.validate();
                }
                return false;
            case 1:
                if (experienceFragment != null) {
                    boolean valid = experienceFragment.validate();
                    if (!valid) {
                        Toast.makeText(this, "Vui lòng chọn kinh nghiệm tập luyện", Toast.LENGTH_SHORT).show();
                    }
                    return valid;
                }
                return false;
            case 2:
                if (currentBodyFragment != null) {
                    boolean valid = currentBodyFragment.validate();
                    if (!valid) {
                        Toast.makeText(this, "Vui lòng chọn cơ thể hiện tại", Toast.LENGTH_SHORT).show();
                    }
                    return valid;
                }
                return false;
            case 3:
                if (targetBodyFragment != null) {
                    return targetBodyFragment.validate();
                }
                return false;
            default:
                return false;
        }
    }

    private void updateStepIndicator(int position) {
        int step = position + 1;
        tvStepIndicator.setText("Bước " + step + "/4");
        progressBar.setProgress((step * 25));
    }

    private void updateButtonVisibility(int position) {
        btnBack.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        btnNext.setText(position == adapter.getItemCount() - 1 ? "Hoàn tất" : "Tiếp theo");
    }

    private void saveProfile() {
        setLoading(true);

        // Collect all data from fragments before saving
        collectAllData();

        // Validate all data
        if (!validateAllData()) {
            setLoading(false);
            return;
        }

        // Create Goals object
        Profile.Goals goals = new Profile.Goals();
        goals.setTargetWeightKg(Integer.parseInt(targetWeight));
        goals.setTargetBodyType(targetBodyType);

        // Create Profile object
        Profile profile = new Profile();
        profile.setUid(uid);
        profile.setBirthday(birthday);
        profile.setGender(gender);
        profile.setHeightCm(Integer.parseInt(height));
        profile.setWeightKg(Integer.parseInt(weight));
        profile.setCurrentBodyType(currentBodyType);
        profile.setDailyTrainingMinutes(Integer.parseInt(dailyMinutes));
        profile.setWeeklyGoal(Integer.parseInt(weeklyGoal));
        profile.setExperienceLevel(selectedExperienceLevel);
        profile.setTrainingStartTime(trainingStartTime);
        profile.setTrainingEndTime(trainingEndTime);
        profile.setGoals(goals);
        profile.setLastUpdatedAt(System.currentTimeMillis());

        db.collection("profiles").document(uid)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e("RegistrationInfoActivity", "save failed", e);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void collectAllData() {
        // Get data from fragments if available
        if (experienceFragment != null) {
            selectedExperienceLevel = experienceFragment.getSelectedExperienceLevel();
        }
        if (currentBodyFragment != null) {
            currentBodyType = currentBodyFragment.getCurrentBodyType();
        }
        if (targetBodyFragment != null) {
            targetBodyType = targetBodyFragment.getTargetBodyType();
            targetWeight = targetBodyFragment.getTargetWeight();
        }
    }

    private boolean validateAllData() {
        // Validate all required fields
        if (birthday == null || birthday.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ngày sinh", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (gender == null || gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (height == null || height.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập chiều cao", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (weight == null || weight.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập cân nặng", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (dailyMinutes == null || dailyMinutes.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập thời gian tập mỗi ngày", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (weeklyGoal == null || weeklyGoal.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số ngày tập trong tuần", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedExperienceLevel == null || selectedExperienceLevel.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn kinh nghiệm tập luyện", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (currentBodyType == null || currentBodyType.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn cơ thể hiện tại", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (targetBodyType == null || targetBodyType.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn cơ thể mục tiêu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (targetWeight == null || targetWeight.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập cân nặng mục tiêu", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setLoading(boolean on) {
        progressLoading.setVisibility(on ? View.VISIBLE : View.GONE);
        btnNext.setEnabled(!on);
        btnBack.setEnabled(!on);
    }

    // BasicInfoListener
    @Override
    public void onBasicInfoChanged(String birthday, String gender, String height, String weight,
                                    String dailyMinutes, String weeklyGoal, String trainingStartTime, String trainingEndTime) {
        this.birthday = birthday;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.dailyMinutes = dailyMinutes;
        this.weeklyGoal = weeklyGoal;
        this.trainingStartTime = trainingStartTime;
        this.trainingEndTime = trainingEndTime;

        // Update body images in other fragments
        if (currentBodyFragment != null) {
            currentBodyFragment.updateBodyImages();
        }
        if (targetBodyFragment != null) {
            targetBodyFragment.updateBodyImages();
        }
    }

    @Override
    public String getGender() {
        return gender != null ? gender : "female";
    }

    @Override
    public void setGender(String gender) {
        this.gender = gender;
    }

    // ExperienceListener
    @Override
    public void onExperienceSelected(String experienceLevel) {
        this.selectedExperienceLevel = experienceLevel;
    }

    // CurrentBodyListener
    @Override
    public void onCurrentBodySelected(String bodyType) {
        this.currentBodyType = bodyType;
    }

    // TargetBodyListener
    @Override
    public void onTargetBodySelected(String bodyType) {
        this.targetBodyType = bodyType;
    }

    @Override
    public void onTargetWeightChanged(String weight) {
        this.targetWeight = weight;
    }
}

