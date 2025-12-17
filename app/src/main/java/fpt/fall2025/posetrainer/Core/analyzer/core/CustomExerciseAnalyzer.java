package fpt.fall2025.posetrainer.Core.analyzer.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomExerciseAnalyzer - Phân tích bài tập tùy chỉnh dựa trên config từ Gemini
 * Config được lưu trong Exercise.MediaPipe.config
 */
public class CustomExerciseAnalyzer implements ExerciseAnalyzerInterface {

    private Map<String, Object> config;
    private List<String> stateSequence;
    private int correctCount;
    private int incorrectCount;
    private boolean incorrectPosture;
    private String prevState;
    private String currState;
    private double inactiveTime;
    private double inactiveTimeFront;
    private double startInactiveTime;
    private double startInactiveTimeFront;
    private boolean cameraWarning;
    private int offsetAngle;
    private List<String> feedbackList;
    private boolean repCountedForCurrentCycle; // Track xem đã đếm rep cho chu kỳ hiện tại chưa

    // Thresholds từ config (defaults được tăng để dễ detect hơn và ít khắt khe hơn)
    private int offsetThresh = 45; // Tăng từ 30 lên 45 để camera ít warning hơn
    private double inactiveThresh = 8.0; // Tăng từ 5.0 lên 8.0 để cho phép nhiều thời gian hơn
    private int cntFrameThresh = 2; // Giảm từ 3 xuống 2 để detect nhanh hơn

    private static final String TAG = "CustomExerciseAnalyzer";

    public CustomExerciseAnalyzer(Map<String, Object> config) {
        android.util.Log.d(TAG, "Initializing CustomExerciseAnalyzer");
        android.util.Log.d(TAG, "Config is null: " + (config == null));
        
        if (config != null) {
            android.util.Log.d(TAG, "Config keys: " + config.keySet());
            android.util.Log.d(TAG, "Config size: " + config.size());
        }
        
        this.config = config;
        this.stateSequence = new ArrayList<>();
        this.correctCount = 0;
        this.incorrectCount = 0;
        this.incorrectPosture = false;
        this.prevState = null;
        this.currState = null;
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
        this.feedbackList = new ArrayList<>();
        this.repCountedForCurrentCycle = false;

        // Load thresholds from config
        if (config != null && config.containsKey("thresholds")) {
            Object thresholdsObj = config.get("thresholds");
            android.util.Log.d(TAG, "Thresholds found, type: " + (thresholdsObj != null ? thresholdsObj.getClass().getName() : "null"));
            
            if (thresholdsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> thresholds = (Map<String, Object>) thresholdsObj;
                android.util.Log.d(TAG, "Thresholds keys: " + thresholds.keySet());
                
                if (thresholds.containsKey("offsetThresh")) {
                    Object val = thresholds.get("offsetThresh");
                    if (val instanceof Number) {
                        int loadedThresh = ((Number) val).intValue();
                        // Đảm bảo tối thiểu 45° để tránh warning quá nhiều và dễ thực hiện hơn
                        offsetThresh = Math.max(45, loadedThresh);
                        android.util.Log.d(TAG, "offsetThresh loaded: " + loadedThresh + ", using: " + offsetThresh);
                    }
                }
                if (thresholds.containsKey("inactiveThresh")) {
                    Object val = thresholds.get("inactiveThresh");
                    if (val instanceof Number) {
                        inactiveThresh = ((Number) val).doubleValue();
                        android.util.Log.d(TAG, "inactiveThresh loaded: " + inactiveThresh);
                    }
                }
                if (thresholds.containsKey("cntFrameThresh")) {
                    Object val = thresholds.get("cntFrameThresh");
                    if (val instanceof Number) {
                        cntFrameThresh = ((Number) val).intValue();
                        android.util.Log.d(TAG, "cntFrameThresh loaded: " + cntFrameThresh);
                    }
                }
            } else {
                android.util.Log.e(TAG, "Thresholds is not a Map, cannot load");
            }
        } else {
            android.util.Log.w(TAG, "No thresholds found in config, using defaults");
        }
        
        // Load state machine from config
        if (config != null && config.containsKey("stateMachine")) {
            Object stateMachineObj = config.get("stateMachine");
            android.util.Log.d(TAG, "StateMachine found, type: " + (stateMachineObj != null ? stateMachineObj.getClass().getName() : "null"));
        } else {
            android.util.Log.e(TAG, "No stateMachine found in config!");
        }
        
        // FIX: Validate and auto-fix config issues
        if (config != null) {
            validateAndFixConfig(config);
        }
        
        android.util.Log.d(TAG, "CustomExerciseAnalyzer initialized with config: " + (config != null ? "YES" : "NO"));
    }
    
