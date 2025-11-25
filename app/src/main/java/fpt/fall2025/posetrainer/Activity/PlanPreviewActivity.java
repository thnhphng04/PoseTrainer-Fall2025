package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.R;

public class PlanPreviewActivity extends AppCompatActivity {
    private static final String TAG = "PlanPreviewActivity";
    
    private RecyclerView rvDays;
    private ProgressBar progress;
    private Button btnGenerate, btnAccept;
    private TextView tvHeader, tvSub;
    private EditText etDesiredDays;
    private PlanModels.Plan currentPlan;
    private String uid;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_preview);

        // Initialize views
        rvDays = findViewById(R.id.rvDays);
        progress = findViewById(R.id.progress);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnAccept = findViewById(R.id.btnAccept);
        tvHeader = findViewById(R.id.tvHeader);
        tvSub = findViewById(R.id.tvSub);
        etDesiredDays = findViewById(R.id.etDesiredDays);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        uid = currentUser.getUid();
        db = FirebaseFirestore.getInstance();
        rvDays.setLayoutManager(new LinearLayoutManager(this));

        // Set initial state
        setLoading(false);
        btnAccept.setEnabled(false);

        // Setup click listeners
        btnGenerate.setOnClickListener(v -> checkProfileAndGenerate());
        // Long click để force regenerate
        btnGenerate.setOnLongClickListener(v -> {
            Toast.makeText(this, "Đang tạo lại kế hoạch mới...", Toast.LENGTH_SHORT).show();
            generatePlan(true);
            return true;
        });
        btnAccept.setOnClickListener(v -> acceptPlan());
    }

    private void setLoading(boolean loading) {
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnGenerate != null) {
            btnGenerate.setEnabled(!loading);
        }
        if (btnAccept != null) {
            btnAccept.setEnabled(!loading && currentPlan != null && 
                    currentPlan.days != null && currentPlan.days.size() > 0);
        }
    }

    /**
     * Kiểm tra profile tồn tại trước khi generate plan
     */
    private void checkProfileAndGenerate() {
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvSub.setText("Đang kiểm tra hồ sơ...");

        // Kiểm tra profile có tồn tại không
        db.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Profile tồn tại, tiếp tục generate plan
                        generatePlan(false);
                    } else {
                        setLoading(false);
                        String errorMsg = "Chưa có hồ sơ. Vui lòng hoàn thành questionnaire trước.";
                        tvSub.setText(errorMsg);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.w(TAG, "Profile not found for uid: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String errorMsg = "Không thể kiểm tra hồ sơ. Vui lòng thử lại.";
                    tvSub.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error checking profile", e);
                });
    }

    private void generatePlan(boolean force) {
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        setLoading(true);
        tvSub.setText(force ? "Đang tạo lại kế hoạch tập luyện..." : "Đang tạo kế hoạch tập luyện...");

        // Lấy số ngày mong muốn từ EditText
        String desiredDaysStr = etDesiredDays.getText().toString().trim();
        Integer desiredDays = null;
        if (!desiredDaysStr.isEmpty()) {
            try {
                desiredDays = Integer.parseInt(desiredDaysStr);
                if (desiredDays <= 0) {
                    desiredDays = null; // Invalid, ignore
                }
            } catch (NumberFormatException e) {
                // Invalid input, ignore
                Log.w(TAG, "Invalid desiredDays input: " + desiredDaysStr);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (force) {
            data.put("force", true);
        }
        if (desiredDays != null) {
            data.put("desiredDays", desiredDays);
            Log.d(TAG, "Sending desiredDays: " + desiredDays);
        }

        Log.d(TAG, "Gọi generatePlan với uid: " + uid + ", force: " + force);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("generatePlan")
                .call(data)
                .addOnSuccessListener((HttpsCallableResult r) -> {
                    try {
                        Object obj = r.getData();
                        if (!(obj instanceof Map)) {
                            setLoading(false);
                            String errorMsg = "Phản hồi không hợp lệ từ server";
                            tvSub.setText(errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Unexpected response type: " + (obj != null ? obj.getClass().getName() : "null"));
                            return;
                        }

                        Map res = (Map) obj;
                        
                        // ✅ Xử lý cached response
                        Object statusObj = res.get("status");
                        String status = statusObj != null ? String.valueOf(statusObj) : "unknown";
                        Log.d(TAG, "Response status: " + status);
                        
                        if ("cached".equals(status)) {
                            Log.d(TAG, "Nhận được plan từ cache");
                            tvSub.setText("Đang tải kế hoạch đã lưu...");
                        } else {
                            Log.d(TAG, "Nhận được plan mới từ AI");
                            Object modelObj = res.get("model");
                            String model = modelObj != null ? String.valueOf(modelObj) : "unknown";
                            Log.d(TAG, "Model sử dụng: " + model);
                        }
                        
                        Object planObj = res.get("plan");
                        
                        if (planObj == null || !(planObj instanceof Map)) {
                            setLoading(false);
                            String errorMsg = "Không tìm thấy kế hoạch trong phản hồi";
                            tvSub.setText(errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Plan not found in response. Response keys: " + res.keySet());
                            return;
                        }

                        Map planMap = (Map) planObj;
                        currentPlan = PlanModels.Plan.from(planMap);
                        
                        // ✅ Log để debug reason
                        if (currentPlan != null && currentPlan.days != null) {
                            int totalItems = 0;
                            int itemsWithReason = 0;
                            for (PlanModels.Day day : currentPlan.days) {
                                if (day.items != null) {
                                    for (PlanModels.Item item : day.items) {
                                        totalItems++;
                                        if (item.reason != null && !item.reason.isEmpty()) {
                                            itemsWithReason++;
                                            Log.d(TAG, "Item có reason: " + item.name + " - " + item.reason);
                                        } else {
                                            Log.d(TAG, "Item KHÔNG có reason: " + item.name);
                                        }
                                    }
                                }
                            }
                            Log.d(TAG, "Tổng số items: " + totalItems + ", items có reason: " + itemsWithReason);
                        }
                        
                        if (currentPlan == null || currentPlan.days == null || currentPlan.days.isEmpty()) {
                            setLoading(false);
                            String errorMsg = "Kế hoạch được tạo nhưng không có dữ liệu";
                            tvSub.setText(errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Plan is null or empty");
                            return;
                        }

                        // Load profile để lấy thời gian tập luyện và tính toán thời gian cho từng bài tập
                        loadProfileAndCalculateTimes(currentPlan);
                        setLoading(false);
                        
                        String successMsg = "cached".equals(status) ? 
                                "Đã tải kế hoạch đã lưu!" : "Tạo kế hoạch thành công!";
                        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Plan generated successfully: " + currentPlan.days.size() + " days");
                    } catch (Exception e) {
                        setLoading(false);
                        String errorMsg = "Lỗi khi xử lý phản hồi: " + e.getMessage();
                        tvSub.setText(errorMsg);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error processing response", e);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "generatePlan failed", e);
                    
                    String errorMsg = getErrorMessage(e);
                    tvSub.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }


    /**
     * Load profile và tính toán thời gian cho từng bài tập
     */
    private void loadProfileAndCalculateTimes(PlanModels.Plan plan) {
        if (plan == null || plan.days == null || plan.days.isEmpty()) {
            render(plan);
            return;
        }

        // Load profile để lấy trainingStartTime và trainingEndTime
        db.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String trainingStartTime = documentSnapshot.getString("trainingStartTime");
                        String trainingEndTime = documentSnapshot.getString("trainingEndTime");
                        
                        // Tính toán thời gian cho từng bài tập
                        calculateExerciseTimes(plan, trainingStartTime, trainingEndTime);
                    }
                    // Render ngay cả khi không có thời gian
                    render(plan);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile for time calculation", e);
                    // Render ngay cả khi có lỗi
                    render(plan);
                });
    }

    /**
     * Tính toán thời gian bắt đầu và kết thúc cho từng bài tập trong mỗi ngày
     */
    private void calculateExerciseTimes(PlanModels.Plan plan, String trainingStartTime, String trainingEndTime) {
        if (trainingStartTime == null || trainingStartTime.isEmpty() || 
            trainingEndTime == null || trainingEndTime.isEmpty()) {
            Log.d(TAG, "No training time range provided, skipping time calculation");
            return;
        }

        try {
            // Parse thời gian bắt đầu và kết thúc
            String[] startParts = trainingStartTime.split(":");
            String[] endParts = trainingEndTime.split(":");
            
            if (startParts.length != 2 || endParts.length != 2) {
                Log.w(TAG, "Invalid time format: " + trainingStartTime + " - " + trainingEndTime);
                return;
            }

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;
            int totalAvailableMinutes = endTotalMinutes - startTotalMinutes;

            if (totalAvailableMinutes <= 0) {
                Log.w(TAG, "Invalid time range: end time must be after start time");
                return;
            }

            // Tính toán thời gian cho từng ngày
            for (PlanModels.Day day : plan.days) {
                if (day.items == null || day.items.isEmpty()) {
                    continue;
                }

                // Tính tổng thời gian ước tính cho tất cả bài tập trong ngày (tính bằng giây)
                int totalEstimatedSeconds = 0;
                java.util.List<Integer> itemSecondsList = new java.util.ArrayList<>();
                
                for (PlanModels.Item item : day.items) {
                    // Ước tính: mỗi rep mất ~2 giây, cộng thêm rest time
                    int exerciseTimePerSet = item.reps * 2; // 2 giây mỗi rep
                    int totalExerciseTime = exerciseTimePerSet * item.sets;
                    int totalRestTime = item.restSec * Math.max(0, item.sets - 1); // Rest giữa các sets
                    int itemTotalSeconds = totalExerciseTime + totalRestTime;
                    itemSecondsList.add(itemTotalSeconds);
                    totalEstimatedSeconds += itemTotalSeconds;
                }

                // ✅ ƯU TIÊN: Sử dụng toàn bộ khoảng thời gian người dùng đã chọn
                // Không bị giới hạn bởi day.estMinutes, mà sử dụng hết totalAvailableMinutes
                int availableMinutes = totalAvailableMinutes;
                int availableSeconds = availableMinutes * 60;

                // ✅ Tính scale factor:
                // - Nếu thời gian ước tính LỚN HƠN thời gian có sẵn: scale DOWN để vừa với khoảng thời gian
                // - Nếu thời gian ước tính NHỎ HƠN: giữ nguyên, thời gian còn lại sẽ được phân bổ đều
                float scaleFactor = 1.0f;
                if (totalEstimatedSeconds > availableSeconds) {
                    // Scale DOWN: thời gian ước tính dài hơn, cần rút ngắn để vừa
                    scaleFactor = (float) availableSeconds / totalEstimatedSeconds;
                    Log.d(TAG, String.format("Day %d: Estimated time (%d sec) > Available time (%d sec), scaling DOWN by factor %.2f", 
                        day.dayIndex, totalEstimatedSeconds, availableSeconds, scaleFactor));
                } else {
                    // Thời gian ước tính ngắn hơn hoặc bằng, sẽ phân bổ đều thời gian còn lại
                    Log.d(TAG, String.format("Day %d: Estimated time (%d sec) <= Available time (%d sec), will distribute remaining time", 
                        day.dayIndex, totalEstimatedSeconds, availableSeconds));
                }
                
                // Tính thời gian còn lại sau khi trừ đi thời gian ước tính (đã scale nếu cần)
                int scaledEstimatedSeconds = (int) (totalEstimatedSeconds * scaleFactor);
                int remainingSeconds = availableSeconds - scaledEstimatedSeconds;

                // Phân bổ thời gian cho từng bài tập
                int currentTimeMinutes = startTotalMinutes;
                int totalAllocatedSeconds = 0;
                
                // Phân bổ thời gian còn lại đều cho các bài tập (thêm vào thời gian nghỉ giữa các bài)
                int extraSecondsPerItem = remainingSeconds > 0 && day.items.size() > 0 
                    ? remainingSeconds / day.items.size() : 0;
                
                for (int i = 0; i < day.items.size(); i++) {
                    PlanModels.Item item = day.items.get(i);
                    
                    // Tính thời gian cho bài tập này (đã scale)
                    int itemSeconds = (int) (itemSecondsList.get(i) * scaleFactor);
                    
                    // Thêm thời gian nghỉ bổ sung nếu có (để lấp đầy khoảng thời gian)
                    if (i < day.items.size() - 1) {
                        // Thêm thời gian nghỉ giữa các bài tập
                        itemSeconds += extraSecondsPerItem;
                    } else {
                        // Bài tập cuối cùng: thêm tất cả thời gian còn lại để đảm bảo kết thúc đúng giờ
                        int remainingForLastItem = availableSeconds - totalAllocatedSeconds - itemSeconds;
                        if (remainingForLastItem > 0) {
                            itemSeconds += remainingForLastItem;
                        }
                    }
                    
                    // Đảm bảo ít nhất 30 giây
                    if (itemSeconds < 30) {
                        itemSeconds = 30;
                    }
                    
                    int itemMinutes = (itemSeconds / 60) + (itemSeconds % 60 > 0 ? 1 : 0); // Làm tròn lên
                    
                    // Đảm bảo ít nhất 1 phút
                    if (itemMinutes < 1) {
                        itemMinutes = 1;
                    }

                    // Tính thời gian bắt đầu và kết thúc
                    int itemStartMinutes = currentTimeMinutes;
                    int itemEndMinutes = currentTimeMinutes + itemMinutes;

                    // Đảm bảo không vượt quá thời gian kết thúc
                    if (itemEndMinutes > endTotalMinutes) {
                        itemEndMinutes = endTotalMinutes;
                        itemMinutes = itemEndMinutes - itemStartMinutes;
                        if (itemMinutes < 1) {
                            itemMinutes = 1;
                        }
                    }

                    // Convert về format HH:mm
                    int startH = itemStartMinutes / 60;
                    int startM = itemStartMinutes % 60;
                    int endH = itemEndMinutes / 60;
                    int endM = itemEndMinutes % 60;

                    // Đảm bảo giờ trong khoảng 0-23
                    startH = startH % 24;
                    endH = endH % 24;

                    item.startTime = String.format("%02d:%02d", startH, startM);
                    item.endTime = String.format("%02d:%02d", endH, endM);

                    // Cập nhật thời gian cho bài tập tiếp theo
                    currentTimeMinutes = itemEndMinutes;
                    totalAllocatedSeconds += itemSeconds;

                    // Nếu đã hết thời gian, dừng lại
                    if (currentTimeMinutes >= endTotalMinutes) {
                        // Gán thời gian null cho các bài tập còn lại
                        for (int j = i + 1; j < day.items.size(); j++) {
                            day.items.get(j).startTime = null;
                            day.items.get(j).endTime = null;
                        }
                        break;
                    }
                }
                
                // Log để debug
                int finalMinutes = currentTimeMinutes - startTotalMinutes;
                Log.d(TAG, String.format("Day %d: Allocated %d minutes out of %d available minutes (%.1f%%)", 
                    day.dayIndex, finalMinutes, availableMinutes, 
                    availableMinutes > 0 ? ((float)finalMinutes / availableMinutes * 100) : 0));
            }

            Log.d(TAG, "Successfully calculated exercise times for all days");
        } catch (Exception e) {
            Log.e(TAG, "Error calculating exercise times", e);
        }
    }

    private void render(PlanModels.Plan plan) {
        if (plan == null || plan.days == null || plan.days.isEmpty()) {
            tvSub.setText("Kế hoạch trống");
            Log.w(TAG, "Plan is null or empty in render()");
            return;
        }

        String info = String.format("%d tuần • %d ngày tập", plan.weekCount, plan.days.size());
        tvSub.setText(info);
        
        if (rvDays != null) {
            rvDays.setAdapter(new PlanDayAdapter(plan.days));
        }
        
        setLoading(false); // Ensure buttons are enabled
    }

    private void acceptPlan() {
        if (currentPlan == null || currentPlan.days == null || currentPlan.days.isEmpty()) {
            Toast.makeText(this, "Vui lòng tạo kế hoạch trước khi chấp nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvSub.setText("Đang xóa kế hoạch cũ...");

        // Xóa workouts cũ từ AI trước khi lưu mới
        deleteOldAIWorkouts();
    }

    /**
     * Xóa tất cả workouts cũ có source="ai" của user
     */
    private void deleteOldAIWorkouts() {
        tvSub.setText("Đang tìm và xóa kế hoạch cũ...");
        
        // Query tất cả workouts có source="ai" của user này
        db.collection("user_workouts")
                .whereEqualTo("uid", uid)
                .whereEqualTo("source", "ai")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalOldWorkouts = queryDocumentSnapshots.size();
                    Log.d(TAG, "Tìm thấy " + totalOldWorkouts + " workout cũ từ AI");
                    
                    if (totalOldWorkouts == 0) {
                        // Không có workout cũ, tiếp tục lưu mới
                        convertPlanToWorkoutsAndSchedule();
                        return;
                    }
                    
                    // Xóa từng workout
                    int[] deletedCount = {0};
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String workoutId = doc.getId();
                        Log.d(TAG, "Đang xóa workout cũ: " + workoutId);
                        
                        db.collection("user_workouts")
                                .document(workoutId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    deletedCount[0]++;
                                    Log.d(TAG, "Đã xóa workout " + deletedCount[0] + "/" + totalOldWorkouts);
                                    
                                    // Khi đã xóa hết, xóa schedule cũ và tiếp tục lưu mới
                                    if (deletedCount[0] == totalOldWorkouts) {
                                        deleteOldSchedule();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi xóa workout: " + workoutId, e);
                                    deletedCount[0]++;
                                    // Vẫn tiếp tục dù có lỗi
                                    if (deletedCount[0] == totalOldWorkouts) {
                                        deleteOldSchedule();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi query workouts cũ", e);
                    // Vẫn tiếp tục lưu mới dù có lỗi
                    deleteOldSchedule();
                });
    }

    /**
     * Xóa schedule cũ (nếu có title là "Kế hoạch tập luyện AI")
     */
    private void deleteOldSchedule() {
        tvSub.setText("Đang xóa lịch tập cũ...");
        
        db.collection("schedules")
                .whereEqualTo("uid", uid)
                .whereEqualTo("title", "Kế hoạch tập luyện AI")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String scheduleId = doc.getId();
                        Log.d(TAG, "Đang xóa schedule cũ: " + scheduleId);
                        
                        db.collection("schedules")
                                .document(scheduleId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Đã xóa schedule cũ");
                                    convertPlanToWorkoutsAndSchedule();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi xóa schedule cũ", e);
                                    // Vẫn tiếp tục lưu mới dù có lỗi
                                    convertPlanToWorkoutsAndSchedule();
                                });
                    } else {
                        Log.d(TAG, "Không tìm thấy schedule cũ");
                        convertPlanToWorkoutsAndSchedule();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi query schedule cũ", e);
                    // Vẫn tiếp tục lưu mới dù có lỗi
                    convertPlanToWorkoutsAndSchedule();
                });
    }

    /**
     * Convert plan thành UserWorkouts và lưu vào Firestore
     * Mỗi Day trong plan sẽ trở thành một UserWorkout
     */
    private void convertPlanToWorkoutsAndSchedule() {
        tvSub.setText("Đang tạo kế hoạch mới...");
        
        // ✅ Load profile để lấy trainingStartTime và weeklyGoal trước khi tạo Schedule
        db.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String trainingStartTime = "08:00"; // Default fallback
                    int weeklyGoal = 4; // Default fallback
                    
                    if (documentSnapshot.exists()) {
                        String profileStartTime = documentSnapshot.getString("trainingStartTime");
                        if (profileStartTime != null && !profileStartTime.isEmpty()) {
                            trainingStartTime = profileStartTime;
                            Log.d(TAG, "Sử dụng trainingStartTime từ profile: " + trainingStartTime);
                        } else {
                            Log.d(TAG, "Profile không có trainingStartTime, sử dụng mặc định: 08:00");
                        }
                        
                        // ✅ Lấy weeklyGoal từ profile
                        Long weeklyGoalLong = documentSnapshot.getLong("weeklyGoal");
                        if (weeklyGoalLong != null) {
                            weeklyGoal = weeklyGoalLong.intValue();
                            Log.d(TAG, "Sử dụng weeklyGoal từ profile: " + weeklyGoal + " ngày/tuần");
                        } else {
                            Log.d(TAG, "Profile không có weeklyGoal, sử dụng mặc định: 4 ngày/tuần");
                        }
                    } else {
                        Log.d(TAG, "Profile không tồn tại, sử dụng mặc định: 08:00 và 4 ngày/tuần");
                    }
                    
                    // Tạo workouts và schedule với thời gian và weeklyGoal từ profile
                    createWorkoutsAndScheduleWithTime(trainingStartTime, weeklyGoal);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi load profile để lấy trainingStartTime và weeklyGoal", e);
                    // Fallback: sử dụng mặc định
                    createWorkoutsAndScheduleWithTime("08:00", 4);
                });
    }

    /**
     * Tạo workouts và schedule với thời gian cụ thể
     * @param trainingStartTime Thời gian bắt đầu tập luyện (format: "HH:mm")
     * @param weeklyGoal Số ngày có thể tập luyện trong 1 tuần
     */
    private void createWorkoutsAndScheduleWithTime(String trainingStartTime, int weeklyGoal) {
        List<UserWorkout> workouts = new ArrayList<>();
        List<Schedule.ScheduleItem> scheduleItems = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;

        // ✅ Tính toán ngày bắt đầu: bắt đầu từ ngày mai (hoặc hôm nay nếu chưa qua giờ tập)
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // ✅ Sử dụng trainingStartTime để xác định xem đã qua thời gian tập chưa
        int trainingHour = 8; // Default
        int trainingMinute = 0;
        try {
            String[] timeParts = trainingStartTime.split(":");
            if (timeParts.length == 2) {
                trainingHour = Integer.parseInt(timeParts[0]);
                trainingMinute = Integer.parseInt(timeParts[1]);
            }
        } catch (Exception e) {
            Log.w(TAG, "Lỗi parse trainingStartTime, sử dụng mặc định 8:00", e);
        }
        
        // ✅ Xác định ngày bắt đầu: LUÔN bắt đầu từ ngày mai để tránh ngày trong quá khứ
        // Không bắt đầu hôm nay vì có thể đã qua giờ tập, dẫn đến exactDate trong quá khứ
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Luôn bắt đầu từ ngày mai
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Calendar startDate = (Calendar) calendar.clone();
        
        // ✅ Tạo Calendar cho ngày hôm nay (dùng chung cho toàn bộ method)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // ✅ Đảm bảo startDate không phải là quá khứ (double check)
        if (startDate.before(today)) {
            // Nếu vì lý do nào đó startDate vẫn là quá khứ, set về ngày mai
            startDate = (Calendar) today.clone();
            startDate.add(Calendar.DAY_OF_MONTH, 1);
            Log.w(TAG, "⚠️ startDate là quá khứ, đã điều chỉnh về ngày mai: " + 
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(startDate.getTime()));
        }
        
        Log.d(TAG, String.format("Bắt đầu từ ngày: %s, weeklyGoal: %d ngày/tuần", 
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(startDate.getTime()), 
            weeklyGoal));

        // Convert mỗi Day thành UserWorkout
        // ✅ Tính toán ngày bắt đầu: bắt đầu từ ngày mai (hoặc hôm nay nếu chưa qua giờ tập)
        Calendar firstWorkoutDate = (Calendar) startDate.clone();
        
        // ✅ Tìm thứ 2 của tuần chứa startDate
        Calendar currentWeekMonday = (Calendar) startDate.clone();
        int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        currentWeekMonday.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        currentWeekMonday.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekMonday.set(Calendar.MINUTE, 0);
        currentWeekMonday.set(Calendar.SECOND, 0);
        currentWeekMonday.set(Calendar.MILLISECOND, 0);
        
        // ✅ Logic mới: Nhóm các ngày theo tuần
        // Mỗi tuần có weeklyGoal ngày, bắt đầu từ thứ 2 (dayOfWeek = 1, 2, 3, ...)
        // currentDayInWeek: vị trí trong tuần (0 = thứ 2, 1 = thứ 3, ..., weeklyGoal-1 = thứ cuối)
        int currentDayInWeek = 0;
        
        // ✅ Ngày hiện tại: bắt đầu từ thứ 2 của tuần chứa startDate
        // Nếu startDate không phải thứ 2, vẫn bắt đầu từ thứ 2 của tuần đó
        Calendar currentDate = (Calendar) currentWeekMonday.clone();
        
        Log.d(TAG, String.format("Bắt đầu: %s, Thứ 2 tuần hiện tại: %s, weeklyGoal: %d ngày/tuần", 
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(startDate.getTime()),
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(currentWeekMonday.getTime()),
            weeklyGoal));
        
        for (PlanModels.Day day : currentPlan.days) {
            // Tạo UserWorkout từ Day
            UserWorkout workout = createUserWorkoutFromDay(day, currentTime);
            workouts.add(workout);

            // ✅ QUAN TRỌNG: Kiểm tra và chuyển tuần TRƯỚC KHI tính exactDate
            // Nếu đã đủ weeklyGoal ngày trong tuần hiện tại, chuyển sang tuần tiếp theo (thứ 2)
            if (currentDayInWeek >= weeklyGoal) {
                // Chuyển sang tuần tiếp theo: thứ 2 của tuần sau
                currentWeekMonday.add(Calendar.DAY_OF_MONTH, 7);
                currentDate = (Calendar) currentWeekMonday.clone();
                currentDayInWeek = 0;
                Log.d(TAG, String.format("✅ Đã đủ %d ngày trong tuần, chuyển sang tuần mới (thứ 2): %s, reset currentDayInWeek về 0", 
                    weeklyGoal,
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(currentDate.getTime())));
            }
            
            // ✅ Tính exactDate: thứ 2 của tuần hiện tại + currentDayInWeek ngày
            // Điều này đảm bảo: tuần 1 = thứ 2, 3, 4; tuần 2 = thứ 2, 3, 4; ...
            Calendar exactDateCalendar = (Calendar) currentWeekMonday.clone();
            exactDateCalendar.add(Calendar.DAY_OF_MONTH, currentDayInWeek);
            
            // ✅ Đảm bảo exactDate không phải là quá khứ
            if (exactDateCalendar.before(today)) {
                // Nếu exactDate là quá khứ, điều chỉnh về ngày mai
                exactDateCalendar = (Calendar) today.clone();
                exactDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
                // Cập nhật lại currentWeekMonday và currentDayInWeek
                int newDayOfWeek = exactDateCalendar.get(Calendar.DAY_OF_WEEK);
                int newDaysFromMonday = (newDayOfWeek == Calendar.SUNDAY) ? 6 : (newDayOfWeek - Calendar.MONDAY);
                currentWeekMonday = (Calendar) exactDateCalendar.clone();
                currentWeekMonday.add(Calendar.DAY_OF_MONTH, -newDaysFromMonday);
                currentDayInWeek = newDaysFromMonday;
                currentDate = (Calendar) exactDateCalendar.clone();
                Log.w(TAG, String.format("⚠️ Day %d: exactDate là quá khứ, đã điều chỉnh về: %s", 
                    day.dayIndex,
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(exactDateCalendar.getTime())));
            }
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String exactDate = sdf.format(exactDateCalendar.getTime());
            
            // ✅ Tính dayOfWeek cho ScheduleItem
            // dayOfWeek = currentDayInWeek + 1 (vì bắt đầu từ thứ 2 = 1)
            // Ví dụ: currentDayInWeek = 0 → dayOfWeek = 1 (thứ 2)
            //        currentDayInWeek = 1 → dayOfWeek = 2 (thứ 3)
            //        currentDayInWeek = 2 → dayOfWeek = 3 (thứ 4)
            int scheduleDay = currentDayInWeek + 1; // 1=Monday, 2=Tuesday, ..., weeklyGoal=thứ cuối
            
            // ✅ Đảm bảo scheduleDay không vượt quá 7
            if (scheduleDay > 7) {
                scheduleDay = 7; // Tối đa là chủ nhật
                Log.w(TAG, String.format("⚠️ Day %d: scheduleDay vượt quá 7, đã điều chỉnh về 7", day.dayIndex));
            }
            
            List<Integer> daysOfWeek = new ArrayList<>();
            daysOfWeek.add(scheduleDay);
            
            String dayName = "";
            switch (scheduleDay) {
                case 1: dayName = "Thứ 2"; break;
                case 2: dayName = "Thứ 3"; break;
                case 3: dayName = "Thứ 4"; break;
                case 4: dayName = "Thứ 5"; break;
                case 5: dayName = "Thứ 6"; break;
                case 6: dayName = "Thứ 7"; break;
                case 7: dayName = "Chủ nhật"; break;
            }
            
            Log.d(TAG, String.format("Day %d: exactDate=%s, dayOfWeek=%d (%s), dayInWeek=%d/%d, weekMonday=%s", 
                day.dayIndex, exactDate, scheduleDay, dayName, currentDayInWeek + 1, weeklyGoal,
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(currentWeekMonday.getTime())));
            
            // ✅ Tăng counter SAU KHI đã tính exactDate và dayOfWeek
            currentDayInWeek++;

            // ✅ Sử dụng trainingStartTime từ profile thay vì hardcode "08:00"
            Schedule.ScheduleItem scheduleItem = new Schedule.ScheduleItem(
                daysOfWeek,
                trainingStartTime, // Sử dụng thời gian từ profile
                workout.getId(), // Link đến workout ID
                exactDate
            );
            scheduleItems.add(scheduleItem);
        }

        // Lưu tất cả workouts vào Firestore
        saveWorkoutsToFirestore(workouts, scheduleItems);
    }

    /**
     * Tạo UserWorkout từ một Day trong plan
     */
    private UserWorkout createUserWorkoutFromDay(PlanModels.Day day, long currentTime) {
        UserWorkout workout = new UserWorkout();
        
        // Generate unique ID - sử dụng timestamp và dayIndex để đảm bảo unique
        // Format: ai_<timestamp>_<dayIndex>
        String workoutId = "ai_" + currentTime + "_" + day.dayIndex;
        workout.setId(workoutId);
        workout.setUid(uid);
        
        // Tạo title từ dayIndex và focus
        String focusText = day.focus != null && !day.focus.isEmpty() ? day.focus : "fullbody";
        String title = "Ngày " + day.dayIndex + ": " + capitalizeFirst(focusText);
        workout.setTitle(title);
        
        // Tạo description
        String description = String.format("Kế hoạch tập luyện AI - %d phút", day.estMinutes);
        workout.setDescription(description);
        
        // Set source là "ai"
        workout.setSource("ai");
        
        // Set timestamps
        workout.setCreatedAt(currentTime);
        workout.setUpdatedAt(currentTime);
        
        // Convert items từ PlanModels.Item sang UserWorkout.UserWorkoutItem
        List<UserWorkout.UserWorkoutItem> workoutItems = new ArrayList<>();
        for (int i = 0; i < day.items.size(); i++) {
            PlanModels.Item planItem = day.items.get(i);
            
            // Tạo ExerciseConfig từ plan item
            UserWorkout.ExerciseConfig config = new UserWorkout.ExerciseConfig(
                planItem.sets,
                planItem.reps,
                planItem.restSec,
                "medium" // Mặc định difficulty
            );
            
            // Tạo UserWorkoutItem
            UserWorkout.UserWorkoutItem workoutItem = new UserWorkout.UserWorkoutItem(
                i + 1, // order
                planItem.exerciseId,
                config
            );
            
            workoutItems.add(workoutItem);
        }
        
        workout.setItems(workoutItems);
        
        return workout;
    }

    /**
     * Lưu tất cả workouts vào Firestore, sau đó tạo Schedule
     */
    private void saveWorkoutsToFirestore(List<UserWorkout> workouts, List<Schedule.ScheduleItem> scheduleItems) {
        if (workouts.isEmpty()) {
            setLoading(false);
            Toast.makeText(this, "Không có workout để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        tvSub.setText("Đang lưu " + workouts.size() + " bài tập...");

        // Lưu từng workout
        int[] savedCount = {0};
        int totalCount = workouts.size();

        for (UserWorkout workout : workouts) {
            FirebaseService.getInstance().saveUserWorkout(workout, success -> {
                savedCount[0]++;
                Log.d(TAG, "Saved workout " + savedCount[0] + "/" + totalCount + ": " + workout.getTitle());

                // Khi đã lưu hết workouts, tạo Schedule
                if (savedCount[0] == totalCount) {
                    createScheduleFromItems(scheduleItems);
                }
            });
        }
    }

    /**
     * Tạo Schedule từ scheduleItems và lưu vào Firestore
     */
    private void createScheduleFromItems(List<Schedule.ScheduleItem> scheduleItems) {
        tvSub.setText("Đang tạo lịch tập luyện...");

        // Tạo NotificationSettings mặc định
        Schedule.NotificationSettings notificationSettings = new Schedule.NotificationSettings(
            true, // enabled
            15, // remindBeforeMin (15 minutes)
            "default" // sound
        );

        // Tạo Schedule
        Schedule schedule = new Schedule(
            null, // id - sẽ được tạo bởi Firestore
            uid,
            "Kế hoạch tập luyện AI",
            java.util.TimeZone.getDefault().getID(),
            scheduleItems,
            notificationSettings
        );

        // Lưu Schedule vào Firestore
        FirebaseService.getInstance().saveSchedule(schedule, success -> {
            setLoading(false);
            if (success) {
                String successMsg = "Đã lưu " + scheduleItems.size() + " bài tập vào lịch tập luyện!";
                tvSub.setText(successMsg);
                Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Schedule saved successfully with " + scheduleItems.size() + " items");
                
                // Đóng activity sau khi lưu thành công
                finish();
            } else {
                String errorMsg = "Lỗi khi lưu lịch tập luyện";
                tvSub.setText(errorMsg);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save schedule");
            }
        });
    }

    /**
     * Convert Calendar day of week to Schedule day of week format
     * Calendar: Sunday=1, Monday=2, ..., Saturday=7
     * Schedule: Monday=1, Tuesday=2, ..., Sunday=7
     */
    private int convertCalendarDayToScheduleDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY:
                return 1; // Monday = 1
            case Calendar.TUESDAY:
                return 2; // Tuesday = 2
            case Calendar.WEDNESDAY:
                return 3; // Wednesday = 3
            case Calendar.THURSDAY:
                return 4; // Thursday = 4
            case Calendar.FRIDAY:
                return 5; // Friday = 5
            case Calendar.SATURDAY:
                return 6; // Saturday = 6
            case Calendar.SUNDAY:
                return 7; // Sunday = 7
            default:
                return 1; // Default to Monday
        }
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Parse Firebase Functions error and return user-friendly message
     */
    private String getErrorMessage(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
            String code = ffe.getCode().name();
            String message = ffe.getMessage();
            
            Log.d(TAG, "FirebaseFunctionsException - Code: " + code + ", Message: " + message);
            
            switch (ffe.getCode()) {
                case INTERNAL:
                    return "Lỗi nội bộ server. Vui lòng thử lại sau hoặc kiểm tra Firebase Functions logs.";
                    
                case NOT_FOUND:
                    return "Không tìm thấy function. Vui lòng kiểm tra cấu hình Firebase Functions.";
                    
                case PERMISSION_DENIED:
                    return "Không có quyền truy cập. Vui lòng đăng nhập lại.";
                    
                case UNAUTHENTICATED:
                    return "Chưa xác thực. Vui lòng đăng nhập lại.";
                    
                case INVALID_ARGUMENT:
                    return "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin.";
                    
                case DEADLINE_EXCEEDED:
                    return "Yêu cầu mất quá nhiều thời gian. Vui lòng thử lại.";
                    
                case RESOURCE_EXHAUSTED:
                    return "Tài nguyên đã hết. Vui lòng thử lại sau.";
                    
                case FAILED_PRECONDITION:
                    // ✅ Cải thiện error message cho trường hợp profile not found
                    if (message != null && message.contains("Profile not found")) {
                        return "Chưa có hồ sơ. Vui lòng hoàn thành questionnaire trước khi tạo kế hoạch tập luyện.";
                    }
                    if (message != null && message.contains("No suitable exercises")) {
                        return "Không tìm thấy bài tập phù hợp. Vui lòng kiểm tra collection 'exercises' trong Firestore.";
                    }
                    return "Điều kiện không đáp ứng. " + (message != null ? message : "Vui lòng kiểm tra profile của bạn.");
                    
                case ABORTED:
                    return "Yêu cầu bị hủy. Vui lòng thử lại.";
                    
                case OUT_OF_RANGE:
                    return "Dữ liệu vượt quá giới hạn.";
                    
                case UNIMPLEMENTED:
                    return "Tính năng chưa được triển khai.";
                    
                case UNAVAILABLE:
                    return "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.";
                    
                case DATA_LOSS:
                    return "Mất dữ liệu. Vui lòng thử lại.";
                    
                default:
                    if (message != null && !message.isEmpty()) {
                        return "Lỗi: " + message;
                    }
                    return "Không thể tạo kế hoạch. Vui lòng thử lại sau.";
            }
        }
        
        // Generic error message
        String errorMsg = e.getMessage();
        if (errorMsg != null && !errorMsg.isEmpty()) {
            return "Lỗi: " + errorMsg;
        }
        
        return "Không thể tạo kế hoạch. Vui lòng thử lại sau.";
    }
}
