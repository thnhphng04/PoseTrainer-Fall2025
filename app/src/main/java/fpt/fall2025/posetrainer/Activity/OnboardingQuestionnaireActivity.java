package fpt.fall2025.posetrainer.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import fpt.fall2025.posetrainer.R;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.R;

public class OnboardingQuestionnaireActivity extends AppCompatActivity {

    // TextInputLayouts
    private TextInputLayout tilBirthday, tilGender, tilHeight, tilWeight,
            tilDailyMinutes, tilExperience, tilTargetWeight;

    // Inputs
    private TextInputEditText etBirthday, etHeight, etWeight, etDailyMinutes, etTargetWeight;
    private MaterialAutoCompleteTextView ddGender, ddExperience;

    // Body CURRENT grid
    private MaterialCardView[] bodyCards;
    private ImageView[] bodyImages;
    private TextView[] bodyTexts;

    // Body TARGET grid
    private MaterialCardView[] targetCards;
    private ImageView[] targetImages;
    private TextView[] targetTexts;

    // State
    private String currentBodyType = null; // very_lean | lean | normal | overweight | obese
    private String targetBodyType  = null; // very_lean | lean | normal | overweight | obese

    // Resource arrays for 10 images
    private final int[] maleBodies = {
            R.drawable.male_body_very_lean,
            R.drawable.male_body_lean,
            R.drawable.male_body_normal,
            R.drawable.male_body_overweight,
            R.drawable.male_body_obese
    };
    private final int[] femaleBodies = {
            R.drawable.female_body_very_lean,
            R.drawable.female_body_lean,
            R.drawable.female_body_normal,
            R.drawable.female_body_overweight,
            R.drawable.female_body_obese
    };
    private final String[] bodyLabels = {"Very Lean", "Lean", "Normal", "Overweight", "Obese"};
    private final String[] bodyTypes  = {"very_lean", "lean", "normal", "overweight", "obese"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_questionnaire);

        bindViews();
        setupDropdowns();
        setupBirthdayPicker();
        setupCurrentBodyGrid();
        setupTargetBodyGrid();

        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        // TIL
        tilBirthday     = findViewById(R.id.til_birthday);
        tilGender       = findViewById(R.id.til_gender);
        tilHeight       = findViewById(R.id.til_height);
        tilWeight       = findViewById(R.id.til_weight);
        tilDailyMinutes = findViewById(R.id.til_daily_minutes);
        tilExperience   = findViewById(R.id.til_experience);
        tilTargetWeight = findViewById(R.id.til_target_weight); // << thêm

        // Inputs
        etBirthday     = findViewById(R.id.et_birthday);
        etHeight       = findViewById(R.id.et_height);
        etWeight       = findViewById(R.id.et_weight);
        etDailyMinutes = findViewById(R.id.et_daily_minutes);
        etTargetWeight = findViewById(R.id.et_target_weight);   // << thêm

        ddGender     = findViewById(R.id.dd_gender);
        ddExperience = findViewById(R.id.dd_experience);

        // CURRENT body grid
        bodyCards = new MaterialCardView[]{
                findViewById(R.id.card_body_1),
                findViewById(R.id.card_body_2),
                findViewById(R.id.card_body_3),
                findViewById(R.id.card_body_4),
                findViewById(R.id.card_body_5)
        };
        bodyImages = new ImageView[]{
                findViewById(R.id.ivBody1),
                findViewById(R.id.ivBody2),
                findViewById(R.id.ivBody3),
                findViewById(R.id.ivBody4),
                findViewById(R.id.ivBody5)
        };
        bodyTexts = new TextView[]{
                findViewById(R.id.tvBody1),
                findViewById(R.id.tvBody2),
                findViewById(R.id.tvBody3),
                findViewById(R.id.tvBody4),
                findViewById(R.id.tvBody5)
        };

        // TARGET body grid
        targetCards = new MaterialCardView[]{
                findViewById(R.id.card_target_1),
                findViewById(R.id.card_target_2),
                findViewById(R.id.card_target_3),
                findViewById(R.id.card_target_4),
                findViewById(R.id.card_target_5)
        };

        targetImages = new ImageView[]{
                findViewById(R.id.ivTarget1),
                findViewById(R.id.ivTarget2),
                findViewById(R.id.ivTarget3),
                findViewById(R.id.ivTarget4), // <-- bỏ dấu chấm, có dấu phẩy
                findViewById(R.id.ivTarget5)
        };

