package fpt.fall2025.posetrainer.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.R;

public class OnboardingQuestionnaireActivity extends AppCompatActivity {

    private TextInputLayout tilBirthday, tilGender, tilHeight, tilWeight, tilBodyType,
            tilDailyMinutes, tilExperience, tilGoalType, tilTargetWeight, tilTargetBody;

    private TextInputEditText etBirthday, etHeight, etWeight, etDailyMinutes, etTargetWeight;
    private MaterialAutoCompleteTextView ddGender, ddBodyType, ddExperience, ddGoalType, ddTargetBody;
    private Button btnSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_questionnaire);

        bindViews();
        setupDropdowns();
        setupBirthdayPicker();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        tilBirthday = findViewById(R.id.til_birthday);
        tilGender = findViewById(R.id.til_gender);
        tilHeight = findViewById(R.id.til_height);
        tilWeight = findViewById(R.id.til_weight);
        tilBodyType = findViewById(R.id.til_body_type);
        tilDailyMinutes = findViewById(R.id.til_daily_minutes);
        tilExperience = findViewById(R.id.til_experience);
        tilGoalType = findViewById(R.id.til_goal_type);
        tilTargetWeight = findViewById(R.id.til_target_weight);
        tilTargetBody = findViewById(R.id.til_target_body);

        etBirthday = findViewById(R.id.et_birthday);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etDailyMinutes = findViewById(R.id.et_daily_minutes);
        etTargetWeight = findViewById(R.id.et_target_weight);

        ddGender = findViewById(R.id.dd_gender);
        ddBodyType = findViewById(R.id.dd_body_type);
        ddExperience = findViewById(R.id.dd_experience);
        ddGoalType = findViewById(R.id.dd_goal_type);
        ddTargetBody = findViewById(R.id.dd_target_body);

        btnSave = findViewById(R.id.btn_save);
    }

    private void setupDropdowns() {
        // Dropdown data
        String[] genders = new String[]{"male", "female", "other"};
        String[] bodyTypes = new String[]{"very_lean", "lean", "normal", "overweight", "obese"};
        String[] expLevels = new String[]{"beginner", "intermediate", "advanced"};
        String[] goalTypes = new String[]{"lose_fat", "gain_muscle", "maintain"};
        String[] targetBodies = bodyTypes; // có thể giống set body types

        ddGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));
        ddBodyType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bodyTypes));
        ddExperience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expLevels));
        ddGoalType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, goalTypes));
        ddTargetBody.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, targetBodies));
    }

    private void setupBirthdayPicker() {
        etBirthday.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int y = c.get(Calendar.YEAR) - 20; // default -20y
            int m = c.get(Calendar.MONTH);
            int d = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String mm = String.format("%02d", month + 1);
                String dd = String.format("%02d", dayOfMonth);
                etBirthday.setText(year + "-" + mm + "-" + dd); // ISO yyyy-MM-dd
            }, y, m, d);
            dlg.show();
        });
    }

    private void saveProfile() {
        clearErrors();

        // Read values
        String birthday = safeText(etBirthday);
        String gender = safeText(ddGender);
        String heightStr = safeText(etHeight);
        String weightStr = safeText(etWeight);
        String currentBodyType = safeText(ddBodyType);
        String dailyMinutesStr = safeText(etDailyMinutes);
        String experience = safeText(ddExperience);
        String goalType = safeText(ddGoalType);
        String targetWeightStr = safeText(etTargetWeight);
        String targetBodyType = safeText(ddTargetBody);

        // Validate minimal
        if (TextUtils.isEmpty(birthday)) { tilBirthday.setError("Chọn ngày sinh"); return; }
        if (TextUtils.isEmpty(gender)) { tilGender.setError("Chọn giới tính"); return; }
        if (TextUtils.isEmpty(heightStr)) { tilHeight.setError("Nhập chiều cao (cm)"); return; }
        if (TextUtils.isEmpty(weightStr)) { tilWeight.setError("Nhập cân nặng (kg)"); return; }
        if (TextUtils.isEmpty(currentBodyType)) { tilBodyType.setError("Chọn body hiện tại"); return; }
        if (TextUtils.isEmpty(dailyMinutesStr)) { tilDailyMinutes.setError("Nhập phút tập/ngày"); return; }
        if (TextUtils.isEmpty(experience)) { tilExperience.setError("Chọn kinh nghiệm"); return; }
        if (TextUtils.isEmpty(goalType)) { tilGoalType.setError("Chọn loại mục tiêu"); return; }
        if (TextUtils.isEmpty(targetWeightStr)) { tilTargetWeight.setError("Nhập cân nặng mục tiêu"); return; }
        if (TextUtils.isEmpty(targetBodyType)) { tilTargetBody.setError("Chọn body mục tiêu"); return; }

        int heightCm = parseInt(heightStr);
        int weightKg = parseInt(weightStr);
        int dailyMinutes = parseInt(dailyMinutesStr);
        int targetWeightKg = parseInt(targetWeightStr);

        if (heightCm <= 0) { tilHeight.setError("Chiều cao không hợp lệ"); return; }
        if (weightKg <= 0) { tilWeight.setError("Cân nặng không hợp lệ"); return; }
        if (dailyMinutes <= 0) { tilDailyMinutes.setError("Phút tập phải > 0"); return; }
        if (targetWeightKg <= 0) { tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ"); return; }

        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        String uid = getIntent().getStringExtra("uid");
        if (fu != null) uid = fu.getUid();
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_LONG).show();
            return;
        }

        // Build map theo schema Profile mở rộng
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("birthday", birthday);
        data.put("gender", gender);
        data.put("heightCm", heightCm);
        data.put("weightKg", weightKg);
        data.put("currentBodyType", currentBodyType);
        data.put("dailyTrainingMinutes", dailyMinutes);
        data.put("experienceLevel", experience);
        data.put("lastUpdatedAt", System.currentTimeMillis());

        // goals sub-map
        Map<String, Object> goals = new HashMap<>();
        goals.put("type", goalType);
        goals.put("targetWeightKg", targetWeightKg);
        goals.put("targetBodyType", targetBodyType);
        data.put("goals", goals);

        // preferences nếu chưa có sẽ merge vào; nếu đã có giữ nguyên
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("units", "metric");
        prefs.put("cameraMode", "front");
        data.put("preferences", prefs);

        btnSave.setEnabled(false);
        FirebaseFirestore.getInstance()
                .collection("profiles").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Lưu hồ sơ thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Log.e("PROFILE_SAVE", "Error", e);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        tilBirthday.setError(null);
        tilGender.setError(null);
        tilHeight.setError(null);
        tilWeight.setError(null);
        tilBodyType.setError(null);
        tilDailyMinutes.setError(null);
        tilExperience.setError(null);
        tilGoalType.setError(null);
        tilTargetWeight.setError(null);
        tilTargetBody.setError(null);
    }

    private String safeText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String safeText(MaterialAutoCompleteTextView v) {
        return v.getText() != null ? v.getText().toString().trim() : "";
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
