package fpt.fall2025.posetrainer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter for displaying schedule items in RecyclerView with filtering
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    public enum FilterMode {
        ALL, PAST, FUTURE
    }
    
    private List<Schedule.ScheduleItem> scheduleItems;
    private List<String> workoutNames;
    
    public ScheduleAdapter(List<Schedule.ScheduleItem> scheduleItems, List<String> workoutNames) {
        this.scheduleItems = scheduleItems != null ? scheduleItems : new ArrayList<>();
        this.workoutNames = workoutNames != null ? workoutNames : new ArrayList<>();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule.ScheduleItem item = scheduleItems.get(position);
        holder.bind(item, workoutNames, position);
    }

    @Override
    public int getItemCount() {
        return scheduleItems.size();
    }

    public void updateSchedules(List<Schedule.ScheduleItem> newItems, List<String> newWorkoutNames) {
        this.scheduleItems = newItems != null ? newItems : new ArrayList<>();
        this.workoutNames = newWorkoutNames != null ? newWorkoutNames : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * Filter items based on filter mode
     */
    public void filterItems(List<Schedule.ScheduleItem> allItems, List<String> allNames, 
                           FilterMode mode, 
                           List<Schedule.ScheduleItem> outFilteredItems, 
                           List<String> outFilteredNames) {
        outFilteredItems.clear();
        outFilteredNames.clear();
        
        // Get current date
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get start of current week (Monday)
        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        
        for (int i = 0; i < allItems.size(); i++) {
            Schedule.ScheduleItem item = allItems.get(i);
            String workoutName = (i < allNames.size()) ? allNames.get(i) : "";
            
            boolean isPast = isScheduleItemPast(item, today, startOfWeek);
            
            if (mode == FilterMode.PAST && isPast) {
                outFilteredItems.add(item);
                outFilteredNames.add(workoutName);
            } else if (mode == FilterMode.FUTURE && !isPast) {
                outFilteredItems.add(item);
                outFilteredNames.add(workoutName);
            }
        }
    }
    
    /**
     * Check if a schedule item is in the past
     */
    private boolean isScheduleItemPast(Schedule.ScheduleItem item, Calendar today, Calendar startOfWeek) {
        if (item.getDayOfWeek() == null || item.getDayOfWeek().isEmpty()) {
            return false;
        }
        
        // Check if any day in the schedule item is in the past
        for (Integer dayOfWeek : item.getDayOfWeek()) {
            // Calculate the actual date for this day in the current week
            Calendar dayDate = (Calendar) startOfWeek.clone();
            int daysToAdd = (dayOfWeek - 1); // Schedule: Monday=1, ..., Sunday=7
            dayDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
            
            // Compare with today
            int dateComparison = dayDate.compareTo(today);
            
            // If any day is in the past or today, consider it past
            if (dateComparison <= 0) {
                return true;
            }
        }
        
        return false;
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvWorkoutName;
        private TextView tvDays;
        private TextView tvTime;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkoutName = itemView.findViewById(R.id.tv_workout_name);
            tvDays = itemView.findViewById(R.id.tv_days);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        public void bind(Schedule.ScheduleItem item, List<String> workoutNames, int position) {
            // Set workout name
            String workoutName = "Bài tập không xác định";
            if (position >= 0 && position < workoutNames.size()) {
                String name = workoutNames.get(position);
                if (name != null && !name.isEmpty() && !name.equals("Đang tải...")) {
                    workoutName = name;
                } else if (item.getWorkoutId() != null) {
                    workoutName = item.getWorkoutId();
                }
            } else if (item.getWorkoutId() != null) {
                workoutName = item.getWorkoutId();
            }
            tvWorkoutName.setText(workoutName);

            // Set days with actual dates
            if (item.getDayOfWeek() != null && !item.getDayOfWeek().isEmpty()) {
                StringBuilder daysText = new StringBuilder("Ngày: ");
                
                // Get start of current week (Monday)
                Calendar startOfWeek = Calendar.getInstance();
                startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
                startOfWeek.set(Calendar.MINUTE, 0);
                startOfWeek.set(Calendar.SECOND, 0);
                startOfWeek.set(Calendar.MILLISECOND, 0);
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                
                for (int i = 0; i < item.getDayOfWeek().size(); i++) {
                    int day = item.getDayOfWeek().get(i);
                    if (day >= 1 && day <= 7) {
                        if (i > 0) daysText.append(", ");
                        
                        // Calculate actual date for this day
                        Calendar dayDate = (Calendar) startOfWeek.clone();
                        int daysToAdd = (day - 1); // Schedule: Monday=1, ..., Sunday=7
                        dayDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
                        
                        // Format day name and date
                        String[] dayNames = {"", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy", "Chủ nhật"};
                        daysText.append(dayNames[day]).append(" (").append(dateFormat.format(dayDate.getTime())).append(")");
                    }
                }
                tvDays.setText(daysText.toString());
            } else {
                tvDays.setText("Ngày: Chưa có");
            }

            // Set time
            if (item.getTimeLocal() != null) {
                tvTime.setText("Giờ: " + item.getTimeLocal());
            } else {
                tvTime.setText("Giờ: Chưa có");
            }
        }
    }
}
