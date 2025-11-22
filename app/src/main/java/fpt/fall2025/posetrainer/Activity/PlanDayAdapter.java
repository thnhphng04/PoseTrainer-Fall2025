package fpt.fall2025.posetrainer.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.R;

public class PlanDayAdapter extends RecyclerView.Adapter<PlanDayAdapter.VH> {
    private final List<PlanModels.Day> data;

    public PlanDayAdapter(List<PlanModels.Day> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDayTitle, tvFocus;
        RecyclerView rv;

        VH(View v) {
            super(v);
            tvDayTitle = v.findViewById(R.id.tvDayTitle);
            tvFocus = v.findViewById(R.id.tvFocus);
            rv = v.findViewById(R.id.rvItems);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= data.size()) {
            return;
        }

        PlanModels.Day day = data.get(position);
        if (day == null) {
            return;
        }

        // Set day title với exactDate
        if (holder.tvDayTitle != null) {
            // ✅ Tính toán exactDate dựa trên dayIndex (tương tự logic trong PlanPreviewActivity)
            String exactDate = calculateExactDate(day.dayIndex);
            
            // ✅ Đổi "Day" thành "Ngày" và thêm exactDate
            String title = String.format("Ngày %d • %d phút", day.dayIndex, day.estMinutes);
            if (exactDate != null && !exactDate.isEmpty()) {
                // Format: "Ngày 1 • 60 phút • 25/12/2024"
                title += " • " + formatDateForDisplay(exactDate);
            }
            holder.tvDayTitle.setText(title);
        }

        // Set focus
        if (holder.tvFocus != null) {
            String focusText = "Focus: " + (day.focus == null || day.focus.isEmpty() ? "-" : day.focus);
            holder.tvFocus.setText(focusText);
        }

        // Setup nested RecyclerView for exercises
        if (holder.rv != null) {
            List<PlanModels.Item> items = day.items != null ? day.items : new ArrayList<>();
            holder.rv.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.rv.setAdapter(new PlanExerciseAdapter(items));
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    /**
     * Tính toán exactDate dựa trên dayIndex
     * Logic tương tự như trong PlanPreviewActivity.createWorkoutsAndScheduleWithTime()
     */
    private String calculateExactDate(int dayIndex) {
        try {
            // Tính toán ngày bắt đầu (thứ 2 của tuần hiện tại hoặc tuần tiếp theo)
            Calendar calendar = Calendar.getInstance();
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysUntilMonday = (Calendar.MONDAY - currentDayOfWeek + 7) % 7;
            
            // Nếu đã qua 8h sáng thứ 2, bắt đầu từ tuần sau
            if (daysUntilMonday == 0 && calendar.get(Calendar.HOUR_OF_DAY) >= 8) {
                daysUntilMonday = 7;
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, daysUntilMonday);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Calendar weekStart = (Calendar) calendar.clone();

            // Tính toán exactDate: thứ 2 + (dayIndex - 1) ngày
            Calendar exactDateCalendar = (Calendar) weekStart.clone();
            exactDateCalendar.add(Calendar.DAY_OF_MONTH, dayIndex - 1);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(exactDateCalendar.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format date từ "yyyy-MM-dd" sang "dd/MM/yyyy" để hiển thị
     */
    private String formatDateForDisplay(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            return dateStr; // Trả về nguyên bản nếu parse lỗi
        }
    }
}
