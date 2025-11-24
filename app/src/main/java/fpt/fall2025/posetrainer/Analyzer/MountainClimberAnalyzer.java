package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mountain Climber Analyzer - Phân tích bài tập Mountain Climber
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class MountainClimberAnalyzer implements ExerciseAnalyzerInterface {

    private MountainClimberThresholds thresholds;
    private List<String> nearStateSequence;
    private List<String> farStateSequence;
    private int correctCount;
    private int incorrectCount;
    private boolean nearIncorrectPosture;
    private boolean farIncorrectPosture;
    private String prevState;
    private String currState;
    private String nearCurrState;
    private String farCurrState;
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

    public MountainClimberAnalyzer() {
        this.thresholds = MountainClimberThresholds.defaultBeginner();
        this.nearStateSequence = new ArrayList<>();
        this.farStateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.nearIncorrectPosture = false;
        this.farIncorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.displayText = new boolean[3];
        this.countFrames = new int[3];
        this.lowerHips = false;
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
    }

    public MountainClimberAnalyzer(MountainClimberThresholds thresholds) {
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
        cameraWarning = offsetAngle > thresholds.getOffsetThresh();
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
                        leftHip, leftKnee, leftAnkle, rightHip, rightKnee, rightAnkle
                );
            } else {
                // Bên phải nhìn rõ hơn
                points = Arrays.asList(
                        rightEar, rightShoulder, rightElbow, rightWrist,
                        rightHip, rightKnee, rightAnkle, leftHip, leftKnee, leftAnkle
                );
            }

            Map<String, Float> ear = points.get(0);
            Map<String, Float> shldr = points.get(1);
            Map<String, Float> elbow = points.get(2);
            Map<String, Float> wrist = points.get(3);
            Map<String, Float> nearHip = points.get(4);
            Map<String, Float> nearKnee = points.get(5);
            Map<String, Float> nearAnkle = points.get(6);
            Map<String, Float> farHip = points.get(7);
            Map<String, Float> farKnee = points.get(8);
            Map<String, Float> farAnkle = points.get(9);


            // Tính các góc mới
            int elbowAngle = calculateAngle(shldr, elbow, wrist);
            int backCheck = calculateAngleWithDownVertical(shldr, nearHip);
            int positionCheck = calculateAngleWithUpVertical(nearAnkle, shldr);

            int nearKneeAngle = calculateAngle(nearHip, nearKnee, nearAnkle);
            int farKneeAngle = calculateAngle(farHip, farKnee, farAnkle);



            // State machine
            nearCurrState = getState(elbowAngle, positionCheck, nearKneeAngle);
            farCurrState = getState(elbowAngle, positionCheck, farKneeAngle);


            updateNearStateSequence(nearCurrState);
            updateFarStateSequence(farCurrState);
            // Đếm mountain climber đúng/sai cho từng chân riêng biệt
            String message = "";

            // Kiểm tra chân near
            if ("s3".equals(nearCurrState)) {
                // Kiểm tra nếu đã hoàn thành chu kỳ đầy đủ (có s2 và s3)
                boolean nearComplete = nearStateSequence.contains("s2");
                if (nearComplete && backCheck > thresholds.getBackNormal()) {
                    displayText[0] = true;
                    nearIncorrectPosture = true;
                    feedbackList.add("Hông nâng quá cao");
                    incorrectCount++;
                    message = "INCORRECT";
                }
                else if (nearComplete && !nearIncorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
                }
                nearStateSequence.clear();
                nearIncorrectPosture = false;
            }

            // Kiểm tra chân far
            if ("s3".equals(farCurrState)) {
                // Kiểm tra nếu đã hoàn thành chu kỳ đầy đủ (có s2 và s3)
                boolean farComplete = farStateSequence.contains("s2");
                if (farComplete && backCheck > thresholds.getBackNormal()) {
                    displayText[0] = true;
                    farIncorrectPosture = true;
                    feedbackList.add("Hông nâng quá cao");
                    incorrectCount++;
                    message = "INCORRECT";
                }
                else if (farComplete && !farIncorrectPosture) {
                    correctCount++;
                    message = "CORRECT";
                }

                farStateSequence.clear();
                farIncorrectPosture = false;
            }


            /*
            // Kiểm tra chân near
            if ("s1".equals(nearCurrState)) {
                // Kiểm tra nếu đã hoàn thành chu kỳ đầy đủ (có s2 và s3)
                boolean nearComplete = nearStateSequence.contains("s2") && nearStateSequence.contains("s3");
                if (nearComplete && !nearIncorrectPosture) {
                    correctCount++;
                    repCompleted = true;
                    message = "CORRECT";
                } else if (nearIncorrectPosture) {
                    incorrectCount++;
                    message = "INCORRECT";
                }
                nearStateSequence.clear();
                nearIncorrectPosture = false;
            }
            else if ("s2".equals(nearCurrState) || "s3".equals(nearCurrState)) { //đang trong quá trình thực hiện
                // Feedback động tác
                if (backCheck > thresholds.getBackNormal()) {
                    displayText[0] = true;
                    nearIncorrectPosture = true;
                    feedbackList.add("Hông nâng quá cao");
                }
            }
            
            // Kiểm tra chân far
            if ("s1".equals(farCurrState)) {
                // Kiểm tra nếu đã hoàn thành chu kỳ đầy đủ (có s2 và s3)
                boolean farComplete = farStateSequence.contains("s2") && farStateSequence.contains("s3");
                if (farComplete && !farIncorrectPosture) {
                    correctCount++;
                    repCompleted = true;
                    message = "CORRECT";
                } else if (farIncorrectPosture && !repCompleted) {
                    incorrectCount++;
                    message = "INCORRECT";
                }
                farStateSequence.clear();
                farIncorrectPosture = false;
            }
            else if ("s2".equals(farCurrState) || "s3".equals(farCurrState)) { //đang trong quá trình thực hiện
                // Feedback động tác
                if (backCheck > thresholds.getBackNormal()) {
                    displayText[0] = true;
                    farIncorrectPosture = true;
                    feedbackList.add("Hông nâng quá cao");
                }
            }
             */

            // Inactivity logic - kiểm tra nếu cả hai chân đều không thay đổi
            boolean bothStatesUnchanged = (nearCurrState != null && nearCurrState.equals(prevState)) && 
                                        (farCurrState != null && farCurrState.equals(prevState));
            
            if (bothStatesUnchanged) {
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
            // Tạo state string kết hợp cho cả hai chân
            String combinedState = "Near:" + (nearCurrState != null ? nearCurrState : "null") +
                                 " Far:" + (farCurrState != null ? farCurrState : "null");
            System.out.println(combinedState);
            feedback.setCurrentState(combinedState);

            return feedback;

        }

        // Nếu lệch camera, trả về feedback cảnh báo
        return new ExerciseFeedback(
                correctCount, incorrectCount, "", cameraWarning, offsetAngle, new ArrayList<>(feedbackList)
        );
    }

    @Override
    public String getExerciseType() {
        return "mountainclimber";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32}; // All required landmarks
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            MountainClimberThresholds proThresholds = MountainClimberThresholds.defaultPro();
            result.put("elbowNormal", proThresholds.getElbowNormal());
            result.put("backNormal", proThresholds.getBackNormal());
            result.put("kneeNormal", proThresholds.getKneeNormal());
            result.put("kneeTrans", proThresholds.getKneeTrans());
            result.put("kneePass", proThresholds.getKneePass());
            result.put("offsetThresh", proThresholds.getOffsetThresh());
            result.put("inactiveThresh", proThresholds.getInactiveThresh());
            result.put("cntFrameThresh", proThresholds.getCntFrameThresh());
        } else {
            result.put("elbowNormal", thresholds.getElbowNormal());
            result.put("backNormal", thresholds.getBackNormal());
            result.put("kneeNormal", thresholds.getKneeNormal());
            result.put("kneeTrans", thresholds.getKneeTrans());
            result.put("kneePass", thresholds.getKneePass());
            result.put("offsetThresh", thresholds.getOffsetThresh());
            result.put("inactiveThresh", thresholds.getInactiveThresh());
            result.put("cntFrameThresh", thresholds.getCntFrameThresh());
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("elbowNormal")) {
            this.thresholds.setElbowNormal((Integer) thresholds.get("elbowNormal"));
        }
        if (thresholds.containsKey("backNormal")) {
            this.thresholds.setBackNormal((Integer) thresholds.get("backNormal"));
        }
        if (thresholds.containsKey("kneeNormal")) {
            this.thresholds.setKneeNormal((Integer) thresholds.get("kneeNormal"));
        }
        if (thresholds.containsKey("kneeTrans")) {
            this.thresholds.setKneeTrans((int[]) thresholds.get("kneeTrans"));
        }
        if (thresholds.containsKey("kneePass")) {
            this.thresholds.setKneePass((Integer) thresholds.get("kneePass"));
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
        this.nearIncorrectPosture = false;
        this.farIncorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.nearCurrState = null;
        this.farCurrState = null;
        this.nearStateSequence.clear();
        this.farStateSequence.clear();
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

    private int calculateAngleWithDownVertical(Map<String, Float> from, Map<String, Float> to) {
        if (from == null || to == null) return 0;

        float[] v1 = {0f, 1f}; // vector thẳng đứng hướng xuống
        float[] v2 = {to.get("x") - from.get("x"), to.get("y") - from.get("y")};

        float dot = v1[0] * v2[0] + v1[1] * v2[1];
        float norm1 = (float) Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        float norm2 = (float) Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);

        if (norm2 == 0) return 0; // tránh chia cho 0

        float cosTheta = Math.max(-1f, Math.min(1f, dot / (norm1 * norm2)));
        double theta = Math.acos(cosTheta);

        return (int) Math.toDegrees(theta);
    }

    private String getState(int elbowAngle, int positionCheck, int kneeAngle) {
        if (elbowAngle > thresholds.getElbowNormal() &&
            positionCheck > 60 &&
            kneeAngle > thresholds.getKneeNormal()) {
            return "s1";
        } else if ((kneeAngle <= thresholds.getKneeTrans()[0] && kneeAngle >= thresholds.getKneeTrans()[1]) &&
                    positionCheck > 60) {
            return "s2";
        } else if (kneeAngle <= thresholds.getKneePass() &&
                positionCheck > 60) {
            return "s3";
        }
        return null;
    }

    private void updateNearStateSequence(String state) {
        if (state == null) return;
        if ("s2".equals(state)) {
            if ((!nearStateSequence.contains("s3") && nearStateSequence.stream().filter(s -> s.equals("s2")).count() == 0) ||
                    (nearStateSequence.contains("s3") && nearStateSequence.stream().filter(s -> s.equals("s2")).count() == 1)) {
                nearStateSequence.add(state);
            }
        } else if ("s3".equals(state)) {
            if (!nearStateSequence.contains(state) && nearStateSequence.contains("s2")) {
                nearStateSequence.add(state);
            }
        }
    }

    private void updateFarStateSequence(String state) {
        if (state == null) return;
        if ("s2".equals(state)) {
            if ((!farStateSequence.contains("s3") && farStateSequence.stream().filter(s -> s.equals("s2")).count() == 0) ||
                    (farStateSequence.contains("s3") && farStateSequence.stream().filter(s -> s.equals("s2")).count() == 1)) {
                farStateSequence.add(state);
            }
        } else if ("s3".equals(state)) {
            if (!farStateSequence.contains(state) && farStateSequence.contains("s2")) {
                farStateSequence.add(state);
            }
        }
    }

    // Inner class for MountainClimberThresholds
    public static class MountainClimberThresholds {
        private int elbowNormal;
        private int backNormal;
        private int kneeNormal;
        private int[] kneeTrans;
        private int kneePass;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public MountainClimberThresholds() {}

        public MountainClimberThresholds(int elbowNormal, int backNormal, int kneeNormal, int[] kneeTrans,
                                int kneePass,
                                int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.elbowNormal = elbowNormal;
            this.backNormal = backNormal;
            this.kneeNormal = kneeNormal;
            this.kneeTrans = kneeTrans;
            this.kneePass = kneePass;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static MountainClimberThresholds defaultBeginner() {
            return new MountainClimberThresholds(
                    150, 95, 150, new int[]{145, 90}, 80,
                    45, 15.0, 50
            );
        }

        public static MountainClimberThresholds defaultPro() {
            return new MountainClimberThresholds(
                    150, 85, 150, new int[]{145, 80}, 70,
                    45, 15.0, 50
            );
        }

        // Getters and Setters

        public int getElbowNormal() {
            return elbowNormal;
        }

        public void setElbowNormal(int elbowNormal) {
            this.elbowNormal = elbowNormal;
        }

        public int getBackNormal() {
            return backNormal;
        }

        public void setBackNormal(int backNormal) {
            this.backNormal = backNormal;
        }

        public int getKneeNormal() {
            return kneeNormal;
        }

        public void setKneeNormal(int kneeNormal) {
            this.kneeNormal = kneeNormal;
        }

        public int[] getKneeTrans() {
            return kneeTrans;
        }

        public void setKneeTrans(int[] kneeTrans) {
            this.kneeTrans = kneeTrans;
        }

        public int getKneePass() {
            return kneePass;
        }

        public void setKneePass(int kneePass) {
            this.kneePass = kneePass;
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
