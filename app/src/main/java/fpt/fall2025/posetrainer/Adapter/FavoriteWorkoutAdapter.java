package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Activity.UserWorkoutDetailActivity;
import fpt.fall2025.posetrainer.Domain.FavoriteWorkoutItem;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.databinding.ItemFavoriteWorkoutBinding;

import java.util.ArrayList;

/**
 * Adapter để hiển thị danh sách favorite workout templates và user workouts trong tab Yêu thích
 * Sử dụng layout item_favorite_workout.xml riêng biệt
 */
public class FavoriteWorkoutAdapter extends RecyclerView.Adapter<FavoriteWorkoutAdapter.Viewholder> {
    private final ArrayList<FavoriteWorkoutItem> list;
    private Context context;

    public FavoriteWorkoutAdapter(ArrayList<FavoriteWorkoutItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public FavoriteWorkoutAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ItemFavoriteWorkoutBinding binding = ItemFavoriteWorkoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteWorkoutAdapter.Viewholder holder, int position) {
        FavoriteWorkoutItem item = list.get(position);
        
        // Set title
        holder.binding.tvTitle.setText(item.getTitle());
        
        // Set description
        String description = item.getDescription();
        if (description != null && !description.isEmpty()) {
            holder.binding.tvDescription.setText(description);
            holder.binding.tvDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvDescription.setVisibility(android.view.View.GONE);
        }
        
        // Set duration
        holder.binding.tvDuration.setText(item.getDurationMin() + " min");
        
        // Set exercise count
        holder.binding.tvExerciseCount.setText(item.getExerciseCount() + " Exercise");
        
        // Set image
        int resId = getImageResourceForWorkout(item);
        Glide.with(holder.itemView.getContext())
                .load(resId)
                .into(holder.binding.ivThumbnail);

        // Set click listener
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                if (item.isUserWorkout()) {
                    // Mở UserWorkoutDetailActivity cho UserWorkout
                    Intent intent = new Intent(context, UserWorkoutDetailActivity.class);
                    intent.putExtra("userWorkoutId", item.getId());
                    context.startActivity(intent);
                } else {
                    // Mở WorkoutActivity cho WorkoutTemplate
                    Intent intent = new Intent(context, WorkoutActivity.class);
                    intent.putExtra("workoutTemplateId", item.getId());
                    intent.putExtra("fromMainActivity", true);
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Lấy image resource dựa trên workout type
     */
    private int getImageResourceForWorkout(FavoriteWorkoutItem item) {
        // Default image
        int defaultResId = context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
        
        if (item.isUserWorkout()) {
            // Cho UserWorkout, sử dụng source để xác định image
            UserWorkout userWorkout = item.getUserWorkout();
            if (userWorkout != null && userWorkout.getSource() != null) {
                switch (userWorkout.getSource()) {
                    case "template":
                        return context.getResources().getIdentifier("pic_2", "drawable", context.getPackageName());
                    case "ai":
                        return context.getResources().getIdentifier("pic_3", "drawable", context.getPackageName());
                    case "custom":
                    default:
                        return defaultResId;
                }
            }
            return defaultResId;
        } else {
            // Cho WorkoutTemplate, sử dụng focus
            WorkoutTemplate workoutTemplate = item.getWorkoutTemplate();
            if (workoutTemplate != null && workoutTemplate.getFocus() != null && !workoutTemplate.getFocus().isEmpty()) {
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
    }

    /**
     * Cập nhật danh sách favorite workout items
     */
    public void updateFavoriteWorkoutItems(ArrayList<FavoriteWorkoutItem> newList) {
        this.list.clear();
        this.list.addAll(newList);
        notifyDataSetChanged();
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ItemFavoriteWorkoutBinding binding;

        public Viewholder(ItemFavoriteWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

