package fpt.fall2025.posetrainer.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Activity.ExerciseDetailActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Helper.GlideImageLoader;
import fpt.fall2025.posetrainer.R;

/**
 * EditWorkoutAdapter - Adapter cho RecyclerView trong EditWorkoutActivity
 * Hiển thị danh sách exercises với khả năng xóa và drag & drop
 */
public class EditWorkoutAdapter extends RecyclerView.Adapter<EditWorkoutAdapter.ExerciseViewHolder> {
    private ArrayList<Exercise> exercises;
    private OnExerciseRemovedListener listener;
    private OnExerciseReorderListener reorderListener;

    public EditWorkoutAdapter(ArrayList<Exercise> exercises, OnExerciseRemovedListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }
    
    public void setOnExerciseReorderListener(OnExerciseReorderListener listener) {
        this.reorderListener = listener;
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
        // Notify all items changed to update order numbers
        notifyItemRangeChanged(0, exercises.size());
        // Notify listener that exercises have been reordered
        if (reorderListener != null) {
            reorderListener.onExercisesReordered();
        }
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
        private ImageButton setsPlusBtn;
        private ImageButton setsMinusBtn;
        private ImageButton repsPlusBtn;
        private ImageButton repsMinusBtn;
        private Exercise currentExercise;
        private int currentPosition;

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
            setsPlusBtn = itemView.findViewById(R.id.setsPlusBtn);
            setsMinusBtn = itemView.findViewById(R.id.setsMinusBtn);
            repsPlusBtn = itemView.findViewById(R.id.repsPlusBtn);
            repsMinusBtn = itemView.findViewById(R.id.repsMinusBtn);
        }

        public void bind(Exercise exercise, int position) {
            this.currentExercise = exercise;
            this.currentPosition = position;
            
            // Ensure DefaultConfig exists
            if (exercise.getDefaultConfig() == null) {
                exercise.setDefaultConfig(new Exercise.DefaultConfig(3, 12, 30, "Beginner"));
            }
            
            // Set exercise name
            titleTxt.setText(exercise.getName());
            
            // Set level/difficulty - convert to Vietnamese
            String levelText = convertLevelToVietnamese(exercise.getLevel());
            difficultyBtn.setText(levelText);
            
            // Set sets and reps
            int currentSets = exercise.getDefaultConfig().getSets();
            int currentReps = exercise.getDefaultConfig().getReps();
            if (currentSets <= 0) currentSets = 3;
            if (currentReps <= 0) currentReps = 12;
            
            setsTxt.setText(String.valueOf(currentSets));
            repsTxt.setText(String.valueOf(currentReps));

            // Set rest duration
            int restSec = exercise.getDefaultConfig().getRestSec();
            if (restSec <= 0) restSec = 30;
            
            if (restSec >= 60) {
                int minutes = restSec / 60;
                int seconds = restSec % 60;
                durationTxt.setText(seconds > 0 ? minutes + "m " + seconds + "s rest" : minutes + "m rest");
            } else {
                durationTxt.setText(restSec + "s rest");
            }
            
            // Set order number
            orderNumberTxt.setText(String.valueOf(position + 1));
            
            // Load exercise image
            loadExerciseImage(exercise);
            
            // Setup click listeners for buttons first
            setupClickListeners();
            
            // Set click listener for item to show exercise detail
            // Use a flag to track if a button was clicked
            itemView.setOnClickListener(v -> {
                // Check if any button was recently clicked (within same touch event)
                // This is handled by Android's event system - buttons will consume the click
                if (currentExercise != null) {
                    ExerciseDetailActivity.show(itemView.getContext(), currentExercise);
                }
            });
            
            // Make the entire item draggable
            orderNumberTxt.setTag("drag_handle");
        }
        
        /**
         * Setup click listeners for buttons
         */
        private void setupClickListeners() {
            // Remove button - stop event propagation
            removeBtn.setOnClickListener(v -> {
                v.setTag("button_clicked");
                if (listener != null) {
                    listener.onExerciseRemoved(currentPosition);
                }
            });
            
            // Sets controls - stop event propagation
            setsPlusBtn.setOnClickListener(v -> {
                v.setTag("button_clicked");
                updateSets(1);
            });
            setsMinusBtn.setOnClickListener(v -> {
                v.setTag("button_clicked");
                updateSets(-1);
            });
            
            // Reps controls - stop event propagation
            repsPlusBtn.setOnClickListener(v -> {
                v.setTag("button_clicked");
                updateReps(1);
            });
            repsMinusBtn.setOnClickListener(v -> {
                v.setTag("button_clicked");
                updateReps(-1);
            });
        }
        
        /**
         * Update sets value
         */
        private void updateSets(int delta) {
            if (currentExercise == null || currentExercise.getDefaultConfig() == null) return;
            
            int currentSets = currentExercise.getDefaultConfig().getSets();
            if (currentSets <= 0) currentSets = 3;
            
            int newSets = currentSets + delta;
            if (newSets < 1) newSets = 1; // Minimum 1 set
            if (newSets > 10) newSets = 10; // Maximum 10 sets
            
            currentExercise.getDefaultConfig().setSets(newSets);
            setsTxt.setText(String.valueOf(newSets));
            
            // Notify listener to update duration
            if (reorderListener != null) {
                reorderListener.onExercisesReordered();
            }
        }
        
        /**
         * Update reps value
         */
        private void updateReps(int delta) {
            if (currentExercise == null || currentExercise.getDefaultConfig() == null) return;
            
            int currentReps = currentExercise.getDefaultConfig().getReps();
            if (currentReps <= 0) currentReps = 12;
            
            int newReps = currentReps + delta;
            if (newReps < 1) newReps = 1; // Minimum 1 rep
            if (newReps > 50) newReps = 50; // Maximum 50 reps
            
            currentExercise.getDefaultConfig().setReps(newReps);
            repsTxt.setText(String.valueOf(newReps));
            
            // Notify listener to update duration
            if (reorderListener != null) {
                reorderListener.onExercisesReordered();
            }
        }

        /**
         * Load exercise image using GlideImageLoader - hỗ trợ tất cả các loại URL
         */
        private void loadExerciseImage(Exercise exercise) {
            if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
                String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                // GlideImageLoader tự động xử lý: Google Drive, Google Image Search, direct URLs, local drawables
                GlideImageLoader.loadImage(itemView.getContext(), thumbnailUrl, pic);
            } else {
                // Fallback to default image
                int resId = itemView.getContext().getResources().getIdentifier("pic_1_1", "drawable", itemView.getContext().getPackageName());
                Glide.with(itemView.getContext())
                        .load(resId)
                        .into(pic);
            }
        }
    }

    /**
     * Convert English level to Vietnamese for display
     */
    private String convertLevelToVietnamese(String englishLevel) {
        if (englishLevel == null || englishLevel.isEmpty()) {
            return "Người mới bắt đầu";
        }

        String lowerLevel = englishLevel.toLowerCase();
        if (lowerLevel.contains("beginner") || lowerLevel.contains("mới")) {
            return "Người mới bắt đầu";
        } else if (lowerLevel.contains("intermediate") || lowerLevel.contains("trung")) {
            return "Trung bình";
        } else if (lowerLevel.contains("advanced") || lowerLevel.contains("nâng") || lowerLevel.contains("pro")) {
            return "Nâng cao";
        }

        return "Người mới bắt đầu"; // Default
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
        void onExercisesReordered();
    }
}

