package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SitUpTwist Analyzer - Phân tích bài tập Sit-Up Twist
 * Kết hợp Sit-Up (hip angle) + Russian Twist (shoulder ratio)
 * 3 States: s1 (down) → s2 (up center) → s3 (twist) → s1
 */
public class SitUpTwistAnalyzer implements ExerciseAnalyzerInterface {

    private SitUpTwistThresholds thresholds;
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

    public SitUpTwistAnalyzer() {
        this.thresholds = SitUpTwistThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[4];
        this.countFrames = new int[4];
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }

    public SitUpTwistAnalyzer(SitUpTwistThresholds thresholds) {
        this();
        this.thresholds = thresholds;
    }

    @Override
    public ExerciseFeedback analyze(List<Map<String, Float>> landmarks) {

        if (landmarks == null || landmarks.size() < 33) {
            return new ExerciseFeedback();
        }

        // Lấy các điểm cần thiết
        Map<String, Float> nose = getLandmark(landmarks, 0);
        Map<String, Float> leftEar = getLandmark(landmarks, 7);
        Map<String, Float> rightEar = getLandmark(landmarks, 8);
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
        int positionCheck = calculateAngleWithUpVertical(leftAnkle, leftShoulder);
        cameraWarning = positionCheck < 30;

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

            // Chọn bên để phân tích dựa trên visibility score
            // Tính average visibility cho mỗi bên
            float leftAvgVis = (
                leftShoulder.getOrDefault("visibility", 0f) +
                leftHip.getOrDefault("visibility", 0f) +
                leftKnee.getOrDefault("visibility", 0f) +
                leftAnkle.getOrDefault("visibility", 0f)
            ) / 4.0f;
            
            float rightAvgVis = (
                rightShoulder.getOrDefault("visibility", 0f) +
                rightHip.getOrDefault("visibility", 0f) +
                rightKnee.getOrDefault("visibility", 0f) +
                rightAnkle.getOrDefault("visibility", 0f)
            ) / 4.0f;

            List<Map<String, Float>> points;
            if (leftAvgVis > rightAvgVis) {
                // Bên trái nhìn rõ hơn
                points = Arrays.asList(
                        leftEar, leftShoulder, leftHip, leftKnee, leftAnkle, leftFoot
                );
            } else {
                // Bên phải nhìn rõ hơn
                points = Arrays.asList(
                        rightEar, rightShoulder, rightHip, rightKnee, rightAnkle, rightFoot
                );
            }

            Map<String, Float> ear = points.get(0);
            Map<String, Float> shldr = points.get(1);
            Map<String, Float> hip = points.get(2);
            Map<String, Float> knee = points.get(3);
            Map<String, Float> ankle = points.get(4);
            Map<String, Float> foot = points.get(5);

            // Tính các góc - từ Sit-Up
            int hipAngle = calculateAngle(shldr, hip, knee);      // Góc shoulder-hip-knee
            int kneeAngle = calculateAngle(hip, knee, ankle);     // Góc hip-knee-ankle
            
            // Tính ratio - từ Russian Twist
            float dx1 = rightShoulder.get("x") - leftShoulder.get("x");
            float dy1 = rightShoulder.get("y") - leftShoulder.get("y");
            float shoulderWidth = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);

            float dx2 = shldr.get("x") - hip.get("x");
            float dy2 = shldr.get("y") - hip.get("y");
            float torsoLength = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

            float ratio = shoulderWidth / torsoLength;

            // State machine - 3 states dựa vào hip angle + ratio
            currState = getState(hipAngle, ratio);
            updateStateSequence(currState);

            // Đếm Sit-Up Twist đúng/sai
            String message = "";
            
            // bắt lỗi khi ở s2 (up center)
            if ("s2".equals(currState)) {
                if (kneeAngle < thresholds.getKneeThresholds()[0]) {
                    displayText[0] = true;
                    incorrectPosture = true;
                    feedbackList.add("Giữ gối ở góc 90°");
                }
                if (kneeAngle > thresholds.getKneeThresholds()[1]) {
                    displayText[1] = true;
                    incorrectPosture = true;
                    feedbackList.add("Gập gối nhiều hơn");
                }
            }