    /**
     * Validate and auto-fix config issues:
     * 1. Expand bodyAngle ranges if too narrow
     * 2. Fix elbow angle overlaps
     * 3. Adjust ranges to be more realistic
     */
    private void validateAndFixConfig(Map<String, Object> config) {
        try {
            if (!config.containsKey("states")) {
                return;
            }
            
            Object statesObj = config.get("states");
            if (!(statesObj instanceof Map)) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> states = (Map<String, Object>) statesObj;
            
            android.util.Log.d(TAG, "=== VALIDATING AND FIXING CONFIG ===");
            
            // Track elbow angle ranges to detect overlaps
            Map<String, double[]> elbowRanges = new HashMap<>();
            
            for (String stateName : states.keySet()) {
                Object stateConfigObj = states.get(stateName);
                if (!(stateConfigObj instanceof Map)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> stateConfig = (Map<String, Object>) stateConfigObj;
                if (!stateConfig.containsKey("conditions")) {
                    continue;
                }
                
                Object conditionsObj = stateConfig.get("conditions");
                if (!(conditionsObj instanceof Map)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) conditionsObj;
                
                for (String conditionName : conditions.keySet()) {
                    Object conditionValue = conditions.get(conditionName);
                    if (!(conditionValue instanceof Map)) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> condition = (Map<String, Object>) conditionValue;
                    
                    Object minObj = condition.get("min");
                    Object maxObj = condition.get("max");
                    
                    if (!(minObj instanceof Number) || !(maxObj instanceof Number)) {
                        continue;
                    }
                    
                    double min = ((Number) minObj).doubleValue();
                    double max = ((Number) maxObj).doubleValue();
                    String nameLower = conditionName.toLowerCase();
                    
                    // FIX 1: Expand bodyAngle ranges if too narrow
                    if (nameLower.contains("body") && nameLower.contains("angle")) {
                        // If range is too narrow (< 15 degrees) or too high (> 165), expand it
                        if (max - min < 15 || min > 165) {
                            double oldMin = min;
                            double oldMax = max;
                            // Expand to [155, 180] for push-up/plank exercises
                            min = Math.min(155, min - 10);
                            max = Math.max(180, max + 5);
                            condition.put("min", min);
                            condition.put("max", max);
                            android.util.Log.w(TAG, "🔧 Fixed bodyAngle in " + stateName + ": [" + oldMin + ", " + oldMax + "] -> [" + min + ", " + max + "]");
                        }
                    }
                    
                    // FIX 2: Track elbow angles to detect overlaps
                    if (nameLower.contains("elbow") && nameLower.contains("angle")) {
                        String elbowKey = stateName + "_" + conditionName;
                        elbowRanges.put(elbowKey, new double[]{min, max});
                    }
                }
            }
            
            // FIX 3: Fix elbow angle overlaps
            fixElbowAngleOverlaps(states, elbowRanges);
            
            android.util.Log.d(TAG, "=== CONFIG VALIDATION COMPLETE ===");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error validating config: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fix elbow angle overlaps by adjusting ranges
     */
    private void fixElbowAngleOverlaps(Map<String, Object> states, Map<String, double[]> elbowRanges) {
        // Group elbow angles by state
        Map<String, List<String>> stateElbows = new HashMap<>();
        for (String key : elbowRanges.keySet()) {
            String[] parts = key.split("_", 2);
            if (parts.length == 2) {
                String stateName = parts[0];
                if (!stateElbows.containsKey(stateName)) {
                    stateElbows.put(stateName, new ArrayList<>());
                }
                stateElbows.get(stateName).add(key);
            }
        }
        
        // Check for overlaps between states
        List<String> stateNames = new ArrayList<>(stateElbows.keySet());
        for (int i = 0; i < stateNames.size(); i++) {
            for (int j = i + 1; j < stateNames.size(); j++) {
                String state1 = stateNames.get(i);
                String state2 = stateNames.get(j);
                
                List<String> elbows1 = stateElbows.get(state1);
                List<String> elbows2 = stateElbows.get(state2);
                
                for (String key1 : elbows1) {
                    for (String key2 : elbows2) {
                        double[] range1 = elbowRanges.get(key1);
                        double[] range2 = elbowRanges.get(key2);
                        
                        if (range1 == null || range2 == null) {
                            continue;
                        }
                        
                        // Check for overlap (with tolerance)
                        double tolerance = 5.0;
                        boolean hasOverlap = !(range1[1] + tolerance < range2[0] - tolerance || 
                                              range2[1] + tolerance < range1[0] - tolerance);
                        
                        if (hasOverlap && Math.abs(range1[0] - range2[0]) < 20) {
                            // Significant overlap detected, adjust ranges
                            String[] parts1 = key1.split("_", 2);
                            String[] parts2 = key2.split("_", 2);
                            
                            if (parts1.length == 2 && parts2.length == 2) {
                                String elbowType1 = parts1[1];
                                String elbowType2 = parts2[1];
                                
                                // If same elbow type (both left or both right), fix overlap
                                if (elbowType1.equals(elbowType2)) {
                                    // Adjust: move one range slightly
                                    if (range1[0] < range2[0]) {
                                        // Move range2 up
                                        double newMin2 = range1[1] + 5;
                                        double newMax2 = range2[1];
                                        if (newMin2 < newMax2) {
                                            updateConditionRange(states, state2, elbowType2, newMin2, newMax2);
                                            android.util.Log.w(TAG, "🔧 Fixed elbow overlap: " + state2 + "." + elbowType2 + 
                                                              " [" + range2[0] + ", " + range2[1] + "] -> [" + newMin2 + ", " + newMax2 + "]");
                                        }
                                    } else {
                                        // Move range1 up
                                        double newMin1 = range2[1] + 5;
                                        double newMax1 = range1[1];
                                        if (newMin1 < newMax1) {
                                            updateConditionRange(states, state1, elbowType1, newMin1, newMax1);
                                            android.util.Log.w(TAG, "🔧 Fixed elbow overlap: " + state1 + "." + elbowType1 + 
                                                              " [" + range1[0] + ", " + range1[1] + "] -> [" + newMin1 + ", " + newMax1 + "]");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update a condition range in a state
     */
    @SuppressWarnings("unchecked")
    private void updateConditionRange(Map<String, Object> states, String stateName, String conditionName, double min, double max) {
        try {
            Object stateConfigObj = states.get(stateName);
            if (!(stateConfigObj instanceof Map)) {
                return;
            }
            
            Map<String, Object> stateConfig = (Map<String, Object>) stateConfigObj;
            if (!stateConfig.containsKey("conditions")) {
                return;
            }
            
            Object conditionsObj = stateConfig.get("conditions");
            if (!(conditionsObj instanceof Map)) {
                return;
            }
            
            Map<String, Object> conditions = (Map<String, Object>) conditionsObj;
            Object conditionValue = conditions.get(conditionName);
            if (!(conditionValue instanceof Map)) {
                return;
            }
            
            Map<String, Object> condition = (Map<String, Object>) conditionValue;
            condition.put("min", min);
            condition.put("max", max);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating condition range: " + e.getMessage(), e);
        }
    }

    @Override
    public ExerciseFeedback analyze(List<Map<String, Float>> landmarks) {
        if (landmarks == null || landmarks.size() < 33) {
            android.util.Log.w(TAG, "Invalid landmarks: null or size < 33");
            return new ExerciseFeedback();
        }
        
        if (config == null) {
            android.util.Log.e(TAG, "Config is NULL in analyze()!");
            ExerciseFeedback feedback = new ExerciseFeedback();
            feedback.setMessage("CONFIG_ERROR");
            feedback.setFeedbackList(new ArrayList<>(Arrays.asList("Config không tồn tại")));
            return feedback;
        }

        // Get required landmarks
        Map<String, Float> nose = getLandmark(landmarks, 0);
        Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
        Map<String, Float> rightShoulder = getLandmark(landmarks, 12);

        // Calculate offset angle
        offsetAngle = calculateOffsetAngle(leftShoulder, nose, rightShoulder);
        // Tăng threshold để dễ detect hơn (chỉ warning khi thực sự lệch nhiều)
        cameraWarning = offsetAngle > offsetThresh;
        
        // Log để debug mỗi frame (giảm frequency)
        if (System.currentTimeMillis() % 1000 < 100) { // Log mỗi giây
            android.util.Log.d(TAG, "Offset: " + offsetAngle + 
                              "°, threshold: " + offsetThresh + 
                              "°, warning: " + cameraWarning);
        }
        
        feedbackList.clear();

        double now = System.nanoTime() / 1e9;
        String message = ""; // Declare message outside the if-else block
        
        if (cameraWarning) {
            // Chỉ warning nghiêm trọng khi offset > 70°, còn lại vẫn cho phép detect state
            // Tăng từ 60° lên 70° để dễ thực hiện hơn
            if (offsetAngle > 70) {
                inactiveTimeFront += now - startInactiveTimeFront;
                startInactiveTimeFront = now;
                if (inactiveTimeFront >= inactiveThresh) {
                    correctCount = 0;
                    incorrectCount = 0;
                    inactiveTimeFront = 0.0;
                }
                feedbackList.add("CAMERA NOT ALIGNED PROPERLY!!!");
                feedbackList.add("OFFSET ANGLE: " + offsetAngle);
                prevState = null;
                currState = null;
                startInactiveTime = now;
                inactiveTime = 0.0;
            } else {
                // Offset vừa phải (45-70°), vẫn cho phép detect nhưng có warning nhẹ
                // Tăng range để dễ thực hiện hơn
                inactiveTimeFront = 0.0;
                startInactiveTimeFront = now;
                // Vẫn tiếp tục detect state
                currState = determineState(landmarks);
                
                // Log state detection mỗi giây
                if (System.currentTimeMillis() % 1000 < 100) {
                    android.util.Log.d(TAG, "Current state: " + (currState != null ? currState : "NULL") + 
                                      ", prevState: " + (prevState != null ? prevState : "NULL"));
                }
                
                // Nếu không detect được state, sử dụng fallback
                if (currState == null) {
                    android.util.Log.w(TAG, "Cannot determine state, using fallback");
                    if (prevState != null) {
                        currState = prevState; // Giữ state cũ
                        android.util.Log.d(TAG, "Using previous state: " + prevState);
                    } else {
                        currState = "preparation"; // Default state
                        android.util.Log.d(TAG, "Using default state: preparation");
                    }
                }
                
                updateStateSequence(currState);
                
                // Count reps - Logic giống SquatAnalyzer: đếm khi quay lại state đầu tiên sau khi đã đi qua các states khác
                List<String> stateSeq = getStateSequence();
                
                // Log state sequence mỗi giây
                if (System.currentTimeMillis() % 1000 < 100) {
                    android.util.Log.d(TAG, "State sequence size: " + stateSeq.size() + ", sequence: " + stateSeq + ", currState: " + currState);
                }
                
                // FIX: Đếm rep - Logic giống SquatAnalyzer CHÍNH XÁC
                // SquatAnalyzer: đếm khi currState == "s1" và stateSequence.size() == 3
                // Sequence trong SquatAnalyzer: s1 -> s2 -> s1 (3 states, quay lại s1)
                // Tương tự: đếm khi currState == firstState VÀ sequence đã quay lại firstState (last element == firstState)
                if (stateSeq != null && stateSeq.size() >= 3 && !repCountedForCurrentCycle) {
                    String firstState = stateSeq.get(0);
                    String lastState = stateSeq.get(stateSeq.size() - 1);
                    
                    // CRITICAL FIX: Chỉ đếm khi:
                    // 1. currState == firstState (đang ở state đầu tiên)
                    // 2. lastState == firstState (sequence đã quay lại state đầu tiên)
                    // 3. Sequence có ít nhất 1 state khác ở giữa (để đảm bảo đã hoàn thành chu kỳ)
                    if (currState != null && currState.equals(firstState) && lastState.equals(firstState)) {
                        // Kiểm tra xem có đi qua ít nhất 1 state khác không (để đảm bảo đã hoàn thành chu kỳ)
                        boolean hasDifferentState = false;
                        for (int i = 1; i < stateSeq.size() - 1; i++) { // Bỏ qua first và last (đều là firstState)
                            if (!stateSeq.get(i).equals(firstState)) {
                                hasDifferentState = true;
                                break;
                            }
                        }
                        
                        // FIX: Chỉ đếm rep khi sequence có size hợp lý (3-4 states) và có state khác ở giữa
                        // Ví dụ hợp lệ: [start, mid1, start] (3 states) hoặc [start, mid1, mid2, start] (4 states)
                        if (hasDifferentState && stateSeq.size() >= 3 && stateSeq.size() <= 4) {
                            if (!incorrectPosture) {
                                correctCount++;
                                message = "CORRECT";
                                android.util.Log.d(TAG, "✅ Rep completed! Correct count: " + correctCount + ", sequence: " + stateSeq);
                            } else {
                                incorrectCount++;
                                message = "INCORRECT";
                                android.util.Log.d(TAG, "❌ Rep completed with errors! Incorrect count: " + incorrectCount + ", sequence: " + stateSeq);
                            }
                            // Mark rep as counted for this cycle
                            repCountedForCurrentCycle = true;
                            // Clear sequence sau khi đếm để bắt đầu rep mới (giống SquatAnalyzer)
                            stateSequence.clear();
                            incorrectPosture = false;
                            prevState = null; // Reset để bắt đầu sequence mới
                        } else if (stateSeq.size() > 4) {
                            // Sequence quá dài, clear để tránh đếm sai
                            android.util.Log.w(TAG, "⚠️ Sequence too long (" + stateSeq.size() + "), clearing to prevent false counting");
                            stateSequence.clear();
                            prevState = null;
                            repCountedForCurrentCycle = false; // Reset flag
                        }
                    }
                } else if (stateSeq != null && stateSeq.size() < 3) {
                    // Sequence quá ngắn, reset flag để có thể đếm rep mới
                    repCountedForCurrentCycle = false;
                }
                // Check form errors BEFORE rep counting to ensure incorrectPosture is set correctly
                checkFormErrors(landmarks);
            }
        } else {
            inactiveTimeFront = 0.0;
            startInactiveTimeFront = now;

            // Determine current state based on config
            currState = determineState(landmarks);
            
            // Log state detection mỗi giây
            if (System.currentTimeMillis() % 1000 < 100) {
                android.util.Log.d(TAG, "Current state: " + (currState != null ? currState : "NULL") + 
                                  ", prevState: " + (prevState != null ? prevState : "NULL"));
            }
            
            // Nếu không detect được state, sử dụng fallback
            if (currState == null) {
                android.util.Log.w(TAG, "Cannot determine state, using fallback");
                if (prevState != null) {
                    currState = prevState; // Giữ state cũ
                    android.util.Log.d(TAG, "Using previous state: " + prevState);
                } else {
                    currState = "preparation"; // Default state
                    android.util.Log.d(TAG, "Using default state: preparation");
                }
            }
            
            updateStateSequence(currState);

            // Count reps - Logic giống SquatAnalyzer: đếm khi quay lại state đầu tiên sau khi đã đi qua các states khác
            List<String> stateSeq = getStateSequence();
            
            // Log state sequence mỗi giây
            if (System.currentTimeMillis() % 1000 < 100) {
                android.util.Log.d(TAG, "State sequence size: " + stateSeq.size() + ", sequence: " + stateSeq + ", currState: " + currState);
            }
            
            // FIX: Đếm rep - Logic giống SquatAnalyzer CHÍNH XÁC
            // SquatAnalyzer: đếm khi currState == "s1" và stateSequence.size() == 3
            // Sequence trong SquatAnalyzer: s1 -> s2 -> s1 (3 states, quay lại s1)
            // Tương tự: đếm khi currState == firstState VÀ sequence đã quay lại firstState (last element == firstState)
            if (stateSeq != null && stateSeq.size() >= 3 && !repCountedForCurrentCycle) {
                String firstState = stateSeq.get(0);
                String lastState = stateSeq.get(stateSeq.size() - 1);
                
                // CRITICAL FIX: Chỉ đếm khi:
                // 1. currState == firstState (đang ở state đầu tiên)
                // 2. lastState == firstState (sequence đã quay lại state đầu tiên)
                // 3. Sequence có ít nhất 1 state khác ở giữa (để đảm bảo đã hoàn thành chu kỳ)
                if (currState != null && currState.equals(firstState) && lastState.equals(firstState)) {
                    // Kiểm tra xem có đi qua ít nhất 1 state khác không (để đảm bảo đã hoàn thành chu kỳ)
                    boolean hasDifferentState = false;
                    for (int i = 1; i < stateSeq.size() - 1; i++) { // Bỏ qua first và last (đều là firstState)
                        if (!stateSeq.get(i).equals(firstState)) {
                            hasDifferentState = true;
                            break;
                        }
                    }
                    
                    // FIX: Chỉ đếm rep khi sequence có size hợp lý (3-4 states) và có state khác ở giữa
                    // Ví dụ hợp lệ: [start, mid1, start] (3 states) hoặc [start, mid1, mid2, start] (4 states)
                    if (hasDifferentState && stateSeq.size() >= 3 && stateSeq.size() <= 4) {
                        if (!incorrectPosture) {
                            correctCount++;
                            message = "CORRECT";
                            android.util.Log.d(TAG, "✅ Rep completed! Correct count: " + correctCount + ", sequence: " + stateSeq);
                        } else {
                            incorrectCount++;
                            message = "INCORRECT";
                            android.util.Log.d(TAG, "❌ Rep completed with errors! Incorrect count: " + incorrectCount + ", sequence: " + stateSeq);
                        }
                        // Mark rep as counted for this cycle
                        repCountedForCurrentCycle = true;
                        // Clear sequence sau khi đếm để bắt đầu rep mới (giống SquatAnalyzer)
                        stateSequence.clear();
                        incorrectPosture = false;
                        prevState = null; // Reset để bắt đầu sequence mới
                    } else if (stateSeq.size() > 4) {
                        // Sequence quá dài, clear để tránh đếm sai
                        android.util.Log.w(TAG, "⚠️ Sequence too long (" + stateSeq.size() + "), clearing to prevent false counting");
                        stateSequence.clear();
                        prevState = null;
                        repCountedForCurrentCycle = false; // Reset flag
                    }
                }
            } else if (stateSeq != null && stateSeq.size() < 3) {
                // Sequence quá ngắn, reset flag để có thể đếm rep mới
                repCountedForCurrentCycle = false;
            }

            // Check form errors BEFORE rep counting to ensure incorrectPosture is set correctly
            checkFormErrors(landmarks);
        }

        ExerciseFeedback feedback = new ExerciseFeedback();
        feedback.setCorrectCount(correctCount);
        feedback.setIncorrectCount(incorrectCount);
        feedback.setMessage(message);
        feedback.setCameraWarning(cameraWarning);
        feedback.setOffsetAngle(offsetAngle);
        feedback.setFeedbackList(new ArrayList<>(feedbackList));
        feedback.setCurrentState(currState);

        // Log feedback values để debug
        if (System.currentTimeMillis() % 1000 < 100) {
            android.util.Log.d(TAG, "=== FEEDBACK CREATED ===");
            android.util.Log.d(TAG, "correctCount: " + correctCount);
            android.util.Log.d(TAG, "incorrectCount: " + incorrectCount);
            android.util.Log.d(TAG, "message: " + message);
            android.util.Log.d(TAG, "currentState: " + currState);
            android.util.Log.d(TAG, "stateSequence size: " + stateSequence.size());
        }

        return feedback;
    }

    private String determineState(List<Map<String, Float>> landmarks) {
        if (config == null) {
            android.util.Log.e(TAG, "Config is null in determineState");
            return null;
        }
        
        if (!config.containsKey("states")) {
            android.util.Log.e(TAG, "Config does not contain 'states' key. Available keys: " + config.keySet());
            return null;
        }

        Object statesObj = config.get("states");
        if (statesObj == null) {
            android.util.Log.e(TAG, "States object is null");
            return null;
        }
        
        if (!(statesObj instanceof Map)) {
            android.util.Log.e(TAG, "States is not a Map, type: " + statesObj.getClass().getName());
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) statesObj;
        
        // Log states mỗi 2 giây để không spam
        if (System.currentTimeMillis() % 2000 < 200) {
            android.util.Log.d(TAG, "Available states: " + states.keySet());
        }

        // Check each state's conditions
        for (String stateName : states.keySet()) {
            try {
                Object stateConfigObj = states.get(stateName);
                if (stateConfigObj == null || !(stateConfigObj instanceof Map)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> stateConfig = (Map<String, Object>) stateConfigObj;
                if (!stateConfig.containsKey("conditions")) {
                    continue;
                }

                Object conditionsObj = stateConfig.get("conditions");
                if (conditionsObj == null || !(conditionsObj instanceof Map)) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) conditionsObj;

                // Đếm số conditions match (không cần tất cả, chỉ cần > 50%)
                int totalConditions = 0;
                int matchedConditions = 0;
                
                for (String conditionName : conditions.keySet()) {
                    try {
                        Object conditionValue = conditions.get(conditionName);
                        if (conditionValue instanceof Map) {
                            totalConditions++;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> condition = (Map<String, Object>) conditionValue;
                            if (checkCondition(landmarks, conditionName, condition)) {
                                matchedConditions++;
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error checking condition " + conditionName + ": " + e.getMessage());
                    }
                }

                // FIX: Giảm yêu cầu match để dễ đạt được hơn (giảm độ khó)
                // Logic: 
                // - 1 condition: phải match 100%
                // - 2 conditions: chỉ cần match >= 50% (1/2) - giảm từ 100%
                // - 3+ conditions: chỉ cần match >= 50% - giảm từ 70%
                boolean allConditionsMet;
                if (totalConditions == 1) {
                    allConditionsMet = matchedConditions == 1; // 100% - bắt buộc
                } else if (totalConditions == 2) {
                    allConditionsMet = matchedConditions >= 1; // >= 50% (1/2) - giảm độ khó
                } else {
                    // >= 50% for 3+ conditions - giảm từ 70% để dễ đạt hơn
                    double matchRatio = (double) matchedConditions / totalConditions;
                    allConditionsMet = matchRatio >= 0.50;
                }

                if (allConditionsMet) {
                    android.util.Log.d(TAG, "✅ State matched: " + stateName + " (" + matchedConditions + "/" + totalConditions + " conditions)");
                    return stateName;
                } else {
                    // Log tại sao không match (chỉ log mỗi 2 giây)
                    if (System.currentTimeMillis() % 2000 < 200) {
                        android.util.Log.d(TAG, "❌ State NOT matched: " + stateName + " (" + matchedConditions + "/" + totalConditions + " conditions)");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error processing state " + stateName + ": " + e.getMessage(), e);
                continue;
            }
        }

        // Nếu không match state nào, log để debug
        if (System.currentTimeMillis() % 2000 < 200) {
            android.util.Log.w(TAG, "⚠️ No state matched! Returning fallback: " + (prevState != null ? prevState : "preparation"));
        }
        
        return prevState != null ? prevState : "preparation";
    }

    private boolean checkCondition(List<Map<String, Float>> landmarks, String conditionName, Map<String, Object> condition) {
        try {
            if (condition == null || landmarks == null) {
                return false;
            }
            
            // Get min/max values
            Object minObj = condition.get("min");
            Object maxObj = condition.get("max");
            
            double min = minObj instanceof Number ? ((Number) minObj).doubleValue() : 0;
            double max = maxObj instanceof Number ? ((Number) maxObj).doubleValue() : 180;
            
            // FIX: Tăng tolerance để giảm độ khó - cho phép người dùng dễ đạt được conditions hơn
            // Tolerance lớn hơn = dễ match hơn = ít bị đếm sai hơn
            double rangeSize = max - min;
            double tolerance;
            if (rangeSize < 20) {
                tolerance = 8.0; // Tăng từ 3.0 lên 8.0 - narrow range cần tolerance lớn hơn
            } else if (rangeSize < 40) {
                tolerance = 12.0; // Tăng từ 5.0 lên 12.0 - medium range
            } else if (rangeSize < 60) {
                tolerance = 15.0; // Tăng từ 7.0 lên 15.0 - wide range
            } else {
                tolerance = 20.0; // Tăng từ 10.0 lên 20.0 - very wide range
            }
            
            double adjustedMin = min - tolerance;
            double adjustedMax = max + tolerance;
            
            // FIX: Special handling for bodyAngle - expand range if too narrow (giảm độ khó)
            String nameLower = conditionName != null ? conditionName.toLowerCase() : "";
            if (nameLower.contains("body") && nameLower.contains("angle")) {
                // Body angle in push-up/plank is typically 155-180, not 170-180
                if (min > 165) {
                    adjustedMin = Math.min(150, min - 20); // Expand downward nhiều hơn
                }
                if (max - min < 25) {
                    adjustedMax = Math.max(180, max + 10); // Expand upward nhiều hơn nếu quá hẹp
                }
            }
            
            // FIX: Mở rộng ranges cho tất cả angles để giảm độ khó
            // Đảm bảo adjusted range đủ rộng để người dùng dễ đạt được
            if (adjustedMax - adjustedMin < 30) {
                // Nếu range vẫn quá hẹp sau khi adjust, mở rộng thêm
                double center = (adjustedMin + adjustedMax) / 2;
                adjustedMin = Math.max(0, center - 20);
                adjustedMax = Math.min(180, center + 20);
            }
            
            // FIX: Ensure adjusted range is valid
            adjustedMin = Math.max(0, adjustedMin);
            adjustedMax = Math.min(180, adjustedMax);

            // Calculate angle based on condition name
            double angle = 0;
            if (conditionName != null && conditionName.toLowerCase().contains("angle")) {
                angle = calculateAngleForCondition(landmarks, conditionName);
            } else if (conditionName != null && conditionName.toLowerCase().contains("distance")) {
                angle = calculateDistanceForCondition(landmarks, conditionName);
            } else {
                // Nếu không match, thử tính angle mặc định
                angle = calculateAngleForCondition(landmarks, conditionName);
            }

            // Log để debug
            if (Math.abs(angle - (min + max) / 2) > 20) {
                android.util.Log.d(TAG, "Condition: " + conditionName + ", angle: " + angle + ", range: [" + adjustedMin + ", " + adjustedMax + "]");
            }

            return angle >= adjustedMin && angle <= adjustedMax;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in checkCondition: " + e.getMessage(), e);
            return false;
        }
    }

    private double calculateAngleForCondition(List<Map<String, Float>> landmarks, String conditionName) {
        if (conditionName == null || landmarks == null) {
            return 0;
        }
        
        String nameLower = conditionName.toLowerCase();
        
        // Hip angle: shoulder-hip-knee
        if (nameLower.contains("hip")) {
            Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
            Map<String, Float> leftHip = getLandmark(landmarks, 23);
            Map<String, Float> leftKnee = getLandmark(landmarks, 25);
            if (isValidLandmark(leftShoulder) && isValidLandmark(leftHip) && isValidLandmark(leftKnee)) {
                return calculateAngle(leftShoulder, leftHip, leftKnee);
            }
        }
        // Knee angle: hip-knee-ankle
        else if (nameLower.contains("knee")) {
            Map<String, Float> leftHip = getLandmark(landmarks, 23);
            Map<String, Float> leftKnee = getLandmark(landmarks, 25);
            Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
            if (isValidLandmark(leftHip) && isValidLandmark(leftKnee) && isValidLandmark(leftAnkle)) {
                return calculateAngle(leftHip, leftKnee, leftAnkle);
            }
        }
        // Shoulder angle: nose-shoulder-hip
        else if (nameLower.contains("shoulder")) {
            Map<String, Float> nose = getLandmark(landmarks, 0);
            Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
            Map<String, Float> leftHip = getLandmark(landmarks, 23);
            if (isValidLandmark(nose) && isValidLandmark(leftShoulder) && isValidLandmark(leftHip)) {
                return calculateAngle(nose, leftShoulder, leftHip);
            }
        }
        // Elbow angle: shoulder-elbow-wrist
        else if (nameLower.contains("elbow")) {
            // Kiểm tra left hoặc right
            Map<String, Float> shoulder, elbow, wrist;
            if (nameLower.contains("left")) {
                shoulder = getLandmark(landmarks, 11);
                elbow = getLandmark(landmarks, 13);
                wrist = getLandmark(landmarks, 15);
            } else if (nameLower.contains("right")) {
                shoulder = getLandmark(landmarks, 12);
                elbow = getLandmark(landmarks, 14);
                wrist = getLandmark(landmarks, 16);
            } else {
                // Default to left
                shoulder = getLandmark(landmarks, 11);
                elbow = getLandmark(landmarks, 13);
                wrist = getLandmark(landmarks, 15);
            }
            if (isValidLandmark(shoulder) && isValidLandmark(elbow) && isValidLandmark(wrist)) {
                return calculateAngle(shoulder, elbow, wrist);
            }
        }
        // Ankle angle: knee-ankle-foot
        else if (nameLower.contains("ankle")) {
            Map<String, Float> leftKnee = getLandmark(landmarks, 25);
            Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
            Map<String, Float> leftFoot = getLandmark(landmarks, 31);
            if (isValidLandmark(leftKnee) && isValidLandmark(leftAnkle) && isValidLandmark(leftFoot)) {
                return calculateAngle(leftKnee, leftAnkle, leftFoot);
            }
        }
        // Wrist angle: elbow-wrist-hand
        else if (nameLower.contains("wrist")) {
            Map<String, Float> leftElbow = getLandmark(landmarks, 13);
            Map<String, Float> leftWrist = getLandmark(landmarks, 15);
            Map<String, Float> leftIndex = getLandmark(landmarks, 19);
            if (isValidLandmark(leftElbow) && isValidLandmark(leftWrist) && isValidLandmark(leftIndex)) {
                return calculateAngle(leftElbow, leftWrist, leftIndex);
            }
        }
        // Body angle: shoulder-hip-ankle (góc nghiêng của cơ thể)
        else if (nameLower.contains("body")) {
            Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
            Map<String, Float> leftHip = getLandmark(landmarks, 23);
            Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
            if (isValidLandmark(leftShoulder) && isValidLandmark(leftHip) && isValidLandmark(leftAnkle)) {
                return calculateAngle(leftShoulder, leftHip, leftAnkle);
            }
        }
        
        // Default: return 0 if no match
        android.util.Log.w(TAG, "Unknown condition name: " + conditionName);
        return 0;
    }
    
    private boolean isValidLandmark(Map<String, Float> landmark) {
        return landmark != null && 
               landmark.containsKey("x") && landmark.containsKey("y") &&
               landmark.get("x") != null && landmark.get("y") != null &&
               landmark.get("x") > 0 && landmark.get("y") > 0;
    }

    private double calculateDistanceForCondition(List<Map<String, Float>> landmarks, String conditionName) {
        // Calculate distance between landmarks
        // Simplified - adjust based on actual needs
        return 0;
    }

    private void checkFormErrors(List<Map<String, Float>> landmarks) {
        try {
            // Reset incorrectPosture at start of each frame check
            // It will be set to true if SEVERE form error is detected (not minor errors)
            boolean hadError = incorrectPosture;
            incorrectPosture = false;
            
            if (config == null || !config.containsKey("feedbackMessages")) {
                return;
            }

            Object feedbackMessagesObj = config.get("feedbackMessages");
            if (feedbackMessagesObj == null || !(feedbackMessagesObj instanceof Map)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> feedbackMessages = (Map<String, Object>) feedbackMessagesObj;
            if (!feedbackMessages.containsKey("formErrors")) {
                return;
            }

            Object formErrorsObj = feedbackMessages.get("formErrors");
            if (formErrorsObj == null || !(formErrorsObj instanceof Map)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> formErrors = (Map<String, Object>) formErrorsObj;

            // Check for common form errors
            // FIX: Chỉ set incorrectPosture = true nếu có LỖI NGHIÊM TRỌNG (severe errors)
            // Lỗi nhẹ chỉ thêm vào feedbackList, không đếm là incorrect
            int severeErrorCount = 0;
            for (String errorKey : formErrors.keySet()) {
                Object errorMessageObj = formErrors.get(errorKey);
                if (errorMessageObj instanceof String) {
                    String errorMessage = (String) errorMessageObj;
                    if (checkFormError(landmarks, errorKey)) {
                        if (!feedbackList.contains(errorMessage)) {
                            feedbackList.add(errorMessage);
                        }
                        
                        // FIX: Chỉ đếm là severe error nếu là lỗi nghiêm trọng
                        // Lỗi nghiêm trọng: sagging (hips quá thấp), insufficient depth (không đủ sâu)
                        // Lỗi nhẹ: elbow flaring (có thể chấp nhận được)
                        String keyLower = errorKey.toLowerCase();
                        if (keyLower.contains("sagging") || keyLower.contains("insufficient") || 
                            keyLower.contains("depth") || keyLower.contains("bent") && keyLower.contains("knee")) {
                            severeErrorCount++;
                        }
                    }
                }
            }
            
            // FIX: Chỉ set incorrectPosture = true nếu có ít nhất 1 lỗi nghiêm trọng
            // Hoặc có nhiều lỗi cùng lúc (>= 2 lỗi)
            if (severeErrorCount >= 1 || feedbackList.size() >= 2) {
                incorrectPosture = true;
            }
            
            // Log form error detection
            if (incorrectPosture && !hadError) {
                android.util.Log.d(TAG, "⚠️ Severe form error detected: " + feedbackList + " (severeCount: " + severeErrorCount + ")");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in checkFormErrors: " + e.getMessage(), e);
        }
    }

    private boolean checkFormError(List<Map<String, Float>> landmarks, String errorKey) {
        if (landmarks == null || landmarks.size() < 33) {
            return false;
        }
        
        String keyLower = errorKey.toLowerCase();
        
        try {
            // Check for common form errors based on error key
            if (keyLower.contains("sagging") || keyLower.contains("hips")) {
                // Check if hips are sagging (body angle too low)
                Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
                Map<String, Float> leftHip = getLandmark(landmarks, 23);
                Map<String, Float> leftAnkle = getLandmark(landmarks, 27);
                if (isValidLandmark(leftShoulder) && isValidLandmark(leftHip) && isValidLandmark(leftAnkle)) {
                    double bodyAngle = calculateAngle(leftShoulder, leftHip, leftAnkle);
                    // If body angle is too low (< 150°), hips are sagging
                    if (bodyAngle < 150) {
                        return true;
                    }
                }
            } else if (keyLower.contains("elbow") && (keyLower.contains("flare") || keyLower.contains("flaring"))) {
                // Check if elbows are flaring (elbow angle too wide)
                Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
                Map<String, Float> leftElbow = getLandmark(landmarks, 13);
                Map<String, Float> leftWrist = getLandmark(landmarks, 15);
                if (isValidLandmark(leftShoulder) && isValidLandmark(leftElbow) && isValidLandmark(leftWrist)) {
                    double elbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist);
                    // If elbow angle is too wide (> 100°), elbows are flaring
                    if (elbowAngle > 100 && elbowAngle < 160) {
                        return true;
                    }
                }
            } else if (keyLower.contains("neck") || keyLower.contains("craning")) {
                // Check if neck is craning (head position relative to shoulders)
                Map<String, Float> nose = getLandmark(landmarks, 0);
                Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
                Map<String, Float> rightShoulder = getLandmark(landmarks, 12);
                if (isValidLandmark(nose) && isValidLandmark(leftShoulder) && isValidLandmark(rightShoulder)) {
                    // Calculate if head is too far forward or back
                    float shoulderMidY = (leftShoulder.get("y") + rightShoulder.get("y")) / 2;
                    float noseY = nose.get("y");
                    // If nose is significantly below shoulder midpoint, neck might be craning
                    if (noseY > shoulderMidY + 0.1) {
                        return true;
                    }
                }
            } else if (keyLower.contains("depth") || keyLower.contains("insufficient")) {
                // Check if depth is insufficient (for push-up: elbow angle not bent enough)
                Map<String, Float> leftShoulder = getLandmark(landmarks, 11);
                Map<String, Float> leftElbow = getLandmark(landmarks, 13);
                Map<String, Float> leftWrist = getLandmark(landmarks, 15);
                if (isValidLandmark(leftShoulder) && isValidLandmark(leftElbow) && isValidLandmark(leftWrist)) {
                    double elbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist);
                    // If elbow angle is too wide (> 120°), depth is insufficient
                    if (elbowAngle > 120) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking form error " + errorKey + ": " + e.getMessage());
        }
        
        return false;
    }

    private void updateStateSequence(String newState) {
        if (newState == null) return;

        // FIX: Logic đơn giản hơn - luôn thêm state mới nếu khác với prevState
        // Quan trọng: Cho phép thêm state đầu tiên (firstState) vào cuối sequence để đếm rep
        if (prevState == null || !prevState.equals(newState)) {
            // Nếu sequence quá dài (>4), clear và bắt đầu lại
            if (stateSequence.size() > 4) {
                android.util.Log.w(TAG, "⚠️ Sequence too long, clearing: " + stateSequence);
                stateSequence.clear();
                prevState = null;
                repCountedForCurrentCycle = false; // Reset flag khi clear sequence
            }
            
            // CRITICAL FIX: Cho phép thêm firstState vào cuối sequence để đếm rep
            // Ví dụ: [start, mid1, mid2] -> thêm start -> [start, mid1, mid2, start] -> đếm rep
            if (stateSequence.size() > 0) {
                String firstState = stateSequence.get(0);
                // Nếu đang quay lại firstState và sequence đã có ít nhất 2 states (start -> mid)
                // Thì cho phép thêm firstState vào cuối để hoàn thành chu kỳ
                if (newState.equals(firstState) && stateSequence.size() >= 2 && !stateSequence.get(stateSequence.size() - 1).equals(firstState)) {
                    // Chỉ thêm nếu phần tử cuối cùng chưa phải là firstState
                    stateSequence.add(newState);
                    prevState = newState;
                    android.util.Log.d(TAG, "✅ Added firstState to complete cycle: " + stateSequence);
                } else if (!newState.equals(firstState)) {
                    // Thêm state mới (không phải firstState)
                    stateSequence.add(newState);
                    prevState = newState;
                    android.util.Log.d(TAG, "State changed: " + (prevState != null ? prevState : "null") + " -> " + newState + ", sequence: " + stateSequence);
                }
                // Nếu newState == firstState nhưng sequence.size() < 2, không thêm (chưa hoàn thành chu kỳ)
                // Nếu phần tử cuối đã là firstState, không thêm nữa (tránh duplicate)
            } else {
                // Sequence rỗng, thêm state đầu tiên
                stateSequence.add(newState);
                prevState = newState;
                android.util.Log.d(TAG, "Initial state: " + newState);
            }
            
            // Reset flag khi state thay đổi sau khi đã đếm rep (để có thể đếm rep mới)
            if (repCountedForCurrentCycle && stateSequence.size() == 1) {
                repCountedForCurrentCycle = false;
                android.util.Log.d(TAG, "Reset repCountedForCurrentCycle - new cycle started");
            }
        }
    }

    private List<String> getStateSequence() {
        // Luôn trả về stateSequence hiện tại, không trả về null
        // Log để debug
        if (System.currentTimeMillis() % 2000 < 200) {
            android.util.Log.d(TAG, "getStateSequence: size=" + stateSequence.size() + ", sequence=" + stateSequence);
        }
        return stateSequence;
    }

    @Override
    public String getExerciseType() {
        return "custom";
    }

    @Override
    public int[] getRequiredLandmarks() {
        if (config != null && config.containsKey("requiredLandmarks")) {
            Object landmarksObj = config.get("requiredLandmarks");
            if (landmarksObj instanceof List) {
                List<?> landmarks = (List<?>) landmarksObj;
                int[] result = new int[landmarks.size()];
                for (int i = 0; i < landmarks.size(); i++) {
                    Object val = landmarks.get(i);
                    if (val instanceof Number) {
                        result[i] = ((Number) val).intValue();
                    } else {
                        result[i] = 0;
                    }
                }
                return result;
            }
        }
        // Default landmarks
        return new int[]{0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32};
    }

    @Override
    public Map<String, Object> getThresholds(String level) {
        Map<String, Object> result = new HashMap<>();
        if (config != null && config.containsKey("thresholds")) {
            Map<String, Object> thresholds = (Map<String, Object>) config.get("thresholds");
            if (thresholds != null) {
                result.putAll(thresholds);
            }
        }
        return result;
    }

    @Override
    public void updateThresholds(Map<String, Object> thresholds) {
        if (config != null && config.containsKey("thresholds")) {
            Map<String, Object> existingThresholds = (Map<String, Object>) config.get("thresholds");
            if (existingThresholds != null) {
                existingThresholds.putAll(thresholds);
            }
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
        this.repCountedForCurrentCycle = false;
        this.inactiveTime = 0.0;
        this.inactiveTimeFront = 0.0;
        this.startInactiveTime = System.nanoTime() / 1e9;
        this.startInactiveTimeFront = System.nanoTime() / 1e9;
        this.cameraWarning = false;
        this.offsetAngle = 0;
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
}


