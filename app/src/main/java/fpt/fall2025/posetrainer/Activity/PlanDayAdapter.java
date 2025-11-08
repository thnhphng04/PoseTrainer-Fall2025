package fpt.fall2025.posetrainer.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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

        // Set day title
        if (holder.tvDayTitle != null) {
            String title = String.format("Day %d • %d phút", day.dayIndex, day.estMinutes);
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
}
