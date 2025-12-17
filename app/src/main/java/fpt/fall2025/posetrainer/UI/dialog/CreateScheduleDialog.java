package fpt.fall2025.posetrainer.UI.dialog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import fpt.fall2025.posetrainer.UI.adapter.workout.WorkoutSelectionAdapter;

public class CreateScheduleDialog extends DialogFragment {
    private static final String TAG = "CreateScheduleDialog";
    
    // Views
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private Button btnMyWorkouts;
    private Button btnTemplates;
    private EditText etSearch;
    private ImageView ivClearSearch;
    private RecyclerView recyclerViewWorkouts;
    private LinearLayout llSelectedWorkout;
    private TextView tvSelectedWorkout;
    private Button btnCancel;
    private Button btnSave;
    
    // Data
    private Calendar selectedDate;
    private Calendar selectedTime;
    private ArrayList<WorkoutTemplate> workoutTemplates;
    private ArrayList<UserWorkout> userWorkouts;
    private ArrayList<Object> displayedWorkouts; // Currently displayed workouts
    private ArrayList<Object> allMyWorkouts; // All user workouts
    private ArrayList<Object> allTemplateWorkouts; // All template workouts
    private WorkoutTemplateDAO workoutTemplateDAO;
    private UserWorkoutDAO userWorkoutDAO;
    private WorkoutSelectionAdapter workoutAdapter;
    private OnScheduleCreatedListener listener;
    
    // State
    private boolean isShowingMyWorkouts = true; // true = "Của tôi", false = "Mẫu"
    private Object selectedWorkout; // Currently selected workout
    
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
        btnMyWorkouts = view.findViewById(R.id.btn_my_workouts);
        btnTemplates = view.findViewById(R.id.btn_templates);
        etSearch = view.findViewById(R.id.et_search);
        ivClearSearch = view.findViewById(R.id.iv_clear_search);
        recyclerViewWorkouts = view.findViewById(R.id.recycler_view_workouts);
        llSelectedWorkout = view.findViewById(R.id.ll_selected_workout);
        tvSelectedWorkout = view.findViewById(R.id.tv_selected_workout);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);
        
        LinearLayout llDatePicker = view.findViewById(R.id.ll_date_picker);
        LinearLayout llTimePicker = view.findViewById(R.id.ll_time_picker);
        
        llDatePicker.setOnClickListener(v -> showDatePicker());
        llTimePicker.setOnClickListener(v -> showTimePicker());
        
        // Setup RecyclerView
        recyclerViewWorkouts.setLayoutManager(new LinearLayoutManager(getContext()));
        displayedWorkouts = new ArrayList<>();
        workoutAdapter = new WorkoutSelectionAdapter(displayedWorkouts);
        workoutAdapter.setOnWorkoutSelectedListener((workout, position) -> {
            selectedWorkout = workout;
            updateSelectedWorkoutDisplay();
        });
        recyclerViewWorkouts.setAdapter(workoutAdapter);
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveSchedule();
            }
        });
        
        // Tab buttons
        btnMyWorkouts.setOnClickListener(v -> switchToMyWorkouts());
        btnTemplates.setOnClickListener(v -> switchToTemplates());
        
        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWorkouts(s.toString());
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            ivClearSearch.setVisibility(View.GONE);
        });
    }
    
    private void switchToMyWorkouts() {
        isShowingMyWorkouts = true;
        updateTabButtons();
        displayedWorkouts.clear();
        displayedWorkouts.addAll(allMyWorkouts);
        workoutAdapter.updateList(displayedWorkouts);
        filterWorkouts(etSearch.getText().toString());
    }
    
    private void switchToTemplates() {
        isShowingMyWorkouts = false;
        updateTabButtons();
        displayedWorkouts.clear();
        displayedWorkouts.addAll(allTemplateWorkouts);
        workoutAdapter.updateList(displayedWorkouts);
        filterWorkouts(etSearch.getText().toString());
    }
    
    private void updateTabButtons() {
        if (isShowingMyWorkouts) {
            btnMyWorkouts.setBackgroundResource(R.drawable.button_primary);
            btnMyWorkouts.setTextColor(ContextCompat.getColor(requireContext(), R.color.daily_text_primary));
            btnTemplates.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.daily_background));
            btnTemplates.setTextColor(ContextCompat.getColor(requireContext(), R.color.daily_text_secondary));
        } else {
            btnMyWorkouts.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.daily_background));
            btnMyWorkouts.setTextColor(ContextCompat.getColor(requireContext(), R.color.daily_text_secondary));
            btnTemplates.setBackgroundResource(R.drawable.button_primary);
            btnTemplates.setTextColor(ContextCompat.getColor(requireContext(), R.color.daily_text_primary));
        }
    }
    
    private void filterWorkouts(String query) {
        ArrayList<Object> filtered = new ArrayList<>();
        ArrayList<Object> source = isShowingMyWorkouts ? allMyWorkouts : allTemplateWorkouts;
        
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(source);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Object workout : source) {
                String title = "";
                if (workout instanceof WorkoutTemplate) {
                    title = ((WorkoutTemplate) workout).getTitle();
                } else if (workout instanceof UserWorkout) {
                    title = ((UserWorkout) workout).getTitle();
                }
                if (title.toLowerCase().contains(lowerQuery)) {
                    filtered.add(workout);
                }
            }
        }
        
        displayedWorkouts.clear();
        displayedWorkouts.addAll(filtered);
        workoutAdapter.updateList(displayedWorkouts);
    }
    
    private void updateSelectedWorkoutDisplay() {
        if (selectedWorkout != null) {
            String title = "";
            if (selectedWorkout instanceof WorkoutTemplate) {
                title = ((WorkoutTemplate) selectedWorkout).getTitle();
            } else if (selectedWorkout instanceof UserWorkout) {
                title = ((UserWorkout) selectedWorkout).getTitle();
            }
            tvSelectedWorkout.setText(title);
            llSelectedWorkout.setVisibility(View.VISIBLE);
        } else {
            llSelectedWorkout.setVisibility(View.GONE);
        }
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
        allMyWorkouts = new ArrayList<>();
        allTemplateWorkouts = new ArrayList<>();
        
        // Load templates
        workoutTemplateDAO.getPublicTemplates(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                workoutTemplates = new ArrayList<>(task.getResult());
                allTemplateWorkouts.clear();
                allTemplateWorkouts.addAll(workoutTemplates);
                
                // Load user workouts
                com.google.firebase.auth.FirebaseUser currentUser = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    userWorkoutDAO.getByUserId(currentUser.getUid(), userWorkoutTask -> {
                        if (userWorkoutTask.isSuccessful() && userWorkoutTask.getResult() != null) {
                            userWorkouts = new ArrayList<>(userWorkoutTask.getResult());
                            allMyWorkouts.clear();
                            allMyWorkouts.addAll(userWorkouts);
                            updateWorkoutList();
                        } else {
                            updateWorkoutList();
                        }
                    });
                } else {
                    updateWorkoutList();
                }
            } else {
                updateWorkoutList();
            }
        });
    }
    
    private void updateWorkoutList() {
        // Show "Của tôi" by default
        isShowingMyWorkouts = true;
        updateTabButtons();
        displayedWorkouts.clear();
        displayedWorkouts.addAll(allMyWorkouts);
        workoutAdapter.updateList(displayedWorkouts);
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
        
        if (selectedWorkout == null) {
            Toast.makeText(getContext(), "Vui lòng chọn bài tập", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveSchedule() {
        if (selectedDate == null || selectedTime == null || selectedWorkout == null) {
            return;
        }
        
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

