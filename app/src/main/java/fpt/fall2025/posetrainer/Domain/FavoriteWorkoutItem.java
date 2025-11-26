package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;

/**
 * Wrapper class để chứa cả WorkoutTemplate và UserWorkout trong danh sách yêu thích
 */
public class FavoriteWorkoutItem implements Serializable {
    private WorkoutTemplate workoutTemplate;
    private UserWorkout userWorkout;
    private boolean isUserWorkout; // true nếu là UserWorkout, false nếu là WorkoutTemplate

    public FavoriteWorkoutItem(WorkoutTemplate workoutTemplate) {
        this.workoutTemplate = workoutTemplate;
        this.userWorkout = null;
        this.isUserWorkout = false;
    }

    public FavoriteWorkoutItem(UserWorkout userWorkout) {
        this.workoutTemplate = null;
        this.userWorkout = userWorkout;
        this.isUserWorkout = true;
    }

    public WorkoutTemplate getWorkoutTemplate() {
        return workoutTemplate;
    }

    public UserWorkout getUserWorkout() {
        return userWorkout;
    }

    public boolean isUserWorkout() {
        return isUserWorkout;
    }

    /**
     * Lấy title của workout (từ WorkoutTemplate hoặc UserWorkout)
     */
    public String getTitle() {
        if (isUserWorkout && userWorkout != null) {
            return userWorkout.getTitle();
        } else if (workoutTemplate != null) {
            return workoutTemplate.getTitle();
        }
        return "";
    }

    /**
     * Lấy description của workout
     */
    public String getDescription() {
        if (isUserWorkout && userWorkout != null) {
            return userWorkout.getDescription();
        } else if (workoutTemplate != null) {
            return workoutTemplate.getDescription();
        }
        return "";
    }

    /**
     * Lấy duration (phút) của workout
     */
    public int getDurationMin() {
        if (isUserWorkout && userWorkout != null) {
            // Tính duration từ items của UserWorkout (ước tính)
            if (userWorkout.getItems() != null) {
                // Ước tính: mỗi exercise ~3 phút
                int itemsCount = (userWorkout.getItems() != null) ? userWorkout.getItems().size() : 0;
                return itemsCount * 3;
            }
            return 0;
        } else if (workoutTemplate != null) {
            return workoutTemplate.getEstDurationMin();
        }
        return 0;
    }

    /**
     * Lấy số lượng exercises
     */
    public int getExerciseCount() {
        if (isUserWorkout && userWorkout != null) {
            return userWorkout.getItems() != null ? userWorkout.getItems().size() : 0;
        } else if (workoutTemplate != null) {
            return workoutTemplate.getItems() != null ? workoutTemplate.getItems().size() : 0;
        }
        return 0;
    }

    /**
     * Lấy ID của workout
     */
    public String getId() {
        if (isUserWorkout && userWorkout != null) {
            return userWorkout.getId();
        } else if (workoutTemplate != null) {
            return workoutTemplate.getId();
        }
        return "";
    }
}

