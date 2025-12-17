package fpt.fall2025.posetrainer.UI.adapter.workout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

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
        
        if (workout instanceof WorkoutTemplate) {
            WorkoutTemplate template = (WorkoutTemplate) workout;
            holder.binding.tvWorkoutTitle.setText(template.getTitle());
            
            int exerciseCount = (template.getItems() != null) ? template.getItems().size() : 0;
            holder.binding.tvExerciseCount.setText(exerciseCount + " bài tập");
            holder.binding.tvDuration.setText(template.getEstDurationMin() + " phút");
        } else if (workout instanceof UserWorkout) {
            UserWorkout userWorkout = (UserWorkout) workout;
            holder.binding.tvWorkoutTitle.setText(userWorkout.getTitle());
            
            int exerciseCount = (userWorkout.getItems() != null) ? userWorkout.getItems().size() : 0;
            holder.binding.tvExerciseCount.setText(exerciseCount + " bài tập");
            
            // Calculate estimated duration: ~3 minutes per exercise
            int estimatedDuration = exerciseCount * 3;
            holder.binding.tvDuration.setText(estimatedDuration + " phút");
        }

        // Show/hide selected indicator
        if (position == selectedPosition) {
            holder.binding.ivSelected.setVisibility(View.VISIBLE);
        } else {
            holder.binding.ivSelected.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            
            if (oldPosition >= 0) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onWorkoutSelected(workout, position);
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

