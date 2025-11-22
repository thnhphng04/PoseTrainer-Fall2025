package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (force) {
            data.put("force", true);
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
                        
                        if (currentPlan == null || currentPlan.days == null || currentPlan.days.isEmpty()) {
                            setLoading(false);
                            String errorMsg = "Kế hoạch được tạo nhưng không có dữ liệu";
                            tvSub.setText(errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Plan is null or empty");
                            return;
                        }

                        render(currentPlan);
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
        List<UserWorkout> workouts = new ArrayList<>();
        List<Schedule.ScheduleItem> scheduleItems = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;

        // Tính toán ngày bắt đầu (thứ 2 của tuần hiện tại hoặc tuần tiếp theo)
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysUntilMonday = (Calendar.MONDAY - currentDayOfWeek + 7) % 7;
        if (daysUntilMonday == 0 && calendar.get(Calendar.HOUR_OF_DAY) >= 8) {
            // Nếu đã qua 8h sáng thứ 2, bắt đầu từ tuần sau
            daysUntilMonday = 7;
        }
        calendar.add(Calendar.DAY_OF_MONTH, daysUntilMonday);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Calendar weekStart = (Calendar) calendar.clone();

        // Convert mỗi Day thành UserWorkout
        for (PlanModels.Day day : currentPlan.days) {
            // Tạo UserWorkout từ Day
            UserWorkout workout = createUserWorkoutFromDay(day, currentTime);
            workouts.add(workout);

            // Tạo ScheduleItem cho Day này
            // dayIndex 1-7 tương ứng với thứ 2-8 (Monday-Sunday)
            // Chuyển dayIndex thành dayOfWeek (1=Monday, 2=Tuesday, ..., 7=Sunday)
            int dayOfWeek = day.dayIndex;
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                dayOfWeek = ((day.dayIndex - 1) % 7) + 1; // Đảm bảo trong khoảng 1-7
            }

            List<Integer> daysOfWeek = new ArrayList<>();
            daysOfWeek.add(dayOfWeek);

            // Tính toán exactDate: thứ 2 + (dayIndex - 1) ngày
            Calendar exactDateCalendar = (Calendar) weekStart.clone();
            exactDateCalendar.add(Calendar.DAY_OF_MONTH, dayOfWeek - 1);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String exactDate = sdf.format(exactDateCalendar.getTime());

            Schedule.ScheduleItem scheduleItem = new Schedule.ScheduleItem(
                daysOfWeek,
                "08:00", // Mặc định 8:00 sáng, user có thể chỉnh sau
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
