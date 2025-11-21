package fpt.fall2025.posetrainer.Fragment.Registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

import fpt.fall2025.posetrainer.R;

public class BasicInfoFragment extends Fragment {

    private TextInputLayout tilBirthday, tilGender, tilHeight, tilWeight,
            tilDailyMinutes, tilWeeklyGoal;
    private TextInputEditText etBirthday, etHeight, etWeight, etDailyMinutes, etWeeklyGoal;
    private MaterialAutoCompleteTextView ddGender;

    private final String[] genderLabels = {"Nam", "Nữ"};
    private BasicInfoListener listener;

    public interface BasicInfoListener {
        void onBasicInfoChanged(String birthday, String gender, String height, String weight,
                                String dailyMinutes, String weeklyGoal);
        String getGender();
        void setGender(String gender);
    }

    public void setListener(BasicInfoListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_basic_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupDropdowns();
        setupBirthdayPicker();
        setupTextWatchers();
    }

    private void bindViews(View view) {
        tilBirthday = view.findViewById(R.id.til_birthday);
        tilGender = view.findViewById(R.id.til_gender);
        tilHeight = view.findViewById(R.id.til_height);
        tilWeight = view.findViewById(R.id.til_weight);
        tilDailyMinutes = view.findViewById(R.id.til_daily_minutes);
        tilWeeklyGoal = view.findViewById(R.id.til_weekly_goal);

        etBirthday = view.findViewById(R.id.et_birthday);
        etHeight = view.findViewById(R.id.et_height);
        etWeight = view.findViewById(R.id.et_weight);
        etDailyMinutes = view.findViewById(R.id.et_daily_minutes);
        etWeeklyGoal = view.findViewById(R.id.et_weekly_goal);
        ddGender = view.findViewById(R.id.dd_gender);
    }

    private void setupDropdowns() {
        ddGender.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, genderLabels));

        // Mặc định "Nữ"
        String currentGender = listener != null ? listener.getGender() : null;
        if (TextUtils.isEmpty(currentGender)) {
            ddGender.setText("Nữ", false);
            if (listener != null) {
                listener.setGender("female");
            }
        } else {
            String label = "male".equals(currentGender) ? "Nam" : "Nữ";
            ddGender.setText(label, false);
        }

        ddGender.setOnItemClickListener((parent, v, position, id) -> {
            String selected = genderLabels[position];
            String gender = selected.equals("Nam") ? "male" : "female";
            if (listener != null) {
                listener.setGender(gender);
            }
            notifyChanges();
        });
    }

    private void setupBirthdayPicker() {
        etBirthday.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();

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
                    Calendar defaultCal = Calendar.getInstance();
                    defaultCal.add(Calendar.YEAR, -20);
                    selectedDate = defaultCal.getTimeInMillis();
                }
            } else {
                Calendar defaultCal = Calendar.getInstance();
                defaultCal.add(Calendar.YEAR, -20);
                selectedDate = defaultCal.getTimeInMillis();
            }

            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -100);
            minDate.set(Calendar.HOUR_OF_DAY, 0);
            minDate.set(Calendar.MINUTE, 0);
            minDate.set(Calendar.SECOND, 0);
            minDate.set(Calendar.MILLISECOND, 0);
            long minDateMillis = minDate.getTimeInMillis();

            Calendar maxDate = Calendar.getInstance();
            maxDate.set(Calendar.HOUR_OF_DAY, 23);
            maxDate.set(Calendar.MINUTE, 59);
            maxDate.set(Calendar.SECOND, 59);
            maxDate.set(Calendar.MILLISECOND, 999);
            long maxDateMillis = maxDate.getTimeInMillis();

            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setStart(minDateMillis);
            constraintsBuilder.setEnd(maxDateMillis);
            constraintsBuilder.setValidator(DateValidatorPointBackward.before(maxDateMillis));
            CalendarConstraints constraints = constraintsBuilder.build();

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
                notifyChanges();
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupTextWatchers() {
        etHeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) notifyChanges();
        });
        etWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) notifyChanges();
        });
        etDailyMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) notifyChanges();
        });
        etWeeklyGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) notifyChanges();
        });
    }

    private void notifyChanges() {
        if (listener != null) {
            String birthday = etBirthday.getText() != null ? etBirthday.getText().toString().trim() : "";
            String genderLabel = ddGender.getText() != null ? ddGender.getText().toString().trim() : "";
            String gender = genderLabel.equals("Nam") ? "male" : "female";
            String height = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
            String weight = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
            String dailyMinutes = etDailyMinutes.getText() != null ? etDailyMinutes.getText().toString().trim() : "";
            String weeklyGoal = etWeeklyGoal.getText() != null ? etWeeklyGoal.getText().toString().trim() : "";

            listener.onBasicInfoChanged(birthday, gender, height, weight, dailyMinutes, weeklyGoal);
        }
    }

    public boolean validate() {
        clearErrors();

        String birthday = etBirthday.getText() != null ? etBirthday.getText().toString().trim() : "";
        String genderLabel = ddGender.getText() != null ? ddGender.getText().toString().trim() : "";
        String heightStr = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
        String weightStr = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
        String dailyStr = etDailyMinutes.getText() != null ? etDailyMinutes.getText().toString().trim() : "";
        String weeklyStr = etWeeklyGoal.getText() != null ? etWeeklyGoal.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(birthday)) {
            tilBirthday.setError("Chọn ngày sinh");
            isValid = false;
        }
        if (TextUtils.isEmpty(genderLabel)) {
            tilGender.setError("Chọn giới tính");
            isValid = false;
        }
        if (TextUtils.isEmpty(heightStr)) {
            tilHeight.setError("Nhập chiều cao");
            isValid = false;
        } else {
            try {
                int height = Integer.parseInt(heightStr);
                if (height <= 0) {
                    tilHeight.setError("Chiều cao không hợp lệ");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilHeight.setError("Chiều cao không hợp lệ");
                isValid = false;
            }
        }
        if (TextUtils.isEmpty(weightStr)) {
            tilWeight.setError("Nhập cân nặng");
            isValid = false;
        } else {
            try {
                int weight = Integer.parseInt(weightStr);
                if (weight <= 0) {
                    tilWeight.setError("Cân nặng không hợp lệ");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilWeight.setError("Cân nặng không hợp lệ");
                isValid = false;
            }
        }
        if (TextUtils.isEmpty(dailyStr)) {
            tilDailyMinutes.setError("Nhập phút tập/ngày");
            isValid = false;
        } else {
            try {
                int daily = Integer.parseInt(dailyStr);
                if (daily <= 0) {
                    tilDailyMinutes.setError("Phút tập phải > 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilDailyMinutes.setError("Phút tập không hợp lệ");
                isValid = false;
            }
        }
        if (TextUtils.isEmpty(weeklyStr)) {
            tilWeeklyGoal.setError("Nhập số ngày tập/tuần");
            isValid = false;
        } else {
            try {
                int weekly = Integer.parseInt(weeklyStr);
                if (weekly <= 0 || weekly > 7) {
                    tilWeeklyGoal.setError("Số ngày tập phải từ 1-7");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilWeeklyGoal.setError("Số ngày tập không hợp lệ");
                isValid = false;
            }
        }

        return isValid;
    }

    private void clearErrors() {
        tilBirthday.setError(null);
        tilGender.setError(null);
        tilHeight.setError(null);
        tilWeight.setError(null);
        tilDailyMinutes.setError(null);
        tilWeeklyGoal.setError(null);
    }
}


