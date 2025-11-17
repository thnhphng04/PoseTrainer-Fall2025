package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Profile;
import fpt.fall2025.posetrainer.R;

public class EditGoalsActivity extends AppCompatActivity {

    // ===== Views =====
    private TextInputLayout tilBirthday, tilGender, tilHeight, tilWeight,
            tilDailyMinutes, tilWeeklyGoal, tilExperience, tilTargetWeight;
    private TextInputEditText etBirthday, etHeight, etWeight, etDailyMinutes, etWeeklyGoal, etTargetWeight;
    private MaterialAutoCompleteTextView ddGender, ddExperience;
    private ProgressBar progress;

    // Grid: body hiện tại
    private MaterialCardView[] bodyCards;
    private ImageView[] bodyImages;
    private TextView[] bodyTexts;

    // Grid: body mục tiêu
    private MaterialCardView[] targetCards;
    private ImageView[] targetImages;
    private TextView[] targetTexts;

    // ===== State =====
    private String currentBodyType = null; // very_lean|lean|normal|overweight|obese
    private String targetBodyType  = null; // very_lean|lean|normal|overweight|obese

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private final String[] genders   = {"male", "female"};
    private final String[] genderLabels = {"Nam", "Nữ"};
    private final String[] expLevels = {"beginner", "intermediate", "advanced"};
    private final String[] expLabels = {"Người mới bắt đầu", "Trung bình", "Nâng cao"};
    private final String[] bodyTypes = {"very_lean", "lean", "normal", "overweight", "obese"};
    private final String[] bodyLabels= {"Rất gầy", "Gầy", "Bình thường", "Thừa cân", "Béo phì"};

    // 10 ảnh mẫu theo giới tính
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
        setupCurrentGrid();
        setupTargetGrid();

