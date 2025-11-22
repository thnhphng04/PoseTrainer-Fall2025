package fpt.fall2025.posetrainer.View;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fpt.fall2025.posetrainer.R;

/**
 * Custom view để hiển thị calendar heatmap với highlight ngày có tập
 */
public class CalendarHeatmapView extends LinearLayout {
    private RecyclerView recyclerView;
    private CalendarAdapter adapter;
    private Set<String> workoutDates; // Set of dates in "yyyy-MM-dd" format
    private Calendar currentMonth;

    public CalendarHeatmapView(Context context) {
        super(context);
        init(context);
    }

    public CalendarHeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CalendarHeatmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        
        // Initialize current month
        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        currentMonth.set(Calendar.HOUR_OF_DAY, 0);
        currentMonth.set(Calendar.MINUTE, 0);
        currentMonth.set(Calendar.SECOND, 0);
        currentMonth.set(Calendar.MILLISECOND, 0);
        
        workoutDates = new HashSet<>();
        
        // Create RecyclerView
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Setup GridLayoutManager with 7 columns (days of week)
        GridLayoutManager layoutManager = new GridLayoutManager(context, 7);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new CalendarAdapter();
        recyclerView.setAdapter(adapter);
        
        addView(recyclerView);
    }

    /**
     * Set workout dates from list of date strings (format: "yyyy-MM-dd")
     */
    public void setWorkoutDates(List<String> dates) {
        workoutDates.clear();
        if (dates != null) {
            workoutDates.addAll(dates);
        }
        if (adapter != null) {
            adapter.generateDays();
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Set workout dates from UserProgress calendar map
     */
    public void setWorkoutDatesFromProgress(java.util.Map<String, Boolean> calendar) {
        List<String> dates = new ArrayList<>();
        if (calendar != null) {
            for (java.util.Map.Entry<String, Boolean> entry : calendar.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    dates.add(entry.getKey());
                }
            }
        }
        setWorkoutDates(dates);
    }

    /**
     * RecyclerView Adapter for calendar grid
     */
    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {
        private List<CalendarDay> days;

        CalendarAdapter() {
            days = new ArrayList<>();
            generateDays();
        }

        private void generateDays() {
            days.clear();
            
            // Add day headers (Sun, Mon, Tue, etc.)
            String[] dayHeaders = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
            for (String header : dayHeaders) {
                days.add(new CalendarDay(header, true, false));
            }
            
            // Get first day of month
            Calendar cal = (Calendar) currentMonth.clone();
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            // Add empty cells before first day
            int startOffset = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7;
            for (int i = 0; i < startOffset; i++) {
                days.add(new CalendarDay("", false, false));
            }
            
            // Add days of month
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (int day = 1; day <= daysInMonth; day++) {
                cal.set(Calendar.DAY_OF_MONTH, day);
                String dateKey = dateFormat.format(cal.getTime());
                boolean hasWorkout = workoutDates.contains(dateKey);
                days.add(new CalendarDay(String.valueOf(day), false, hasWorkout));
            }
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            CalendarDay day = days.get(position);
            holder.bind(day);
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tv_calendar_day);
            }

            void bind(CalendarDay day) {
                tvDay.setText(day.text);
                
                if (day.isHeader) {
                    // Header style
                    tvDay.setTextColor(Color.parseColor("#99ffffff"));
                    tvDay.setBackgroundColor(Color.TRANSPARENT);
                    tvDay.setTextSize(12);
                } else if (day.text.isEmpty()) {
                    // Empty cell
                    tvDay.setVisibility(View.INVISIBLE);
                } else {
                    // Day cell
                    tvDay.setVisibility(View.VISIBLE);
                    tvDay.setTextSize(14);
                    
                    if (day.hasWorkout) {
                        // Highlight workout day
                        tvDay.setTextColor(Color.WHITE);
                        tvDay.setBackgroundColor(Color.parseColor("#4d9df2"));
                    } else {
                        // Normal day
                        tvDay.setTextColor(Color.parseColor("#ffffff"));
                        tvDay.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        }
    }

    /**
     * Data class for calendar day
     */
    private static class CalendarDay {
        String text;
        boolean isHeader;
        boolean hasWorkout;

        CalendarDay(String text, boolean isHeader, boolean hasWorkout) {
            this.text = text;
            this.isHeader = isHeader;
            this.hasWorkout = hasWorkout;
        }
    }
}
