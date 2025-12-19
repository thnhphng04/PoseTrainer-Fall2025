package fpt.fall2025.posetrainer.UI.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.DAL.ProfileDAO;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;

/**
 * Activity để xử lý deep link từ email verification
 * Được mở khi người dùng click vào link xác minh email từ email
 */
public class EmailVerificationActivity extends AppCompatActivity {
    private static final String TAG = "EmailVerificationActivity";
    private AuthService authService;
    private UserDAO userDAO;
    private ProfileDAO profileDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        authService = new AuthService();
        userDAO = new UserDAO();
        profileDAO = new ProfileDAO();
        
        // Xử lý intent
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Xử lý intent từ deep link
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, "Received deep link: " + data.toString());
            
            // Kiểm tra xem có phải là email verification link không
            String mode = data.getQueryParameter("mode");
            String oobCode = data.getQueryParameter("oobCode");
            
            if ("verifyEmail".equals(mode) && oobCode != null) {
                // Đây là email verification link
                verifyEmail(oobCode);
            } else {
                // Không phải email verification link, reload user và kiểm tra
                checkEmailVerificationStatus();
            }
        } else {
            // Không có data, chỉ kiểm tra trạng thái
            checkEmailVerificationStatus();
        }
    }

    /**
     * Xác minh email với oobCode từ deep link
     */
    private void verifyEmail(String oobCode) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.applyActionCode(oobCode)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Email verification thành công");
                // Reload user để lấy trạng thái mới
                reloadUserAndContinue();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Email verification thất bại", e);
                Toast.makeText(this, "Xác minh email thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Vẫn kiểm tra trạng thái để đảm bảo
                checkEmailVerificationStatus();
            });
    }

    /**
     * Kiểm tra trạng thái xác minh email
     */
    private void checkEmailVerificationStatus() {
        FirebaseUser user = authService.getCurrentUser();
        if (user != null) {
            // Reload user để lấy trạng thái mới nhất
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    authService.refreshUser();
                    FirebaseUser reloadedUser = authService.getCurrentUser();
                    if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                        Log.d(TAG, "✅ Email đã được xác minh");
                        Toast.makeText(this, "Email đã được xác minh thành công!", Toast.LENGTH_SHORT).show();
                        continueToNextScreen();
                    } else {
                        Log.d(TAG, "Email chưa được xác minh");
                        // Vẫn cho tiếp tục vào app
                        continueToNextScreen();
                    }
                } else {
                    Log.e(TAG, "Error reloading user", task.getException());
                    // Vẫn cho tiếp tục vào app
                    continueToNextScreen();
                }
            });
        } else {
            // Không có user, quay về login
            goToLogin();
        }
    }

    /**
     * Reload user và tiếp tục vào màn hình tiếp theo
     */
    private void reloadUserAndContinue() {
        FirebaseUser user = authService.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                authService.refreshUser();
                continueToNextScreen();
            });
        } else {
            continueToNextScreen();
        }
    }

    /**
     * Tiếp tục vào màn hình tiếp theo (Questionnaire hoặc Main)
     * Kiểm tra và tạo user document nếu chưa có
     */
    private void continueToNextScreen() {
        FirebaseUser user = authService.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        String uid = user.getUid();
        
        // Kiểm tra xem user document đã tồn tại chưa
        // Nếu chưa (tức là đây là lần đầu xác minh email sau khi đăng ký), tạo user document và profile
        userDAO.getById(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // User document đã tồn tại, tiếp tục bình thường
                goToNextScreenAfterCheck();
            } else {
                // User document chưa tồn tại, tạo mới
                Log.d(TAG, "User document chưa tồn tại, đang tạo mới...");
                createUserDocumentAndProfile(user, () -> {
                    goToNextScreenAfterCheck();
                });
            }
        });
    }
    
    /**
     * Tạo user document và profile
     */
    private void createUserDocumentAndProfile(FirebaseUser user, Runnable onComplete) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
        
        long now = System.currentTimeMillis() / 1000;
        User.NotificationSettings notification = new User.NotificationSettings(null, true);
        User newUser = new User(
            uid,
            email,
            displayName,
            photoUrl,
            Arrays.asList("password"),
            now,
            now,
            notification,
            Arrays.asList("user")
        );
        
        // Tạo user document
        userDAO.save(newUser, saveTask -> {
            if (saveTask.isSuccessful()) {
                Log.d(TAG, "✅ Đã tạo user document");
                
                // Tạo profile
                Map<String, Object> init = new HashMap<>();
                init.put("uid", uid);
                init.put("lastUpdatedAt", System.currentTimeMillis());
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("units", "metric");
                prefs.put("cameraMode", "front");
                init.put("preferences", prefs);
                
                profileDAO.getDocument(uid).set(init)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Đã tạo profile");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Lỗi tạo profile", e);
                        // Vẫn tiếp tục dù có lỗi
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
            } else {
                Log.e(TAG, "❌ Lỗi tạo user document", saveTask.getException());
                // Vẫn tiếp tục dù có lỗi
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    /**
     * Chuyển đến màn hình tiếp theo sau khi đã kiểm tra user document
     */
    private void goToNextScreenAfterCheck() {
        FirebaseUser user = authService.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        String uid = user.getUid();
        
        // Kiểm tra xem user đã có profile chưa
        // Nếu chưa có profile → vào Questionnaire
        // Nếu đã có profile → vào MainActivity
        profileDAO.getDocument(uid).get()
            .addOnSuccessListener(snap -> {
                Intent intent;
                if (snap.exists()) {
                    // Đã có profile, vào MainActivity
                    intent = new Intent(this, MainActivity.class);
                } else {
                    // Chưa có profile, vào Questionnaire
                    intent = new Intent(this, RegistrationInfoActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking profile", e);
                // Fallback: vào Questionnaire
                Intent intent = new Intent(this, RegistrationInfoActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
    }

    /**
     * Quay về màn hình đăng nhập
     */
    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