            // Đếm ngay khi đến s3 (twist) - đã đi qua s1 và s2
            if ("s3".equals(currState)) {
                Boolean complete = stateSequence.contains("s1") && stateSequence.contains("s2");
                
                // Kiểm tra lỗi khi xoay
                if (kneeAngle < thresholds.getKneeThresholds()[0]) {
                    displayText[0] = true;
                    incorrectPosture = true;
                    feedbackList.add("Giữ gối ở góc 90°");
                } else if (kneeAngle > thresholds.getKneeThresholds()[1]) {
                    displayText[1] = true;
                    incorrectPosture = true;
                    feedbackList.add("Gập gối nhiều hơn");
                }
                
                // Đếm rep khi complete
                if (complete) {
                    if (incorrectPosture) {
                        incorrectCount++;
                        message = "INCORRECT";
                    } else {
                        correctCount++;
                        message = "CORRECT";
                    }
                }
                stateSequence.clear();
                incorrectPosture = false;
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
            feedback.setCurrentState(currState + " | Hip: " + hipAngle + " | Knee: " + kneeAngle + "° | Ratio: " + String.format("%.2f", ratio));

            return feedback;
        }

        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
                correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }

    @Override
    public String getExerciseType() {
        return "situptwist";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 23, 24, 25, 26, 27, 28, 31, 32};
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            SitUpTwistThresholds proThresholds = SitUpTwistThresholds.defaultPro();
            result.put("hipThresholds", proThresholds.getHipThresholds());
            result.put("kneeThresholds", proThresholds.getKneeThresholds());
            result.put("ratioThreshold", proThresholds.getRatioThreshold());
            result.put("offsetThresh", proThresholds.getOffsetThresh());
            result.put("inactiveThresh", proThresholds.getInactiveThresh());
            result.put("cntFrameThresh", proThresholds.getCntFrameThresh());
        } else {
            result.put("hipThresholds", thresholds.getHipThresholds());
            result.put("kneeThresholds", thresholds.getKneeThresholds());
            result.put("ratioThreshold", thresholds.getRatioThreshold());
            result.put("offsetThresh", thresholds.getOffsetThresh());
            result.put("inactiveThresh", thresholds.getInactiveThresh());
            result.put("cntFrameThresh", thresholds.getCntFrameThresh());
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("hipThresholds")) {
            this.thresholds.setHipThresholds((int[]) thresholds.get("hipThresholds"));
        }
        if (thresholds.containsKey("kneeThresholds")) {
            this.thresholds.setKneeThresholds((int[]) thresholds.get("kneeThresholds"));
        }
        if (thresholds.containsKey("ratioThreshold")) {
            this.thresholds.setRatioThreshold((float[]) thresholds.get("ratioThreshold"));
        }
        if (thresholds.containsKey("offsetThresh")) {
            this.thresholds.setOffsetThresh((Integer) thresholds.get("offsetThresh"));
        }
        if (thresholds.containsKey("inactiveThresh")) {
            this.thresholds.setInactiveThresh((Double) thresholds.get("inactiveThresh"));
        }
        if (thresholds.containsKey("cntFrameThresh")) {
            this.thresholds.setCntFrameThresh((Integer) thresholds.get("cntFrameThresh"));
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

        float[] v1 = {0f, -1f};
        float[] v2 = {to.get("x") - from.get("x"), to.get("y") - from.get("y")};
        float dot = v1[0] * v2[0] + v1[1] * v2[1];
        float norm1 = (float) Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        float norm2 = (float) Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);
        float cosTheta = Math.max(-1f, Math.min(1f, dot / (norm1 * norm2)));
        double theta = Math.acos(cosTheta);
        return (int) Math.toDegrees(theta);
    }

    private String getState(int hipAngle, float ratio) {
        // s1: Down (nằm xuống) - hip angle lớn
        if (hipAngle > thresholds.getHipThresholds()[1]) {
            return "s1";
        }
        
        // s2 & s3: Up (ngồi lên) - hip angle nhỏ
        // Phân biệt s2 (center) vs s3 (twist) bằng ratio
        if (hipAngle < thresholds.getHipThresholds()[0]) {
            if (ratio < thresholds.getRatioThreshold()[0]) {
                return "s2";  // Up Center - chưa xoay (ratio nhỏ)
            } else if (ratio > thresholds.getRatioThreshold()[1]) {
                return "s3";  // Twist - đã xoay (ratio lớn)
            }
        }
        
        return prevState; // Giữ nguyên state nếu đang transition
    }

    private void updateStateSequence(String state) {
        if (state == null) return;
        
        if ("s1".equals(state) && stateSequence.isEmpty()) {
            stateSequence.add(state);
        } else if ("s2".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s1")) {
                stateSequence.add(state);
            }
        } else if ("s3".equals(state)) {
            if (!stateSequence.contains(state) && (stateSequence.contains("s1") || stateSequence.contains("s2"))) {
                stateSequence.add(state);
            }
        }
    }

    // Inner class for SitUpTwistThresholds
    public static class SitUpTwistThresholds {
        private int[] hipThresholds;      // [min, max] để phân biệt nằm/ngồi
        private int[] kneeThresholds;     // [min, max] góc knee
        private float[] ratioThreshold;   // [min, max] để phân biệt center/twist
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public SitUpTwistThresholds() {}

        public SitUpTwistThresholds(int[] hipThresholds, int[] kneeThresholds, float[] ratioThreshold,
                                   int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.hipThresholds = hipThresholds;
            this.kneeThresholds = kneeThresholds;
            this.ratioThreshold = ratioThreshold;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static SitUpTwistThresholds defaultBeginner() {
            return new SitUpTwistThresholds(
                    new int[]{60, 140},
                    new int[]{50, 110},
                    new float[]{0.3f, 0.45f},
                    45, 15.0, 50
            );
        }

        public static SitUpTwistThresholds defaultPro() {
            return new SitUpTwistThresholds(
                    new int[]{50, 150},
                    new int[]{55, 105},
                    new float[]{0.3f, 0.5f},
                    45, 15.0, 50
            );
        }

        // Getters and Setters
        public int[] getHipThresholds() {
            return hipThresholds;
        }

        public void setHipThresholds(int[] hipThresholds) {
            this.hipThresholds = hipThresholds;
        }

        public int[] getKneeThresholds() {
            return kneeThresholds;
        }

        public void setKneeThresholds(int[] kneeThresholds) {
            this.kneeThresholds = kneeThresholds;
        }

        public float[] getRatioThreshold() {
            return ratioThreshold;
        }

        public void setRatioThreshold(float[] ratioThreshold) {
            this.ratioThreshold = ratioThreshold;
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

