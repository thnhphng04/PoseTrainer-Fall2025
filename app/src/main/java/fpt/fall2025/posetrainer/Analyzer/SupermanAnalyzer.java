package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Superman Analyzer - Phân tích bài tập Superman
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class SupermanAnalyzer implements ExerciseAnalyzerInterface {

    private SupermanThresholds thresholds;
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

    public SupermanAnalyzer() {
        this.thresholds = SupermanThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[3];
        this.countFrames = new int[3];
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }

    public SupermanAnalyzer(SupermanThresholds thresholds) {
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
            feedbackList.add("Camera lệch, vui lòng chỉnh lại!");
            feedbackList.add("Góc lệch: " + offsetAngle);
            prevState = null;
            currState = null;
            startInactiveTime = now;
            inactiveTime = 0.0;
        } else {
            inactiveTimeFront = 0.0;
            startInactiveTimeFront = now;

            // Chọn bên chân trụ (dựa vào khoảng cách vai-bàn chân)
            float distL = Math.abs(leftFoot.get("y") - leftShoulder.get("y"));
            float distR = Math.abs(rightFoot.get("y") - rightShoulder.get("y"));

            List<Map<String, Float>> points;
            Boolean check = null;
            if (distL > distR) {
                // Sử dụng bên trái
                points = Arrays.asList(
                        leftEar, leftShoulder, leftHip, leftKnee, leftAnkle, leftFoot
                );
                check = leftShoulder.get("x") > leftAnkle.get("x");
            } else {
                // Sử dụng bên phải
                points = Arrays.asList(
                        rightEar, rightShoulder, rightHip, rightKnee, rightAnkle, rightFoot
                );
                check = rightShoulder.get("x") < rightAnkle.get("x");
            }

            Map<String, Float> ear = points.get(0);
            Map<String, Float> shldr = points.get(1);
            Map<String, Float> hip = points.get(2);
            Map<String, Float> knee = points.get(3);
            Map<String, Float> ankle = points.get(4);
            Map<String, Float> foot = points.get(5);

            // Tính các góc
            int hipAngle = calculateAngleContainingUpVertical(shldr, hip, knee);       // Góc shoulder-hip-knee


            // State machine - phân biệt nằm vs ngồi dựa vào hip angle
            currState = getState(hipAngle, check);
            updateStateSequence(currState);

            // Đếm Sit-Up đúng/sai
            String message = "";
            
            // Chỉ hiển thị feedback khi đang ở trạng thái s1 (nằm xuống)
            if ("s1".equals(currState)) {
                // Feedback động tác khi nằm

            }

            // Khi hoàn thành 1 rep (từ s1 -> s2)
            if ("s2".equals(currState)) {
                Boolean complete = stateSequence.contains("s1");
                if (complete) {

                    
                    // Nếu không có lỗi thì đếm là đúng
                    if (!incorrectPosture) {
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
            feedback.setCurrentState(currState + " | Hip: " + hipAngle);

            return feedback;
        }

        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
                correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }

    @Override
    public String getExerciseType() {
        return "Superman";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 23, 24, 25, 26, 27, 28, 31, 32}; // Required landmarks for sit-up
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            SupermanThresholds proThresholds = SupermanThresholds.defaultPro();
            result.put("hipThresholds", proThresholds.getHipThresholds());
            result.put("offsetThresh", proThresholds.getOffsetThresh());
            result.put("inactiveThresh", proThresholds.getInactiveThresh());
            result.put("cntFrameThresh", proThresholds.getCntFrameThresh());
        } else {
            result.put("hipThresholds", thresholds.getHipThresholds());
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

    private int calculateAngleContainingUpVertical(Map<String, Float> p1, Map<String, Float> p2, Map<String, Float> p3) {
        if (p1 == null || p2 == null || p3 == null) return 0;

        // Vector p2→p1 và p2→p3
        float v1x = p1.get("x") - p2.get("x");
        float v1y = p1.get("y") - p2.get("y");
        float v2x = p3.get("x") - p2.get("x");
        float v2y = p3.get("y") - p2.get("y");

        // ⚠ atan2(vx, vy) => 0° hướng xuống (do vy là trục Y dương)
        double a1 = Math.toDegrees(Math.atan2(v1x, v1y));
        double a2 = Math.toDegrees(Math.atan2(v2x, v2y));

        // Chuẩn hóa về [0, 360)
        a1 = (a1 + 360) % 360;
        a2 = (a2 + 360) % 360;

        // 🔼 Up vertical = 180° (trục Y âm)
        double up = 180.0;

        // Tính góc theo chiều kim đồng hồ từ a1 → a2
        double diff = (a2 - a1 + 360) % 360;

        // Kiểm tra xem 180° có nằm trong cung a1→a2 hay không
        boolean containsUp;
        if (a1 <= a2) {
            containsUp = (up >= a1 && up <= a2);
        } else {
            // Cung đi qua 0°
            containsUp = (up >= a1 || up <= a2);
        }

        // Nếu không chứa hướng lên → lấy phần bù
        if (!containsUp) {
            diff = 360 - diff;
        }

        return (int) diff;
    }


    private String getState(int hipAngle, Boolean check) {
        if (hipAngle > thresholds.getHipThresholds()[1] && check) {
            return "s1";
        } else if (hipAngle < thresholds.getHipThresholds()[0] && check) {
            return "s2";
        }
        return prevState;
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

    // Inner class for SupermanThresholds
    public static class SupermanThresholds {
        private int[] hipThresholds;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public SupermanThresholds() {}

        public SupermanThresholds(int[] hipThresholds,
                              int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.hipThresholds = hipThresholds;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static SupermanThresholds defaultBeginner() {
            return new SupermanThresholds(
                    new int[]{160, 170},
                    45, 15.0, 50
            );
        }

        public static SupermanThresholds defaultPro() {
            return new SupermanThresholds(
                    new int[]{160, 170},
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
