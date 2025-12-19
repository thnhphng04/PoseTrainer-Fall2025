package fpt.fall2025.posetrainer.UI.adapter.workout;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.UI.activity.ExerciseActivity;
import fpt.fall2025.posetrainer.UI.activity.ExerciseDetailActivity;
import fpt.fall2025.posetrainer.UI.dialog.ExerciseDetailDialog;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Util.GlideImageLoader;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.ViewholderUserWorkoutExerciseBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserWorkoutExerciseAdapter extends RecyclerView.Adapter<UserWorkoutExerciseAdapter.Viewholder> {

    // Level mapping (English -> Vietnamese)
    private static final Map<String, String> LEVEL_MAP = new HashMap<String, String>() {{
        put("beginner", "Dễ");
        put("intermediate", "Trung bình");
        put("pro", "Khó");
        put("advanced", "Khó");
    }};

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
            int itemsCount = (userWorkout.getItems() != null) ? userWorkout.getItems().size() : 0;
            android.util.Log.d("UserWorkoutExerciseAdapter", "UserWorkout items: " + itemsCount);
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

        // Load thumbnail image - sử dụng GlideImageLoader để hỗ trợ tất cả các loại URL
        if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
            String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
            // GlideImageLoader tự động xử lý: Google Drive, Google Image Search, direct URLs, local drawables
            GlideImageLoader.loadImage(context, thumbnailUrl, holder.binding.pic);
        } else {
            // Fallback to default image
            int resId = context.getResources().getIdentifier("pic_1_1", "drawable", context.getPackageName());
            Glide.with(context)
                    .load(resId)
                    .into(holder.binding.pic);
        }

        // Set click listeners
        holder.binding.getRoot().setOnClickListener(v -> {
            // Get custom config from UserWorkout if available
            UserWorkout.UserWorkoutItem workoutItemForClick = getUserWorkoutItemForExercise(exercise.getId());
            
            if (workoutItemForClick != null && workoutItemForClick.getConfig() != null) {
                // Use custom config from UserWorkout
                ExerciseDetailDialog.show(context, exercise, 
                    workoutItemForClick.getConfig().getSets(), 
                    workoutItemForClick.getConfig().getReps(), 
                    workoutItemForClick.getConfig().getDifficulty());
            } else {
                // Use default config
                ExerciseDetailDialog.show(context, exercise);
            }
        });

        // Sets and reps change listeners
        holder.binding.setsMinusBtn.setOnClickListener(v -> {
            if (holder.currentSets > 1) {
                holder.currentSets--;
                holder.isUpdatingFromWatcher = true;
                holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
                holder.binding.setsTxt.setSelection(holder.binding.setsTxt.getText().length());
                holder.isUpdatingFromWatcher = false;
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.setsPlusBtn.setOnClickListener(v -> {
            if (holder.currentSets < 10) {
                holder.currentSets++;
                holder.isUpdatingFromWatcher = true;
                holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
                holder.binding.setsTxt.setSelection(holder.binding.setsTxt.getText().length());
                holder.isUpdatingFromWatcher = false;
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.repsMinusBtn.setOnClickListener(v -> {
            if (holder.currentReps > 1) {
                holder.currentReps--;
                holder.isUpdatingFromWatcher = true;
                holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
                holder.binding.repsTxt.setSelection(holder.binding.repsTxt.getText().length());
                holder.isUpdatingFromWatcher = false;
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });

        holder.binding.repsPlusBtn.setOnClickListener(v -> {
            if (holder.currentReps < 50) {
                holder.currentReps++;
                holder.isUpdatingFromWatcher = true;
                holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
                holder.binding.repsTxt.setSelection(holder.binding.repsTxt.getText().length());
                holder.isUpdatingFromWatcher = false;
                holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                if (listener != null) {
                    listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                }
            }
        });
        
        // Setup TextWatchers for EditText fields
        setupTextWatchers(holder, position);

        // Set difficulty text in Vietnamese
        final String difficulty = getDifficultyText(workoutItem, exercise);
        final String vietnameseLevel = LEVEL_MAP.get(difficulty.toLowerCase());
        final String displayDifficulty = vietnameseLevel != null ? vietnameseLevel : "Trung bình";
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

    /**
     * Setup TextWatchers for EditText fields to handle keyboard input
     */
    private void setupTextWatchers(Viewholder holder, int position) {
        // Remove existing watchers to avoid duplicates
        if (holder.setsTextWatcher != null) {
            holder.binding.setsTxt.removeTextChangedListener(holder.setsTextWatcher);
        }
        if (holder.repsTextWatcher != null) {
            holder.binding.repsTxt.removeTextChangedListener(holder.repsTextWatcher);
        }
        
        // Sets TextWatcher
        holder.setsTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.isUpdatingFromWatcher) {
                    return;
                }
                
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    return;
                }
                
                try {
                    int value = Integer.parseInt(text);
                    // Validate range: 1-10 for sets
                    if (value < 1) {
                        value = 1;
                    } else if (value > 10) {
                        value = 10;
                    }
                    
                    // Update if different
                    if (value != holder.currentSets) {
                        holder.isUpdatingFromWatcher = true;
                        holder.currentSets = value;
                        holder.binding.setsTxt.setText(String.valueOf(value));
                        holder.binding.setsTxt.setSelection(holder.binding.setsTxt.getText().length());
                        holder.isUpdatingFromWatcher = false;
                        holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                        
                        if (listener != null) {
                            listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, reset to current value
                    holder.isUpdatingFromWatcher = true;
                    holder.binding.setsTxt.setText(String.valueOf(holder.currentSets));
                    holder.binding.setsTxt.setSelection(holder.binding.setsTxt.getText().length());
                    holder.isUpdatingFromWatcher = false;
                }
            }
        };
        holder.binding.setsTxt.addTextChangedListener(holder.setsTextWatcher);
        
        // Reps TextWatcher
        holder.repsTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.isUpdatingFromWatcher) {
                    return;
                }
                
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    return;
                }
                
                try {
                    int value = Integer.parseInt(text);
                    // Validate range: 1-50 for reps
                    if (value < 1) {
                        value = 1;
                    } else if (value > 50) {
                        value = 50;
                    }
                    
                    // Update if different
                    if (value != holder.currentReps) {
                        holder.isUpdatingFromWatcher = true;
                        holder.currentReps = value;
                        holder.binding.repsTxt.setText(String.valueOf(value));
                        holder.binding.repsTxt.setSelection(holder.binding.repsTxt.getText().length());
                        holder.isUpdatingFromWatcher = false;
                        holder.binding.durationTxt.setText(holder.currentSets + " sets x " + holder.currentReps + " reps");
                        
                        if (listener != null) {
                            listener.onSetsRepsChanged(position, holder.currentSets, holder.currentReps);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, reset to current value
                    holder.isUpdatingFromWatcher = true;
                    holder.binding.repsTxt.setText(String.valueOf(holder.currentReps));
                    holder.binding.repsTxt.setSelection(holder.binding.repsTxt.getText().length());
                    holder.isUpdatingFromWatcher = false;
                }
            }
        };
        holder.binding.repsTxt.addTextChangedListener(holder.repsTextWatcher);
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderUserWorkoutExerciseBinding binding;
        int currentSets = 3;
        int currentReps = 12;
        boolean isUpdatingFromWatcher = false;
        TextWatcher setsTextWatcher;
        TextWatcher repsTextWatcher;

        public Viewholder(ViewholderUserWorkoutExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
