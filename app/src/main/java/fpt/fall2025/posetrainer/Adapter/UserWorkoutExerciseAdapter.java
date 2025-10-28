package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Activity.ExerciseActivity;
import fpt.fall2025.posetrainer.Activity.ExerciseDetailActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.ViewholderUserWorkoutExerciseBinding;

import java.util.ArrayList;

public class UserWorkoutExerciseAdapter extends RecyclerView.Adapter<UserWorkoutExerciseAdapter.Viewholder> {

    private final ArrayList<Exercise> list;
    private final UserWorkout userWorkout;
    private Context context;
    private OnSetsRepsChangedListener listener;
    private OnDifficultyChangedListener difficultyListener;

    public UserWorkoutExerciseAdapter(ArrayList<Exercise> list, UserWorkout userWorkout) {
        this.list = list;
        this.userWorkout = userWorkout;
    }
    
    public interface OnSetsRepsChangedListener {
        void onSetsRepsChanged(int exerciseIndex, int sets, int reps);
    }
    
    public interface OnDifficultyChangedListener {
        void onDifficultyChanged(int exerciseIndex, String difficulty);
    }
    
    public void setOnSetsRepsChangedListener(OnSetsRepsChangedListener listener) {
        this.listener = listener;
    }
    
    public void setOnDifficultyChangedListener(OnDifficultyChangedListener listener) {
        this.difficultyListener = listener;
    }
    
