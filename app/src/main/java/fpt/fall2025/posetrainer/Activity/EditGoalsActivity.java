package fpt.fall2025.posetrainer.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.R;

public class EditGoalsActivity extends AppCompatActivity {

    private TextInputLayout tilBirthday, tilGender, tilHeight, tilWeight, tilBodyType,
            tilDailyMinutes, tilExperience, tilGoalType, tilTargetWeight, tilTargetBody;
    private TextInputEditText etBirthday, etHeight, etWeight, etDailyMinutes, etTargetWeight;
    private MaterialAutoCompleteTextView ddGender, ddBodyType, ddExperience, ddGoalType, ddTargetBody;
    private Button btnSave, btnCancel;
    private ProgressBar progress;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private final String[] genders = {"male", "female", "other"};
    private final String[] bodyTypes = {"very_lean", "lean", "normal", "overweight", "obese"};
    private final String[] expLevels = {"beginner", "intermediate", "advanced"};
    private final String[] goalTypes = {"lose_fat", "gain_muscle", "maintain"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_goals);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        uid = fu.getUid();

        bindViews();
        setupDropdowns();
        setupBirthdayPicker();

        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
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
        btnCancel = findViewById(R.id.btn_cancel);
        progress = findViewById(R.id.progress);
    }

    private void setupDropdowns() {
        ddGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));
        ddBodyType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bodyTypes));
        ddExperience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expLevels));
        ddGoalType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, goalTypes));
        ddTargetBody.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bodyTypes));
    }

    private void setupBirthdayPicker() {
        etBirthday.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int y = c.get(Calendar.YEAR) - 20;
            int m = c.get(Calendar.MONTH);
            int d = c.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String mm = String.format("%02d", month + 1);
                String dd = String.format("%02d", dayOfMonth);
                etBirthday.setText(year + "-" + mm + "-" + dd);
            }, y, m, d).show();
        });
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? android.view.View.VISIBLE : android.view.View.GONE);
        btnSave.setEnabled(!on);
        btnCancel.setEnabled(!on);
    }

    private void loadProfile() {
        setLoading(true);
        db.collection("profiles").document(uid).get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e("EditGoalsActivity", "loadProfile failed", e);
                    Toast.makeText(this, "Không tải được dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void bindProfile(DocumentSnapshot doc) {
        setLoading(false);
        if (!doc.exists()) return;

        // base
        etBirthday.setText(doc.getString("birthday"));
        setText(ddGender, doc.getString("gender"));
        setInt(etHeight, doc.getLong("heightCm"));
        setInt(etWeight, doc.getLong("weightKg"));
        setText(ddBodyType, doc.getString("currentBodyType"));
        setInt(etDailyMinutes, doc.getLong("dailyTrainingMinutes"));
        setText(ddExperience, doc.getString("experienceLevel"));

        // goals
        if (doc.contains("goals") && doc.get("goals") instanceof Map) {
            Map<?, ?> g = (Map<?, ?>) doc.get("goals");
            setText(ddGoalType, str(g.get("type")));
            setInt(etTargetWeight, toLong(g.get("targetWeightKg")));
            setText(ddTargetBody, str(g.get("targetBodyType")));
        }
    }

    private void saveProfile() {
        clearErrors();

        String birthday = txt(etBirthday);
        String gender = txt(ddGender);
        String heightStr = txt(etHeight);
        String weightStr = txt(etWeight);
        String currentBodyType = txt(ddBodyType);
        String dailyStr = txt(etDailyMinutes);
        String exp = txt(ddExperience);
        String goalType = txt(ddGoalType);
        String targetWeightStr = txt(etTargetWeight);
        String targetBodyType = txt(ddTargetBody);

        // Validate nhẹ
        if (TextUtils.isEmpty(birthday)) { tilBirthday.setError("Chọn ngày sinh"); return; }
        if (TextUtils.isEmpty(gender)) { tilGender.setError("Chọn giới tính"); return; }
        if (TextUtils.isEmpty(heightStr)) { tilHeight.setError("Nhập chiều cao"); return; }
        if (TextUtils.isEmpty(weightStr)) { tilWeight.setError("Nhập cân nặng"); return; }
        if (TextUtils.isEmpty(currentBodyType)) { tilBodyType.setError("Chọn body hiện tại"); return; }
        if (TextUtils.isEmpty(dailyStr)) { tilDailyMinutes.setError("Nhập phút tập/ngày"); return; }
        if (TextUtils.isEmpty(exp)) { tilExperience.setError("Chọn kinh nghiệm"); return; }
        if (TextUtils.isEmpty(goalType)) { tilGoalType.setError("Chọn loại mục tiêu"); return; }
        if (TextUtils.isEmpty(targetWeightStr)) { tilTargetWeight.setError("Nhập cân nặng mục tiêu"); return; }
        if (TextUtils.isEmpty(targetBodyType)) { tilTargetBody.setError("Chọn body mục tiêu"); return; }

        int height = pInt(heightStr), weight = pInt(weightStr), daily = pInt(dailyStr), targetW = pInt(targetWeightStr);
        if (height <= 0) { tilHeight.setError("Chiều cao không hợp lệ"); return; }
        if (weight <= 0) { tilWeight.setError("Cân nặng không hợp lệ"); return; }
        if (daily <= 0) { tilDailyMinutes.setError("Phút tập phải > 0"); return; }
        if (targetW <= 0) { tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ"); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("birthday", birthday);
        data.put("gender", gender);
        data.put("heightCm", height);
        data.put("weightKg", weight);
        data.put("currentBodyType", currentBodyType);
        data.put("dailyTrainingMinutes", daily);
        data.put("experienceLevel", exp);
        data.put("lastUpdatedAt", System.currentTimeMillis());

        Map<String, Object> goals = new HashMap<>();
        goals.put("type", goalType);
        goals.put("targetWeightKg", targetW);
        goals.put("targetBodyType", targetBodyType);
        data.put("goals", goals);

        setLoading(true);
        db.collection("profiles").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Cập nhật mục tiêu thành công", Toast.LENGTH_SHORT).show();
                    // Quay về Profile (onResume ProfileFragment sẽ reload)
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e("EditGoalsActivity", "save failed", e);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /* ---------- helpers ---------- */
    private void setText(MaterialAutoCompleteTextView v, String s) { if (s != null) v.setText(s, false); }
    private void setInt(TextInputEditText v, Long n) { if (n != null) v.setText(String.valueOf(n)); }
    private String str(Object o) { return o == null ? null : String.valueOf(o); }
    private Long toLong(Object o) { try { return o == null ? null : Long.parseLong(o.toString()); } catch (Exception e) { return null; } }

    private String txt(TextInputEditText et) { return et.getText() != null ? et.getText().toString().trim() : ""; }
    private String txt(MaterialAutoCompleteTextView et) { return et.getText() != null ? et.getText().toString().trim() : ""; }
    private int pInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }

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
}