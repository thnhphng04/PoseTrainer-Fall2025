package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RussianTwist Analyzer - Phân tích bài tập Push-Up
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class RussianTwistAnalyzer implements ExerciseAnalyzerInterface {

    private RussianTwistThresholds thresholds;
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

    public RussianTwistAnalyzer() {
        this.thresholds = RussianTwistThresholds.defaultBeginner();
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

    public RussianTwistAnalyzer(RussianTwistThresholds thresholds) {
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
        Map<String, Float> leftEar = getLandmark(landmarks, 7);
        Map<String, Float> rightEar = getLandmark(landmarks, 8);
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
        Map<String, Float> leftFoot = getLandmark(landmarks, 31);
        Map<String, Float> rightFoot = getLandmark(landmarks, 32);

        // Tính offset angle để phát hiện lệch camera
        offsetAngle = calculateOffsetAngle(leftShoulder, nose, rightShoulder);
        int positionCheck = calculateAngleWithUpVertical(leftAnkle, leftShoulder);
        cameraWarning = positionCheck < 45;

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
                leftElbow.getOrDefault("visibility", 0f) +
                leftHip.getOrDefault("visibility", 0f) +
                leftKnee.getOrDefault("visibility", 0f)
            ) / 4.0f;
            
            float rightAvgVis = (
                rightShoulder.getOrDefault("visibility", 0f) +
                rightElbow.getOrDefault("visibility", 0f) +
                rightHip.getOrDefault("visibility", 0f) +
                rightKnee.getOrDefault("visibility", 0f)
            ) / 4.0f;

            List<Map<String, Float>> points;
            if (leftAvgVis > rightAvgVis) {
                // Bên trái nhìn rõ hơn
                points = Arrays.asList(
                        leftEar, leftShoulder, leftElbow, leftWrist,
                        leftHip, leftKnee, leftAnkle, leftFoot
                );
            } else {
                // Bên phải nhìn rõ hơn
                points = Arrays.asList(
                        rightEar, rightShoulder, rightElbow, rightWrist,
                        rightHip, rightKnee, rightAnkle, rightFoot
                );
            }

            Map<String, Float> ear = points.get(0);
            Map<String, Float> shldr = points.get(1);
            Map<String, Float> elbow = points.get(2);
            Map<String, Float> wrist = points.get(3);
            Map<String, Float> hip = points.get(4);
            Map<String, Float> knee = points.get(5);
            Map<String, Float> ankle = points.get(6);
            Map<String, Float> foot = points.get(7);


            // Tính các góc mới
            int hipAngle = calculateAngle(nose, hip, knee);
            int kneeAngle = calculateAngle(hip, knee, ankle);
            int earElbowHipAngle = calculateAngle(ear, elbow, hip); // Góc ear-elbow-hip (đỉnh là elbow)


            //tính khoảng cách giữa hai vai
            float dx1 = rightShoulder.get("x") - leftShoulder.get("x");
            float dy1 = rightShoulder.get("y") - leftShoulder.get("y");
            float shoulderWidth = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);

            //tính khoảng cách từ shoulder đến hip (độ dài thân)
            float dx2 = shldr.get("x") - hip.get("x");
            float dy2 = shldr.get("y") - hip.get("y");
            float torsoLength = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

            //
            float ratio = shoulderWidth/torsoLength;


            // State machine
            currState = getState(ratio);
            updateStateSequence(currState);


            // Đếm RussianTwist đúng/sai
            String message = "";
            if ("s1".equals(currState)) { //hiển thị lỗi tư thế ở s1(chỉ hiển thị lỗi chứ không bắt lỗi)
                // Feedback động tác
                if (hipAngle > thresholds.getHipThresholds()[1]) {
                    displayText[1] = true;
                    feedbackList.add("Over-Extended Hips");
                }
                if (kneeAngle < thresholds.getKneeThresholds()[0]) {
                    displayText[2] = true;
                    feedbackList.add("Over-Bent Knees");
                }
                if (kneeAngle > thresholds.getKneeThresholds()[1]) {
                    displayText[3] = true;
                    feedbackList.add("Over-Extended Knees");
                }

            }

            if ("s2".equals(currState)) {
                Boolean complete = stateSequence.contains("s1");
                if (complete) {
                    if (hipAngle < thresholds.getHipThresholds()[0]) {
                        displayText[0] = true;
                        incorrectPosture = true;
                        feedbackList.add("Under-Extended Hips");
                        incorrectCount++;
                        message = "INCORRECT";
                    }
                    if (hipAngle > thresholds.getHipThresholds()[1]) {
                        displayText[1] = true;
                        incorrectPosture = true;
                        feedbackList.add("Over-Extended Hips");
                        incorrectCount++;
                        message = "INCORRECT";
                    }
                    if (kneeAngle < thresholds.getKneeThresholds()[0]) {
                        displayText[2] = true;
                        incorrectPosture = true;
                        feedbackList.add("Over-Bent Knees");
                        incorrectCount++;
                        message = "INCORRECT";
                    }
                    if (kneeAngle > thresholds.getKneeThresholds()[1]) {
                        displayText[3] = true;
                        incorrectPosture = true;
                        feedbackList.add("Over-Extended Knees");
                        incorrectCount++;
                        message = "INCORRECT";
                    }
                }
                if (complete && !incorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
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
            feedback.setCurrentState(currState + " " + String.format("%.2f", ratio) + " " + hipAngle + " " + kneeAngle);

            return feedback;
        }

        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
                correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }

    @Override
    public String getExerciseType() {
        return "russiantwist";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32}; // All required landmarks
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            RussianTwistThresholds proThresholds = RussianTwistThresholds.defaultPro();
            result.put("ratioThreshold", proThresholds.getRatioThreshold());
            result.put("hipThresholds", proThresholds.getHipThresholds());
            result.put("kneeThresholds", proThresholds.getKneeThresholds());
            result.put("offsetThresh", proThresholds.getOffsetThresh());
            result.put("inactiveThresh", proThresholds.getInactiveThresh());
            result.put("cntFrameThresh", proThresholds.getCntFrameThresh());
        } else {
            result.put("ratioThreshold", thresholds.getRatioThreshold());
            result.put("hipThresholds", thresholds.getHipThresholds());
            result.put("kneeThresholds", thresholds.getKneeThresholds());
            result.put("offsetThresh", thresholds.getOffsetThresh());
            result.put("inactiveThresh", thresholds.getInactiveThresh());
            result.put("cntFrameThresh", thresholds.getCntFrameThresh());
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("ratioThreshold")) {
            this.thresholds.setRatioThreshold((float[]) thresholds.get("ratioThreshold"));
        }
        if (thresholds.containsKey("hipThresholds")) {
            this.thresholds.setHipThresholds((int[]) thresholds.get("hipThresholds"));
        }
        if (thresholds.containsKey("kneeThresholds")) {
            this.thresholds.setKneeThresholds((int[]) thresholds.get("kneeThresholds"));
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

    private String getState(float ratio) {
        if (ratio < thresholds.getRatioThreshold()[0]) {
            return "s1";
        } else if(ratio > thresholds.getRatioThreshold()[1]) {
            return "s2";
        }
        return null;
    }

    private void updateStateSequence(String state) {
        if (state == null) return;
        if ("s1".equals(state) && stateSequence.isEmpty()) {
            stateSequence.add(state);
            }
        if ("s2".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s1")) {
                stateSequence.add(state);
            }
        }
    }

    // Inner class for RussianTwistThresholds
    public static class RussianTwistThresholds {
        private float[] ratioThreshold;
        private int[] hipThresholds;
        private int[] kneeThresholds;

        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public RussianTwistThresholds() {}

        public RussianTwistThresholds(float[] ratioThreshold, int[] hipThresholds, int[] kneeThresholds,
                                int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.ratioThreshold = ratioThreshold;
            this.hipThresholds = hipThresholds;
            this.kneeThresholds = kneeThresholds;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static RussianTwistThresholds defaultBeginner() {
            return new RussianTwistThresholds(
                    new float[]{0.3f, 0.45f}, new int[]{60, 100}, new int[]{90, 140},
                    35, 15.0, 50
            );
        }

        public static RussianTwistThresholds defaultPro() {
            return new RussianTwistThresholds(
                    new float[]{0.3f, 0.55f}, new int[]{70, 90}, new int[]{90, 130},
                    35, 15.0, 50
            );
        }

        // Getters and Setters

        public float[] getRatioThreshold() {
            return ratioThreshold;
        }

        public void setRatioThreshold(float[] ratioThreshold) {
            this.ratioThreshold = ratioThreshold;
        }

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
