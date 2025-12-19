package fpt.fall2025.posetrainer.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;
import java.util.List;

import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseAuthContext;

/**
 * AuthService - Service để quản lý authentication operations
 * Xử lý đăng nhập, đăng ký, Google Sign-In
 */
public class AuthService {
    private static final String TAG = "AuthService";
    
    private FirebaseAuthContext authContext;
    private UserDAO userDAO;
    
    public AuthService() {
        this.authContext = FirebaseAuthContext.getInstance();
        this.userDAO = new UserDAO();
    }
    
    /**
     * Đăng nhập với email và password
     */
    public void signInWithEmailPassword(@NonNull String email, @NonNull String password, 
                                       @Nullable OnCompleteListener<AuthResult> listener) {
        Log.d(TAG, "Đang đăng nhập với email: " + email);
        authContext.getAuth()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✅ Đăng nhập thành công");
                    authContext.refreshUser();
                } else {
                    Log.e(TAG, "❌ Đăng nhập thất bại", task.getException());
                }
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    /**
     * Đăng ký với email và password
     */
    public void createUserWithEmailPassword(@NonNull String email, @NonNull String password,
                                           @Nullable OnCompleteListener<AuthResult> listener) {
        Log.d(TAG, "Đang tạo tài khoản với email: " + email);
        authContext.getAuth()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✅ Tạo tài khoản thành công");
                    authContext.refreshUser();
                } else {
                    Log.e(TAG, "❌ Tạo tài khoản thất bại", task.getException());
                }
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    /**
     * Đăng nhập với Google (sử dụng idToken)
     */
    public void signInWithGoogle(@NonNull String idToken, 
                                 @Nullable OnCompleteListener<AuthResult> listener) {
        Log.d(TAG, "Đang đăng nhập với Google");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        authContext.getAuth()
            .signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✅ Đăng nhập Google thành công");
                    authContext.refreshUser();
                } else {
                    Log.e(TAG, "❌ Đăng nhập Google thất bại", task.getException());
                }
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
    
    /**
     * Tạo user document trong Firestore từ FirebaseUser
     */
    public void createUserDocument(@NonNull FirebaseUser firebaseUser, 
                                   @NonNull List<String> providers,
                                   @Nullable OnCompleteListener<Void> listener) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";
        
        Log.d(TAG, "Đang tạo user document cho UID: " + uid);
        
        long now = System.currentTimeMillis() / 1000;
        User.NotificationSettings notification = new User.NotificationSettings(null, true);
        User newUser = new User(
            uid,
            email,
            displayName,
            photoUrl,
            providers,
            now,
            now,
            notification,
            Arrays.asList("user")
        );
        
