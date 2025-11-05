package fpt.fall2025.posetrainer.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import fpt.fall2025.posetrainer.Activity.ExerciseActivity;
import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.databinding.DialogExerciseDetailBinding;

/**
 * ExerciseDetailDialog - Dialog hiển thị chi tiết bài tập
 * Hiển thị tên, level, category, sets x reps, ảnh/video demo
 */
public class ExerciseDetailDialog extends Dialog {
    private static final String TAG = "ExerciseDetailDialog";

    private DialogExerciseDetailBinding binding;
    private Exercise exercise;
    private Context context;
    private int customSets = -1;
    private int customReps = -1;
    private String customDifficulty = null;

    public ExerciseDetailDialog(@NonNull Context context, Exercise exercise) {
        super(context);
        this.context = context;
        this.exercise = exercise;
    }

    public ExerciseDetailDialog(@NonNull Context context, Exercise exercise, int customSets, int customReps, String customDifficulty) {
        super(context);
        this.context = context;
        this.exercise = exercise;
        this.customSets = customSets;
        this.customReps = customReps;
        this.customDifficulty = customDifficulty;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DialogExerciseDetailBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // Set dialog properties
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Make dialog cancelable when clicking outside
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        if (exercise == null) {
            Toast.makeText(context, "No exercise data provided", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        setupUI();
        updateExerciseUI();

        // Debug binding
        Log.d(TAG, "Binding closeBtn: " + (binding.closeBtn != null ? "NOT NULL" : "NULL"));
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Close button with null check and enhanced debugging
        if (binding.closeBtn != null) {
            Log.d(TAG, "Close button found, setting listener");

            // Ensure the button is clickable and focusable
            binding.closeBtn.setClickable(true);
            binding.closeBtn.setFocusable(true);
            binding.closeBtn.setFocusableInTouchMode(true);

            binding.closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Close button clicked!");
                    dismiss();
                }
            });

            // Background selector is already set in XML

            Log.d(TAG, "Close button listener set successfully");
        } else {
            Log.e(TAG, "Close button is null!");
        }
    }

    /**
     * Update UI with exercise data
     */
    private void updateExerciseUI() {
        if (exercise == null) return;

        // Set exercise name
        binding.exerciseNameTxt.setText(exercise.getName());

        // Set level
        binding.exerciseLevelTxt.setText(exercise.getLevel());

        // Set category (first category if available)
        if (exercise.getCategory() != null && !exercise.getCategory().isEmpty()) {
            binding.exerciseCategoryTxt.setText(exercise.getCategory().get(0));
        } else {
            binding.exerciseCategoryTxt.setText("General");
        }

        // Set sets x reps - use custom config if available, otherwise use default
        int sets, reps;
        if (customSets > 0 && customReps > 0) {
            sets = customSets;
            reps = customReps;
        } else if (exercise.getDefaultConfig() != null) {
            sets = exercise.getDefaultConfig().getSets();
            reps = exercise.getDefaultConfig().getReps();
        } else {
            sets = 3;
            reps = 12;
        }
        String setsReps = sets + " x " + reps;
        binding.exerciseSetsRepsTxt.setText(setsReps);

        // Set muscles (if available)
        if (exercise.getMuscles() != null && !exercise.getMuscles().isEmpty()) {
            StringBuilder musclesText = new StringBuilder();
            for (int i = 0; i < exercise.getMuscles().size(); i++) {
                if (i > 0) musclesText.append(", ");
                musclesText.append(exercise.getMuscles().get(i));
            }
            binding.exerciseMusclesTxt.setText(musclesText.toString());
        } else {
            binding.exerciseMusclesTxt.setText("Full body");
        }

        // Set equipment (if available)
        if (exercise.getEquipment() != null && !exercise.getEquipment().isEmpty()) {
            StringBuilder equipmentText = new StringBuilder();
            for (int i = 0; i < exercise.getEquipment().size(); i++) {
                if (i > 0) equipmentText.append(", ");
                equipmentText.append(exercise.getEquipment().get(i));
            }
            binding.exerciseEquipmentTxt.setText(equipmentText.toString());
        } else {
            binding.exerciseEquipmentTxt.setText("No equipment");
        }

        // Load media (demo video or thumbnail)
        loadExerciseMedia();
    }

    /**
     * Load exercise media (video or image)
     */
    private void loadExerciseMedia() {
        if (exercise.getMedia() != null) {
            // Try to load demo video first
            if (exercise.getMedia().getDemoVideoUrl() != null && !exercise.getMedia().getDemoVideoUrl().isEmpty()) {
                // For now, just show a placeholder
                // TODO: Implement video player
                binding.mediaPlaceholderTxt.setText("Demo Video Available");
                binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
                Log.d(TAG, "Demo video URL: " + exercise.getMedia().getDemoVideoUrl());
            }
            // Try thumbnail if no video
            else if (exercise.getMedia().getThumbnailUrl() != null && !exercise.getMedia().getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                        .load(exercise.getMedia().getThumbnailUrl())
                        .into(binding.exerciseImageView);
                binding.exerciseImageView.setVisibility(View.VISIBLE);
                binding.mediaPlaceholderTxt.setVisibility(View.GONE);
                Log.d(TAG, "Thumbnail URL: " + exercise.getMedia().getThumbnailUrl());
            }
            // No media available
            else {
                binding.mediaPlaceholderTxt.setText("No media available");
                binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
                binding.exerciseImageView.setVisibility(View.GONE);
            }
        } else {
            // No media object
            binding.mediaPlaceholderTxt.setText("No media available");
            binding.mediaPlaceholderTxt.setVisibility(View.VISIBLE);
            binding.exerciseImageView.setVisibility(View.GONE);
        }
    }

    /**
     * Static method to show dialog
     */
    public static void show(Context context, Exercise exercise) {
        try {
            ExerciseDetailDialog dialog = new ExerciseDetailDialog(context, exercise);
            dialog.show();
            Log.d(TAG, "Dialog shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage(), e);
        }
    }

    /**
     * Static method to show dialog with custom config
     */
    public static void show(Context context, Exercise exercise, int customSets, int customReps, String customDifficulty) {
        try {
            ExerciseDetailDialog dialog = new ExerciseDetailDialog(context, exercise, customSets, customReps, customDifficulty);
            dialog.show();
            Log.d(TAG, "Dialog shown successfully with custom config");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage(), e);
        }
    }
}
