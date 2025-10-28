package fpt.fall2025.posetrainer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.R;

/**
 * EditWorkoutAdapter - Adapter cho RecyclerView trong EditWorkoutActivity
 * Hiển thị danh sách exercises với khả năng xóa và drag & drop
 */
public class EditWorkoutAdapter extends RecyclerView.Adapter<EditWorkoutAdapter.ExerciseViewHolder> {
    private ArrayList<Exercise> exercises;
    private OnExerciseRemovedListener listener;

    public EditWorkoutAdapter(ArrayList<Exercise> exercises, OnExerciseRemovedListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.bind(exercise, position);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    /**
     * Update exercises list
     */
    public void updateExercises(ArrayList<Exercise> newExercises) {
        this.exercises = newExercises;
        notifyDataSetChanged();
    }

    /**
     * Move exercise from one position to another
     */
    public void moveExercise(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }
        
        // Remove the item from the original position
        Exercise exercise = exercises.remove(fromPosition);
        
        // Insert it at the new position
        exercises.add(toPosition, exercise);
        
        // Notify the adapter about the move (smooth animation)
        notifyItemMoved(fromPosition, toPosition);
    }
    
    /**
     * Update order numbers after drag operation is complete
     */
    public void updateOrderNumbers() {
        notifyDataSetChanged();
    }

    /**
     * Get exercise at position
     */
    public Exercise getExerciseAt(int position) {
        return exercises.get(position);
    }

    /**
     * ViewHolder class
     */
    public class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private ImageView pic;
        private TextView titleTxt;
        private TextView durationTxt;
        private TextView difficultyBtn;
        private TextView setsTxt;
        private TextView repsTxt;
        private TextView orderNumberTxt;
        private ImageButton removeBtn;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            pic = itemView.findViewById(R.id.pic);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            durationTxt = itemView.findViewById(R.id.durationTxt);
            difficultyBtn = itemView.findViewById(R.id.difficultyBtn);
            setsTxt = itemView.findViewById(R.id.setsTxt);
            repsTxt = itemView.findViewById(R.id.repsTxt);
            orderNumberTxt = itemView.findViewById(R.id.orderNumberTxt);
            removeBtn = itemView.findViewById(R.id.removeBtn);
        }

        public void bind(Exercise exercise, int position) {
            // Set exercise name (no duplicate counting)
            titleTxt.setText(exercise.getName());
            
            // Set level/difficulty
            difficultyBtn.setText(exercise.getLevel());
            
            // Set sets and reps separately
            if (exercise.getDefaultConfig() != null) {
                setsTxt.setText(String.valueOf(exercise.getDefaultConfig().getSets()));
                repsTxt.setText(String.valueOf(exercise.getDefaultConfig().getReps()));
            } else {
                setsTxt.setText("3");
                repsTxt.setText("12");
            }

            // Set rest duration
            if (exercise.getDefaultConfig() != null && exercise.getDefaultConfig().getRestSec() > 0) {
                int restSec = exercise.getDefaultConfig().getRestSec();
                if (restSec >= 60) {
                    int minutes = restSec / 60;
                    int seconds = restSec % 60;
                    if (seconds > 0) {
                        durationTxt.setText(minutes + "m " + seconds + "s rest");
                    } else {
                        durationTxt.setText(minutes + "m rest");
                    }
                } else {
                    durationTxt.setText(restSec + "s rest");
                }
            } else {
                durationTxt.setText("30s rest");
            }
            
            // Set order number
            orderNumberTxt.setText(String.valueOf(position + 1));
            
            // Load exercise image
            loadExerciseImage(exercise);
            
            // Remove button click listener
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onExerciseRemoved(position);
                    }
                }
            });
            
            // Make the entire item draggable by setting the order number as drag handle
            orderNumberTxt.setTag("drag_handle");
        }

        /**
         * Load exercise image using Glide
         */
        private void loadExerciseImage(Exercise exercise) {
            if (exercise.getMedia() != null) {
                // Try to load thumbnail first
                if (exercise.getMedia().getThumbnailUrl() != null && !exercise.getMedia().getThumbnailUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(exercise.getMedia().getThumbnailUrl())
                            .placeholder(R.drawable.ic_favorite_border)
                            .error(R.drawable.ic_favorite_border)
                            .into(pic);
                } else {
                    // No thumbnail available, use default icon
                    pic.setImageResource(R.drawable.ic_favorite_border);
                }
            } else {
                // No media object, use default icon
                pic.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }

    /**
     * Interface for exercise removal callback
     */
    public interface OnExerciseRemovedListener {
        void onExerciseRemoved(int position);
    }

    /**
     * Interface for exercise reorder callback
     */
    public interface OnExerciseReorderListener {
        void onExerciseMoved(int fromPosition, int toPosition);
    }
}

