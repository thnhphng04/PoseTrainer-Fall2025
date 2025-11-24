package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LegRaise Analyzer - Phân tích bài tập Leg Raise
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 * 3 States: s1 (down) → s2 (raising) → s3 (up) → s2 (lowering) → s1 (down)
 * Sequence: [s2, s3, s2] → size=3, đếm khi về s1
 */
public class FloorTricepDipAnalyzer implements ExerciseAnalyzerInterface {

    private FloorTricepDipThresholds thresholds;
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

    public FloorTricepDipAnalyzer() {
        this.thresholds = FloorTricepDipThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[2];
        this.countFrames = new int[2];
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }

    public FloorTricepDipAnalyzer(FloorTricepDipThresholds thresholds) {
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
        cameraWarning = offsetAngle > thresholds.getOffsetThresh() || positionCheck < 45;

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
                        leftEar, leftShoulder, leftHip, leftKnee, leftAnkle, leftFoot, leftElbow, leftWrist
                );
            } else {
                // Bên phải nhìn rõ hơn
                points = Arrays.asList(
                        rightEar, rightShoulder, rightHip, rightKnee, rightAnkle, rightFoot, rightElbow, rightWrist
                );
            }

            Map<String, Float> ear = points.get(0);
            Map<String, Float> shldr = points.get(1);
            Map<String, Float> hip = points.get(2);
            Map<String, Float> knee = points.get(3);
            Map<String, Float> ankle = points.get(4);
            Map<String, Float> foot = points.get(5);
            Map<String, Float> elbow = points.get(6);
            Map<String, Float> wrist = points.get(7);

            // Tính các góc
            int hipAngle = calculateAngle(shldr, hip, ankle);
            int kneeAngle = calculateAngle(hip, knee, ankle);
            int elbowAngle = calculateAngle(shldr, elbow, wrist);

            float hipY = hip.get("y");
            float ankleY = ankle.get("y");
            float footLength = Math.abs(foot.get("x") - ankle.get("x"));


            // State machine
            currState = getState(elbowAngle, hipY, ankleY, footLength);
            updateStateSequence(currState);

            // Đếm Leg Raise đúng/sai
            String message = "";
            if ("s1".equals(currState) && stateSequence.size() == 1) {
                if (hipY > (ankleY - footLength*thresholds.getHeightRatio())){
                    displayText[0] = true;
                    incorrectPosture = true;
                    feedbackList.add("Hông hạ quá thấp!");
                }

                if (!incorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
                } else {
                    incorrectCount++;
                    message = "INCORRECT";
                }
                stateSequence.clear();
                incorrectPosture = false;
            }
            if ("s1".equals(currState)) {
                if (hipY > (ankleY - footLength*thresholds.getHeightRatio())){
                    displayText[0] = true;
                    feedbackList.add("Hông hạ quá thấp!");
                }
                if (kneeAngle > thresholds.getKneeThreshold()) {
                    displayText[1] = true;
                    feedbackList.add("Gập gối nhiều hơn");
                }

            } else if ("s2".equals(currState)) {
                if (kneeAngle > thresholds.getKneeThreshold()) {
                    displayText[1] = true;
                    incorrectPosture = true;
                    feedbackList.add("Gập gối nhiều hơn");
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
            feedback.setCurrentState(currState + " | Hip: " + hipAngle + "° | Knee: " + kneeAngle + "°" + (hipY > (ankleY - footLength*thresholds.getHeightRatio())));

            return feedback;
        }

        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
                correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }

    @Override
    public String getExerciseType() {
        return "floortricepdip";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 23, 24, 25, 26, 27, 28, 31, 32}; // Required landmarks for leg raise
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            FloorTricepDipThresholds proThresholds = FloorTricepDipThresholds.defaultPro();
            result.put("elbowThreshold", proThresholds.getElbowThreshold());
            result.put("kneeThreshold", proThresholds.getKneeThreshold());
            result.put("heightRatio", proThresholds.getHeightRatio());
            result.put("offsetThresh", proThresholds.getOffsetThresh());
            result.put("inactiveThresh", proThresholds.getInactiveThresh());
            result.put("cntFrameThresh", proThresholds.getCntFrameThresh());
        } else {
            result.put("elbowThreshold", thresholds.getElbowThreshold());
            result.put("kneeThreshold", thresholds.getKneeThreshold());
            result.put("heightRatio", thresholds.getHeightRatio());
            result.put("offsetThresh", thresholds.getOffsetThresh());
            result.put("inactiveThresh", thresholds.getInactiveThresh());
            result.put("cntFrameThresh", thresholds.getCntFrameThresh());
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("hipThresholds")) {
            this.thresholds.setElbowThreshold((int) thresholds.get("elbowThreshold"));
        }
        if (thresholds.containsKey("kneeThresholds")) {
            this.thresholds.setKneeThreshold((int) thresholds.get("kneeThreshold"));
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

    private String getState(int elbowAngle, float hipY, float ankleY, float footLength) {
        if (elbowAngle > thresholds.getElbowThreshold()) {
            return "s1";
        } else if (elbowAngle < thresholds.getElbowThreshold() && hipY > (ankleY - footLength*thresholds.getHeightRatio()) ) {
            return "s2";
        }
        return prevState;
    }

    private void updateStateSequence(String state) {
        if (state == null) return;
        if ("s2".equals(state)) {
            if (!stateSequence.contains("s2")) {
                stateSequence.add(state);
            }
        }
    }

    // Inner class for FloorTricepDipThresholds
    public static class FloorTricepDipThresholds {
        private int elbowThreshold;
        private int kneeThreshold;
        private double heightRatio;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public FloorTricepDipThresholds() {}

        public FloorTricepDipThresholds(int elbowThreshold, int kneeThreshold, double heightRatio,
                                 int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.elbowThreshold = elbowThreshold;
            this.kneeThreshold = kneeThreshold;
            this.heightRatio = heightRatio;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static FloorTricepDipThresholds defaultBeginner() {
            return new FloorTricepDipThresholds(
                    160, 100, 1,
                    45, 15.0, 50
            );
        }

        public static FloorTricepDipThresholds defaultPro() {
            return new FloorTricepDipThresholds(
                    160, 95, 1,
                    45, 15.0, 50
            );
        }

        // Getters and Setters

        public int getElbowThreshold() {
            return elbowThreshold;
        }

        public void setElbowThreshold(int elbowThreshold) {
            this.elbowThreshold = elbowThreshold;
        }

        public int getKneeThreshold() {
            return kneeThreshold;
        }

        public void setKneeThreshold(int kneeThreshold) {
            this.kneeThreshold = kneeThreshold;
        }

        public double getHeightRatio() {
            return heightRatio;
        }

        public void setHeightRatio(float heightRatio) {
            this.heightRatio = heightRatio;
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


