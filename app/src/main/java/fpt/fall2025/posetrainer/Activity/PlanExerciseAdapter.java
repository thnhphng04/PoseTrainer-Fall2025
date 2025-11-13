package fpt.fall2025.posetrainer.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.R;

public class PlanExerciseAdapter extends RecyclerView.Adapter<PlanExerciseAdapter.VH> {
    private final List<PlanModels.Item> data;

    public PlanExerciseAdapter(List<PlanModels.Item> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvMeta = v.findViewById(R.id.tvMeta);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_exercise, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= data.size()) {
            return;
        }

        PlanModels.Item item = data.get(position);
        if (item == null) {
            return;
        }

        // Set exercise name
        if (holder.tvName != null) {
            String name = item.name;
            if (name == null || name.isEmpty() || "null".equals(name)) {
                name = item.exerciseId != null && !item.exerciseId.isEmpty() && !"null".equals(item.exerciseId)
                        ? item.exerciseId : "Exercise";
            }
            holder.tvName.setText(name);
        }

        // Set exercise metadata
        if (holder.tvMeta != null) {
            String meta = String.format("%d reps × %d sets • %ds nghỉ", item.reps, item.sets, item.restSec);
            holder.tvMeta.setText(meta);
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }
}
