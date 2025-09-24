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
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.ViewholderExerciseBinding;

import java.util.ArrayList;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.Viewholder> {

    private final ArrayList<Exercise> list;
    private final WorkoutTemplate workoutTemplate;
    private Context context;
    private OnSetsRepsChangedListener listener;
    private OnDifficultyChangedListener difficultyListener;

    public ExerciseAdapter(ArrayList<Exercise> list, WorkoutTemplate workoutTemplate) {
        this.list = list;
        this.workoutTemplate = workoutTemplate;
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

    @NonNull
    @Override
    public ExerciseAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderExerciseBinding binding = ViewholderExerciseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseAdapter.Viewholder holder, int position) {
        Exercise exercise = list.get(position);
        
        holder.binding.titleTxt.setText(exercise.getName());
        
        // Initialize sets and reps from default config
        int defaultSets = 3;
        int defaultReps = 12;
        if (exercise.getDefaultConfig() != null) {
            defaultSets = exercise.getDefaultConfig().getSets();
            defaultReps = exercise.getDefaultConfig().getReps();
        }
        
        // Set initial values
        holder.binding.setsTxt.setText(String.valueOf(defaultSets));
        holder.binding.repsTxt.setText(String.valueOf(defaultReps));
        holder.binding.durationTxt.setText(defaultSets + " sets x " + defaultReps + " reps");
        
        // Store current values
        holder.currentSets = defaultSets;
        holder.currentReps = defaultReps;

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

        // Set up sets and reps controls
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
            holder.currentSets++;
            holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
            holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
            if (listener != null) {
                listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
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
            holder.currentReps++;
            holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
            holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
            if (listener != null) {
                listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
            }
        });

        // Difficulty button
        holder.binding.difficultyBtn.setOnClickListener(v -> {
            // Toggle between beginner and pro
            if (holder.currentDifficulty.equals("beginner")) {
                holder.currentDifficulty = "pro";
                holder.binding.difficultyBtn.setText("Pro");
                holder.binding.difficultyBtn.setBackgroundResource(R.drawable.blue_bg);
            } else {
                holder.currentDifficulty = "beginner";
                holder.binding.difficultyBtn.setText("Beginner");
                holder.binding.difficultyBtn.setBackgroundResource(R.drawable.orange_bg);
            }
            
            if (difficultyListener != null) {
                difficultyListener.onDifficultyChanged(position, holder.currentDifficulty);
            }
        });

        // Remove individual start button - exercises will be started via "Start Workout" button
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderExerciseBinding binding;
        int currentSets = 3;
        int currentReps = 12;
        String currentDifficulty = "beginner";

        public Viewholder(ViewholderExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
