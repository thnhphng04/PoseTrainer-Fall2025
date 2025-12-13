package fpt.fall2025.posetrainer.UI.adapter.exercise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Util.GlideImageLoader;
import fpt.fall2025.posetrainer.R;

/**
 * CustomExerciseAdapter - Adapter cho RecyclerView trong CustomExercisesActivity
 * Hiển thị danh sách custom exercises của user
 */
public class CustomExerciseAdapter extends RecyclerView.Adapter<CustomExerciseAdapter.ExerciseViewHolder> {
    private ArrayList<Exercise> exercises;
    private OnExerciseClickListener listener;

    public CustomExerciseAdapter(ArrayList<Exercise> exercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_selection, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
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
        private TextView categoryTxt;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            pic = itemView.findViewById(R.id.pic);
            titleTxt = itemView.findViewById(R.id.titleTxt);
            durationTxt = itemView.findViewById(R.id.durationTxt);
            difficultyBtn = itemView.findViewById(R.id.difficultyBtn);
            setsTxt = itemView.findViewById(R.id.setsTxt);
            repsTxt = itemView.findViewById(R.id.repsTxt);
            categoryTxt = itemView.findViewById(R.id.categoryTxt);
        }

        public void bind(Exercise exercise) {
            // Set exercise name
            titleTxt.setText(exercise.getName());
            
            // Set level/difficulty
            if (exercise.getLevel() != null) {
                difficultyBtn.setText(exercise.getLevel());
            } else {
                difficultyBtn.setText("beginner");
            }
            
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
            
            // Set category
            if (exercise.getCategory() != null && !exercise.getCategory().isEmpty()) {
                StringBuilder categoryText = new StringBuilder();
                for (int i = 0; i < exercise.getCategory().size(); i++) {
                    if (i > 0) categoryText.append(", ");
                    categoryText.append(exercise.getCategory().get(i));
                }
                categoryTxt.setText(categoryText.toString());
            } else {
                categoryTxt.setText("Custom");
            }

            // Load exercise image
            loadExerciseImage(exercise);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExerciseClick(exercise);
                }
            });
        }

        /**
         * Load exercise image using GlideImageLoader
         */
        private void loadExerciseImage(Exercise exercise) {
            if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
                String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
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
     * Interface for exercise click callback
     */
    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }
}


