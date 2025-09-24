package fpt.fall2025.posetrainer.Manager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Exercise;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;

public class WorkoutSessionManager implements Serializable {
    public enum SessionState {
        NOT_STARTED,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        CANCELLED
    }
    
    public enum ExerciseState {
        PREPARING,
        IN_EXERCISE,
        RESTING,
        COMPLETED
    }
    
    private WorkoutTemplate workoutTemplate;
    private ArrayList<Exercise> exercises;
    private SessionState sessionState;
    private ExerciseState exerciseState;
    private int currentExerciseIndex;
    private int currentSet;
    private int currentRep;
    private long sessionStartTime;
    private long exerciseStartTime;
    private long restStartTime;
    private int totalSets;
    private int totalReps;
    private int completedSets;
    private int completedReps;
    
    // Callback interface
    public interface WorkoutSessionListener {
        void onExerciseChanged(Exercise exercise, int exerciseIndex, int totalExercises);
        void onSetChanged(int set, int totalSets);
        void onRepChanged(int rep, int totalReps);
        void onRestStarted(int restSeconds);
        void onRestFinished();
        void onExerciseCompleted(Exercise exercise);
        void onWorkoutCompleted();
        void onSessionPaused();
        void onSessionResumed();
    }
    
    private WorkoutSessionListener listener;
    
    public WorkoutSessionManager(WorkoutTemplate workoutTemplate, ArrayList<Exercise> exercises) {
        this.workoutTemplate = workoutTemplate;
        this.exercises = exercises;
        this.sessionState = SessionState.NOT_STARTED;
        this.exerciseState = ExerciseState.PREPARING;
        this.currentExerciseIndex = 0;
        this.currentSet = 1;
        this.currentRep = 1;
        this.completedSets = 0;
        this.completedReps = 0;
    }
    
    public void setListener(WorkoutSessionListener listener) {
        this.listener = listener;
    }
    
    public void startWorkout() {
        if (sessionState == SessionState.NOT_STARTED) {
            sessionState = SessionState.IN_PROGRESS;
            sessionStartTime = System.currentTimeMillis();
            
            // Calculate total sets and reps for current exercise
            calculateCurrentExerciseTotals();
            
            // Notify listener
            if (listener != null) {
                listener.onExerciseChanged(getCurrentExercise(), currentExerciseIndex, exercises.size());
                listener.onSetChanged(currentSet, totalSets);
                listener.onRepChanged(currentRep, totalReps);
            }
        }
    }
    
    public void pauseWorkout() {
        if (sessionState == SessionState.IN_PROGRESS) {
            sessionState = SessionState.PAUSED;
            if (listener != null) {
                listener.onSessionPaused();
            }
        }
    }
    
    public void resumeWorkout() {
        if (sessionState == SessionState.PAUSED) {
            sessionState = SessionState.IN_PROGRESS;
            if (listener != null) {
                listener.onSessionResumed();
            }
        }
    }
    
    public void completeRep() {
        if (sessionState == SessionState.IN_PROGRESS && exerciseState == ExerciseState.IN_EXERCISE) {
            currentRep++;
            completedReps++;
            
            if (listener != null) {
                listener.onRepChanged(currentRep, totalReps);
            }
            
            // Check if set is completed
            if (currentRep > totalReps) {
                completeSet();
            }
        }
    }
    
    private void completeSet() {
        currentSet++;
        completedSets++;
        currentRep = 1;
        
        if (listener != null) {
            listener.onSetChanged(currentSet, totalSets);
            listener.onRepChanged(currentRep, totalReps);
        }
        
        // Check if exercise is completed
        if (currentSet > totalSets) {
            completeExercise();
        } else {
            // Start rest period
            startRest();
        }
    }
    
    private void completeExercise() {
        exerciseState = ExerciseState.COMPLETED;
        
        if (listener != null) {
            listener.onExerciseCompleted(getCurrentExercise());
        }
        
        // Move to next exercise
        currentExerciseIndex++;
        if (currentExerciseIndex < exercises.size()) {
            // Start next exercise
            startNextExercise();
        } else {
            // Workout completed
            completeWorkout();
        }
    }
    
    private void startNextExercise() {
        currentSet = 1;
        currentRep = 1;
        exerciseState = ExerciseState.PREPARING;
        calculateCurrentExerciseTotals();
        
        if (listener != null) {
            listener.onExerciseChanged(getCurrentExercise(), currentExerciseIndex, exercises.size());
            listener.onSetChanged(currentSet, totalSets);
            listener.onRepChanged(currentRep, totalReps);
        }
    }
    
    private void startRest() {
        exerciseState = ExerciseState.RESTING;
        restStartTime = System.currentTimeMillis();
        
        // Get rest duration from exercise config
        Exercise currentExercise = getCurrentExercise();
        int restSeconds = 60; // default
        if (currentExercise.getDefaultConfig() != null) {
            restSeconds = currentExercise.getDefaultConfig().getRestSec();
        }
        
        if (listener != null) {
            listener.onRestStarted(restSeconds);
        }
    }
    
    public void finishRest() {
        if (exerciseState == ExerciseState.RESTING) {
            exerciseState = ExerciseState.IN_EXERCISE;
            if (listener != null) {
                listener.onRestFinished();
            }
        }
    }
    
    private void completeWorkout() {
        sessionState = SessionState.COMPLETED;
        if (listener != null) {
            listener.onWorkoutCompleted();
        }
    }
    
    private void calculateCurrentExerciseTotals() {
        Exercise currentExercise = getCurrentExercise();
        if (currentExercise != null && currentExercise.getDefaultConfig() != null) {
            totalSets = currentExercise.getDefaultConfig().getSets();
            totalReps = currentExercise.getDefaultConfig().getReps();
        } else {
            totalSets = 3; // default
            totalReps = 10; // default
        }
    }
    
    public void cancelWorkout() {
        sessionState = SessionState.CANCELLED;
    }
    
    // Getters
    public WorkoutTemplate getWorkoutTemplate() {
        return workoutTemplate;
    }
    
    public ArrayList<Exercise> getExercises() {
        return exercises;
    }
    
    public SessionState getSessionState() {
        return sessionState;
    }
    
    public ExerciseState getExerciseState() {
        return exerciseState;
    }
    
    public Exercise getCurrentExercise() {
        if (currentExerciseIndex < exercises.size()) {
            return exercises.get(currentExerciseIndex);
        }
        return null;
    }
    
    public int getCurrentExerciseIndex() {
        return currentExerciseIndex;
    }
    
    public int getCurrentSet() {
        return currentSet;
    }
    
    public int getCurrentRep() {
        return currentRep;
    }
    
    public int getTotalSets() {
        return totalSets;
    }
    
    public int getTotalReps() {
        return totalReps;
    }
    
    public int getCompletedSets() {
        return completedSets;
    }
    
    public int getCompletedReps() {
        return completedReps;
    }
    
    public long getSessionDuration() {
        if (sessionStartTime > 0) {
            return System.currentTimeMillis() - sessionStartTime;
        }
        return 0;
    }
    
    public boolean isWorkoutActive() {
        return sessionState == SessionState.IN_PROGRESS;
    }
    
    public boolean isPaused() {
        return sessionState == SessionState.PAUSED;
    }
    
    public boolean isCompleted() {
        return sessionState == SessionState.COMPLETED;
    }
}
