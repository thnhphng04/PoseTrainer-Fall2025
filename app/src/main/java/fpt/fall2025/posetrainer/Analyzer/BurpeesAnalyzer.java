package fpt.fall2025.posetrainer.Analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PushUp Analyzer - Phân tích bài tập Push-Up
 * Implement ExerciseAnalyzerInterface để có thể sử dụng chung CameraFragment
 */
public class BurpeesAnalyzer implements ExerciseAnalyzerInterface {

    private BurpeesThresholds thresholds;
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

    public BurpeesAnalyzer() {
        this.thresholds = BurpeesThresholds.defaultBeginner();
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
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

    public BurpeesAnalyzer(BurpeesThresholds thresholds) {
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
            feedbackList.add("CAMERA NOT ALIGNED PROPERLY!!!");
            feedbackList.add("OFFSET ANGLE: " + offsetAngle);
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
            if (distL > distR) {
                // Sử dụng bên trái
                points = Arrays.asList(
                        leftEar, leftShoulder, leftElbow, leftWrist,
                        leftHip, leftKnee, leftAnkle, leftFoot
                );
            } else {
                // Sử dụng bên phải
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
            int elbowAngle = calculateAngle(shldr, elbow, wrist);
            int shldrAngle = calculateAngle(ear, shldr, hip);
            int hipAngle = calculateAngle(shldr, hip, knee);
            int kneeAngle = calculateAngle(hip, knee, ankle);
            int earElbowHipAngle = calculateAngle(ear, elbow, hip); // Góc ear-elbow-hip (đỉnh là elbow)


            float footYs4 = 0; //sử dụng cho s4
            float footY = foot.get("y");
            float kneeY = knee.get("y");

            int positionCheck = calculateAngleWithUpVertical(ankle, shldr);

            // State machine
            currState = getState(elbowAngle, earElbowHipAngle, positionCheck, kneeAngle);
            updateStateSequence(currState);


            // Đếm Burpees đúng/sai
            String message = "";
            if ("s5".equals(currState)) {
                Boolean complete = stateSequence.containsAll(Arrays.asList("s1", "s2", "s3", "s4"));
                if (complete && incorrectPosture) {
                    incorrectCount++;
                    message = "INCORRECT";
                } else if (complete && !incorrectPosture) {
                    if ((footY - footYs4) > (kneeY - footY) / 10) {
                        correctCount++;
                        message = "CORRECT";
                    }
                }
                stateSequence.clear();
                incorrectPosture = false;
            } else if ("s2".equals(currState) || "s3".equals(currState)) { //ở state pushup
                // Feedback động tác
                if (shldrAngle < thresholds.getShldrMin()) {
                    displayText[0] = true;
                    incorrectPosture = true;
                    feedbackList.add("BENT NECK");
                }
                if (hipAngle < thresholds.getHipMin()) {
                    displayText[1] = true;
                    incorrectPosture = true;
                    feedbackList.add("BENT HIP");
                }
                if (kneeAngle < thresholds.getKneeMin()) {
                    displayText[2] = true;
                    incorrectPosture = true;
                    feedbackList.add("BENT KNEE");
                }

                if (earElbowHipAngle >= thresholds.getEarElbowHipTrans()[0] &&
                        earElbowHipAngle <= thresholds.getEarElbowHipTrans()[1] &&
                        stateSequence.stream().filter(s -> s.equals("s2")).count() == 1) {
                    lowerHips = true;
                    feedbackList.add("Continue lowering");
                }
            } else if ("s4".equals(currState)) {
                footYs4 = foot.get("y");
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

            if (stateSequence.contains("s3") || "s1".equals(currState)) {
                lowerHips = false;
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
        return "burpees";
    }

    @Override
    public int[] getRequiredLandmarks() {
        return new int[]{0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32}; // All required landmarks
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if ("pro".equals(level)) {
            BurpeesThresholds proThresholds = BurpeesThresholds.defaultPro();
            result.put("elbowNormal", proThresholds.getElbowNormal());
            result.put("earElbowHipNormal", proThresholds.getEarElbowHipNormal());
            result.put("earElbowHipTrans", proThresholds.getEarElbowHipTrans());
            result.put("earElbowHipPass", proThresholds.getEarElbowHipPass());
            result.put("shldrMin", proThresholds.getShldrMin());
            result.put("hipMin", proThresholds.getHipMin());
            result.put("kneeMin", proThresholds.getKneeMin());
            result.put("kneePreJump", proThresholds.getKneePreJump());
        } else {
            result.put("elbowNormal", thresholds.getElbowNormal());
            result.put("earElbowHipNormal", thresholds.getEarElbowHipNormal());
            result.put("earElbowHipTrans", thresholds.getEarElbowHipTrans());
            result.put("earElbowHipPass", thresholds.getEarElbowHipPass());
            result.put("shldrMin", thresholds.getShldrMin());
            result.put("hipMin", thresholds.getHipMin());
            result.put("kneePreJump", thresholds.getKneePreJump());
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (thresholds.containsKey("shldrMin")) {
            this.thresholds.setShldrMin((Integer) thresholds.get("shldrMin"));
        }
        if (thresholds.containsKey("hipMin")) {
            this.thresholds.setHipMin((Integer) thresholds.get("hipMin"));
        }
        if (thresholds.containsKey("kneeMin")) {
            this.thresholds.setKneeMin((Integer) thresholds.get("kneeMin"));
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

    private String getState(int elbowAngle, int earElbowHipAngle, int positionCheck, int kneeAngle) {
        if (elbowAngle > thresholds.getElbowNormal() &&
                earElbowHipAngle < thresholds.getEarElbowHipNormal() &&
                positionCheck > 60) {
            System.out.println("s1");
            return "s1";
        } else if (earElbowHipAngle >= thresholds.getEarElbowHipTrans()[0] &&
                earElbowHipAngle <= thresholds.getEarElbowHipTrans()[1] &&
                positionCheck > 60) {
            System.out.println("s2");
            return "s2";
        } else if (earElbowHipAngle >= thresholds.getEarElbowHipPass()[0] &&
                earElbowHipAngle <= thresholds.getEarElbowHipPass()[1] &&
                positionCheck > 60) {
            System.out.println("s3");
            return "s3";
        } else if (kneeAngle < thresholds.getKneePreJump() &&
                positionCheck < 45) {
            System.out.println("s4");
            return "s4";
        } else if (kneeAngle >= thresholds.getKneeMin()&&
                elbowAngle > thresholds.getElbowNormal() &&
                positionCheck < 30) {
            System.out.println("s5");
            return "s5";
        }
        return null;
    }

    private void updateStateSequence(String state) {
        if (state == null) return;
        if ("s1".equals(state) && stateSequence.isEmpty()) {
            stateSequence.add(state);
        } else if ("s2".equals(state)) {
            if ((!stateSequence.contains("s3") && stateSequence.stream().filter(s -> s.equals("s2")).count() == 0) ||
                    (stateSequence.contains("s3") && stateSequence.stream().filter(s -> s.equals("s2")).count() == 1)) {
                stateSequence.add(state);
            }
        } else if ("s3".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s2")) {
                stateSequence.add(state);
            }
        } else if ("s4".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s3")) {
                stateSequence.add(state);
            }
        } else if ("s5".equals(state)) {
            if (!stateSequence.contains(state) && stateSequence.contains("s4")) {
                stateSequence.add(state);
            }
        }
    }

    // Inner class for BurpeesThresholds
    public static class BurpeesThresholds {
        private int elbowNormal;
        private int earElbowHipNormal;
        private int[] earElbowHipTrans;
        private int[] earElbowHipPass;
        private int shldrMin;
        private int hipMin;
        private int kneeMin;
        private int kneePreJump;
        private int offsetThresh;
        private double inactiveThresh;
        private int cntFrameThresh;

        public BurpeesThresholds() {
        }

        public BurpeesThresholds(int elbowNormal, int earElbowHipNormal, int[] earElbowHipTrans,
                                 int[] earElbowHipPass, int shldrMin, int hipMin, int kneeMin,
                                 int kneePreJump,
                                 int offsetThresh, double inactiveThresh, int cntFrameThresh) {
            this.elbowNormal = elbowNormal;
            this.earElbowHipNormal = earElbowHipNormal;
            this.earElbowHipTrans = earElbowHipTrans;
            this.earElbowHipPass = earElbowHipPass;
            this.shldrMin = shldrMin;
            this.hipMin = hipMin;
            this.kneeMin = kneeMin;
            this.kneePreJump = kneePreJump;
            this.offsetThresh = offsetThresh;
            this.inactiveThresh = inactiveThresh;
            this.cntFrameThresh = cntFrameThresh;
        }

        public static BurpeesThresholds defaultBeginner() {
            return new BurpeesThresholds(
                    150, 120, new int[]{125, 150}, new int[]{155, 180},
                    120, 160, 150, 120,
                    35, 15.0, 50
            );
        }

        public static BurpeesThresholds defaultPro() {
            return new BurpeesThresholds(
                    150, 120, new int[]{125, 150}, new int[]{155, 180},
                    135, 160, 160, 120,
                    35, 15.0, 50
            );
        }

        public int getElbowNormal() {
            return elbowNormal;
        }

        public void setElbowNormal(int elbowNormal) {
            this.elbowNormal = elbowNormal;
        }

        public int getEarElbowHipNormal() {
            return earElbowHipNormal;
        }

        public void setEarElbowHipNormal(int earElbowHipNormal) {
            this.earElbowHipNormal = earElbowHipNormal;
        }

        public int[] getEarElbowHipTrans() {
            return earElbowHipTrans;
        }

        public void setEarElbowHipTrans(int[] earElbowHipTrans) {
            this.earElbowHipTrans = earElbowHipTrans;
        }

        public int[] getEarElbowHipPass() {
            return earElbowHipPass;
        }

        public void setEarElbowHipPass(int[] earElbowHipPass) {
            this.earElbowHipPass = earElbowHipPass;
        }

        public int getShldrMin() {
            return shldrMin;
        }

        public void setShldrMin(int shldrMin) {
            this.shldrMin = shldrMin;
        }

        public int getHipMin() {
            return hipMin;
        }

        public void setHipMin(int hipMin) {
            this.hipMin = hipMin;
        }

        public int getKneeMin() {
            return kneeMin;
        }

        public void setKneeMin(int kneeMin) {
            this.kneeMin = kneeMin;
        }

        public int getKneePreJump() {
            return kneePreJump;
        }

        public void setKneePreJump(int kneePreJump) {
            this.kneePreJump = kneePreJump;
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