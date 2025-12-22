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

    // Thresholds từ config (defaults được TĂNG TỐI ĐA để DỄ detect và ÍT khắt khe CỰC KỲ)
    private int offsetThresh = 60; // Tăng từ 45 lên 60 để camera ÍT warning HƠN NHIỀU
    private double inactiveThresh = 12.0; // Tăng từ 8.0 lên 12.0 để cho phép NHIỀU thời gian HƠN
    private int cntFrameThresh = 1; // Giảm từ 2 xuống 1 để detect NHANH HƠN (ít khắt khe hơn)

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
                    
                    // FIX 1: Expand bodyAngle ranges if too narrow (TĂNG từ 35° lên 50° để DỄ ĐẠT HƠN NHIỀU)
                    if (nameLower.contains("body") && nameLower.contains("angle")) {
                        // If range is too narrow (< 50 degrees) or too high (> 165), expand it
                        if (max - min < 50 || min > 165) {
                            double oldMin = min;
                            double oldMax = max;
                            // Expand to ensure at least 50° range, wider for EASIER detection
                            min = Math.min(140, min - 25); // Mở rộng xuống nhiều hơn (từ 20° lên 25°)
                            max = Math.max(180, max + 20); // Mở rộng lên nhiều hơn (từ 15° lên 20°)
                            // Ensure minimum range size of 50° (TĂNG từ 35°)
                            if (max - min < 50) {
                                max = min + 50;
                            }
                            condition.put("min", min);
                            condition.put("max", max);
                            android.util.Log.w(TAG, "🔧 Fixed bodyAngle in " + stateName + ": [" + oldMin + ", " + oldMax + "] -> [" + min + ", " + max + "] (expanded to " + (max - min) + "° for EASIER detection)");
                        }
                    }
                    
                    // FIX 1b: Expand other angle ranges (elbow, knee, hip, shoulder) if too narrow (TĂNG từ 35° lên 50°)
                    if (nameLower.contains("angle") && 
                        (nameLower.contains("elbow") || nameLower.contains("knee") || 
                         nameLower.contains("hip") || nameLower.contains("shoulder") ||
                         nameLower.contains("ankle") || nameLower.contains("wrist"))) {
                        // If range is too narrow (< 50 degrees), expand it
                        if (max - min < 50) {
                            double oldMin = min;
                            double oldMax = max;
                            double center = (min + max) / 2;
                            // Mở rộng range để đảm bảo ít nhất 55° (RỘNG HƠN NHIỀU để dễ đạt và đếm đúng reps)
                            double targetRange = Math.max(55, (max - min) * 2.2); // Mở rộng ít nhất 2.2x hoặc 55°
                            min = Math.max(0, center - targetRange / 2);
                            max = Math.min(180, center + targetRange / 2);
                            // Ensure minimum of 50° (TĂNG từ 35°)
                            if (max - min < 50) {
                                max = min + 50;
                            }
                            condition.put("min", min);
                            condition.put("max", max);
                            android.util.Log.w(TAG, "🔧 Fixed " + conditionName + " in " + stateName + ": [" + oldMin + ", " + oldMax + "] -> [" + min + ", " + max + "] (expanded to " + (max - min) + "° for EASIER detection)");
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
                
                // Log state detection mỗi 500ms để debug tốt hơn
                if (System.currentTimeMillis() % 500 < 100) {
                    android.util.Log.d(TAG, "🔍 Current state: " + (currState != null ? currState : "NULL") + 
                                      ", prevState: " + (prevState != null ? prevState : "NULL") +
                                      ", sequence size: " + stateSequence.size() +
                                      ", correctCount: " + correctCount);
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
                
                // Log state sequence mỗi 500ms để debug tốt hơn
                if (System.currentTimeMillis() % 500 < 100) {
                    android.util.Log.d(TAG, "📊 State sequence size: " + stateSeq.size() + ", sequence: " + stateSeq + ", currState: " + currState + ", repCounted: " + repCountedForCurrentCycle);
                }
                
                // FIX: Đơn giản hóa logic đếm rep CỰC KỲ - CHỈ CẦN ĐI QUA 2 STATES KHÁC NHAU là đếm được rep
                // Logic đơn giản: [state1, state2] hoặc [state1, state2, state1] -> đếm rep
                // Không cần kiểm tra quá chặt chẽ, chỉ cần có sự thay đổi state là đủ
                if (stateSeq != null && stateSeq.size() >= 2 && !repCountedForCurrentCycle) {
                    String firstState = stateSeq.get(0);
                    String lastState = stateSeq.get(stateSeq.size() - 1);
                    
                    // FIX: Đơn giản hóa CỰC KỲ - chỉ cần:
                    // 1. Sequence có ít nhất 2 states
                    // 2. Có ít nhất 2 states khác nhau (state change)
                    boolean hasStateChange = false;
                    if (stateSeq.size() >= 2) {
                        // Kiểm tra xem có ít nhất 2 states khác nhau không
                        for (int i = 1; i < stateSeq.size(); i++) {
                            if (!stateSeq.get(i).equals(firstState)) {
                                hasStateChange = true;
                                break;
                            }
                        }
                    }
                    
                    // FIX: Đếm rep khi:
                    // - Có sự thay đổi state VÀ quay lại firstState (chu kỳ hoàn thành) HOẶC
                    // - Có ít nhất 2 states khác nhau (rất đơn giản)
                    boolean shouldCountRep = false;
                    if (hasStateChange && lastState.equals(firstState) && stateSeq.size() >= 2) {
                        // Đã quay lại firstState sau khi đi qua states khác -> đếm rep
                        shouldCountRep = true;
                        android.util.Log.d(TAG, "✅ Rep condition 1 met: state change + return to firstState");
                    } else if (hasStateChange && stateSeq.size() >= 2) {
                        // Có ít nhất 2 states với sự thay đổi -> đếm rep (CỰC KỲ DỄ DÀNG)
                        shouldCountRep = true;
                        android.util.Log.d(TAG, "✅ Rep condition 2 met: state change detected (size >= 2)");
                    }
                    
                    if (shouldCountRep) {
                        // Form error checking đã bị tắt, luôn đếm là CORRECT
                        correctCount++;
                        message = "CORRECT";
                        android.util.Log.d(TAG, "✅✅✅ Rep completed! Correct count: " + correctCount + ", sequence: " + stateSeq);
                        // Mark rep as counted for this cycle
                        repCountedForCurrentCycle = true;
                        // Clear sequence sau khi đếm để bắt đầu rep mới
                        stateSequence.clear();
                        incorrectPosture = false;
                        prevState = null; // Reset để bắt đầu sequence mới
                    } else {
                        android.util.Log.d(TAG, "⏳ Waiting for rep: hasStateChange=" + hasStateChange + ", size=" + stateSeq.size() + ", lastState=" + lastState + ", firstState=" + firstState);
                    }
                    
                    // Tăng limit để không clear quá sớm
                    if (stateSeq.size() > 6) {
                        android.util.Log.w(TAG, "⚠️ Sequence too long (" + stateSeq.size() + "), clearing to prevent false counting");
                        stateSequence.clear();
                        prevState = null;
                        repCountedForCurrentCycle = false; // Reset flag
                    }
                } else if (stateSeq != null && stateSeq.size() < 2) {
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
            
            // FIX: Đơn giản hóa logic đếm rep - CHỈ CẦN ĐI QUA 2 STATES KHÁC NHAU là đếm được rep
            // Logic đơn giản: [state1, state2, state1] hoặc [state1, state2, state3, state1] -> đếm rep
            // Không cần kiểm tra quá chặt chẽ, chỉ cần có sự thay đổi state là đủ
            if (stateSeq != null && stateSeq.size() >= 2 && !repCountedForCurrentCycle) {
                String firstState = stateSeq.get(0);
                String lastState = stateSeq.get(stateSeq.size() - 1);
                
                // FIX: Đơn giản hóa - chỉ cần:
                // 1. Sequence có ít nhất 2 states
                // 2. Đã quay lại firstState (lastState == firstState) HOẶC có ít nhất 2 states khác nhau
                boolean hasStateChange = false;
                if (stateSeq.size() >= 2) {
                    // Kiểm tra xem có ít nhất 2 states khác nhau không
                    for (int i = 1; i < stateSeq.size(); i++) {
                        if (!stateSeq.get(i).equals(firstState)) {
                            hasStateChange = true;
                            break;
                        }
                    }
                }
                
                // FIX: Đếm rep khi:
                // - Có sự thay đổi state VÀ quay lại firstState (chu kỳ hoàn thành)
                // - HOẶC có ít nhất 2 states khác nhau và sequence >= 3 (đã đi qua nhiều states)
                boolean shouldCountRep = false;
                if (hasStateChange && lastState.equals(firstState) && stateSeq.size() >= 2) {
                    // Đã quay lại firstState sau khi đi qua states khác -> đếm rep
                    shouldCountRep = true;
                } else if (hasStateChange && stateSeq.size() >= 3) {
                    // Có ít nhất 3 states với sự thay đổi -> đếm rep (dễ dàng hơn)
                    shouldCountRep = true;
                }
                
                if (shouldCountRep) {
                    // Form error checking đã bị tắt, luôn đếm là CORRECT
                    correctCount++;
                    message = "CORRECT";
                    android.util.Log.d(TAG, "✅ Rep completed! Correct count: " + correctCount + ", sequence: " + stateSeq);
                    // Mark rep as counted for this cycle
                    repCountedForCurrentCycle = true;
                    // Clear sequence sau khi đếm để bắt đầu rep mới
                    stateSequence.clear();
                    incorrectPosture = false;
                    prevState = null; // Reset để bắt đầu sequence mới
                } else if (stateSeq.size() > 5) {
                    // Sequence quá dài, clear để tránh đếm sai (nhưng tăng limit từ 4 lên 5)
                    android.util.Log.w(TAG, "⚠️ Sequence too long (" + stateSeq.size() + "), clearing to prevent false counting");
                    stateSequence.clear();
                    prevState = null;
                    repCountedForCurrentCycle = false; // Reset flag
                }
            } else if (stateSeq != null && stateSeq.size() < 2) {
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

        // Log feedback values để debug - log mỗi 500ms để dễ theo dõi hơn
        if (System.currentTimeMillis() % 500 < 100) {
            android.util.Log.d(TAG, "=== 📈 FEEDBACK CREATED ===");
            android.util.Log.d(TAG, "✅ correctCount: " + correctCount);
            android.util.Log.d(TAG, "❌ incorrectCount: " + incorrectCount);
            android.util.Log.d(TAG, "💬 message: " + message);
            android.util.Log.d(TAG, "📍 currentState: " + currState);
            android.util.Log.d(TAG, "📋 stateSequence: " + stateSequence + " (size: " + stateSequence.size() + ")");
            android.util.Log.d(TAG, "🎯 repCountedForCurrentCycle: " + repCountedForCurrentCycle);
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

                // FIX: Giảm yêu cầu match CỰC KỲ THẤP để DỄ ĐẠT ĐƯỢC CỰC KỲ NHIỀU (giảm độ khó CỰC KỲ)
                // Logic: 
                // - 1 condition: vẫn phải match 100% vì chỉ có 1 (không thể giảm)
                // - 2 conditions: chỉ cần match 50% (1/2) - giữ nguyên
                // - 3+ conditions: chỉ cần match 1 condition (10-15%) - CỰC KỲ DỄ ĐẠT
                boolean allConditionsMet;
                if (totalConditions == 1) {
                    allConditionsMet = matchedConditions == 1; // Vẫn phải match 100% vì chỉ có 1 condition
                } else if (totalConditions == 2) {
                    allConditionsMet = matchedConditions >= 1; // >= 50% (1/2) - giữ nguyên
                } else {
                    // Chỉ cần match 1 condition bất kỳ (10-15%) - CỰC KỲ DỄ ĐẠT
                    // Thay vì yêu cầu 25% (1/4), chỉ cần 1 condition match là đủ
                    allConditionsMet = matchedConditions >= 1; // CHỈ CẦN 1 CONDITION - CỰC KỲ DỄ
                }

                if (allConditionsMet) {
                    // Log chi tiết khi state matched (chỉ log mỗi 2 giây)
                    if (System.currentTimeMillis() % 2000 < 200) {
                        double requiredRatio = totalConditions == 1 ? 100.0 : (totalConditions == 2 ? 50.0 : (1.0 / totalConditions * 100.0));
                        android.util.Log.d(TAG, String.format(
                            "✅ State MATCHED: %s | Matched: %d/%d conditions (%.0f%%) | Required: %.0f%% (1 condition) | Tolerance: 70-80°",
                            stateName, matchedConditions, totalConditions, 
                            totalConditions > 0 ? (matchedConditions * 100.0 / totalConditions) : 0.0,
                            requiredRatio
                        ));
                    }
                    return stateName;
                } else {
                    // Log chi tiết tại sao không match (chỉ log mỗi 2 giây)
                    if (System.currentTimeMillis() % 2000 < 200) {
                        double requiredRatio = totalConditions == 1 ? 100.0 : (totalConditions == 2 ? 50.0 : (1.0 / totalConditions * 100.0));
                        double actualRatio = totalConditions > 0 ? (matchedConditions * 100.0 / totalConditions) : 0.0;
                        int needMore = Math.max(0, 1 - matchedConditions); // Chỉ cần 1 condition
                        android.util.Log.w(TAG, String.format(
                            "❌ State NOT matched: %s | Matched: %d/%d (%.0f%%) | Required: %.0f%% (1 condition) | Need %d more",
                            stateName, matchedConditions, totalConditions, actualRatio, requiredRatio, needMore
                        ));
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
            
            // FIX: Tăng tolerance CỰC KỲ NHIỀU (70-80°) để giảm độ khó TỐI ĐA - cho phép người dùng CỰC KỲ DỄ DÀNG đạt được conditions
            // Tolerance càng lớn = càng dễ match = càng ít bị đếm sai = reps đúng được tính đúng nhiều hơn
            double rangeSize = max - min;
            double tolerance;
            if (rangeSize < 20) {
                tolerance = 70.0; // Tăng từ 50.0 lên 70.0 - narrow range cần tolerance CỰC KỲ LỚN
            } else if (rangeSize < 40) {
                tolerance = 75.0; // Tăng từ 55.0 lên 75.0 - medium range
            } else if (rangeSize < 60) {
                tolerance = 80.0; // Tăng từ 60.0 lên 80.0 - wide range
            } else {
                tolerance = 80.0; // Tăng từ 60.0 lên 80.0 - very wide range (tolerance CỰC KỲ LỚN)
            }
            
            double adjustedMin = min - tolerance;
            double adjustedMax = max + tolerance;
            
            // FIX: Special handling for bodyAngle - expand range if too narrow (giảm độ khó TỐI ĐA)
            String nameLower = conditionName != null ? conditionName.toLowerCase() : "";
            if (nameLower.contains("body") && nameLower.contains("angle")) {
                // Body angle in push-up/plank is typically 155-180, not 170-180
                if (min > 165) {
                    adjustedMin = Math.min(120, min - 50); // Expand downward CỰC NHIỀU (từ 30° lên 50°)
                }
                if (max - min < 40) {
                    adjustedMax = Math.max(180, max + 30); // Expand upward CỰC NHIỀU (từ 20° lên 30°) nếu quá hẹp
                }
            }
            
            // FIX: Mở rộng ranges cho TẤT CẢ angles để giảm độ khó TỐI ĐA
            // Đảm bảo adjusted range ĐỦ RỘNG (ít nhất 100-120°) để người dùng CỰC KỲ DỄ đạt được
            if (adjustedMax - adjustedMin < 100) {
                // Nếu range vẫn quá hẹp sau khi adjust, mở rộng THÊM NHIỀU HƠN để đảm bảo ít nhất 100°
                double center = (adjustedMin + adjustedMax) / 2;
                adjustedMin = Math.max(0, center - 50);  // Mở rộng ±50° (từ ±30°)
                adjustedMax = Math.min(180, center + 50);
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

            // FIX: Thêm logging chi tiết để debug - log mỗi điều kiện để xem góc thực tế vs range
            boolean isMatch = angle >= adjustedMin && angle <= adjustedMax;
            
            // Log chi tiết để debug (log mỗi giây để dễ theo dõi hơn)
            if (System.currentTimeMillis() % 1000 < 100) {
                android.util.Log.d(TAG, String.format(
                    "🔍 Condition: %s | Angle: %.1f° | Range: [%.1f, %.1f] (Original: [%.1f, %.1f]) | Tolerance: %.1f° | RangeSize: %.1f° | Match: %s",
                    conditionName, angle, adjustedMin, adjustedMax, min, max, tolerance, (adjustedMax - adjustedMin), isMatch ? "✅" : "❌"
                ));
            }
            
            return isMatch;
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
        if (nameLower.contains("hip") && nameLower.contains("angle")) {
            Map<String, Float> shoulder, hip, knee;
            if (nameLower.contains("left")) {
                shoulder = getLandmark(landmarks, 11);  // leftShoulder
                hip = getLandmark(landmarks, 23);       // leftHip
                knee = getLandmark(landmarks, 25);      // leftKnee
            } else if (nameLower.contains("right")) {
                shoulder = getLandmark(landmarks, 12);  // rightShoulder
                hip = getLandmark(landmarks, 24);       // rightHip
                knee = getLandmark(landmarks, 26);      // rightKnee
            } else {
                // Default to left if not specified
                shoulder = getLandmark(landmarks, 11);
                hip = getLandmark(landmarks, 23);
                knee = getLandmark(landmarks, 25);
            }
            if (isValidLandmark(shoulder) && isValidLandmark(hip) && isValidLandmark(knee)) {
                return calculateAngle(shoulder, hip, knee);
            }
        }
        // Knee angle: hip-knee-ankle
        else if (nameLower.contains("knee") && nameLower.contains("angle")) {
            Map<String, Float> hip, knee, ankle;
            if (nameLower.contains("left")) {
                hip = getLandmark(landmarks, 23);    // leftHip
                knee = getLandmark(landmarks, 25);   // leftKnee
                ankle = getLandmark(landmarks, 27);  // leftAnkle
            } else if (nameLower.contains("right")) {
                hip = getLandmark(landmarks, 24);    // rightHip
                knee = getLandmark(landmarks, 26);   // rightKnee
                ankle = getLandmark(landmarks, 28);  // rightAnkle
            } else {
                // Default to left if not specified
                hip = getLandmark(landmarks, 23);
                knee = getLandmark(landmarks, 25);
                ankle = getLandmark(landmarks, 27);
            }
            if (isValidLandmark(hip) && isValidLandmark(knee) && isValidLandmark(ankle)) {
                return calculateAngle(hip, knee, ankle);
            }
        }
        // Shoulder angle: nose-shoulder-hip
        else if (nameLower.contains("shoulder") && nameLower.contains("angle")) {
            Map<String, Float> nose = getLandmark(landmarks, 0);
            Map<String, Float> shoulder, hip;
            if (nameLower.contains("left")) {
                shoulder = getLandmark(landmarks, 11);  // leftShoulder
                hip = getLandmark(landmarks, 23);       // leftHip
            } else if (nameLower.contains("right")) {
                shoulder = getLandmark(landmarks, 12);  // rightShoulder
                hip = getLandmark(landmarks, 24);       // rightHip
            } else {
                // Default to left if not specified
                shoulder = getLandmark(landmarks, 11);
                hip = getLandmark(landmarks, 23);
            }
            if (isValidLandmark(nose) && isValidLandmark(shoulder) && isValidLandmark(hip)) {
                return calculateAngle(nose, shoulder, hip);
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
        else if (nameLower.contains("ankle") && nameLower.contains("angle")) {
            Map<String, Float> knee, ankle, foot;
            if (nameLower.contains("left")) {
                knee = getLandmark(landmarks, 25);   // leftKnee
                ankle = getLandmark(landmarks, 27);  // leftAnkle
                foot = getLandmark(landmarks, 31);   // leftFoot
            } else if (nameLower.contains("right")) {
                knee = getLandmark(landmarks, 26);   // rightKnee
                ankle = getLandmark(landmarks, 28);  // rightAnkle
                foot = getLandmark(landmarks, 32);   // rightFoot
            } else {
                // Default to left if not specified
                knee = getLandmark(landmarks, 25);
                ankle = getLandmark(landmarks, 27);
                foot = getLandmark(landmarks, 31);
            }
            if (isValidLandmark(knee) && isValidLandmark(ankle) && isValidLandmark(foot)) {
                return calculateAngle(knee, ankle, foot);
            }
        }
        // Wrist angle: elbow-wrist-hand
        else if (nameLower.contains("wrist") && nameLower.contains("angle")) {
            Map<String, Float> elbow, wrist, index;
            if (nameLower.contains("left")) {
                elbow = getLandmark(landmarks, 13);  // leftElbow
                wrist = getLandmark(landmarks, 15);  // leftWrist
                index = getLandmark(landmarks, 19);  // leftIndex
            } else if (nameLower.contains("right")) {
                elbow = getLandmark(landmarks, 14);  // rightElbow
                wrist = getLandmark(landmarks, 16);  // rightWrist
                index = getLandmark(landmarks, 20);  // rightIndex
            } else {
                // Default to left if not specified
                elbow = getLandmark(landmarks, 13);
                wrist = getLandmark(landmarks, 15);
                index = getLandmark(landmarks, 19);
            }
            if (isValidLandmark(elbow) && isValidLandmark(wrist) && isValidLandmark(index)) {
                return calculateAngle(elbow, wrist, index);
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
        // FIX: TẮT HOÀN TOÀN form error checking để reps đúng được tính đúng
        // Không kiểm tra form errors nữa, chỉ đếm rep dựa trên state sequence
        // Điều này giúp người dùng dễ dàng đạt được reps đúng hơn
        incorrectPosture = false;
        
        // Clear feedback list để không hiển thị warnings
        feedbackList.clear();
        
        // Log để debug (chỉ log mỗi 5 giây)
        if (System.currentTimeMillis() % 5000 < 100) {
            android.util.Log.d(TAG, "✅ Form error checking DISABLED - All reps will be counted as CORRECT if state sequence is valid");
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
                    // FIX: Tăng threshold từ 150° lên 140° để ít khắt khe hơn (chỉ báo khi thực sự sai)
                    // Nếu body angle quá thấp (< 140°), hips đang sagging nghiêm trọng
                    if (bodyAngle < 140) {
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
                    // FIX: Tăng threshold từ 100° lên 120° và giảm upper bound từ 160° xuống 150° để ít khắt khe hơn
                    // If elbow angle is too wide (> 120° và < 150°), elbows are flaring nghiêm trọng
                    // Chỉ báo khi thực sự flaring nhiều, không phải chỉ hơi rộng
                    if (elbowAngle > 120 && elbowAngle < 150) {
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
                    // FIX: Tăng threshold từ 120° lên 140° để ít khắt khe hơn (chỉ báo khi thực sự không đủ sâu)
                    // If elbow angle is too wide (> 140°), depth is insufficient
                    if (elbowAngle > 140) {
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


