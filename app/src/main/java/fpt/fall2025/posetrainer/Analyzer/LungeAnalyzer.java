package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Squat Analyzer - Phân tích bài tập Squat
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class LungeAnalyzer implements ExerciseAnalyzerInterface {

    private LungeThresholds thresholds;
    private List<String> stateSequence;
    private int correctCount;
    private int incorrectCount;
    private boolean incorrectPosture;
    private String prevState;
    private String currState;
    private boolean[] displayText;
    private int[] countFrames;
    private boolean lowerHips;
    private double inactiveTime;
    private double inactiveTimeFront;
    private double startInactiveTime;
    private double startInactiveTimeFront;
    private boolean cameraWarning;
    private int offsetAngle;
    private List<String> feedbackList;

    public LungeAnalyzer() {
        this.thresholds = LungeThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[4];
        this.countFrames = new int[4];
        this.lowerHips = false;
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }

    public LungeAnalyzer(LungeThresholds thresholds) {
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
        Map<String, Float> leftHip = getLandmark(landmarks, 23);
        Map<String, Float> rightHip = getLandmark(landmarks, 24);
        Map<String, Float> leftKnee = getLandmark(landmarks, 25);
        Map<String, Float> rightKnee = getLandmark(landmarks, 26);
        Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
        Map<String, Float> rightAnkle = getLandmark(landmarks, 28);
        Map<String, Float> leftFoot = getLandmark(landmarks, 31);
        Map<String, Float> rightFoot = getLandmark(landmarks, 32);
        
        // Tính offset angle để phát hiện lệch camera
        offsetAngle = calculateOffsetAngle(leftShoulder, nose, rightShoulder);
        int positionCheck = calculateAngleWithUpVertical(leftHip, leftShoulder);
        cameraWarning = offsetAngle > thresholds.getOffsetThresh() || positionCheck > 30;
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
            feedbackList.add("CAMERA NOT ALIGNED PROPERLY!!!");
            feedbackList.add("OFFSET ANGLE: " + offsetAngle);
            prevState = null;
            currState = null;
            startInactiveTime = now;
            inactiveTime = 0.0;
        } else {
            inactiveTimeFront = 0.0;
            startInactiveTimeFront = now;
            
            // Chọn bên để phân tích dựa trên visibility score
            // Tính average visibility cho mỗi bên
            float leftAvgVis = (
                leftShoulder.getOrDefault("visibility", 0f) +
                leftHip.getOrDefault("visibility", 0f)
            ) / 2.0f;
            
            float rightAvgVis = (
                rightShoulder.getOrDefault("visibility", 0f) +
                rightHip.getOrDefault("visibility", 0f)
            ) / 2.0f;

            List<Map<String, Float>> points;
            if (leftAvgVis > rightAvgVis) {
                // Bên trái nhìn rõ hơn
                points = Arrays.asList(
                        leftShoulder, leftHip, leftKnee, leftAnkle,
                        rightShoulder, rightHip, rightKnee, rightAnkle
                );
            } else {
                // Bên phải nhìn rõ hơn
                points = Arrays.asList(
                        rightShoulder, rightHip, rightKnee, rightAnkle,
                        leftShoulder, leftHip, leftKnee, leftAnkle
                );
            }

            Map<String, Float> nearShldr = points.get(0);
            Map<String, Float> nearHip = points.get(1);
            Map<String, Float> nearKnee = points.get(2);
            Map<String, Float> nearAnkle = points.get(3);

            Map<String, Float> farShldr = points.get(4);
            Map<String, Float> farHip = points.get(5);
            Map<String, Float> farKnee = points.get(6);
            Map<String, Float> farAnkle = points.get(7);
            
            // Tính các góc
            int nearHipAngleWithUpVertical = calculateAngleWithUpVertical(nearHip, nearShldr);
            int nearAnkleAngleWithUpVertical = calculateAngleWithUpVertical(nearAnkle, nearKnee);
            int nearHipAngle = calculateAngle(nearShldr, nearHip, nearKnee);
            int nearKneeAngle = calculateAngle(nearHip, nearKnee, nearAnkle);

            //int farHipAngleWithUpVertical = calculateAngleWithUpVertical(farHip, farShldr);
            int farAnkleAngleWithUpVertical = calculateAngleWithUpVertical(farAnkle, farKnee);
            int farHipAngle = calculateAngle(farShldr, farHip, farKnee);
            int farKneeAngle = calculateAngle(farHip, farKnee, farAnkle);

            
            // State machine
            currState = getState(nearHipAngle, farHipAngle, nearKneeAngle, farKneeAngle, positionCheck);
            updateStateSequence(currState);
            
            // Đếm lunge đúng/sai
            String message = "";
            if ("s1".equals(currState)) {
                if (stateSequence.size() == 1 && !incorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
                } else if (incorrectPosture) {
                    incorrectCount++;
                    message = "INCORRECT";
                }
                stateSequence.clear();
                incorrectPosture = false;
            }
            else {
                // Feedback động tác
                if (nearHipAngleWithUpVertical > thresholds.getHipMax()) {
                    displayText[0] = true;
                    feedbackList.add("BEND BACKWARDS");
                }
                if (nearHipAngleWithUpVertical < thresholds.getHipMin() && stateSequence.stream().filter(s -> s.equals("s2")).count() == 1) {
                    displayText[1] = true;
                    feedbackList.add("BEND FORWARD");
                }
                if ("s2n".equals(currState)){
                    if (nearAnkleAngleWithUpVertical > thresholds.getAnkleMax()) {
                        displayText[2] = true;
                        incorrectPosture = true;
                        feedbackList.add("KNEE OVER TOE");
                    }
                }
                if ("s2f".equals(currState)){
                    if (farAnkleAngleWithUpVertical > thresholds.getAnkleMax()) {
                        displayText[2] = true;
                        incorrectPosture = true;
                        feedbackList.add("KNEE OVER TOE");
                    }
                }
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

            feedback.setCurrentState(currState + nearHipAngle + " " + farHipAngle + " " + nearKneeAngle + " " + farKneeAngle);
            
            return feedback;
        }
        
        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
            correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }
    
    @Override
    public String getExerciseType() {
        return "lunge";
    }
    
    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32}; // All required landmarks
    }
    
    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            LungeThresholds proThresholds = LungeThresholds.defaultPro();
            result.put("hipMin", proThresholds.getHipMin());
            result.put("hipMax", proThresholds.getHipMax());
            result.put("ankleMax", proThresholds.getAnkleMax());
        } else {
            result.put("hipMin", thresholds.getHipMin());
            result.put("hipMax", thresholds.getHipMax());
            result.put("ankleMax", thresholds.getAnkleMax());
        }
        return result;
    }
    
    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        // Implementation để cập nhật thresholds
        if (thresholds.containsKey("hipMin")) {
            this.thresholds.setHipMin((Integer) thresholds.get("hipMin"));
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
        this.lowerHips = false;
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
    
    private int calculateAngleWithUpVertical(Map<String, Float> from, Map<String, Float> to) {
        if (from == null || to == null) return 0;
        
        float[] v1 = {0f, -1f}; // vector thẳng đứng hướng lên
        float[] v2 = {to.get("x") - from.get("x"), to.get("y") - from.get("y")};
        float dot = v1[0] * v2[0] + v1[1] * v2[1];
        float norm1 = (float) Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        float norm2 = (float) Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);
        float cosTheta = Math.max(-1f, Math.min(1f, dot / (norm1 * norm2)));
        double theta = Math.acos(cosTheta);
        return (int) Math.toDegrees(theta);
    }
    
    private String getState(int nearHipAngle, int farHipAngle, int nearKneeAngle, int farKneeAngle,int positionCheck) {
        if (nearHipAngle > thresholds.getHipNormal() && farHipAngle > thresholds.getHipNormal() && positionCheck < 30) {
            return "s1";
        } else if (nearHipAngle > thresholds.getHipPass()[1] && farHipAngle < thresholds.getHipPass()[0] &&
                nearKneeAngle < thresholds.getKneePass()[1] && farKneeAngle < thresholds.getKneePass()[0]){
            return "s2f";
        } else if(nearHipAngle < thresholds.getHipPass()[0] && farHipAngle > thresholds.getHipPass()[1] &&
                nearKneeAngle < thresholds.getKneePass()[0] && farKneeAngle < thresholds.getKneePass()[1]){
            return "s2n";
        }
        return null;
    }
    
    private void updateStateSequence(String state) {
        if (state == null) return;
        if ("s2f".equals(state)) {
            if (!stateSequence.contains(state)) {
                stateSequence.add(state);
            }
        }
        if ("s2n".equals(state)) {
            if (!stateSequence.contains(state)) {
                stateSequence.add(state);
            }
        }
    }
    
    // Inner class for LungeThresholds
    public static class LungeThresholds {
        private int hipNormal;
        private int[] hipPass;
        private int[] kneePass;
        private int hipMin;
        private int hipMax;
        private int ankleMax;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;
        
        public LungeThresholds() {}
        
        public LungeThresholds(int hipNormal, int[] hipPass, int[] kneePass,
                               int hipMin, int hipMax, int ankleMax,
                              int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.hipNormal = hipNormal;
            this.hipPass = hipPass;
            this.kneePass = kneePass;
            this.hipMin = hipMin;
            this.hipMax = hipMax;
            this.ankleMax = ankleMax;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }
        
        public static LungeThresholds defaultBeginner() {
            return new LungeThresholds(
                160, new int[]{110, 150}, new int[]{90, 115}, 10, 50, 45, 45, 15.0, 50
            );
        }
        
        public static LungeThresholds defaultPro() {
            return new LungeThresholds(
                160, new int[]{95, 150}, new int[]{90, 105}, 15, 50, 30, 45, 15.0, 50
            );
        }
        
        // Getters and Setters

        public int getHipNormal() {
            return hipNormal;
        }

        public void setHipNormal(int hipNormal) {
            this.hipNormal = hipNormal;
        }

        public int[] getHipPass() {
            return hipPass;
        }

        public void setHipPass(int[] hipPass) {
            this.hipPass = hipPass;
        }

        public int[] getKneePass() {
            return kneePass;
        }

        public void setKneePass(int[] kneePass) {
            this.kneePass = kneePass;
        }

        public int getHipMin() {
            return hipMin;
        }

        public void setHipMin(int hipMin) {
            this.hipMin = hipMin;
        }

        public int getHipMax() {
            return hipMax;
        }

        public void setHipMax(int hipMax) {
            this.hipMax = hipMax;
        }

        public int getAnkleMax() {
            return ankleMax;
        }

        public void setAnkleMax(int ankleMax) {
            this.ankleMax = ankleMax;
        }

        public int getOffsetThresh() {
            return offsetThresh;
        }

        public void setOffsetThresh(int offsetThresh) {
            this.offsetThresh = offsetThresh;
        }

        public double getInactiveThresh() {
            return inactiveThresh;
        }

        public void setInactiveThresh(double inactiveThresh) {
            this.inactiveThresh = inactiveThresh;
        }

        public int getCntFrameThresh() {
            return cntFrameThresh;
        }

        public void setCntFrameThresh(int cntFrameThresh) {
            this.cntFrameThresh = cntFrameThresh;
        }
    }
}
