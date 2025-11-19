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

import java.util.HashMap;
import java.util.Map;

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
        tvSub.setText("Đang kích hoạt kế hoạch...");

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("acceptPlan")
                .call(data)
                .addOnSuccessListener(r -> {
                    setLoading(false);
                    String successMsg = "Kế hoạch đã được kích hoạt thành công!";
                    tvSub.setText(successMsg);
                    Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "acceptPlan success");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String errorMsg = getErrorMessage(e);
                    tvSub.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "acceptPlan failed", e);
                });
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