        targetTexts = new TextView[]{
                findViewById(R.id.tvTarget1),
                findViewById(R.id.tvTarget2),
                findViewById(R.id.tvTarget3),
                findViewById(R.id.tvTarget4),
                findViewById(R.id.tvTarget5)
        };
    }

    private void setupDropdowns() {
        String[] genders   = {"male", "female"};
        String[] expLevels = {"beginner", "intermediate", "advanced"};

        ddGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));
        ddExperience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expLevels));

        // Default gender = female (khớp ảnh mặc định trong XML)
        ddGender.setText("female", false);
        updateAllBodyImages("female");

        ddGender.setOnItemClickListener((parent, view, position, id) -> {
            String g = ddGender.getText() != null ? ddGender.getText().toString().toLowerCase() : "female";
            updateAllBodyImages(g);
        });
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

    private void setupCurrentBodyGrid() {
        for (int i = 0; i < bodyCards.length; i++) {
            bodyCards[i].setTag(bodyTypes[i]);
            final int idx = i;
            bodyCards[i].setOnClickListener(v -> selectBodyCard(idx));
        }
    }

    private void setupTargetBodyGrid() {
        for (int i = 0; i < targetCards.length; i++) {
            targetCards[i].setTag(bodyTypes[i]);
            final int idx = i;
            targetCards[i].setOnClickListener(v -> selectTargetCard(idx));
        }
    }

    /** Cập nhật ảnh và label cho CẢ 2 lưới theo giới tính */
    private void updateAllBodyImages(String gender) {
        int[] drawables = "male".equalsIgnoreCase(gender) ? maleBodies : femaleBodies;

        // current grid
        for (int i = 0; i < 5; i++) {
            bodyImages[i].setImageResource(drawables[i]);
            bodyTexts[i].setText(bodyLabels[i]);
            setCardStroke(bodyCards[i], false);
        }
        currentBodyType = null;

        // target grid
        for (int i = 0; i < 5; i++) {
            targetImages[i].setImageResource(drawables[i]);
            targetTexts[i].setText(bodyLabels[i]);
            setCardStroke(targetCards[i], false);
        }
        targetBodyType = null;
    }

    /** Chọn duy nhất 1 card (current) */
    private void selectBodyCard(int index) {
        for (int i = 0; i < bodyCards.length; i++) {
            boolean selected = (i == index);
            setCardStroke(bodyCards[i], selected);
        }
        Object tag = bodyCards[index].getTag();
        currentBodyType = tag != null ? tag.toString() : null;
    }

    /** Chọn duy nhất 1 card (target) */
    private void selectTargetCard(int index) {
        for (int i = 0; i < targetCards.length; i++) {
            boolean selected = (i == index);
            setCardStroke(targetCards[i], selected);
        }
        Object tag = targetCards[index].getTag();
        targetBodyType = tag != null ? tag.toString() : null;
    }

    private void setCardStroke(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? dp(2) : dp(1));
        card.setStrokeColor(resolveAttr(selected
                ? com.google.android.material.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutline));
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private int resolveAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void saveProfile() {
        clearErrors();

        // Read values
        String birthday        = safeText(etBirthday);
        String gender          = safeText(ddGender);
        String heightStr       = safeText(etHeight);
        String weightStr       = safeText(etWeight);
        String dailyMinutesStr = safeText(etDailyMinutes);
        String experience      = safeText(ddExperience);
        String targetWeightStr = safeText(etTargetWeight); // << thêm

        // Validate minimal
        if (TextUtils.isEmpty(birthday))       { tilBirthday.setError("Chọn ngày sinh"); return; }
        if (TextUtils.isEmpty(gender))         { tilGender.setError("Chọn giới tính"); return; }
        if (TextUtils.isEmpty(heightStr))      { tilHeight.setError("Nhập chiều cao (cm)"); return; }
        if (TextUtils.isEmpty(weightStr))      { tilWeight.setError("Nhập cân nặng (kg)"); return; }
        if (currentBodyType == null)           { Toast.makeText(this, "Chọn body hiện tại", Toast.LENGTH_SHORT).show(); return; }
        if (targetBodyType == null)            { Toast.makeText(this, "Chọn body mục tiêu", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(dailyMinutesStr)){ tilDailyMinutes.setError("Nhập phút tập/ngày"); return; }
        if (TextUtils.isEmpty(experience))     { tilExperience.setError("Chọn kinh nghiệm"); return; }
        if (TextUtils.isEmpty(targetWeightStr)){ tilTargetWeight.setError("Nhập cân nặng mục tiêu"); return; } // << thêm

        int heightCm      = parseInt(heightStr);
        int weightKg      = parseInt(weightStr);
        int dailyMinutes  = parseInt(dailyMinutesStr);
        int targetWeight  = parseInt(targetWeightStr); // << thêm

        if (heightCm <= 0)   { tilHeight.setError("Chiều cao không hợp lệ"); return; }
        if (weightKg <= 0)   { tilWeight.setError("Cân nặng không hợp lệ"); return; }
        if (dailyMinutes <= 0){ tilDailyMinutes.setError("Phút tập phải > 0"); return; }
        if (targetWeight <= 0){ tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ"); return; } // << thêm

        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (fu != null) ? fu.getUid() : null;
        if (TextUtils.isEmpty(uid)) {
            Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_LONG).show();
            return;
        }

        // Build map theo schema Profile (goals có targetBodyType + targetWeightKg)
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

        Map<String, Object> goals = new HashMap<>();
        goals.put("targetBodyType", targetBodyType);
        goals.put("targetWeightKg", targetWeight); // << thêm
        data.put("goals", goals);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("units", "metric");
        prefs.put("cameraMode", "front");
        data.put("preferences", prefs);

        findViewById(R.id.btn_save).setEnabled(false);
        FirebaseFirestore.getInstance()
                .collection("profiles").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Lưu hồ sơ thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.btn_save).setEnabled(true);
                    Log.e("PROFILE_SAVE", "Error", e);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearErrors() {
        tilBirthday.setError(null);
        tilGender.setError(null);
        tilHeight.setError(null);
        tilWeight.setError(null);
        tilDailyMinutes.setError(null);
        tilExperience.setError(null);
        tilTargetWeight.setError(null); // << thêm
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
