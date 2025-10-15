package fpt.fall2025.posetrainer.Analyzer;

import java.util.List;
import java.util.Map;

/**
 * Class chung chứa feedback từ tất cả các analyzer
 * Thay thế cho SquatAnalyzer.Feedback, PushUpAnalyzer.Feedback, etc.
 */
public class ExerciseFeedback {
    
    // Thông tin cơ bản
    private int correctCount;
    private int incorrectCount;
    private String message;
    private boolean cameraWarning;
    private int offsetAngle;
    private List<String> feedbackList;

    // Góc đo cho squat
    private int hipAngle;
    private int kneeAngle;
    private int ankleAngle;

    // Góc đo cho pushup
    private int shoulderAngle;
    private int earElbowHipAngle;

    // Góc đo cho jumping jack
    private int leftArmAngle;
    private int leftLegAngle;
    private int rightArmAngle;
    private int rightLegAngle;
    
    // Trạng thái khác
    private boolean lowerHips;
    private String currentState;
    
    // Constructor mặc định
    public ExerciseFeedback() {
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.message = "";
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.lowerHips = false;
        this.currentState = "";
    }
    
    // Constructor với thông tin cơ bản
    public ExerciseFeedback(int correctCount, int incorrectCount, String message, 
                          boolean cameraWarning, int offsetAngle, List<String> feedbackList) {
        this();
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
        this.message = message;
        this.cameraWarning = cameraWarning;
        this.offsetAngle = offsetAngle;
        this.feedbackList = feedbackList;
    }
    
    // Getters và Setters
    public int getCorrectCount() {
        return correctCount;
    }
    
    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }
    
    public int getIncorrectCount() {
        return incorrectCount;
    }
    
    public void setIncorrectCount(int incorrectCount) {
        this.incorrectCount = incorrectCount;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isCameraWarning() {
        return cameraWarning;
    }
    
    public void setCameraWarning(boolean cameraWarning) {
        this.cameraWarning = cameraWarning;
    }
    
    public int getOffsetAngle() {
        return offsetAngle;
    }
    
    public void setOffsetAngle(int offsetAngle) {
        this.offsetAngle = offsetAngle;
    }
    
    public List<String> getFeedbackList() {
        return feedbackList;
    }
    
    public void setFeedbackList(List<String> feedbackList) {
        this.feedbackList = feedbackList;
    }
    
    // Other states
    public boolean isLowerHips() {
        return lowerHips;
    }
    
    public void setLowerHips(boolean lowerHips) {
        this.lowerHips = lowerHips;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public int getHipAngle() {
        return hipAngle;
    }

    public void setHipAngle(int hipAngle) {
        this.hipAngle = hipAngle;
    }

    public int getKneeAngle() {
        return kneeAngle;
    }

    public void setKneeAngle(int kneeAngle) {
        this.kneeAngle = kneeAngle;
    }

    public int getAnkleAngle() {
        return ankleAngle;
    }

    public void setAnkleAngle(int ankleAngle) {
        this.ankleAngle = ankleAngle;
    }

    public int getShoulderAngle() {
        return shoulderAngle;
    }

    public void setShoulderAngle(int shoulderAngle) {
        this.shoulderAngle = shoulderAngle;
    }

    public int getEarElbowHipAngle() {
        return earElbowHipAngle;
    }

    public void setEarElbowHipAngle(int earElbowHipAngle) {
        this.earElbowHipAngle = earElbowHipAngle;
    }

    public int getLeftArmAngle() {
        return leftArmAngle;
    }

    public void setLeftArmAngle(int leftArmAngle) {
        this.leftArmAngle = leftArmAngle;
    }

    public int getLeftLegAngle() {
        return leftLegAngle;
    }

    public void setLeftLegAngle(int leftLegAngle) {
        this.leftLegAngle = leftLegAngle;
    }

    public int getRightArmAngle() {
        return rightArmAngle;
    }

    public void setRightArmAngle(int rightArmAngle) {
        this.rightArmAngle = rightArmAngle;
    }

    public int getRightLegAngle() {
        return rightLegAngle;
    }

    public void setRightLegAngle(int rightLegAngle) {
        this.rightLegAngle = rightLegAngle;
    }

    /**
     * Tạo ExerciseFeedback từ SquatAnalyzer.Feedback
     */
    public static ExerciseFeedback fromSquatFeedback(Object squatFeedback) {
        // Sẽ implement sau khi có SquatAnalyzer
        return new ExerciseFeedback();
    }
    
    /**
     * Tạo ExerciseFeedback từ PushUpAnalyzer.Feedback
     */
    public static ExerciseFeedback fromPushUpFeedback(Object pushUpFeedback) {
        // Sẽ implement sau khi có PushUpAnalyzer
        return new ExerciseFeedback();
    }
    
    /**
     * Tạo ExerciseFeedback từ JumpingJackAnalyzer.Feedback
     */
    public static ExerciseFeedback fromJumpingJackFeedback(Object jumpingJackFeedback) {
        // Sẽ implement sau khi có JumpingJackAnalyzer
        return new ExerciseFeedback();
    }
}
