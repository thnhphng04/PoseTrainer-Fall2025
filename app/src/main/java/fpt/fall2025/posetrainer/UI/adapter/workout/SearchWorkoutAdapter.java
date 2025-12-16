package fpt.fall2025.posetrainer.UI.adapter.workout;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.UI.activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Util.GlideImageLoader;
import fpt.fall2025.posetrainer.databinding.ItemSearchBinding;

import java.util.ArrayList;

public class SearchWorkoutAdapter extends RecyclerView.Adapter<SearchWorkoutAdapter.Viewholder> {
    private ArrayList<WorkoutTemplate> list;
    private Context context;

    public SearchWorkoutAdapter(ArrayList<WorkoutTemplate> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    public void updateList(ArrayList<WorkoutTemplate> newList) {
        if (newList == null) {
            newList = new ArrayList<>();
        }
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchWorkoutAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ItemSearchBinding binding = ItemSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchWorkoutAdapter.Viewholder holder, int position) {
        WorkoutTemplate workoutTemplate = list.get(position);

        holder.binding.titleTxt.setText(workoutTemplate.getTitle());

        // Load image: ưu tiên thumbnailUrl, nếu null hoặc lỗi thì fallback về pic_1
        int defaultPicResId = context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
        if (workoutTemplate.getThumbnailUrl() != null && !workoutTemplate.getThumbnailUrl().isEmpty()) {
            // Sử dụng GlideImageLoader với error fallback về pic_1
            GlideImageLoader.loadImage(context, workoutTemplate.getThumbnailUrl(), 
                    holder.binding.pic, null, defaultPicResId);
        } else {
            // Fallback về logic cũ dựa trên focus
            int resId = getImageResourceForWorkout(workoutTemplate);
            Glide.with(holder.itemView.getContext())
                    .load(resId)
                    .into(holder.binding.pic);
        }

        int exerciseCount = (workoutTemplate.getItems() != null) ? workoutTemplate.getItems().size() : 0;
        holder.binding.excerciseTxt.setText(exerciseCount + " bài tập");
        holder.binding.durationTxt.setText(workoutTemplate.getEstDurationMin() + " phút");

        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, WorkoutActivity.class);
                intent.putExtra("workoutTemplateId", workoutTemplate.getId());
                intent.putExtra("fromMainActivity", true);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private int getImageResourceForWorkout(WorkoutTemplate workoutTemplate) {
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
        ItemSearchBinding binding;

        public Viewholder(ItemSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}