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
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.DAL.WorkoutTemplateDAO;
import fpt.fall2025.posetrainer.DAL.UserWorkoutDAO;

public class CreateScheduleDialog extends DialogFragment {
    private static final String TAG = "CreateScheduleDialog";
    
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private Spinner spinnerWorkout;
    private Button btnCancel;
    private Button btnSave;
    
    private Calendar selectedDate;
    private Calendar selectedTime;
    private ArrayList<WorkoutTemplate> workoutTemplates;
    private ArrayList<UserWorkout> userWorkouts;
    private ArrayList<Object> allWorkouts; // Combined list for spinner
    private WorkoutTemplateDAO workoutTemplateDAO;
    private UserWorkoutDAO userWorkoutDAO;
    private ArrayAdapter<String> workoutAdapter;
    private OnScheduleCreatedListener listener;
    
    public interface OnScheduleCreatedListener {
        void onScheduleCreated(Schedule.ScheduleItem scheduleItem);
    }
    
    public void setOnScheduleCreatedListener(OnScheduleCreatedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_schedule, null);
        
        workoutTemplateDAO = new WorkoutTemplateDAO();
        userWorkoutDAO = new UserWorkoutDAO();
        
        initViews(view);
        setupListeners();
        loadWorkouts();
        
        // Initialize with current date/time
        selectedDate = Calendar.getInstance();
        selectedTime = Calendar.getInstance();
        updateDateDisplay();
        updateTimeDisplay();
        
        builder.setView(view);
        return builder.create();
    }
    
    private void initViews(View view) {
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        spinnerWorkout = view.findViewById(R.id.spinner_workout);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);
        
        LinearLayout llDatePicker = view.findViewById(R.id.ll_date_picker);
        LinearLayout llTimePicker = view.findViewById(R.id.ll_time_picker);
        
        llDatePicker.setOnClickListener(v -> showDatePicker());
        llTimePicker.setOnClickListener(v -> showTimePicker());
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveSchedule();
            }
        });
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
            true // 24-hour format
        );
        
        timePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
        }
    }
    
    private void updateTimeDisplay() {
        if (selectedTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvSelectedTime.setText(sdf.format(selectedTime.getTime()));
        }
    }
    
    private void loadWorkouts() {
        workoutTemplates = new ArrayList<>();
        userWorkouts = new ArrayList<>();
        allWorkouts = new ArrayList<>();
        
        workoutTemplateDAO.getPublicTemplates(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                workoutTemplates = new ArrayList<>(task.getResult());
                allWorkouts.addAll(workoutTemplates);
                
                // Load user workouts
                com.google.firebase.auth.FirebaseUser currentUser = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    userWorkoutDAO.getByUserId(currentUser.getUid(), userWorkoutTask -> {
                        if (userWorkoutTask.isSuccessful() && userWorkoutTask.getResult() != null) {
                            userWorkouts = new ArrayList<>(userWorkoutTask.getResult());
                            allWorkouts.addAll(userWorkouts);
                            updateWorkoutSpinner();
                        }
                    });
                } else {
                    updateWorkoutSpinner();
                }
            }
        });
    }
    
    private void updateWorkoutSpinner() {
        List<String> workoutNames = new ArrayList<>();
        workoutNames.add("Chọn bài tập");
        
        for (WorkoutTemplate template : workoutTemplates) {
            workoutNames.add(template.getTitle() + " (Mẫu)");
        }
        
        for (UserWorkout userWorkout : userWorkouts) {
            workoutNames.add(userWorkout.getTitle() + " (Của tôi)");
        }
        
        workoutAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            workoutNames
        );
        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkout.setAdapter(workoutAdapter);
    }
    
    private boolean validateInput() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (selectedTime == null) {
            Toast.makeText(getContext(), "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show();
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
        if (selectedDate == null || selectedTime == null) {
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
        
        // Convert Calendar day to Schedule format (Monday=1, ..., Sunday=7)
        int calendarDayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        int scheduleDayOfWeek = convertCalendarDayToScheduleDay(calendarDayOfWeek);
        
        // Create ScheduleItem
        List<Integer> daysOfWeek = new ArrayList<>();
        daysOfWeek.add(scheduleDayOfWeek);
        
        String timeLocal = new SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(selectedTime.getTime());
        
        // Format exact date as "yyyy-MM-dd"
        String exactDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(selectedDate.getTime());
        
        Schedule.ScheduleItem scheduleItem = new Schedule.ScheduleItem(
            daysOfWeek,
            timeLocal,
            workoutId,
            exactDate
        );
        
        if (listener != null) {
            listener.onScheduleCreated(scheduleItem);
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

