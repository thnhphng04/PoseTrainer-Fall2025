package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;

public class SessionExerciseAdapter extends RecyclerView.Adapter<SessionExerciseAdapter.SessionExerciseViewHolder> {

    private Context context;
    private List<Session.PerExercise> perExercises;
    private List<Exercise> exercises;
    private Session currentSession;
    private WorkoutTemplate workoutTemplate;
    private OnExerciseClickListener onExerciseClickListener;

    public interface OnExerciseClickListener {
        void onExerciseClick(Session.PerExercise perExercise, Exercise exercise);
    }

    public SessionExerciseAdapter(Context context, List<Session.PerExercise> perExercises, List<Exercise> exercises, Session currentSession, WorkoutTemplate workoutTemplate) {
        this.context = context;
        this.perExercises = perExercises;
        this.exercises = exercises;
        this.currentSession = currentSession;
        this.workoutTemplate = workoutTemplate;
    }

    public void setOnExerciseClickListener(OnExerciseClickListener listener) {
        this.onExerciseClickListener = listener;
    }

    public void updateSession(Session session) {
        android.util.Log.d("SessionExerciseAdapter", "updateSession called with " + 
            (session != null ? session.getPerExercise().size() + " exercises" : "null session"));
        
        this.currentSession = session;
        this.perExercises = session.getPerExercise();
        notifyDataSetChanged();
        
        android.util.Log.d("SessionExerciseAdapter", "Adapter updated, notifying data set changed");
    }