        loadProfile();

        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());
        View cancel = findViewById(R.id.btn_cancel);
        if (cancel != null) cancel.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        // TIL
        tilBirthday     = findViewById(R.id.til_birthday);
        tilGender       = findViewById(R.id.til_gender);
        tilHeight       = findViewById(R.id.til_height);
        tilWeight       = findViewById(R.id.til_weight);
        tilDailyMinutes = findViewById(R.id.til_daily_minutes);
        tilWeeklyGoal   = findViewById(R.id.til_weekly_goal);
        tilExperience   = findViewById(R.id.til_experience);
        tilTargetWeight = findViewById(R.id.til_target_weight);

        // Inputs
        etBirthday     = findViewById(R.id.et_birthday);
        etHeight       = findViewById(R.id.et_height);
        etWeight       = findViewById(R.id.et_weight);
        etDailyMinutes = findViewById(R.id.et_daily_minutes);
        etWeeklyGoal   = findViewById(R.id.et_weekly_goal);
        etTargetWeight = findViewById(R.id.et_target_weight);

        ddGender     = findViewById(R.id.dd_gender);
        ddExperience = findViewById(R.id.dd_experience);

        progress = findViewById(R.id.progress);

        // body current
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

        // body target
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
                findViewById(R.id.ivTarget4),
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
        ddGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genderLabels));
        ddExperience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expLabels));

        // mặc định female (trùng ảnh trong XML)
        ddGender.setText("Nữ", false);
        updateAllBodyImages("female");

        ddGender.setOnItemClickListener((p, v, pos, id) -> {
            String selected = ddGender.getText() != null ? ddGender.getText().toString() : "Nữ";
            String g = selected.equals("Nam") ? "male" : "female";
            updateAllBodyImages(g);
        });
    }

    private void setupBirthdayPicker() {
        etBirthday.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            
            // Try to parse existing date if available
            String existingDate = etBirthday.getText() != null ? 
                    etBirthday.getText().toString() : null;
            long selectedDate = c.getTimeInMillis();
            
            if (existingDate != null && !existingDate.isEmpty()) {
                try {
                    String[] parts = existingDate.split("-");
                    if (parts.length == 3) {
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]) - 1;
                        int day = Integer.parseInt(parts[2]);
                        Calendar existingCal = Calendar.getInstance();
                        existingCal.set(year, month, day, 0, 0, 0);
                        existingCal.set(Calendar.MILLISECOND, 0);
                        selectedDate = existingCal.getTimeInMillis();
                    }
                } catch (NumberFormatException e) {
                    // Use default (20 years ago)
                    Calendar defaultCal = Calendar.getInstance();
                    defaultCal.add(Calendar.YEAR, -20);
                    selectedDate = defaultCal.getTimeInMillis();
                }
            } else {
                // Default to 20 years ago
                Calendar defaultCal = Calendar.getInstance();
                defaultCal.add(Calendar.YEAR, -20);
                selectedDate = defaultCal.getTimeInMillis();
            }
            
            // Set min date (100 years ago)
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -100);
            minDate.set(Calendar.HOUR_OF_DAY, 0);
            minDate.set(Calendar.MINUTE, 0);
            minDate.set(Calendar.SECOND, 0);
            minDate.set(Calendar.MILLISECOND, 0);
            long minDateMillis = minDate.getTimeInMillis();
            
            // Set max date to today
            Calendar maxDate = Calendar.getInstance();
            maxDate.set(Calendar.HOUR_OF_DAY, 23);
            maxDate.set(Calendar.MINUTE, 59);
            maxDate.set(Calendar.SECOND, 59);
            maxDate.set(Calendar.MILLISECOND, 999);
            long maxDateMillis = maxDate.getTimeInMillis();
            
            // Create calendar constraints
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setStart(minDateMillis);
            constraintsBuilder.setEnd(maxDateMillis);
            constraintsBuilder.setValidator(DateValidatorPointBackward.before(maxDateMillis));
            CalendarConstraints constraints = constraintsBuilder.build();
            
            // Create MaterialDatePicker
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Chọn ngày sinh")
                    .setSelection(selectedDate)
                    .setCalendarConstraints(constraints)
                    .build();
            
            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.setTimeInMillis(selection);
                int year = selectedCal.get(Calendar.YEAR);
                int month = selectedCal.get(Calendar.MONTH) + 1;
                int day = selectedCal.get(Calendar.DAY_OF_MONTH);
                String date = String.format("%04d-%02d-%02d", year, month, day);
                etBirthday.setText(date);
            });
            
            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupCurrentGrid() {
        for (int i = 0; i < bodyCards.length; i++) {
            bodyCards[i].setTag(bodyTypes[i]);
            final int idx = i;
            bodyCards[i].setOnClickListener(v -> selectCurrentCard(idx));
        }
    }

    private void setupTargetGrid() {
        for (int i = 0; i < targetCards.length; i++) {
            targetCards[i].setTag(bodyTypes[i]);
            final int idx = i;
            targetCards[i].setOnClickListener(v -> selectTargetCard(idx));
        }
    }

    /** Cập nhật ảnh + nhãn cho CẢ 2 lưới theo giới tính */
    private void updateAllBodyImages(String gender) {
        int[] drawables = "male".equalsIgnoreCase(gender) ? maleBodies : femaleBodies;

        for (int i = 0; i < 5; i++) {
            // current
            bodyImages[i].setImageResource(drawables[i]);
            bodyTexts[i].setText(bodyLabels[i]);
            setCardStroke(bodyCards[i], false);

            // target
            targetImages[i].setImageResource(drawables[i]);
            targetTexts[i].setText(bodyLabels[i]);
            setCardStroke(targetCards[i], false);
        }
        currentBodyType = null;
        targetBodyType = null;
    }

    /** Chọn duy nhất 1 card (current) */
    private void selectCurrentCard(int index) {
        for (int i = 0; i < bodyCards.length; i++) {
            setCardStroke(bodyCards[i], i == index);
        }
        currentBodyType = String.valueOf(bodyCards[index].getTag());
    }

    /** Chọn duy nhất 1 card (target) */
    private void selectTargetCard(int index) {
        for (int i = 0; i < targetCards.length; i++) {
            setCardStroke(targetCards[i], i == index);
        }
        targetBodyType = String.valueOf(targetCards[index].getTag());
    }

    private void setCardStroke(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? dp(2) : dp(1));
        card.setStrokeColor(resolveAttr(selected
                ? com.google.android.material.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutline));
    }

    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }

    private int resolveAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void setLoading(boolean on) {
        if (progress != null) progress.setVisibility(on ? View.VISIBLE : View.GONE);
        View save = findViewById(R.id.btn_save);
        View cancel = findViewById(R.id.btn_cancel);
        if (save != null) save.setEnabled(!on);
        if (cancel != null) cancel.setEnabled(!on);
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

        // gender trước để chọn bộ ảnh
        String gender = doc.getString("gender");
        if (TextUtils.isEmpty(gender)) gender = "female";
        String genderLabel = "male".equals(gender) ? "Nam" : "Nữ";
        ddGender.setText(genderLabel, false);
        updateAllBodyImages(gender);

        // base
        etBirthday.setText(doc.getString("birthday"));
        setInt(etHeight, doc.getLong("heightCm"));
        setInt(etWeight, doc.getLong("weightKg"));
        setInt(etDailyMinutes, doc.getLong("dailyTrainingMinutes"));
        setInt(etWeeklyGoal, doc.getLong("weeklyGoal"));
        String exp = doc.getString("experienceLevel");
        if (exp != null) {
            int expIndex = -1;
            for (int i = 0; i < expLevels.length; i++) {
                if (expLevels[i].equals(exp)) {
                    expIndex = i;
                    break;
                }
            }
            if (expIndex >= 0 && expIndex < expLabels.length) {
                ddExperience.setText(expLabels[expIndex], false);
            }
        }

        // select current body
        String cur = doc.getString("currentBodyType");
        if (cur != null) {
            for (int i = 0; i < bodyCards.length; i++) {
                if (cur.equals(bodyCards[i].getTag())) {
                    selectCurrentCard(i);
                    break;
                }
            }
        }

        // goals
        if (doc.contains("goals") && doc.get("goals") instanceof Map) {
            Map<?, ?> g = (Map<?, ?>) doc.get("goals");
            setInt(etTargetWeight, toLong(g.get("targetWeightKg")));
            String tgt = str(g.get("targetBodyType"));
            if (tgt != null) {
                for (int i = 0; i < targetCards.length; i++) {
                    if (tgt.equals(targetCards[i].getTag())) {
                        selectTargetCard(i);
                        break;
                    }
                }
            }
        }
    }

    private void saveProfile() {
        clearErrors();

        String birthday = txt(etBirthday);
        String genderLabel = txt(ddGender);
        String gender = genderLabel.equals("Nam") ? "male" : "female";
        String heightStr= txt(etHeight);
        String weightStr= txt(etWeight);
        String dailyStr = txt(etDailyMinutes);
        String weeklyStr = txt(etWeeklyGoal);
        String expLabel = txt(ddExperience);
        String exp = "";
        for (int i = 0; i < expLabels.length; i++) {
            if (expLabels[i].equals(expLabel)) {
                exp = expLevels[i];
                break;
            }
        }
        String targetWeightStr = txt(etTargetWeight);

        if (TextUtils.isEmpty(birthday)) { tilBirthday.setError("Chọn ngày sinh"); return; }
        if (TextUtils.isEmpty(genderLabel))   { tilGender.setError("Chọn giới tính"); return; }
        if (TextUtils.isEmpty(heightStr)){ tilHeight.setError("Nhập chiều cao"); return; }
        if (TextUtils.isEmpty(weightStr)){ tilWeight.setError("Nhập cân nặng"); return; }
        if (currentBodyType == null)     { Toast.makeText(this, "Chọn cơ thể hiện tại", Toast.LENGTH_SHORT).show(); return; }
        if (targetBodyType == null)      { Toast.makeText(this, "Chọn cơ thể mục tiêu", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(dailyStr)) { tilDailyMinutes.setError("Nhập phút tập/ngày"); return; }
        if (TextUtils.isEmpty(weeklyStr)) { tilWeeklyGoal.setError("Nhập số ngày tập/tuần"); return; }
        if (TextUtils.isEmpty(exp))      { tilExperience.setError("Chọn kinh nghiệm"); return; }
        if (TextUtils.isEmpty(targetWeightStr)){ tilTargetWeight.setError("Nhập cân nặng mục tiêu"); return; }

        int height = pInt(heightStr), weight = pInt(weightStr),
                daily = pInt(dailyStr), weekly = pInt(weeklyStr), targetW = pInt(targetWeightStr);
        if (height <= 0) { tilHeight.setError("Chiều cao không hợp lệ"); return; }
        if (weight <= 0) { tilWeight.setError("Cân nặng không hợp lệ"); return; }
        if (daily  <= 0) { tilDailyMinutes.setError("Phút tập phải > 0"); return; }
        if (weekly <= 0 || weekly > 7) { tilWeeklyGoal.setError("Số ngày tập phải từ 1-7"); return; }
        if (targetW<= 0) { tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ"); return; }

        // Tạo Goals object
        Profile.Goals goals = new Profile.Goals();
        goals.setTargetWeightKg(targetW);
        goals.setTargetBodyType(targetBodyType);

        // Tạo Profile object
        Profile profile = new Profile();
        profile.setUid(uid);
        profile.setBirthday(birthday);
        profile.setGender(gender);
        profile.setHeightCm(height);
        profile.setWeightKg(weight);
        profile.setCurrentBodyType(currentBodyType);
        profile.setDailyTrainingMinutes(daily);
        profile.setWeeklyGoal(weekly);
        profile.setExperienceLevel(exp);
        profile.setGoals(goals);
        profile.setLastUpdatedAt(System.currentTimeMillis());

        setLoading(true);
        db.collection("profiles").document(uid)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Cập nhật mục tiêu thành công", Toast.LENGTH_SHORT).show();
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
        tilDailyMinutes.setError(null);
        tilWeeklyGoal.setError(null);
        tilExperience.setError(null);
        tilTargetWeight.setError(null);
    }
}
