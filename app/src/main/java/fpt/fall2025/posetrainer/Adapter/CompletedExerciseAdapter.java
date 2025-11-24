package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Helper.GlideImageLoader;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter để hiển thị danh sách exercises đã hoàn thành trong CompletedExerciseActivity
 */
public class CompletedExerciseAdapter extends RecyclerView.Adapter<CompletedExerciseAdapter.CompletedExerciseViewHolder> {
    private Context context;
    private List<Session.PerExercise> perExercises;
    private List<Exercise> exercises;

    public CompletedExerciseAdapter(Context context, List<Session.PerExercise> perExercises, List<Exercise> exercises) {
        this.context = context;
        this.perExercises = perExercises != null ? perExercises : new ArrayList<>();
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }

    /**
     * Cập nhật danh sách exercises
     */
    public void updateExercises(List<Session.PerExercise> newPerExercises, List<Exercise> newExercises) {
        this.perExercises = newPerExercises != null ? newPerExercises : new ArrayList<>();
        this.exercises = newExercises != null ? newExercises : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CompletedExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_completed_exercise, parent, false);
        return new CompletedExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompletedExerciseViewHolder holder, int position) {
        Session.PerExercise perExercise = perExercises.get(position);
        Exercise exercise = getExerciseById(perExercise.getExerciseId());

        if (exercise == null) {
            // Không tìm thấy exercise, hiển thị thông tin từ perExercise
            holder.exerciseNameTxt.setText("Bài tập #" + perExercise.getExerciseNo());
            // Load default icon với tint (giữ nguyên tint từ XML)
            holder.exerciseThumbnail.setImageResource(R.drawable.ic_core);
            holder.exerciseLevelTxt.setText("Cấp độ");
            holder.exerciseDurationTxt.setText("");
        } else {
            // Hiển thị tên exercise
            holder.exerciseNameTxt.setText(exercise.getName());

            // Hiển thị ảnh exercise
            if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
                String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                // Clear tint trước khi load ảnh thực tế để ảnh hiển thị đúng màu
                holder.exerciseThumbnail.clearColorFilter();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    holder.exerciseThumbnail.setImageTintList(null);
                }
                // Load ảnh thực tế từ URL
                GlideImageLoader.loadImage(context, thumbnailUrl, holder.exerciseThumbnail, R.drawable.pic_1_1, R.drawable.pic_1_1);
            } else {
                // Load default icon với tint (giữ nguyên tint từ XML)
                holder.exerciseThumbnail.setImageResource(R.drawable.ic_core);
            }

            // Hiển thị cấp độ
            String difficulty = perExercise.getDifficultyUsed();
            if (difficulty == null && exercise.getDefaultConfig() != null) {
                difficulty = exercise.getDefaultConfig().getDifficulty();
            }
            String levelText = convertLevelToVietnamese(difficulty);
            holder.exerciseLevelTxt.setText(levelText);

            // Tính toán và hiển thị thời gian ước tính cho bài tập
            int estimatedMinutes = calculateExerciseDuration(perExercise, exercise);
            if (estimatedMinutes > 0) {
                holder.exerciseDurationTxt.setText("⏱️ " + estimatedMinutes + " min");
            } else {
                holder.exerciseDurationTxt.setText("");
            }
        }
    }


    /**
     * Tính toán thời gian ước tính cho bài tập (tính bằng phút)
     */
    private int calculateExerciseDuration(Session.PerExercise perExercise, Exercise exercise) {
        int sets = 0;
        int reps = 0;
        int restSec = 0;

        if (perExercise.getSets() != null && !perExercise.getSets().isEmpty()) {
            sets = perExercise.getSets().size();
            reps = perExercise.getSets().get(0).getTargetReps();
        } else if (exercise != null && exercise.getDefaultConfig() != null) {
            sets = exercise.getDefaultConfig().getSets();
            reps = exercise.getDefaultConfig().getReps();
            restSec = exercise.getDefaultConfig().getRestSec();
        }

        if (sets == 0 || reps == 0) {
            return 0;
        }

        // Ước tính: 2 giây mỗi rep, cộng thêm thời gian nghỉ giữa các set
        final int TIME_PER_REP_SECONDS = 2;
        int exerciseTime = sets * reps * TIME_PER_REP_SECONDS;
        int restTime = (sets - 1) * (restSec > 0 ? restSec : 30); // Default 30s rest
        int totalSeconds = exerciseTime + restTime;

        // Convert to minutes (round up)
        return (int) Math.ceil(totalSeconds / 60.0);
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
     * Lấy Exercise theo ID
     */
    private Exercise getExerciseById(String exerciseId) {
        if (exerciseId == null || exercises == null) {
            return null;
        }

        for (Exercise exercise : exercises) {
            if (exerciseId.equals(exercise.getId())) {
                return exercise;
            }
        }

        return null;
    }

    @Override
    public int getItemCount() {
        return perExercises.size();
    }

    /**
     * ViewHolder cho CompletedExerciseAdapter
     */
    public static class CompletedExerciseViewHolder extends RecyclerView.ViewHolder {
        ImageView exerciseThumbnail;
        TextView exerciseNameTxt;
        TextView exerciseLevelTxt;
        TextView exerciseDurationTxt;

        public CompletedExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseThumbnail = itemView.findViewById(R.id.iv_exercise_thumbnail);
            exerciseNameTxt = itemView.findViewById(R.id.tv_exercise_name);
            exerciseLevelTxt = itemView.findViewById(R.id.tv_exercise_level);
            exerciseDurationTxt = itemView.findViewById(R.id.tv_exercise_duration);
        }
    }
}

