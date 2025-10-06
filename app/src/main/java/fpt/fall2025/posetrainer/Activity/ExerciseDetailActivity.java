package fpt.fall2025.posetrainer.Activity;

import android.content.Context;
import android.content.Intent;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Dialog.ExerciseDetailDialog;

/**
 * ExerciseDetailActivity - Utility class để hiển thị chi tiết bài tập
 * Hiển thị tên, level, category, sets x reps, ảnh/video demo thông qua Dialog
 */
public class ExerciseDetailActivity {
    private static final String TAG = "ExerciseDetailActivity";

    /**
     * Show exercise detail dialog
     * @param context Context để hiển thị dialog
     * @param exercise Exercise object chứa thông tin bài tập
     */
    public static void show(Context context, Exercise exercise) {
        if (exercise == null) {
            return;
        }
        ExerciseDetailDialog.show(context, exercise);
    }

}

