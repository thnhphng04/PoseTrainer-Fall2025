package fpt.fall2025.posetrainer.Service.firebaseContext;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;

/**
 * FirebaseFunctionsContext - Context class để quản lý FirebaseFunctions
 * Sử dụng Singleton pattern để đảm bảo chỉ có 1 instance trong toàn bộ app
 * 
 * Cung cấp:
 * - Khởi tạo FirebaseFunctions instance
 * - Truy cập Cloud Functions để gọi các callable functions
 */
public class FirebaseFunctionsContext {
    private static final String TAG = "FirebaseFunctionsContext";
    private static FirebaseFunctionsContext instance;
    
    private FirebaseFunctions functions;
    private String defaultRegion = "us-central1"; // Default region
    
    /**
     * Private constructor để đảm bảo Singleton pattern
     */
    private FirebaseFunctionsContext() {
        functions = FirebaseFunctions.getInstance(defaultRegion);
        Log.d(TAG, "FirebaseFunctions instance initialized with region: " + defaultRegion);
    }
    
    /**
     * Lấy instance duy nhất của FirebaseFunctionsContext (Singleton)
     */
    public static FirebaseFunctionsContext getInstance() {
        if (instance == null) {
            synchronized (FirebaseFunctionsContext.class) {
                if (instance == null) {
                    instance = new FirebaseFunctionsContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy FirebaseFunctions instance với region mặc định
     * @return FirebaseFunctions instance
     */
    @NonNull
    public FirebaseFunctions getFunctions() {
        return functions;
    }
    
    /**
     * Lấy FirebaseFunctions instance với region cụ thể
     * @param region Region của Cloud Functions (ví dụ: "us-central1", "asia-southeast1")
     * @return FirebaseFunctions instance
     */
    @NonNull
    public FirebaseFunctions getFunctions(@NonNull String region) {
        if (!region.equals(defaultRegion)) {
            return FirebaseFunctions.getInstance(region);
        }
        return functions;
    }
    
    /**
     * Lấy HttpsCallable function với region mặc định
     * @param functionName Tên function
     * @return HttpsCallableReference
     */
    @NonNull
    public HttpsCallableReference getCallable(@NonNull String functionName) {
        return functions.getHttpsCallable(functionName);
    }
    
    /**
     * Lấy HttpsCallable function với region cụ thể
     * @param region Region của Cloud Functions
     * @param functionName Tên function
     * @return HttpsCallableReference
     */
    @NonNull
    public HttpsCallableReference getCallable(@NonNull String region, @NonNull String functionName) {
        return getFunctions(region).getHttpsCallable(functionName);
    }
    
    /**
     * Set region mặc định cho Cloud Functions
     * @param region Region mới (ví dụ: "us-central1", "asia-southeast1")
     */
    public void setDefaultRegion(@NonNull String region) {
        if (!region.equals(defaultRegion)) {
            defaultRegion = region;
            functions = FirebaseFunctions.getInstance(region);
            Log.d(TAG, "Default region changed to: " + region);
        }
    }
    
    /**
     * Lấy region mặc định hiện tại
     * @return Region name
     */
    @NonNull
    public String getDefaultRegion() {
        return defaultRegion;
    }
}

