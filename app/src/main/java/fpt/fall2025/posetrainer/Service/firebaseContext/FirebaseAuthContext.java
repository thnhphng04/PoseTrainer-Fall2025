package fpt.fall2025.posetrainer.Service.firebaseContext;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * FirebaseAuthContext - Context class để quản lý FirebaseAuth instance
 * Sử dụng Singleton pattern để đảm bảo chỉ có 1 instance trong toàn bộ app
 * 
 * Cung cấp:
 * - Khởi tạo FirebaseAuth instance
 * - Truy cập FirebaseUser hiện tại (với cache)
 * - Quản lý cache của FirebaseUser (refresh, clear)
 * - Đăng xuất và clear cache
 * 
 * Lưu ý: Các business logic methods (getCurrentUserId, getCurrentUserEmail, etc.)
 * nên được sử dụng thông qua AuthService
 */
public class FirebaseAuthContext {
    private static final String TAG = "FirebaseAuthContext";
    private static FirebaseAuthContext instance;
    
    private FirebaseAuth mAuth;
    private FirebaseUser cachedUser;
    
    /**
     * Private constructor để đảm bảo Singleton pattern
     */
    private FirebaseAuthContext() {
        mAuth = FirebaseAuth.getInstance();
        cachedUser = mAuth.getCurrentUser();
        
        // Listen for auth state changes để update cache
        mAuth.addAuthStateListener(firebaseAuth -> {
            cachedUser = firebaseAuth.getCurrentUser();
            if (cachedUser != null) {
                Log.d(TAG, "User authenticated: " + cachedUser.getUid());
            } else {
                Log.d(TAG, "User signed out");
            }
        });
    }
    
    /**
     * Lấy instance duy nhất của FirebaseAuthContext (Singleton)
     */
    public static FirebaseAuthContext getInstance() {
        if (instance == null) {
            synchronized (FirebaseAuthContext.class) {
                if (instance == null) {
                    instance = new FirebaseAuthContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy FirebaseAuth instance
     * @return FirebaseAuth instance
     */
    @NonNull
    public FirebaseAuth getAuth() {
        return mAuth;
    }
    
    /**
     * Lấy FirebaseUser hiện tại (từ cache hoặc getCurrentUser)
     * @return FirebaseUser nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        if (cachedUser == null) {
            cachedUser = mAuth.getCurrentUser();
        }
        return cachedUser;
    }
    
    /**
     * Refresh cache của FirebaseUser
     * Nên gọi sau khi thực hiện các thao tác authentication (login, logout, register)
     */
    public void refreshUser() {
        cachedUser = mAuth.getCurrentUser();
        if (cachedUser != null) {
            Log.d(TAG, "User cache refreshed: " + cachedUser.getUid());
        } else {
            Log.d(TAG, "User cache cleared (no user logged in)");
        }
    }
    
    /**
     * Đăng xuất user và clear cache
     */
    public void signOut() {
        mAuth.signOut();
        cachedUser = null;
        Log.d(TAG, "User signed out and cache cleared");
    }
}