    /**
     * Debug method to log adapter state
     */
    public void debugAdapterState() {
        android.util.Log.d("UserWorkoutExerciseAdapter", "=== ADAPTER DEBUG STATE ===");
        android.util.Log.d("UserWorkoutExerciseAdapter", "List size: " + list.size());
        android.util.Log.d("UserWorkoutExerciseAdapter", "UserWorkout: " + (userWorkout != null ? userWorkout.getTitle() : "null"));
        
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                Exercise exercise = list.get(i);
                android.util.Log.d("UserWorkoutExerciseAdapter", "Exercise " + i + ": " + exercise.getName() + " (ID: " + exercise.getId() + ")");
            }
        }
        
        if (userWorkout != null && userWorkout.getItems() != null) {
            android.util.Log.d("UserWorkoutExerciseAdapter", "UserWorkout items: " + userWorkout.getItems().size());
            for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
                android.util.Log.d("UserWorkoutExerciseAdapter", "Item - Order: " + item.getOrder() + ", ExerciseId: " + item.getExerciseId());
            }
        }
        android.util.Log.d("UserWorkoutExerciseAdapter", "=== END ADAPTER DEBUG ===");
    }

    @NonNull
    @Override
    public UserWorkoutExerciseAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderUserWorkoutExerciseBinding binding = ViewholderUserWorkoutExerciseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserWorkoutExerciseAdapter.Viewholder holder, int position) {
        Exercise exercise = list.get(position);
        
        android.util.Log.d("UserWorkoutExerciseAdapter", "=== BINDING POSITION " + position + " ===");
        android.util.Log.d("UserWorkoutExerciseAdapter", "Exercise: " + exercise.getName() + " (ID: " + exercise.getId() + ")");
        android.util.Log.d("UserWorkoutExerciseAdapter", "List size: " + list.size());
        android.util.Log.d("UserWorkoutExerciseAdapter", "UserWorkout: " + (userWorkout != null ? userWorkout.getTitle() : "null"));
        
        holder.binding.titleTxt.setText(exercise.getName());
        
        // Get config from UserWorkout.UserWorkoutItem if available
        UserWorkout.UserWorkoutItem workoutItem = getUserWorkoutItemForExercise(exercise.getId());
        
        android.util.Log.d("UserWorkoutExerciseAdapter", "WorkoutItem found: " + (workoutItem != null ? "Yes" : "No"));
        if (workoutItem != null && workoutItem.getConfig() != null) {
            android.util.Log.d("UserWorkoutExerciseAdapter", "Config - Sets: " + workoutItem.getConfig().getSets() + ", Reps: " + workoutItem.getConfig().getReps());
        }
        
        // Initialize sets and reps - prioritize UserWorkout config over default config
        int initialSets = 3;
        int initialReps = 12;
        
        if (workoutItem != null && workoutItem.getConfig() != null) {
            // Use config from UserWorkout
            initialSets = workoutItem.getConfig().getSets();
            initialReps = workoutItem.getConfig().getReps();
            android.util.Log.d("UserWorkoutExerciseAdapter", "Using UserWorkout config: " + initialSets + " sets x " + initialReps + " reps");
        } else if (exercise.getDefaultConfig() != null) {
            // Fallback to default config from Exercise
            initialSets = exercise.getDefaultConfig().getSets();
            initialReps = exercise.getDefaultConfig().getReps();
            android.util.Log.d("UserWorkoutExerciseAdapter", "Using Exercise default config: " + initialSets + " sets x " + initialReps + " reps");
        } else {
            android.util.Log.d("UserWorkoutExerciseAdapter", "Using fallback config: " + initialSets + " sets x " + initialReps + " reps");
        }
        
        // Set initial values
        holder.binding.setsTxt.setText(String.valueOf(initialSets));
        holder.binding.repsTxt.setText(String.valueOf(initialReps));
        holder.binding.durationTxt.setText(initialSets + " sets x " + initialReps + " reps");
        
        // Store current values
        holder.currentSets = initialSets;
        holder.currentReps = initialReps;

        // Load thumbnail image
        if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
            // Check if it's a local drawable resource
            String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
            if (thumbnailUrl.startsWith("pic_")) {
                // It's a local drawable resource
                int resId = context.getResources().getIdentifier(thumbnailUrl, "drawable", context.getPackageName());
                Glide.with(context)
                        .load(resId)
                        .into(holder.binding.pic);
            } else {
                // It's a remote URL
                Glide.with(context)
                        .load(thumbnailUrl)
                        .into(holder.binding.pic);
            }
        } else {
            // Fallback to default image
            int resId = context.getResources().getIdentifier("pic_1_1", "drawable", context.getPackageName());
            Glide.with(context)
                    .load(resId)
                    .into(holder.binding.pic);
        }

        // Set click listeners
        holder.binding.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(context, ExerciseDetailActivity.class);
            intent.putExtra("exercise", exercise);
            context.startActivity(intent);
        });

        // Sets and reps change listeners
        holder.binding.setsMinusBtn.setOnClickListener(v -> {
            if (holder.currentSets > 1) {
                holder.currentSets--;
                holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.setsPlusBtn.setOnClickListener(v -> {
            if (holder.currentSets < 10) {
                holder.currentSets++;
                holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.repsMinusBtn.setOnClickListener(v -> {
            if (holder.currentReps > 1) {
                holder.currentReps--;
                holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.repsPlusBtn.setOnClickListener(v -> {
            if (holder.currentReps < 50) {
                holder.currentReps++;
                holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        // Set difficulty text
        final String difficulty = getDifficultyText(workoutItem, exercise);
        
        // Capitalize first letter
        final String displayDifficulty = difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1);
        holder.binding.difficultyBtn.setText(displayDifficulty);
        
        // Set difficulty click listener (optional - for future enhancement)
        holder.binding.difficultyBtn.setOnClickListener(v -> {
            // Could implement difficulty selection dialog here
            android.util.Log.d("UserWorkoutExerciseAdapter", "Difficulty clicked: " + displayDifficulty);
        });
    }

    @Override
    public int getItemCount() {
        android.util.Log.d("UserWorkoutExerciseAdapter", "getItemCount: " + list.size());
        return list.size();
    }
    
    @Override
    public void onViewAttachedToWindow(@NonNull Viewholder holder) {
        super.onViewAttachedToWindow(holder);
        android.util.Log.d("UserWorkoutExerciseAdapter", "View attached to window at position: " + holder.getAdapterPosition());
    }
    
    @Override
    public void onViewDetachedFromWindow(@NonNull Viewholder holder) {
        super.onViewDetachedFromWindow(holder);
        android.util.Log.d("UserWorkoutExerciseAdapter", "View detached from window at position: " + holder.getAdapterPosition());
    }

    /**
     * Get difficulty text from UserWorkout config or Exercise default config
     */
    private String getDifficultyText(UserWorkout.UserWorkoutItem workoutItem, Exercise exercise) {
        if (workoutItem != null && workoutItem.getConfig() != null && workoutItem.getConfig().getDifficulty() != null) {
            return workoutItem.getConfig().getDifficulty();
        } else if (exercise.getDefaultConfig() != null && exercise.getDefaultConfig().getDifficulty() != null) {
            return exercise.getDefaultConfig().getDifficulty();
        }
        return "beginner"; // default
    }

    /**
     * Get UserWorkoutItem for a specific exercise ID
     */
    private UserWorkout.UserWorkoutItem getUserWorkoutItemForExercise(String exerciseId) {
        if (userWorkout == null || userWorkout.getItems() == null) {
            return null;
        }
        
        for (UserWorkout.UserWorkoutItem item : userWorkout.getItems()) {
            if (exerciseId.equals(item.getExerciseId())) {
                return item;
            }
        }
        return null;
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderUserWorkoutExerciseBinding binding;
        int currentSets = 3;
        int currentReps = 12;

        public Viewholder(ViewholderUserWorkoutExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
