package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Helper.GlideImageLoader;
import fpt.fall2025.posetrainer.R;

/**
 * Adapter để hiển thị danh sách exercises trong SessionResultActivity
 * Chỉ hiển thị kết quả (read-only), không có nút bắt đầu tập
 */
public class SessionResultExerciseAdapter extends RecyclerView.Adapter<SessionResultExerciseAdapter.SessionResultExerciseViewHolder> {
    private Context context;
    private List<Session.PerExercise> perExercises;
    private List<Exercise> exercises;

    public SessionResultExerciseAdapter(Context context, List<Session.PerExercise> perExercises, List<Exercise> exercises) {
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
    public SessionResultExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_result_exercise, parent, false);
        return new SessionResultExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionResultExerciseViewHolder holder, int position) {
        Session.PerExercise perExercise = perExercises.get(position);
        Exercise exercise = getExerciseById(perExercise.getExerciseId());

        if (exercise == null) {
            // Không tìm thấy exercise, hiển thị thông tin từ perExercise
            holder.exerciseNameTxt.setText("Bài tập #" + perExercise.getExerciseNo());
            holder.exerciseImage.setImageResource(R.drawable.pic_1_1);
        } else {
            // Hiển thị tên exercise
            holder.exerciseNameTxt.setText(exercise.getName());

            // Hiển thị ảnh exercise
            if (exercise.getMedia() != null && exercise.getMedia().getThumbnailUrl() != null) {
                String thumbnailUrl = exercise.getMedia().getThumbnailUrl();
                // GlideImageLoader tự động xử lý: Google Drive, Google Image Search, direct URLs, local drawables
                GlideImageLoader.loadImage(context, thumbnailUrl, holder.exerciseImage);
            } else {
                holder.exerciseImage.setImageResource(R.drawable.pic_1_1);
            }
        }

        // Tính toán số sets và reps
        int totalSets = perExercise.getSets() != null ? perExercise.getSets().size() : 0;
        int targetReps = 0;
        if (perExercise.getSets() != null && !perExercise.getSets().isEmpty()) {
            targetReps = perExercise.getSets().get(0).getTargetReps();
        }

        // Hiển thị sets x reps
        holder.setsRepsTxt.setText(totalSets + " sets x " + targetReps + " reps");

        // Tính toán số sets hoàn thành và bỏ qua
        int completedSets = 0;
        int skippedSets = 0;

        if (perExercise.getSets() != null) {
            for (Session.SetData setData : perExercise.getSets()) {
                String setState = setData.getState();
                if ("completed".equals(setState)) {
                    completedSets++;
                } else if ("skipped".equals(setState)) {
                    skippedSets++;
                }
            }
        }

        // Hiển thị trạng thái exercise
        String state = perExercise.getState();
        holder.statusTxt.setText(getStateDisplayText(state));
        
        // Đặt màu cho status indicator
        int stateColor = getStateColor(state);
        holder.statusIndicator.setBackgroundColor(stateColor);

        // Hiển thị chi tiết từng set
        displaySetsDetails(holder.setsLayout, perExercise.getSets(), totalSets);

        // Hiển thị tổng kết
        int totalCompletedSets = completedSets + skippedSets;
        holder.summaryTxt.setText("Tổng: " + totalCompletedSets + "/" + totalSets + " sets hoàn thành");
    }

    /**
     * Hiển thị chi tiết từng set trong exercise
     */
    private void displaySetsDetails(LinearLayout setsLayout, List<Session.SetData> sets, int totalSets) {
        // Xóa tất cả views cũ
        setsLayout.removeAllViews();

        if (sets == null || sets.isEmpty()) {
            // Không có sets nào
            TextView emptyText = new TextView(context);
            emptyText.setText("Không có set nào");
            emptyText.setTextColor(context.getResources().getColor(R.color.hw_text_secondary, null));
            emptyText.setTextSize(12);
            emptyText.setPadding(0, 8, 0, 0);
            setsLayout.addView(emptyText);
            return;
        }

        // Hiển thị từng set
        for (int i = 0; i < sets.size(); i++) {
            Session.SetData setData = sets.get(i);
            
            // Tạo LinearLayout cho mỗi set
            LinearLayout setRow = new LinearLayout(context);
            setRow.setOrientation(LinearLayout.HORIZONTAL);
            setRow.setPadding(0, 4, 0, 4);
            setRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Set number
            TextView setNumberText = new TextView(context);
            setNumberText.setText("Set " + (i + 1) + ":");
            setNumberText.setTextColor(context.getResources().getColor(R.color.white, null));
            setNumberText.setTextSize(12);
            setNumberText.setMinWidth(60);
            setRow.addView(setNumberText);

            // Target reps
            TextView targetRepsText = new TextView(context);
            targetRepsText.setText(setData.getTargetReps() + " reps");
            targetRepsText.setTextColor(context.getResources().getColor(R.color.hw_text_secondary, null));
            targetRepsText.setTextSize(12);
            targetRepsText.setPadding(8, 0, 0, 0);
            setRow.addView(targetRepsText);

            // Actual reps (nếu có)
            if (setData.getCorrectReps() > 0) {
                TextView actualRepsText = new TextView(context);
                actualRepsText.setText(" → " + setData.getCorrectReps() + " reps");
                actualRepsText.setTextColor(context.getResources().getColor(R.color.white, null));
                actualRepsText.setTextSize(12);
                actualRepsText.setPadding(8, 0, 0, 0);
                setRow.addView(actualRepsText);
            }

            // State indicator
            TextView stateText = new TextView(context);
            String setState = setData.getState();
            stateText.setText("(" + getSetStateDisplayText(setState) + ")");
            stateText.setTextColor(getSetStateColor(setState));
            stateText.setTextSize(11);
            stateText.setPadding(8, 0, 0, 0);
            setRow.addView(stateText);

            setsLayout.addView(setRow);
        }
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

    /**
     * Lấy text hiển thị cho state
     */
    private String getStateDisplayText(String state) {
        if (state == null) {
            return "Chưa bắt đầu";
        }

        switch (state) {
            case "not_started":
                return "Chưa bắt đầu";
            case "doing":
                return "Đang tập";
            case "completed":
                return "Hoàn thành";
            default:
                return "Không xác định";
        }
    }

    /**
     * Lấy màu cho state indicator
     */
    private int getStateColor(String state) {
        if (state == null) {
            return context.getResources().getColor(R.color.hw_text_secondary, null);
        }

        switch (state) {
            case "not_started":
                return context.getResources().getColor(R.color.hw_text_secondary, null);
            case "doing":
                return context.getResources().getColor(R.color.orange, null);
            case "completed":
                return context.getResources().getColor(R.color.hw_primary, null);
            default:
                return context.getResources().getColor(R.color.hw_text_secondary, null);
        }
    }

    /**
     * Lấy text hiển thị cho set state
     */
    private String getSetStateDisplayText(String state) {
        if (state == null) {
            return "Chưa hoàn thành";
        }

        switch (state) {
            case "completed":
                return "Hoàn thành";
            case "skipped":
                return "Đã bỏ qua";
            case "incomplete":
                return "Chưa hoàn thành";
            default:
                return "Không xác định";
        }
    }

    /**
     * Lấy màu cho set state
     */
    private int getSetStateColor(String state) {
        if (state == null) {
            return context.getResources().getColor(R.color.hw_text_secondary, null);
        }

        switch (state) {
            case "completed":
                return context.getResources().getColor(R.color.hw_primary, null);
            case "skipped":
                return context.getResources().getColor(R.color.orange, null);
            case "incomplete":
                return context.getResources().getColor(R.color.hw_text_secondary, null);
            default:
                return context.getResources().getColor(R.color.hw_text_secondary, null);
        }
    }

    @Override
    public int getItemCount() {
        return perExercises.size();
    }

    /**
     * ViewHolder cho SessionResultExerciseAdapter
     */
    public static class SessionResultExerciseViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView exerciseImage;
        TextView exerciseNameTxt;
        TextView setsRepsTxt;
        TextView statusTxt;
        View statusIndicator;
        LinearLayout setsLayout;
        TextView summaryTxt;

        public SessionResultExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseImage = itemView.findViewById(R.id.iv_exercise_image);
            exerciseNameTxt = itemView.findViewById(R.id.tv_exercise_name);
            setsRepsTxt = itemView.findViewById(R.id.tv_sets_reps);
            statusTxt = itemView.findViewById(R.id.tv_status);
            statusIndicator = itemView.findViewById(R.id.view_status_indicator);
            setsLayout = itemView.findViewById(R.id.layout_sets);
            summaryTxt = itemView.findViewById(R.id.tv_summary);
        }
    }
}

