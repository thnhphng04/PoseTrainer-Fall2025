package fpt.fall2025.posetrainer.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.Map;

import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFunctionsContext;

/**
 * FunctionsService - Service để gọi Firebase Cloud Functions
 * Quản lý các thao tác gọi remote functions
 */
public class FunctionsService {
    private static final String TAG = "FunctionsService";
    
    private FirebaseFunctionsContext functionsContext;
    
    public FunctionsService() {
        this.functionsContext = FirebaseFunctionsContext.getInstance();
    }
    
    /**
     * Gọi function generatePlan để tạo workout plan với AI
     * @param uid User ID
     * @param force Force regenerate plan
     * @param desiredDays Số ngày mong muốn (optional)
     * @param listener Callback để xử lý response
     */
    public void callGeneratePlan(@NonNull String uid, boolean force, 
                                 @Nullable Integer desiredDays,
                                 @Nullable OnCompleteListener<HttpsCallableResult> listener) {
        Log.d(TAG, "Gọi generatePlan với uid: " + uid + ", force: " + force);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("uid", uid);
        if (force) {
            data.put("force", true);
        }
        if (desiredDays != null) {
            data.put("desiredDays", desiredDays);
            Log.d(TAG, "Sending desiredDays: " + desiredDays);
        }
        
        functionsContext.getCallable("generatePlan")
            .call(data)
            .addOnSuccessListener(result -> {
                Log.d(TAG, "✅ Gọi generatePlan thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(result));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Gọi generatePlan thất bại", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<HttpsCallableResult>forException(e));
                }
            });
    }
    
    /**
     * Gọi bất kỳ Cloud Function nào
     * @param functionName Tên function
     * @param data Data để gửi
     * @param listener Callback để xử lý response
     */
    public void callFunction(@NonNull String functionName, @NonNull Map<String, Object> data,
                            @Nullable OnCompleteListener<HttpsCallableResult> listener) {
        Log.d(TAG, "Gọi function: " + functionName);
        
        functionsContext.getCallable(functionName)
            .call(data)
            .addOnSuccessListener(result -> {
                Log.d(TAG, "✅ Gọi function thành công: " + functionName);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(result));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Gọi function thất bại: " + functionName, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<HttpsCallableResult>forException(e));
                }
            });
    }
    
    /**
     * Lấy error message từ FirebaseFunctionsException
     */
    @NonNull
    public String getErrorMessage(@NonNull Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException functionsException = (FirebaseFunctionsException) e;
            FirebaseFunctionsException.Code code = functionsException.getCode();
            
            switch (code) {
                case NOT_FOUND:
                    return "Function không tồn tại";
                case PERMISSION_DENIED:
                    return "Không có quyền truy cập";
                case RESOURCE_EXHAUSTED:
                    return "Tài nguyên đã hết, vui lòng thử lại sau";
                case FAILED_PRECONDITION:
                    return "Điều kiện không đủ để thực hiện";
                case ABORTED:
                    return "Thao tác bị hủy";
                case OUT_OF_RANGE:
                    return "Giá trị nằm ngoài phạm vi cho phép";
                case UNIMPLEMENTED:
                    return "Function chưa được triển khai";
                case INTERNAL:
                    return "Lỗi nội bộ server";
                case UNAVAILABLE:
                    return "Service không khả dụng, vui lòng thử lại sau";
                case DATA_LOSS:
                    return "Mất dữ liệu";
                case UNAUTHENTICATED:
                    return "Chưa xác thực";
                default:
                    return "Lỗi không xác định: " + e.getMessage();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
    }
}

