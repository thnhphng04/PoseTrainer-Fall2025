package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.databinding.ViewholderWorktoutBinding;

import java.util.ArrayList;

public class WorkoutTemplateAdapter extends RecyclerView.Adapter<WorkoutTemplateAdapter.Viewholder> {
    private ArrayList<WorkoutTemplate> list; // Thay đổi từ final để có thể update
    private Context context;

    public WorkoutTemplateAdapter(ArrayList<WorkoutTemplate> list) {
        this.list = list != null ? list : new ArrayList<>();
    }
    
    /**
     * Cập nhật danh sách workout templates mới mà không tạo adapter mới
     * Giúp tối ưu hiệu năng và tránh lag khi filter
     */
    public void updateList(ArrayList<WorkoutTemplate> newList) {
        if (newList == null) {
            newList = new ArrayList<>();
        }
        this.list = newList;
        // Thông báo cho RecyclerView rằng toàn bộ data đã thay đổi
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutTemplateAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderWorktoutBinding binding = ViewholderWorktoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutTemplateAdapter.Viewholder holder, int position) {
        WorkoutTemplate workoutTemplate = list.get(position);
        
        System.out.println("WorkoutTemplateAdapter: Binding position " + position + " with template: " + workoutTemplate.getTitle());
        
        holder.binding.titleTxt.setText(workoutTemplate.getTitle());
        
        // Set default image based on workout type or use a default
        int resId = getImageResourceForWorkout(workoutTemplate);
        Glide.with(holder.itemView.getContext())
                .load(resId)
                .into(holder.binding.pic);

        holder.binding.excerciseTxt.setText(workoutTemplate.getItems().size() + " Exercise");
        holder.binding.durationTxt.setText(workoutTemplate.getEstDurationMin() + " min");

        // Set click listener on the root view
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, WorkoutActivity.class);
                intent.putExtra("workoutTemplateId", workoutTemplate.getId());
                intent.putExtra("fromMainActivity", true); // Flag để biết từ MainActivity
                System.out.println("WorkoutTemplateAdapter: Starting WorkoutActivity with ID: " + workoutTemplate.getId() + " from MainActivity");
                context.startActivity(intent);
            } else {
                System.out.println("WorkoutTemplateAdapter: Context is null, cannot start activity");
            }
        });
        
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Get image resource based on workout template focus/type
     */
    private int getImageResourceForWorkout(WorkoutTemplate workoutTemplate) {
        // Default image
        int defaultResId = context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
        
        if (workoutTemplate.getFocus() != null && !workoutTemplate.getFocus().isEmpty()) {
            String focus = workoutTemplate.getFocus().get(0);
            switch (focus) {
                case "push":
                    return context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
                case "legs":
                    return context.getResources().getIdentifier("pic_2", "drawable", context.getPackageName());
                case "cardio":
                    return context.getResources().getIdentifier("pic_3", "drawable", context.getPackageName());
                case "fullbody":
                    return context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
                default:
                    return defaultResId;
            }
        }
        
        return defaultResId;
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderWorktoutBinding binding;

        public Viewholder(ViewholderWorktoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