    @NonNull
    @Override
    public SessionExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_session_exercise, parent, false);
        return new SessionExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionExerciseViewHolder holder, int position) {
        Session.PerExercise perExercise = perExercises.get(position);
        Exercise exercise = getExerciseByOrder(perExercise.getExerciseNo());

        android.util.Log.d("SessionExerciseAdapter", "=== BINDING POSITION " + position + " ===");
        android.util.Log.d("SessionExerciseAdapter", "PerExercise - ExerciseNo: " + perExercise.getExerciseNo() + 
            ", ExerciseId: " + perExercise.getExerciseId() + 
            ", State: " + perExercise.getState());
        android.util.Log.d("SessionExerciseAdapter", "Exercise found: " + (exercise != null ? exercise.getName() : "null"));

        if (exercise == null) {
            android.util.Log.w("SessionExerciseAdapter", "Exercise is null for ID: " + perExercise.getExerciseId());
            return;
        }

        // Set exercise image
        if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
            Glide.with(context)
                    .load(exercise.getMedia().getThumbnailUrl())
                    .placeholder(R.drawable.pic_1_1)
                    .into(holder.exerciseImage);
        } else {
            holder.exerciseImage.setImageResource(R.drawable.pic_1_1);
        }

        // Set exercise info
        holder.exerciseNameTxt.setText(exercise.getName());
        
        // Calculate sets and reps from perExercise
        int totalSets = perExercise.getSets() != null ? perExercise.getSets().size() : 0;
        int targetReps = 0;
        if (perExercise.getSets() != null && !perExercise.getSets().isEmpty()) {
            targetReps = perExercise.getSets().get(0).getTargetReps();
        }
        
        // Compare with default config
        int defaultSets = 3;
        int defaultReps = 12;
        if (exercise.getDefaultConfig() != null) {
            defaultSets = exercise.getDefaultConfig().getSets();
            defaultReps = exercise.getDefaultConfig().getReps();
        }
        
        android.util.Log.d("SessionExerciseAdapter", "Exercise: " + exercise.getName() + 
            " - Sets from perExercise: " + totalSets + ", Reps: " + targetReps +
            " | Default config: " + defaultSets + " sets x " + defaultReps + " reps");
        
        holder.exerciseSetsRepsTxt.setText(totalSets + " sets x " + targetReps + " reps");

        // Calculate completed sets (including both "completed" and "skipped")
        int completedSets = 0;
        int skippedSets = 0;
        int incompleteSets = 0;
        
        if (perExercise.getSets() != null) {
            for (int i = 0; i < perExercise.getSets().size(); i++) {
                Session.SetData setData = perExercise.getSets().get(i);
                String setState = setData.getState();
                
                android.util.Log.d("SessionExerciseAdapter", "Set " + i + " state: " + setState);
                
                if ("completed".equals(setState)) {
                    completedSets++;
                } else if ("skipped".equals(setState)) {
                    skippedSets++;
                } else if ("incomplete".equals(setState)) {
                    incompleteSets++;
                }
            }
        }

        android.util.Log.d("SessionExerciseAdapter", "Sets count - Completed: " + completedSets + 
            ", Skipped: " + skippedSets + ", Incomplete: " + incompleteSets);

        // Display completed sets
        holder.exerciseProgressTxt.setText(completedSets + "/" + totalSets + " sets completed");
        
        // Display skipped sets (only show if there are skipped sets)
        if (skippedSets > 0) {
            holder.exerciseSkippedTxt.setText(skippedSets + " skipped");
            holder.exerciseSkippedTxt.setVisibility(View.VISIBLE);
        } else {
            holder.exerciseSkippedTxt.setVisibility(View.GONE);
        }

        // Set exercise state
        String state = perExercise.getState();
        holder.exerciseStateTxt.setText(getStateDisplayText(state));
        
        // Set state indicator color
        int stateColor = getStateColor(state);
        holder.stateIndicator.setBackgroundColor(stateColor);

        // Handle check mark visibility
        android.util.Log.d("SessionExerciseAdapter", "Position " + position + " - State: " + state + 
            " - Setting check mark visibility");
            
        if ("completed".equals(state)) {
            holder.checkMarkContainer.setVisibility(View.VISIBLE);
            holder.startExerciseBtn.setVisibility(View.GONE);
            holder.resumeExerciseBtn.setVisibility(View.GONE);
            android.util.Log.d("SessionExerciseAdapter", "Position " + position + " - Exercise completed, showing check mark");
        } else {
            holder.checkMarkContainer.setVisibility(View.GONE);
            holder.startExerciseBtn.setVisibility(View.GONE);
            holder.resumeExerciseBtn.setVisibility(View.GONE);
            android.util.Log.d("SessionExerciseAdapter", "Position " + position + " - Individual buttons hidden (using single start/resume button)");
        }

        // No individual click listeners - handled by single button in SessionActivity
    }

    @Override
    public int getItemCount() {
        return perExercises != null ? perExercises.size() : 0;
    }

    private Exercise getExerciseByOrder(int exerciseOrder) {
        if (exercises == null || workoutTemplate == null || workoutTemplate.getItems() == null) {
            android.util.Log.w("SessionExerciseAdapter", "Cannot find exercise: exercises=" + 
                (exercises != null ? exercises.size() : "null") + 
                ", workoutTemplate=" + (workoutTemplate != null ? "not null" : "null"));
            return null;
        }
        
        android.util.Log.d("SessionExerciseAdapter", "Looking for exercise with order: " + exerciseOrder);
        android.util.Log.d("SessionExerciseAdapter", "Available exercises: " + exercises.size());
        android.util.Log.d("SessionExerciseAdapter", "Available workout items: " + workoutTemplate.getItems().size());
        
        // Find WorkoutItem with matching order
        WorkoutTemplate.WorkoutItem workoutItem = null;
        for (WorkoutTemplate.WorkoutItem item : workoutTemplate.getItems()) {
            android.util.Log.d("SessionExerciseAdapter", "Checking item order: " + item.getOrder() + " vs " + exerciseOrder);
            if (item.getOrder() == exerciseOrder) {
                workoutItem = item;
                android.util.Log.d("SessionExerciseAdapter", "Found workout item: " + item.getExerciseId());
                break;
            }
        }
        
        if (workoutItem == null) {
            android.util.Log.w("SessionExerciseAdapter", "No workout item found for order: " + exerciseOrder);
            return null;
        }
        
        // Find Exercise with matching ID
        for (Exercise exercise : exercises) {
            android.util.Log.d("SessionExerciseAdapter", "Checking exercise ID: " + exercise.getId() + " vs " + workoutItem.getExerciseId());
            if (exercise.getId().equals(workoutItem.getExerciseId())) {
                android.util.Log.d("SessionExerciseAdapter", "Found exercise: " + exercise.getName());
                return exercise;
            }
        }
        
        android.util.Log.w("SessionExerciseAdapter", "No exercise found for ID: " + workoutItem.getExerciseId());
        return null;
    }

    private String getStateDisplayText(String state) {
        if (state == null) return "Not Started";
        
        switch (state) {
            case "not_started":
                return "Not Started";
            case "doing":
                return "In Progress";
            case "completed":
                return "Completed";
            case "skipped":
                return "Skipped";
            default:
                return "Not Started";
        }
    }

    private int getStateColor(String state) {
        if (state == null) return context.getResources().getColor(R.color.gray);
        
        switch (state) {
            case "not_started":
                return context.getResources().getColor(R.color.gray);
            case "doing":
                return context.getResources().getColor(R.color.orange);
            case "completed":
                return context.getResources().getColor(R.color.green);
            case "skipped":
                return context.getResources().getColor(R.color.red);
            default:
                return context.getResources().getColor(R.color.gray);
        }
    }

    public static class SessionExerciseViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView exerciseImage;
        TextView exerciseNameTxt;
        TextView exerciseSetsRepsTxt;
        TextView exerciseProgressTxt;
        TextView exerciseSkippedTxt;
        View stateIndicator;
        TextView exerciseStateTxt;
        LinearLayout checkMarkContainer;
        ImageView checkMarkImageView;
        Button startExerciseBtn;
        Button resumeExerciseBtn;

        public SessionExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            
            exerciseImage = itemView.findViewById(R.id.exerciseImage);
            exerciseNameTxt = itemView.findViewById(R.id.exerciseNameTxt);
            exerciseSetsRepsTxt = itemView.findViewById(R.id.exerciseSetsRepsTxt);
            exerciseProgressTxt = itemView.findViewById(R.id.exerciseProgressTxt);
            exerciseSkippedTxt = itemView.findViewById(R.id.exerciseSkippedTxt);
            stateIndicator = itemView.findViewById(R.id.stateIndicator);
            exerciseStateTxt = itemView.findViewById(R.id.exerciseStateTxt);
            checkMarkContainer = itemView.findViewById(R.id.checkMarkContainer);
            checkMarkImageView = itemView.findViewById(R.id.checkMarkImageView);
            startExerciseBtn = itemView.findViewById(R.id.startExerciseBtn);
            resumeExerciseBtn = itemView.findViewById(R.id.resumeExerciseBtn);
        }
    }
}