        userDAO.save(newUser, listener);
    }
    
    /**
     * Cập nhật lastLoginAt cho user
     */
    public void updateLastLogin(@NonNull String uid, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật lastLoginAt cho user: " + uid);
        long now = System.currentTimeMillis() / 1000;
        
        // Lấy user hiện tại, cập nhật lastLoginAt, rồi save lại
        userDAO.getById(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult();
                user.setLastLoginAt(now);
                userDAO.update(uid, user, listener);
            } else {
                Log.w(TAG, "Không tìm thấy user để cập nhật lastLoginAt, tạo mới");
                // Nếu không tìm thấy, tạo user document mới
                FirebaseUser firebaseUser = authContext.getCurrentUser();
                if (firebaseUser != null) {
                    createUserDocument(firebaseUser, Arrays.asList("google.com"), listener);
                } else {
                    if (listener != null) {
                        listener.onComplete(Tasks.<Void>forException(
                            new Exception("Không tìm thấy user để cập nhật")));
                    }
                }
            }
        });
    }
    
    /**
     * Đăng xuất
     */
    public void signOut() {
        Log.d(TAG, "Đang đăng xuất");
        authContext.signOut();
    }
    
    /**
     * Kiểm tra user đã đăng nhập chưa
     */
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * Lấy FirebaseUser hiện tại
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return authContext.getCurrentUser();
    }
    
    /**
     * Lấy UID của user hiện tại
     * @return UID nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Lấy email của user hiện tại
     * @return Email nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    @Nullable
    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * Lấy display name của user hiện tại
     * @return Display name nếu có, null nếu không có
     */
    @Nullable
    public String getCurrentUserDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }
    
    /**
     * Lấy photo URL của user hiện tại
     * @return Photo URL nếu có, null nếu không có
     */
    @Nullable
    public String getCurrentUserPhotoUrl() {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            return user.getPhotoUrl().toString();
        }
        return null;
    }
    
    /**
     * Lấy UID của user hiện tại, throw exception nếu chưa đăng nhập
     * @return UID của user
     * @throws IllegalStateException nếu user chưa đăng nhập
     */
    @NonNull
    public String requireUserId() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            throw new IllegalStateException("User is not logged in");
        }
        return uid;
    }
    
    /**
     * Refresh cache của FirebaseUser
     * Nên gọi sau khi thực hiện các thao tác authentication (login, logout, register)
     */
    public void refreshUser() {
        authContext.refreshUser();
    }
    
    /**
     * Lấy FirebaseAuth instance (để truy cập các method đặc biệt như fetchSignInMethodsForEmail)
     */
    @NonNull
    public com.google.firebase.auth.FirebaseAuth getAuth() {
        return authContext.getAuth();
    }
    
    /**
     * Gửi email xác minh cho user hiện tại
     * @param listener Callback để xử lý kết quả
     */
    public void sendEmailVerification(@Nullable OnCompleteListener<Void> listener) {
        sendEmailVerification(null, listener);
    }
    
    /**
     * Gửi email xác minh cho user hiện tại với ActionCodeSettings
     * @param continueUrl URL để redirect về app sau khi xác minh (có thể null)
     * @param listener Callback để xử lý kết quả
     */
    public void sendEmailVerification(@Nullable String continueUrl, @Nullable OnCompleteListener<Void> listener) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Không thể gửi email verification: User chưa đăng nhập");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(
                    new IllegalStateException("User is not logged in")));
            }
            return;
        }
        
        if (user.isEmailVerified()) {
            Log.d(TAG, "Email đã được xác minh rồi");
            if (listener != null) {
                listener.onComplete(Tasks.forResult(null));
            }
            return;
        }
        
        Log.d(TAG, "Đang gửi email verification cho: " + user.getEmail());
        authContext.getAuth().setLanguageCode("vi");
        
        // Tạo ActionCodeSettings với continueUrl để redirect về app
        com.google.firebase.auth.ActionCodeSettings actionCodeSettings = null;
        if (continueUrl != null && !continueUrl.isEmpty()) {
            actionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
                    .setUrl(continueUrl)
                    .setHandleCodeInApp(true) // Mở trong app nếu có thể
                    .setAndroidPackageName(
                            "fpt.fall2025.posetrainer",
                            true, // install if not available
                            null // minimum version - null để không check
                    )
                    .build();
        }
        
        if (actionCodeSettings != null) {
            user.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Đã gửi email verification thành công với deep link");
                    } else {
                        Log.e(TAG, "❌ Gửi email verification thất bại", task.getException());
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                });
        } else {
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Đã gửi email verification thành công");
                    } else {
                        Log.e(TAG, "❌ Gửi email verification thất bại", task.getException());
                    }
                    if (listener != null) {
                        listener.onComplete(task);
                    }
                });
        }
    }
    
    /**
     * Kiểm tra email đã được xác minh chưa
     * @return true nếu email đã được xác minh, false nếu chưa hoặc user chưa đăng nhập
     */
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }
    
    /**
     * Xóa tài khoản Firebase Auth của user hiện tại
     * @param listener Callback để xử lý kết quả
     */
    public void deleteCurrentUser(@Nullable OnCompleteListener<Void> listener) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Không thể xóa user: User chưa đăng nhập");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(
                    new IllegalStateException("User is not logged in")));
            }
            return;
        }
        
        Log.d(TAG, "Đang xóa user: " + user.getUid());
        user.delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✅ Đã xóa user thành công");
                    authContext.refreshUser();
                } else {
                    Log.e(TAG, "❌ Xóa user thất bại", task.getException());
                }
                if (listener != null) {
                    listener.onComplete(task);
                }
            });
    }
}

