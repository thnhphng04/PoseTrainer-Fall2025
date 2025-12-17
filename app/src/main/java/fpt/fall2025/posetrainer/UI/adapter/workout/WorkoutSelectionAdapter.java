package fpt.fall2025.posetrainer.UI.adapter.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Util.GlideImageLoader;


import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.databinding.ItemWorkoutSelectionBinding;

public class WorkoutSelectionAdapter extends RecyclerView.Adapter<WorkoutSelectionAdapter.ViewHolder> {
    private ArrayList<Object> workouts; // Can contain WorkoutTemplate or UserWorkout
    private OnWorkoutSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnWorkoutSelectedListener {
        void onWorkoutSelected(Object workout, int position);
    }

    public WorkoutSelectionAdapter(ArrayList<Object> workouts) {
        this.workouts = workouts != null ? workouts : new ArrayList<>();
    }

    public void setOnWorkoutSelectedListener(OnWorkoutSelectedListener listener) {
        this.listener = listener;
    }

    public void updateList(ArrayList<Object> newList) {
        this.workouts = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition >= 0) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public Object getSelectedWorkout() {
        if (selectedPosition >= 0 && selectedPosition < workouts.size()) {
            return workouts.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutSelectionBinding binding = ItemWorkoutSelectionBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object workout = workouts.get(position);

        String title = "";
        int exerciseCount = 0;
        int duration = 0;
        String thumbnailUrl = null;

        if (workout instanceof WorkoutTemplate) {
            WorkoutTemplate template = (WorkoutTemplate) workout;
            title = template.getTitle();
            exerciseCount = template.getItems() != null ? template.getItems().size() : 0;
            duration = template.getEstDurationMin();
            thumbnailUrl = template.getThumbnailUrl();

        } else if (workout instanceof UserWorkout) {
            UserWorkout userWorkout = (UserWorkout) workout;
            title = userWorkout.getTitle();
            exerciseCount = userWorkout.getItems() != null ? userWorkout.getItems().size() : 0;
            duration = exerciseCount * 3;
        }

        // Bind UI
        holder.binding.titleTxt.setText(title);
        holder.binding.excerciseTxt.setText(exerciseCount + " bài tập");
        holder.binding.durationTxt.setText(duration + " phút");

        // Load thumbnail
        int defaultPicResId = holder.itemView.getContext()
                .getResources()
                .getIdentifier("pic_1", "drawable",
                        holder.itemView.getContext().getPackageName());

        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            GlideImageLoader.loadImage(
                    holder.itemView.getContext(),
                    thumbnailUrl,
                    holder.binding.pic,
                    null,
                    defaultPicResId
            );
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(defaultPicResId)
                    .into(holder.binding.pic);
        }

        // Selected indicator
        holder.binding.ivSelected.setVisibility(
                holder.getAdapterPosition() == selectedPosition
                        ? View.VISIBLE
                        : View.GONE
        );

        // Click listener — KHÔNG dùng position
        holder.itemView.setOnClickListener(v -> {
            int clickedPos = holder.getAdapterPosition();
            if (clickedPos == RecyclerView.NO_POSITION) return;

            int oldPos = selectedPosition;
            selectedPosition = clickedPos;

            if (oldPos >= 0) notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onWorkoutSelected(workouts.get(clickedPos), clickedPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemWorkoutSelectionBinding binding;

        public ViewHolder(ItemWorkoutSelectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

