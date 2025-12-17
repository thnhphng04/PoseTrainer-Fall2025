package fpt.fall2025.posetrainer.Util;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;

/**
 * Helper class để tính toán calo dựa trên công thức METs
 * Công thức: Calories = METs × weight(kg) × duration(hours)
 *
 * ⚠️ QUY ƯỚC:
 * - Chỉ tính calories cho exercise có state = "completed"
 * - Chỉ dùng thời gian THỰC (startedAt / endedAt)
 * - KHÔNG dùng thời gian ước tính nếu user chưa tập
 */
public class CalorieCalculator {

    /**
     * Tính calories cho toàn bộ workout session
     * CHỈ tính các exercise đã completed
     */
    public static int calculateTotalCaloriesForSession(
            Session session,
            double weightKg,
            Exercise[] exercises
    ) {
        if (session == null || weightKg <= 0 || exercises == null) {
            return 0;
        }

        int totalCalories = 0;

        if (session.getPerExercise() == null) {
            return 0;
        }

        for (Session.PerExercise perExercise : session.getPerExercise()) {

            // ⛔ BỎ QUA exercise chưa completed
            if (!"completed".equals(perExercise.getState())) {
                continue;
            }

            Exercise exercise = findExerciseById(exercises, perExercise.getExerciseId());
            if (exercise == null) {
                continue;
            }

            int exerciseCalories =
                    calculateCaloriesForExerciseFromSets(perExercise, exercise, weightKg);

            totalCalories += exerciseCalories;
        }

        return totalCalories;
    }

    /**
     * Tính calories cho 1 exercise dựa trên thời gian THỰC của các set
     * KHÔNG fallback sang estimate nếu chưa tập
     */
    private static int calculateCaloriesForExerciseFromSets(
            Session.PerExercise perExercise,
            Exercise exercise,
            double weightKg
    ) {
        if (perExercise == null || exercise == null || weightKg <= 0) {
            return 0;
        }

        if (perExercise.getSets() == null || perExercise.getSets().isEmpty()) {
            return 0;
        }

        double mets = 5.0;
        if (exercise.getDefaultConfig() != null &&
                exercise.getDefaultConfig().getMets() > 0) {
            mets = exercise.getDefaultConfig().getMets();
        }

        double totalDurationMinutes = 0.0;
        boolean hasActualTiming = false;

        // ✅ Chỉ tính set có thời gian thực
        for (Session.SetData setData : perExercise.getSets()) {

            if (!"completed".equals(setData.getState())) {
                continue;
            }

            long startedAt = setData.getStartedAt();
            long endedAt = setData.getEndedAt();

            if (startedAt > 0 && endedAt > startedAt) {
                long durationSeconds = endedAt - startedAt;
                totalDurationMinutes += durationSeconds / 60.0;
                hasActualTiming = true;
            }
        }

        // ⛔ Chưa có set nào tập → KHÔNG TÍNH
        if (!hasActualTiming || totalDurationMinutes <= 0) {
            return 0;
        }

        // METs formula
        double calories = mets * weightKg * (totalDurationMinutes / 60.0);

        return (int) Math.round(calories);
    }

    /**
     * Tìm exercise theo ID
     */
    private static Exercise findExerciseById(Exercise[] exercises, String exerciseId) {
        if (exercises == null || exerciseId == null) {
            return null;
        }

        for (Exercise exercise : exercises) {
            if (exercise != null && exerciseId.equals(exercise.getId())) {
                return exercise;
            }
        }
        return null;
    }
}
