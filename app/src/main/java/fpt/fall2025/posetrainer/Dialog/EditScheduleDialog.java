package fpt.fall2025.posetrainer.Dialog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;

public class EditScheduleDialog extends DialogFragment {
    private static final String TAG = "EditScheduleDialog";
    private static final String ARG_SCHEDULE_ITEM = "schedule_item";
    private static final String ARG_ITEM_INDEX = "item_index";
    
    private TextView tvSelectedTime;
    private TextView tvSelectedDate;
    private Spinner spinnerWorkout;
    private Button btnCancel, btnDelete, btnSave;
    
    private Calendar selectedTime;
    private Calendar selectedDate;
    private ArrayList<WorkoutTemplate> workoutTemplates;
    private ArrayList<UserWorkout> userWorkouts;
    private ArrayList<Object> allWorkouts;
    private ArrayAdapter<String> workoutAdapter;
    private Schedule.ScheduleItem originalItem;
    private int itemIndex;
    private OnScheduleUpdatedListener listener;
    
    public interface OnScheduleUpdatedListener {
        void onScheduleUpdated(Schedule.ScheduleItem updatedItem, int index);
        void onScheduleDeleted(int index);
    }
    
    public static EditScheduleDialog newInstance(Schedule.ScheduleItem item, int index) {
        EditScheduleDialog dialog = new EditScheduleDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SCHEDULE_ITEM, item);
        args.putInt(ARG_ITEM_INDEX, index);
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnScheduleUpdatedListener(OnScheduleUpdatedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_schedule, null);
        
        // Get arguments
        if (getArguments() != null) {
            originalItem = (Schedule.ScheduleItem) getArguments().getSerializable(ARG_SCHEDULE_ITEM);
            itemIndex = getArguments().getInt(ARG_ITEM_INDEX, -1);
        }
        
        if (originalItem == null) {
            dismiss();
            return builder.create();
        }
        
        initViews(view);
        setupListeners();
        loadWorkouts();
        populateFields();
        
