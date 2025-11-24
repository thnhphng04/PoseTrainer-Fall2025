package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jumping Jack Analyzer - Phân tích bài tập Jumping Jack
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class JumpingJackAnalyzer implements ExerciseAnalyzerInterface {
    
    private JumpingJackThresholds thresholds;
    private List<String> stateSequence;
    private int correctCount;
    private int incorrectCount;
    private boolean incorrectPosture;
    private String prevState;
    private String currState;
    private boolean[] displayText;
    private int[] countFrames;
    private double inactiveTime;
    private double inactiveTimeFront;
    private double startInactiveTime;
    private double startInactiveTimeFront;
    private boolean cameraWarning;
    private int offsetAngle;
    private List<String> feedbackList;
    
    public JumpingJackAnalyzer() {
        this.thresholds = JumpingJackThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[2];
        this.countFrames = new int[4];
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }
    
    public JumpingJackAnalyzer(JumpingJackThresholds thresholds) {
        this();
        this.thresholds = thresholds;
    }
    
    @Override
    public ExerciseFeedback analyze(List<Map<String, Float>> landmarks) {
        
        if (landmarks == null || landmarks.size() < 33) {
            return new ExerciseFeedback();
        }
        
        // Lấy các điểm cần thiết từ toàn bộ landmarks (33 điểm MediaPipe)
        Map<String, Float> nose = getLandmark(landmarks, 0);
        Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
        Map<String, Float> rightShoulder = getLandmark(landmarks, 12);
        Map<String, Float> leftElbow = getLandmark(landmarks, 13);
        Map<String, Float> rightElbow = getLandmark(landmarks, 14);
        Map<String, Float> leftWrist = getLandmark(landmarks, 15);
        Map<String, Float> rightWrist = getLandmark(landmarks, 16);
        Map<String, Float> leftHip = getLandmark(landmarks, 23);
        Map<String, Float> rightHip = getLandmark(landmarks, 24);
        Map<String, Float> leftKnee = getLandmark(landmarks, 25);
        Map<String, Float> rightKnee = getLandmark(landmarks, 26);
        Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
        Map<String, Float> rightAnkle = getLandmark(landmarks, 28);
        
        // Tính offset angle để phát hiện lệch camera
        offsetAngle = calculateOffsetAngle(leftShoulder, nose, rightShoulder);
        cameraWarning = offsetAngle < 80;
        feedbackList.clear();
        
        double now = System.nanoTime() / 1e9;
        if (cameraWarning) {
            // Đếm thời gian lệch camera
            inactiveTimeFront += now - startInactiveTimeFront;
            startInactiveTimeFront = now;
            if (inactiveTimeFront >= thresholds.getInactiveThresh()) {
                correctCount = 0;
                incorrectCount = 0;
                inactiveTimeFront = 0.0;
            }
            // Feedback cảnh báo camera
            feedbackList.add("Camera lệch, vui lòng chỉnh lại!");
            feedbackList.add("Góc lệch: " + offsetAngle);
            prevState = null;
            currState = null;
            startInactiveTime = now;
            inactiveTime = 0.0;
        } else {
            inactiveTimeFront = 0.0;
            startInactiveTimeFront = now;
            
            // Tính các góc mới
            int leftArmAngle = calculateAngleWithDownVertical(leftShoulder, leftWrist);
            int leftLegAngle = calculateAngleWithDownVertical(leftHip, leftAnkle);

            
            int rightArmAngle = calculateAngleWithDownVertical(rightShoulder, rightWrist);
            int rightLegAngle = calculateAngleWithDownVertical(rightHip, rightAnkle);
            
            // State machine
            currState = getState(leftArmAngle, leftLegAngle, rightArmAngle, rightLegAngle);
            updateStateSequence(currState);
            
            // Đếm jumping jack đúng/sai
            String message = "";
            if ("s1".equals(currState)) {
                if (stateSequence.size() == 3 && !incorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
                } else if (incorrectPosture) {
                    incorrectCount++;
                    message = "INCORRECT";
                }
                stateSequence.clear();
                incorrectPosture = false;
            } else { //không ở state 1
                
            }
            
            // Inactivity logic
            if (currState != null && currState.equals(prevState)) {
                inactiveTime += now - startInactiveTime;
                startInactiveTime = now;
                if (inactiveTime >= thresholds.getInactiveThresh()) {
                    correctCount = 0;
                    incorrectCount = 0;
                }
            } else {
                startInactiveTime = now;
                inactiveTime = 0.0;
            }
            
            prevState = currState;
            
            // Reset feedback nếu quá lâu
            for (int i = 0; i < displayText.length; i++) {
                if (countFrames[i] > thresholds.getCntFrameThresh()) {
                    displayText[i] = false;
                    countFrames[i] = 0;
                }
                if (displayText[i]) countFrames[i]++;
            }
            
            // Tạo ExerciseFeedback
            ExerciseFeedback feedback = new ExerciseFeedback(
                correctCount, incorrectCount, message, cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
            );

            feedback.setCurrentState(currState);
            
            return feedback;
        }
        
        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
            correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }
    
    @Override
    public String getExerciseType() {
        return "jumping_jack";
    }
    
    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28}; // All required landmarks
    }
    
    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            JumpingJackThresholds proThresholds = JumpingJackThresholds.defaultPro();
            result.put("armNormal", proThresholds.getArmNormal());
            result.put("armTrans", proThresholds.getArmTrans());
            result.put("armPass", proThresholds.getArmPass());
            result.put("legNormal", proThresholds.getLegNormal());
            result.put("legPass", proThresholds.getLegPass());
        } else {
            result.put("armNormal", thresholds.getArmNormal());
            result.put("armTrans", thresholds.getArmTrans());
            result.put("armPass", thresholds.getArmPass());
            result.put("legNormal", thresholds.getLegNormal());
            result.put("legPass", thresholds.getLegPass());
        }
        return result;
    }
    
    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("armNormal")) {
            this.thresholds.setArmNormal((int[]) thresholds.get("armNormal"));
        }
        if (thresholds.containsKey("legNormal")) {
            this.thresholds.setLegNormal((int[]) thresholds.get("legNormal"));
        }
    }
    
    @Override
    public void reset() {
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.stateSequence.clear();
        this.feedbackList.clear();
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        // Reset display text and count frames
        for (int i = 0; i < displayText.length; i++) {
            displayText[i] = false;
            countFrames[i] = 0;
        }
    }
    
    // Helper methods
    private Map<String, Float> getLandmark(List<Map<String, Float>> landmarks, int idx) {
        if (landmarks == null || idx >= landmarks.size()) {
            Map<String, Float> defaultPoint = new HashMap<>();
            defaultPoint.put("x", 0f);
            defaultPoint.put("y", 0f);
            return defaultPoint;
        }
        return landmarks.get(idx);
    }
    
    private int calculateOffsetAngle(Map<String, Float> p1, Map<String, Float> p2, Map<String, Float> p3) {
        if (p1 == null || p2 == null || p3 == null) return 0;
        
        float[] a = {p1.get("x") - p2.get("x"), p1.get("y") - p2.get("y")};
        float[] b = {p3.get("x") - p2.get("x"), p3.get("y") - p2.get("y")};
        float dot = a[0] * b[0] + a[1] * b[1];
        float normA = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
        float normB = (float) Math.sqrt(b[0] * b[0] + b[1] * b[1]);
        float cosTheta = Math.max(-1f, Math.min(1f, dot / (normA * normB)));
        double theta = Math.acos(cosTheta);
        return (int) Math.toDegrees(theta);
    }
    
    private int calculateAngle(Map<String, Float> p1, Map<String, Float> p2, Map<String, Float> p3) {
        if (p1 == null || p2 == null || p3 == null) return 0;
        
        float[] a = {p1.get("x") - p2.get("x"), p1.get("y") - p2.get("y")};
        float[] b = {p3.get("x") - p2.get("x"), p3.get("y") - p2.get("y")};
        float dot = a[0] * b[0] + a[1] * b[1];
        float normA = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
        float normB = (float) Math.sqrt(b[0] * b[0] + b[1] * b[1]);
        float cosTheta = Math.max(-1f, Math.min(1f, dot / (normA * normB)));
        double theta = Math.acos(cosTheta);
        return (int) Math.toDegrees(theta);
    }
    
    private int calculateAngleWithDownVertical(Map<String, Float> from, Map<String, Float> to) {
        if (from == null || to == null) return 0;
        
        float[] v1 = {0f, 1f}; // vector thẳng đứng hướng xuống
        float[] v2 = {to.get("x") - from.get("x"), to.get("y") - from.get("y")};
        float dot = v1[0] * v2[0] + v1[1] * v2[1];
        float norm1 = (float) Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        float norm2 = (float) Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);
        float cosTheta = Math.max(-1f, Math.min(1f, dot / (norm1 * norm2)));
        double theta = Math.acos(cosTheta);
        return (int) Math.toDegrees(theta);
    }
    
    private String getState(int leftArmAngle, int leftLegAngle, int rightArmAngle, int rightLegAngle) {
        if (leftArmAngle >= thresholds.getArmNormal()[0] && leftArmAngle <= thresholds.getArmNormal()[1] &&
            rightArmAngle >= thresholds.getArmNormal()[0] && rightArmAngle <= thresholds.getArmNormal()[1] &&
            leftLegAngle >= thresholds.getLegNormal()[0] && leftLegAngle <= thresholds.getLegNormal()[1] &&
            rightLegAngle >= thresholds.getLegNormal()[0] && rightLegAngle <= thresholds.getLegNormal()[1]) {
            return "s1";
        } else if (leftArmAngle >= thresholds.getArmTrans()[0] && leftArmAngle <= thresholds.getArmTrans()[1] &&
                   rightArmAngle >= thresholds.getArmTrans()[0] && rightArmAngle <= thresholds.getArmTrans()[1]) {
            return "s2";
        } else if ((leftArmAngle >= thresholds.getArmPass()[0] && leftArmAngle <= thresholds.getArmPass()[1] &&
                    rightArmAngle >= thresholds.getArmPass()[0] && rightArmAngle <= thresholds.getArmPass()[1]) ||
                   (leftLegAngle >= thresholds.getLegPass()[0] && leftLegAngle <= thresholds.getLegPass()[1] &&
                    rightLegAngle >= thresholds.getLegPass()[0] && rightLegAngle <= thresholds.getLegPass()[1])) {
            return "s3";
        }
        return null;
    }
    
    private void updateStateSequence(String state) {
        if (state == null) return;
        if ("s2".equals(state)) {
            if ((!stateSequence.contains("s3") && stateSequence.stream().filter(s -> s.equals("s2")).count() == 0) ||
                (stateSequence.contains("s3") && stateSequence.stream().filter(s -> s.equals("s2")).count() == 1)) {
                stateSequence.add(state);
            }
        } else if ("s3".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s2")) {
                stateSequence.add(state);
            }
        }
    }
    
    // Inner class for JumpingJackThresholds
    public static class JumpingJackThresholds {
        private int[] armNormal;
        private int[] armTrans;
        private int[] armPass;
        private int[] legNormal;
        private int[] legPass;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;
        
        public JumpingJackThresholds() {}
        
        public JumpingJackThresholds(int[] armNormal, int[] armTrans, int[] armPass, 
                                    int[] legNormal, int[] legPass,
                                    int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.armNormal = armNormal;
            this.armTrans = armTrans;
            this.armPass = armPass;
            this.legNormal = legNormal;
            this.legPass = legPass;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }
        
        public static JumpingJackThresholds defaultBeginner() {
            return new JumpingJackThresholds(
                new int[]{0, 60}, new int[]{65, 145}, new int[]{150, 180},
                new int[]{0, 10}, new int[]{11, 45},
                45, 15.0, 50
            );
        }
        
        public static JumpingJackThresholds defaultPro() {
            return new JumpingJackThresholds(
                new int[]{0, 60}, new int[]{65, 150}, new int[]{160, 180},
                new int[]{0, 10}, new int[]{15, 45},
                45, 15.0, 50
            );
        }
        
        // Getters and Setters
        public int[] getArmNormal() { return armNormal; }
        public void setArmNormal(int[] armNormal) { this.armNormal = armNormal; }
        
        public int[] getArmTrans() { return armTrans; }
        public void setArmTrans(int[] armTrans) { this.armTrans = armTrans; }
        
        public int[] getArmPass() { return armPass; }
        public void setArmPass(int[] armPass) { this.armPass = armPass; }
        
        public int[] getLegNormal() { return legNormal; }
        public void setLegNormal(int[] legNormal) { this.legNormal = legNormal; }
        
        public int[] getLegPass() { return legPass; }
        public void setLegPass(int[] legPass) { this.legPass = legPass; }
        
        public int getOffsetThresh() { return offsetThresh; }
        public void setOffsetThresh(int offsetThresh) { this.offsetThresh = offsetThresh; }
        
        public double getInactiveThresh() { return inactiveThresh; }
        public void setInactiveThresh(double inactiveThresh) { this.inactiveThresh = inactiveThresh; }
        
        public int getCntFrameThresh() { return cntFrameThresh; }
        public void setCntFrameThresh(int cntFrameThresh) { this.cntFrameThresh = cntFrameThresh; }
    }
}
