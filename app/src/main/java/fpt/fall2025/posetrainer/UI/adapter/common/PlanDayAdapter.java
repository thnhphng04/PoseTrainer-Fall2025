package fpt.fall2025.posetrainer.UI.adapter.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Domain.PlanModels;
import fpt.fall2025.posetrainer.UI.adapter.exercise.PlanExerciseAdapter;

public class PlanDayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_DAY = 0;
    private static final int VIEW_TYPE_FOOTER = 1;
    
    private final List<PlanModels.Day> data;
    private OnActionButtonClickListener listener;

    public interface OnActionButtonClickListener {
        void onAccept();
        void onReject();
    }

    public PlanDayAdapter(List<PlanModels.Day> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    public void setOnActionButtonClickListener(OnActionButtonClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // Footer ở cuối cùng
        if (position == data.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_DAY;
    }

    static class DayVH extends RecyclerView.ViewHolder {
        TextView tvDayTitle, tvFocus;
        RecyclerView rv;

        DayVH(View v) {
            super(v);
            tvDayTitle = v.findViewById(R.id.tvDayTitle);
            tvFocus = v.findViewById(R.id.tvFocus);
            rv = v.findViewById(R.id.rvItems);
        }
    }

    static class FooterVH extends RecyclerView.ViewHolder {
        AppCompatButton btnAccept, btnReject;

        FooterVH(View v) {
            super(v);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnReject = v.findViewById(R.id.btnReject);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_footer, parent, false);
            return new FooterVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_day, parent, false);
            return new DayVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterVH) {
            FooterVH footerHolder = (FooterVH) holder;
            footerHolder.btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept();
                }
            });
            footerHolder.btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject();
                }
            });
        } else if (holder instanceof DayVH) {
            DayVH dayHolder = (DayVH) holder;
            
            if (position < 0 || position >= data.size()) {
                return;
            }

            PlanModels.Day day = data.get(position);
            if (day == null) {
                return;
            }

            // Set day title
            if (dayHolder.tvDayTitle != null) {
                String title = String.format("Ngày %d • %d phút", day.dayIndex, day.estMinutes);
                dayHolder.tvDayTitle.setText(title);
            }

            // Set focus
            if (dayHolder.tvFocus != null) {
                String focusText = "Focus: " + (day.focus == null || day.focus.isEmpty() ? "-" : day.focus);
                dayHolder.tvFocus.setText(focusText);
            }

            // Setup nested RecyclerView for exercises
            if (dayHolder.rv != null) {
                List<PlanModels.Item> items = day.items != null ? day.items : new ArrayList<>();
                dayHolder.rv.setLayoutManager(new LinearLayoutManager(dayHolder.itemView.getContext()));
                dayHolder.rv.setAdapter(new fpt.fall2025.posetrainer.UI.adapter.exercise.PlanExerciseAdapter(items));
            }
        }
    }

    @Override
    public int getItemCount() {
        // +1 cho footer
        return (data != null ? data.size() : 0) + 1;
    }
}

