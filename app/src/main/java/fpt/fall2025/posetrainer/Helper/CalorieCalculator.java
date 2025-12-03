package fpt.fall2025.posetrainer.Helper;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.Session;

/**
 * Helper class để tính toán calo dựa trên công thức METs
 * Công thức: Calories = METs × weight(kg) × duration(hours)
 */
public class CalorieCalculator {
    
    /**
     * Tính calo cho một exercise dựa trên METs
     * 
     * @param exercise Exercise object
     * @param weightKg Cân nặng của người dùng (kg)
     * @param durationMinutes Thời gian thực hiện exercise (phút)
     * @return Số calo tiêu hao (kcal)
     */
    public static int calculateCaloriesForExercise(Exercise exercise, double weightKg, double durationMinutes) {
        if (exercise == null || exercise.getDefaultConfig() == null || weightKg <= 0 || durationMinutes <= 0) {
            return 0;
        }
        
        double mets = exercise.getDefaultConfig().getMets();
        if (mets <= 0) {
            mets = 5.0; // Giá trị mặc định
        }
        
        // Công thức METs: Calories = METs × weight(kg) × duration(hours)
        // durationMinutes / 60 để chuyển từ phút sang giờ
        double calories = mets * weightKg * (durationMinutes / 60.0);
        
        return (int) Math.round(calories);
    }
    
    /**
     * Tính tổng calo cho một workout session
     * Ưu tiên sử dụng thời gian thực tế từ SetData (startedAt, endedAt) nếu có
     * 
     * @param session Session object
     * @param weightKg Cân nặng của người dùng (kg)
     * @param exercises Array các exercises trong session
     * @return Tổng số calo tiêu hao (kcal)
     */
    public static int calculateTotalCaloriesForSession(Session session, double weightKg, Exercise[] exercises) {
        if (session == null || weightKg <= 0 || exercises == null) {
            return 0;
        }
        
        int totalCalories = 0;
        
        if (session.getPerExercise() != null) {
            for (Session.PerExercise perExercise : session.getPerExercise()) {
                // Tìm exercise tương ứng
                Exercise exercise = findExerciseById(exercises, perExercise.getExerciseId());
                if (exercise == null) continue;
                
                // Tính calo cho exercise này dựa trên thời gian thực tế của các set
                int exerciseCalories = calculateCaloriesForExerciseFromSets(perExercise, exercise, weightKg);
                totalCalories += exerciseCalories;
            }
        }
        
        return totalCalories;
    }
    
    /**
     * Tính calo cho một exercise dựa trên thời gian thực tế của từng set
     * Ưu tiên sử dụng startedAt/endedAt từ SetData, fallback sang ước tính
     * 
     * @param perExercise PerExercise object chứa các sets
     * @param exercise Exercise object
     * @param weightKg Cân nặng của người dùng (kg)
     * @return Số calo tiêu hao cho exercise này (kcal)
     */
    public static int calculateCaloriesForExerciseFromSets(Session.PerExercise perExercise, Exercise exercise, double weightKg) {
        if (perExercise == null || exercise == null || weightKg <= 0) {
            return 0;
        }
        
        if (perExercise.getSets() == null || perExercise.getSets().isEmpty()) {
            return 0;
        }
        
        double mets = exercise.getDefaultConfig() != null ? exercise.getDefaultConfig().getMets() : 5.0;
        if (mets <= 0) {
            mets = 5.0; // Giá trị mặc định
        }
        
        double totalDurationMinutes = 0.0;
        boolean hasActualTiming = false;
        
        // Tính tổng thời gian từ các set đã hoàn thành (có startedAt và endedAt)
        for (Session.SetData setData : perExercise.getSets()) {
            if (setData.getState() != null && 
                ("completed".equals(setData.getState()) || "skipped".equals(setData.getState()))) {
                
                long startedAt = setData.getStartedAt();
                long endedAt = setData.getEndedAt();
                
                // Nếu có thời gian thực tế, sử dụng nó
                if (startedAt > 0 && endedAt > 0 && endedAt > startedAt) {
                    long durationSeconds = endedAt - startedAt;
                    totalDurationMinutes += durationSeconds / 60.0;
                    hasActualTiming = true;
                }
            }
        }
        
        // Nếu không có thời gian thực tế, fallback sang ước tính
        if (!hasActualTiming || totalDurationMinutes == 0) {
            totalDurationMinutes = calculateExerciseDuration(perExercise, exercise);
        }
        
        // Tính calo: Calories = METs × weight(kg) × duration(hours)
        double calories = mets * weightKg * (totalDurationMinutes / 60.0);
        
        return (int) Math.round(calories);
    }
    
    /**
     * Tính calo dựa trên tổng thời gian và METs trung bình
     * Phương pháp đơn giản hơn khi không có thông tin chi tiết từng exercise
     * 
     * @param averageMets METs trung bình của workout
     * @param weightKg Cân nặng của người dùng (kg)
     * @param durationMinutes Tổng thời gian workout (phút)
     * @return Số calo tiêu hao (kcal)
     */
    public static int calculateCaloriesByDuration(double averageMets, double weightKg, double durationMinutes) {
        if (averageMets <= 0 || weightKg <= 0 || durationMinutes <= 0) {
            return 0;
        }
        
        // Công thức METs: Calories = METs × weight(kg) × duration(hours)
        double calories = averageMets * weightKg * (durationMinutes / 60.0);
        
        return (int) Math.round(calories);
    }
    
    /**
     * Tìm exercise trong array theo ID
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
    
    /**
     * Tính thời gian thực hiện một exercise (phút)
     * Ước tính: sets × reps × 2 giây/rep + rest time giữa các sets
     */
    private static double calculateExerciseDuration(Session.PerExercise perExercise, Exercise exercise) {
        if (perExercise == null || perExercise.getSets() == null || perExercise.getSets().isEmpty()) {
            return 0;
        }
        
        int sets = perExercise.getSets().size();
        int totalReps = 0;
        for (Session.SetData setData : perExercise.getSets()) {
            totalReps += setData.getCorrectReps();
        }
        
        // Ước tính 2 giây cho mỗi rep
        double exerciseTimeSeconds = totalReps * 2.0;
        
        // Thời gian nghỉ giữa các sets
        int restSec = 30; // Mặc định
        if (exercise != null && exercise.getDefaultConfig() != null) {
            restSec = exercise.getDefaultConfig().getRestSec();
        }
        double restTimeSeconds = (sets - 1) * restSec; // Nghỉ giữa các sets, không nghỉ sau set cuối
        
        // Tổng thời gian (phút)
        return (exerciseTimeSeconds + restTimeSeconds) / 60.0;
    }
}

