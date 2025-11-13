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
import java.util.List;
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
            btnAccept.setEnabled(!loading && currentPlan != null && currentPlan.days != null && currentPlan.days.size() > 0);
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

        tvSub.setText("Đang tạo kế hoạch tập luyện...");

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (force) {
            data.put("force", true);
        }

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
                        Object planObj = res.get("plan");
                        
                        if (planObj == null || !(planObj instanceof Map)) {
                            setLoading(false);
                            String errorMsg = "Không tìm thấy kế hoạch trong phản hồi";
                            tvSub.setText(errorMsg);
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Plan not found in response");
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
                        Toast.makeText(this, "Tạo kế hoạch thành công!", Toast.LENGTH_SHORT).show();
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
                    
                    // Log detailed error information for debugging
                    Log.e(TAG, "=== generatePlan FAILED ===");
                    Log.e(TAG, "Exception type: " + e.getClass().getName());
                    Log.e(TAG, "Exception message: " + e.getMessage());
                    
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        Log.e(TAG, "Error Code: " + ffe.getCode());
                        Log.e(TAG, "Error Details: " + ffe.getDetails());
                        Log.e(TAG, "Error Message: " + ffe.getMessage());
                        Log.e(TAG, "Stack trace:", e);
                        
                        // Log thông tin để debug
                        Log.e(TAG, "UID being used: " + uid);
                        Log.e(TAG, "Function region: us-central1");
                        Log.e(TAG, "Function name: generatePlan");
                    }
                    
                    String errorMsg = getErrorMessage(e);
                    tvSub.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }


    private void render(PlanModels.Plan plan) {
        if (plan == null || plan.days == null || plan.days.isEmpty()) {
            tvSub.setText("Kế hoạch trống");
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
                    
                    // Optionally finish activity or show success state
                    // finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String errorMsg = getErrorMessage(e);
                    tvSub.setText(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "acceptPlan failed", e);
                    
                    // Log detailed error information
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        Log.e(TAG, "Error Code: " + ffe.getCode());
                        Log.e(TAG, "Error Details: " + ffe.getDetails());
                        Log.e(TAG, "Error Message: " + ffe.getMessage());
                    }
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
            
            // Handle specific error codes
            switch (ffe.getCode()) {
                case INTERNAL:
                    // Lỗi INTERNAL thường do lỗi trong Firebase Function code
                    // Có thể là: Gemini API error, missing profile, hoặc lỗi logic
                    StringBuilder detailedMsg = new StringBuilder();
                    detailedMsg.append("❌ Lỗi nội bộ server (INTERNAL)\n\n");
                    detailedMsg.append("🔍 Nguyên nhân có thể:\n");
                    detailedMsg.append("1. Firebase Function chưa được deploy\n");
                    detailedMsg.append("   → Chạy: firebase deploy --only functions\n\n");
                    detailedMsg.append("2. Gemini API key chưa được cấu hình\n");
                    detailedMsg.append("   → Chạy: firebase functions:config:set gemini.api_key=\"YOUR_KEY\"\n\n");
                    detailedMsg.append("3. Function có lỗi trong code\n");
                    detailedMsg.append("   → Xem logs: Firebase Console → Functions → Logs\n\n");
                    detailedMsg.append("4. Profile thiếu thông tin\n");
                    detailedMsg.append("   → Kiểm tra collection 'profiles/{uid}' trong Firestore\n\n");
                    detailedMsg.append("📋 Xem file FIREBASE_FUNCTIONS_SETUP.md để biết chi tiết");
                    
                    if (message != null && (message.contains("Gemini") || message.contains("Ask Gemini") || message.contains("API key not valid"))) {
                        detailedMsg = new StringBuilder();
                        detailedMsg.append("❌ Lỗi từ AI Gemini\n\n");
                        
                        // Kiểm tra nếu là lỗi API key
                        if (message.contains("API key not valid") || message.contains("API_KEY_INVALID")) {
                            detailedMsg.append("🔑 API key không hợp lệ!\n\n");
                            detailedMsg.append("🔧 Các bước khắc phục:\n\n");
                            detailedMsg.append("1. Lấy API key mới từ:\n");
                            detailedMsg.append("   https://aistudio.google.com/app/apikey\n\n");
                            detailedMsg.append("2. Set lại secret:\n");
                            detailedMsg.append("   echo \"YOUR_API_KEY\" | firebase functions:secrets:set GEMINI_API_KEY\n\n");
                            detailedMsg.append("3. Deploy lại functions:\n");
                            detailedMsg.append("   firebase deploy --only functions\n\n");
                            detailedMsg.append("4. Kiểm tra API key có quyền:\n");
                            detailedMsg.append("   - Enable Generative Language API\n");
                            detailedMsg.append("   - Không restrict API key\n\n");
                        } else {
                            detailedMsg.append("🔧 Các bước khắc phục:\n\n");
                            detailedMsg.append("1. Kiểm tra Firebase Functions đã deploy:\n");
                            detailedMsg.append("   firebase functions:list\n\n");
                            detailedMsg.append("2. Cấu hình Gemini API key:\n");
                            detailedMsg.append("   echo \"YOUR_KEY\" | firebase functions:secrets:set GEMINI_API_KEY\n");
                            detailedMsg.append("   firebase deploy --only functions\n\n");
                            detailedMsg.append("3. Kiểm tra quota/rate limit của Gemini API\n\n");
                            detailedMsg.append("4. Xem logs chi tiết:\n");
                            detailedMsg.append("   Firebase Console → Functions → generatePlan → Logs\n\n");
                        }
                        detailedMsg.append("📄 Xem FIX_GEMINI_API_KEY.md để biết thêm");
                    }
                    return detailedMsg.toString();
                    
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
                    return "Điều kiện không đáp ứng. Vui lòng kiểm tra profile của bạn.";
                    
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
                        // Try to extract meaningful message
                        if (message.contains("Ask Gemini")) {
                            return "Lỗi từ AI Gemini. Vui lòng kiểm tra:\n1. Firebase Functions đã được deploy\n2. Gemini API key đã được cấu hình\n3. Thử lại sau vài phút";
                        }
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
