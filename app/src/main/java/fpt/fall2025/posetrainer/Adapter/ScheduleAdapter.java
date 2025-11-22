package fpt.fall2025.posetrainer.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
    private OnScheduleItemClickListener itemClickListener;
    private OnScheduleItemLongClickListener itemLongClickListener;
    
    public interface OnScheduleItemClickListener {
        void onItemClick(Schedule.ScheduleItem item, int position);
    }
    
    public interface OnScheduleItemLongClickListener {
        boolean onItemLongClick(Schedule.ScheduleItem item, int position, View view);
    }
    
    public ScheduleAdapter(List<Schedule.ScheduleItem> scheduleItems, List<String> workoutNames) {
        this.scheduleItems = scheduleItems != null ? scheduleItems : new ArrayList<>();
        this.workoutNames = workoutNames != null ? workoutNames : new ArrayList<>();
    }
    
    public void setOnScheduleItemClickListener(OnScheduleItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public void setOnScheduleItemLongClickListener(OnScheduleItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
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
        
        // Get current date and time
        Calendar now = Calendar.getInstance();
        
        for (int i = 0; i < allItems.size(); i++) {
            Schedule.ScheduleItem item = allItems.get(i);
            String workoutName = (i < allNames.size()) ? allNames.get(i) : "";
            
            boolean isPast = isScheduleItemPast(item, now);
            
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
     * Ưu tiên sử dụng exactDate, nếu không có thì fallback về dayOfWeek
     */
    private boolean isScheduleItemPast(Schedule.ScheduleItem item, Calendar now) {
        // Ưu tiên sử dụng exactDate nếu có
        if (item.getExactDate() != null && !item.getExactDate().isEmpty()) {
            try {
                // Parse exactDate (format: "yyyy-MM-dd")
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar scheduleDate = Calendar.getInstance();
                scheduleDate.setTime(dateFormat.parse(item.getExactDate()));
                
                // Set time from timeLocal if available
                if (item.getTimeLocal() != null && !item.getTimeLocal().isEmpty()) {
                    String[] timeParts = item.getTimeLocal().split(":");
                    if (timeParts.length == 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        scheduleDate.set(Calendar.HOUR_OF_DAY, hour);
                        scheduleDate.set(Calendar.MINUTE, minute);
                        scheduleDate.set(Calendar.SECOND, 0);
                        scheduleDate.set(Calendar.MILLISECOND, 0);
                    }
                } else {
                    // Nếu không có timeLocal, set về cuối ngày (23:59:59)
                    scheduleDate.set(Calendar.HOUR_OF_DAY, 23);
                    scheduleDate.set(Calendar.MINUTE, 59);
                    scheduleDate.set(Calendar.SECOND, 59);
                    scheduleDate.set(Calendar.MILLISECOND, 999);
                }
                
                // So sánh với thời gian hiện tại
                return scheduleDate.before(now) || scheduleDate.equals(now);
            } catch (Exception e) {
                Log.e("ScheduleAdapter", "Error parsing exactDate: " + item.getExactDate(), e);
                // Fallback về dayOfWeek nếu parse lỗi
            }
        }
        
        // Fallback: sử dụng dayOfWeek nếu không có exactDate
        if (item.getDayOfWeek() != null && !item.getDayOfWeek().isEmpty()) {
            // Get current date (without time)
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
        }
        
        return false;
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvWorkoutName;
        private TextView tvDays;
        private TextView tvTime;
        private View itemView;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvWorkoutName = itemView.findViewById(R.id.tv_workout_name);
            tvDays = itemView.findViewById(R.id.tv_days);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        public void bind(Schedule.ScheduleItem item, List<String> workoutNames, int position,
                        OnScheduleItemClickListener clickListener,
                        OnScheduleItemLongClickListener longClickListener) {
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
            // Ưu tiên sử dụng exactDate nếu có
            if (item.getExactDate() != null && !item.getExactDate().isEmpty()) {
                try {
                    // Parse exactDate (format: "yyyy-MM-dd")
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    java.util.Date date = inputFormat.parse(item.getExactDate());
                    
                    // Get day name
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    String[] dayNames = {"", "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"};
                    String dayName = (dayOfWeek >= 1 && dayOfWeek <= 7) ? dayNames[dayOfWeek] : "";
                    
                    tvDays.setText("Ngày: " + dayName + " " + outputFormat.format(date));
                } catch (Exception e) {
                    Log.e("ScheduleAdapter", "Error parsing exactDate: " + item.getExactDate(), e);
                    tvDays.setText("Ngày: " + item.getExactDate());
                }
            } else if (item.getDayOfWeek() != null && !item.getDayOfWeek().isEmpty()) {
                // Fallback: sử dụng dayOfWeek nếu không có exactDate
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
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item, position);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onItemLongClick(item, position, itemView);
                }
                return false;
            });
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule.ScheduleItem item = scheduleItems.get(position);
        holder.bind(item, workoutNames, position, itemClickListener, itemLongClickListener);
    }
}