        builder.setView(view);
        return builder.create();
    }
    
    private void initViews(View view) {
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        spinnerWorkout = view.findViewById(R.id.spinner_workout);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnDelete = view.findViewById(R.id.btn_delete);
        btnSave = view.findViewById(R.id.btn_save);
        
        LinearLayout llTimePicker = view.findViewById(R.id.ll_time_picker);
        llTimePicker.setOnClickListener(v -> showTimePicker());
        
        LinearLayout llDatePicker = view.findViewById(R.id.ll_date_picker);
        llDatePicker.setOnClickListener(v -> showDatePicker());
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa lịch tập này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (listener != null) {
                        listener.onScheduleDeleted(itemIndex);
                    }
                    dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
        });
        
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveSchedule();
            }
        });
    }
    
    private void populateFields() {
        // Set time
        if (originalItem.getTimeLocal() != null && !originalItem.getTimeLocal().isEmpty()) {
            try {
                String[] timeParts = originalItem.getTimeLocal().split(":");
                if (timeParts.length == 2) {
                    selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                    selectedTime.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
                    updateTimeDisplay();
                }
            } catch (Exception e) {
                selectedTime = Calendar.getInstance();
                updateTimeDisplay();
            }
        } else {
            selectedTime = Calendar.getInstance();
            updateTimeDisplay();
        }
        
        // Set date from exactDate
        if (originalItem.getExactDate() != null && !originalItem.getExactDate().isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date date = dateFormat.parse(originalItem.getExactDate());
                selectedDate = Calendar.getInstance();
                selectedDate.setTime(date);
                updateDateDisplay();
            } catch (Exception e) {
                // Nếu không parse được exactDate, dùng ngày hiện tại
                selectedDate = Calendar.getInstance();
                updateDateDisplay();
            }
        } else {
            // Nếu không có exactDate, dùng ngày hiện tại
            selectedDate = Calendar.getInstance();
            updateDateDisplay();
        }
    }
    
    private void showTimePicker() {
        Calendar calendar = selectedTime != null ? selectedTime : Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            requireContext(),
            (view, hourOfDay, minute) -> {
                selectedTime = Calendar.getInstance();
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                updateTimeDisplay();
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }
    
    private void showDatePicker() {
        Calendar calendar = selectedDate != null ? selectedDate : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                updateDateDisplay();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        
        datePickerDialog.show();
    }
    
    private void updateTimeDisplay() {
        if (selectedTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvSelectedTime.setText(sdf.format(selectedTime.getTime()));
        }
    }
    
    private void updateDateDisplay() {
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
        }
    }
    
    private void loadWorkouts() {
        workoutTemplates = new ArrayList<>();
        userWorkouts = new ArrayList<>();
        allWorkouts = new ArrayList<>();
        
        FirebaseService.getInstance().loadWorkoutTemplates(
            (androidx.appcompat.app.AppCompatActivity) requireActivity(),
            templates -> {
                workoutTemplates = templates != null ? templates : new ArrayList<>();
                allWorkouts.addAll(workoutTemplates);
                
                com.google.firebase.auth.FirebaseUser currentUser = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    FirebaseService.getInstance().loadUserWorkouts(
                        currentUser.getUid(),
                        (androidx.appcompat.app.AppCompatActivity) requireActivity(),
                        userWorkoutsList -> {
                            userWorkouts = userWorkoutsList != null ? userWorkoutsList : new ArrayList<>();
                            allWorkouts.addAll(userWorkouts);
                            updateWorkoutSpinner();
                        }
                    );
                } else {
                    updateWorkoutSpinner();
                }
            }
        );
    }
    
    private void updateWorkoutSpinner() {
        List<String> workoutNames = new ArrayList<>();
        workoutNames.add("Chọn bài tập");
        
        int selectedIndex = 0;
        String currentWorkoutId = originalItem.getWorkoutId();
        
        for (int i = 0; i < workoutTemplates.size(); i++) {
            WorkoutTemplate template = workoutTemplates.get(i);
            workoutNames.add(template.getTitle() + " (Mẫu)");
            if (template.getId().equals(currentWorkoutId)) {
                selectedIndex = i + 1;
            }
        }
        
        for (int i = 0; i < userWorkouts.size(); i++) {
            UserWorkout userWorkout = userWorkouts.get(i);
            workoutNames.add(userWorkout.getTitle() + " (Của tôi)");
            if (userWorkout.getId().equals(currentWorkoutId)) {
                selectedIndex = workoutTemplates.size() + i + 1;
            }
        }
        
        workoutAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            workoutNames
        );
        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkout.setAdapter(workoutAdapter);
        spinnerWorkout.setSelection(selectedIndex);
    }
    
    private boolean validateInput() {
        if (selectedTime == null) {
            Toast.makeText(getContext(), "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        int selectedPosition = spinnerWorkout.getSelectedItemPosition();
        if (selectedPosition == 0 || allWorkouts == null || selectedPosition > allWorkouts.size()) {
            Toast.makeText(getContext(), "Vui lòng chọn bài tập", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveSchedule() {
        if (selectedTime == null || selectedDate == null) {
            return;
        }
        
        int selectedPosition = spinnerWorkout.getSelectedItemPosition();
        if (selectedPosition <= 0 || selectedPosition > allWorkouts.size()) {
            return;
        }
        
        Object selectedWorkout = allWorkouts.get(selectedPosition - 1);
        String workoutId;
        
        if (selectedWorkout instanceof WorkoutTemplate) {
            workoutId = ((WorkoutTemplate) selectedWorkout).getId();
        } else if (selectedWorkout instanceof UserWorkout) {
            workoutId = ((UserWorkout) selectedWorkout).getId();
        } else {
            return;
        }
        
        String timeLocal = new SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(selectedTime.getTime());
        
        // Format exactDate as "yyyy-MM-dd"
        String exactDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(selectedDate.getTime());
        
        // Tính dayOfWeek từ selectedDate (để tương thích ngược)
        int calendarDayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        int scheduleDayOfWeek = convertCalendarDayToScheduleDay(calendarDayOfWeek);
        List<Integer> daysOfWeek = new ArrayList<>();
        daysOfWeek.add(scheduleDayOfWeek);
        
        Schedule.ScheduleItem updatedItem = new Schedule.ScheduleItem(
            daysOfWeek,
            timeLocal,
            workoutId,
            exactDate
        );
        
        if (listener != null) {
            listener.onScheduleUpdated(updatedItem, itemIndex);
        }
        
        dismiss();
    }
    
    /**
     * Convert Calendar day of week to Schedule day of week format
     * Calendar: Sunday=1, Monday=2, ..., Saturday=7
     * Schedule: Monday=1, Tuesday=2, ..., Sunday=7
     */
    private int convertCalendarDayToScheduleDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
            default:
                return 1;
        }
    }
}

