package fpt.fall2025.posetrainer.UI.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.DAL.ProfileDAO;
import fpt.fall2025.posetrainer.UI.dialog.EmailVerificationDialog;
import fpt.fall2025.posetrainer.UI.dialog.TermsOfServiceDialog;
import fpt.fall2025.posetrainer.R;

public class RegisterAccountActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputEditText editTextDisplayName;
    private TextInputLayout layoutEmail, layoutPassword, layoutConfirmPassword;
    private TextInputLayout layoutDisplayName;
    private Button buttonReg, buttonGoogleSignIn;
    private TextView textViewBackToLogin;
    private CheckBox checkBoxTerms;
    private TextView textViewTermsLink;
    private boolean termsAgreed = false; // Flag để đánh dấu đã đồng ý qua dialog
    private boolean isSettingCheckboxProgrammatically = false; // Flag để tránh trigger listener khi setChecked programmatically

    private AuthService authService;
    private UserDAO userDAO;
    private ProfileDAO profileDAO;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthService();
        userDAO = new UserDAO();
        profileDAO = new ProfileDAO();
        
        // Kiểm tra nếu có user chưa xác minh email, xóa tài khoản
        checkAndCleanupUnverifiedUser();

        // Google Sign-In config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Bind views
        editTextEmail = findViewById(R.id.et_email);
        editTextPassword = findViewById(R.id.et_password);
        editTextConfirmPassword = findViewById(R.id.et_confirm_password);
        editTextDisplayName = findViewById(R.id.et_display_name);

        layoutEmail = findViewById(R.id.til_email);
        layoutPassword = findViewById(R.id.til_password);
        layoutConfirmPassword = findViewById(R.id.til_confirm_password);
        layoutDisplayName = findViewById(R.id.til_display_name);

        buttonReg = findViewById(R.id.btn_register);
        buttonGoogleSignIn = findViewById(R.id.btn_google_signin);
        textViewBackToLogin = findViewById(R.id.tv_back_to_login);
        checkBoxTerms = findViewById(R.id.cb_terms);
        textViewTermsLink = findViewById(R.id.tv_terms_link);

        // Back to login
        textViewBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // Terms and Conditions - Click on checkbox or text to show dialog
        // Ngăn chặn tick trực tiếp, chỉ cho phép tick sau khi đồng ý trong dialog
        checkBoxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Bỏ qua nếu đang setChecked programmatically
            if (isSettingCheckboxProgrammatically) {
                return;
            }
            
            if (isChecked && !termsAgreed) {
                // Nếu cố gắng tick mà chưa đồng ý qua dialog, revert lại và hiển thị dialog
                isSettingCheckboxProgrammatically = true;
                checkBoxTerms.setChecked(false);
                isSettingCheckboxProgrammatically = false;
                showTermsOfServiceDialog();
            }
        });
        
        View.OnClickListener showTermsDialogListener = v -> {
            // Chỉ hiển thị dialog nếu chưa đồng ý
            if (!termsAgreed) {
                showTermsOfServiceDialog();
            }
        };
        // Click vào checkbox hoặc text đều hiển thị dialog
        checkBoxTerms.setOnClickListener(showTermsDialogListener);
        textViewTermsLink.setOnClickListener(showTermsDialogListener);

        // Email/Password register with pre-check
        buttonReg.setOnClickListener(v -> {
            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();
            String confirmPassword = Objects.requireNonNull(editTextConfirmPassword.getText()).toString().trim();
            String displayName = Objects.requireNonNull(editTextDisplayName.getText()).toString().trim();
            String photoUrl = ""; // Không sử dụng URL ảnh đại diện khi đăng ký

            // Reset errors
            layoutEmail.setError(null);
            layoutPassword.setError(null);
            layoutConfirmPassword.setError(null);
            layoutDisplayName.setError(null);

            // Validate
            if (TextUtils.isEmpty(email)) {
                layoutEmail.setError("Vui lòng nhập email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                layoutEmail.setError("Định dạng email không hợp lệ");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                layoutPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }
            if (password.length() < 6) {
                layoutPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                layoutConfirmPassword.setError("Vui lòng nhập lại mật khẩu");
                return;
            }
            if (!password.equals(confirmPassword)) {
                layoutConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                return;
            }
            if (TextUtils.isEmpty(displayName)) {
                layoutDisplayName.setError("Vui lòng nhập tên hiển thị");
                return;
            }
            
            // Check terms and conditions
            if (!checkBoxTerms.isChecked()) {
                Toast.makeText(this, "Bạn cần xác nhận điều khoản sử dụng để đăng ký", Toast.LENGTH_LONG).show();
                return;
            }

            buttonReg.setEnabled(false);

            // 1) Pre-check: email đã tồn tại trên Firebase Auth chưa?
            authService.getAuth().fetchSignInMethodsForEmail(email).addOnCompleteListener(checkTask -> {
                if (!checkTask.isSuccessful()) {
                    buttonReg.setEnabled(true);
                    Toast.makeText(this, "Không kiểm tra được email: " +
                                    (checkTask.getException() != null ? checkTask.getException().getMessage() : ""),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                boolean existed = checkTask.getResult() != null
                        && checkTask.getResult().getSignInMethods() != null
                        && !checkTask.getResult().getSignInMethods().isEmpty();
                if (existed) {
                    buttonReg.setEnabled(true);
                    layoutEmail.setError("Email đã được đăng ký, vui lòng đăng nhập");
                    return;
                }

                // 2) Tạo tài khoản Firebase Auth (NHƯNG CHƯA tạo user document)
                authService.createUserWithEmailPassword(email, password, createTask -> {
                    buttonReg.setEnabled(true);
                    if (createTask.isSuccessful()) {
                        FirebaseUser firebaseUser = authService.getCurrentUser();
                        if (firebaseUser != null) {
                            // Gửi email xác minh (không dùng continueUrl để tránh lỗi domain not allowlisted)
                            authService.sendEmailVerification(sendTask -> {
                                if (sendTask.isSuccessful()) {
                                    Log.d("REGISTER", "✅ Đã gửi email verification");
                                } else {
                                    Log.e("REGISTER", "❌ Lỗi gửi email verification", sendTask.getException());
                                    // Nếu không gửi được email, xóa tài khoản và thông báo lỗi
                                    authService.deleteCurrentUser(deleteTask -> {
                                        String errorMsg = "Không thể gửi email xác minh. ";
                                        if (sendTask.getException() != null) {
                                            String exceptionMsg = sendTask.getException().getMessage();
                                            if (exceptionMsg != null && exceptionMsg.contains("domain")) {
                                                errorMsg += "Vui lòng kiểm tra cấu hình Firebase.";
                                            } else {
                                                errorMsg += "Vui lòng thử lại sau.";
                                            }
                                        } else {
                                            errorMsg += "Vui lòng thử lại sau.";
                                        }
                                        Toast.makeText(RegisterAccountActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                    });
                                    return;
                                }
                                
                                // Hiển thị dialog xác minh email
                                // CHƯA tạo user document và profile - chỉ tạo khi email đã được xác minh
                                showEmailVerificationDialog(email, displayName, photoUrl);
                            });
                        }
                    } else {
                        Toast.makeText(this,
                                "Lỗi đăng ký: " + (createTask.getException() != null ? createTask.getException().getMessage() : "Không xác định"),
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        // Google Sign-In
        buttonGoogleSignIn.setOnClickListener(v -> {
            buttonGoogleSignIn.setEnabled(false);
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    buttonGoogleSignIn.setEnabled(true);
                    Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                buttonGoogleSignIn.setEnabled(true);
                Log.e("GOOGLE_SIGN_IN", "SignIn failed", e);
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        authService.signInWithGoogle(idToken, task -> {
            buttonGoogleSignIn.setEnabled(true);
            if (task.isSuccessful()) {
                AuthResult result = task.getResult();
                boolean isNewUser = result != null
                        && result.getAdditionalUserInfo() != null
                        && result.getAdditionalUserInfo().isNewUser();
                FirebaseUser firebaseUser = authService.getCurrentUser();
                if (firebaseUser != null) {
                    routeAfterAuth(firebaseUser, isNewUser);
                } else {
                    Toast.makeText(this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("FIREBASE_AUTH", "Google credential sign-in error", task.getException());
                Toast.makeText(this, "Xác thực thất bại: " +
                                (task.getException() != null ? task.getException().getMessage() : "unknown"),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /* ======================== Helpers ======================== */
    
    private void showTermsOfServiceDialog() {
        TermsOfServiceDialog dialog = new TermsOfServiceDialog(this);
        dialog.setOnAgreeListener(new TermsOfServiceDialog.OnAgreeListener() {
            @Override
            public void onAgree() {
                // Khi người dùng đồng ý, đánh dấu và tick vào checkbox
                termsAgreed = true;
                isSettingCheckboxProgrammatically = true;
                checkBoxTerms.setChecked(true);
                isSettingCheckboxProgrammatically = false;
            }
            
            @Override
            public void onCancel() {
                // Khi người dùng hủy, không tick checkbox
                // Không làm gì cả, checkbox vẫn ở trạng thái unchecked
            }
        });
        dialog.show();
    }

    private void goToQuestionnaire(String uid) {
        Intent intent = new Intent(getApplicationContext(), RegistrationInfoActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMain() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    private void createUserDoc(String uid, String email, String displayName, String photoUrl,
                               List<String> providers) {
        long now = System.currentTimeMillis() / 1000;
        User.NotificationSettings notification = new User.NotificationSettings(null, true);
        User newUser = new User(uid, email, displayName, photoUrl, providers, now, now, notification, Arrays.asList("user"));
        userDAO.save(newUser, null); // Fire and forget
    }

    /** Tạo profiles/{uid} nếu chưa có: rỗng + defaults */
    private Task<Void> ensureEmptyProfile(String uid) {
        com.google.firebase.firestore.DocumentReference docRef = profileDAO.getDocument(uid);

        Map<String, Object> init = new HashMap<>();
        init.put("uid", uid);
        init.put("lastUpdatedAt", System.currentTimeMillis());
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("units", "metric");
        prefs.put("cameraMode", "front");
        init.put("preferences", prefs);

        return docRef.get().continueWithTask(t -> {
            DocumentSnapshot snap = t.getResult();
            if (snap != null && snap.exists()) {
                return Tasks.forResult(null);
            }
            return docRef.set(init);
        });
    }

    /**
     * Hiển thị dialog xác minh email
     * Bắt buộc phải xác minh email mới được tiếp tục
     * CHỈ tạo user document và profile khi email đã được xác minh
     */
    private void showEmailVerificationDialog(String email, String displayName, String photoUrl) {
        EmailVerificationDialog dialog = EmailVerificationDialog.newInstance(email);
        dialog.setOnVerificationCompleteListener(new EmailVerificationDialog.OnVerificationCompleteListener() {
            @Override
            public void onVerified() {
                // Người dùng đã xác minh email, BÂY GIỜ mới tạo user document và profile
                FirebaseUser user = authService.getCurrentUser();
                if (user != null && user.isEmailVerified()) {
                    String uid = user.getUid();
                    
                    // Tạo user document trong Firestore
                    createUserDoc(uid, email, displayName, photoUrl, Arrays.asList("password"));
                    
                    // Tạo profile
                    ensureEmptyProfile(uid)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(RegisterAccountActivity.this,
                                        "Email đã được xác minh thành công!",
                                        Toast.LENGTH_SHORT).show();
                                goToQuestionnaire(uid);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("REGISTER", "Lỗi khởi tạo profile", e);
                                Toast.makeText(RegisterAccountActivity.this,
                                        "Email đã được xác minh. Đang hoàn tất đăng ký...",
                                        Toast.LENGTH_SHORT).show();
                                goToQuestionnaire(uid);
                            });
                } else {
                    // Vẫn chưa xác minh, không cho tiếp tục
                    Toast.makeText(RegisterAccountActivity.this,
                            "Vui lòng xác minh email trước khi tiếp tục.",
                            Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onDialogClosed() {
                // Người dùng đóng dialog mà chưa xác minh email
                // Xóa tài khoản Firebase Auth vì chưa hoàn tất đăng ký
                FirebaseUser user = authService.getCurrentUser();
                if (user != null && !user.isEmailVerified()) {
                    Log.d("REGISTER", "Người dùng đóng dialog chưa xác minh, xóa tài khoản");
                    authService.deleteCurrentUser(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(RegisterAccountActivity.this,
                                    "Đăng ký chưa hoàn tất. Vui lòng xác minh email để tiếp tục.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("REGISTER", "Lỗi xóa tài khoản", deleteTask.getException());
                        }
                    });
                }
            }
        });
        // Không cho phép đóng dialog bằng cách click bên ngoài hoặc back button
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "EmailVerificationDialog");
    }

    /**
     * Kiểm tra và xóa tài khoản chưa xác minh email (nếu có)
     * Được gọi khi mở RegisterAccountActivity
     */
    private void checkAndCleanupUnverifiedUser() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && !currentUser.isEmailVerified()) {
            // Có user chưa xác minh email, xóa tài khoản
            Log.d("REGISTER", "Phát hiện user chưa xác minh email, đang xóa tài khoản");
            authService.deleteCurrentUser(deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    Log.d("REGISTER", "✅ Đã xóa tài khoản chưa xác minh");
                } else {
                    Log.e("REGISTER", "❌ Lỗi xóa tài khoản", deleteTask.getException());
                }
            });
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại khi resume (trường hợp user quay lại từ email app)
        checkAndCleanupUnverifiedUser();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Khi đóng activity, nếu user chưa xác minh email, xóa tài khoản
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null && !currentUser.isEmailVerified()) {
            // Kiểm tra xem user document đã tồn tại chưa
            userDAO.getById(currentUser.getUid(), task -> {
                if (task.isSuccessful() && task.getResult() == null) {
                    // User document chưa tồn tại → chưa hoàn tất đăng ký → xóa tài khoản
                    Log.d("REGISTER", "Activity bị destroy, user chưa xác minh và chưa có user document, xóa tài khoản");
                    authService.deleteCurrentUser(null);
                }
            });
        }
    }

    /** Điều hướng sau khi đăng nhập Google */
    private void routeAfterAuth(FirebaseUser firebaseUser, boolean isNewUser) {
        String uid = firebaseUser.getUid();

        if (isNewUser) {
            String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
            String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
            String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

            createUserDoc(uid, email, displayName, photoUrl, Arrays.asList("google.com"));
            ensureEmptyProfile(uid).addOnCompleteListener(t -> {
                Toast.makeText(this, "Đăng ký Google thành công", Toast.LENGTH_SHORT).show();
                goToQuestionnaire(uid);
            });
        } else {
            // Nếu đã có profile → vào Main, chưa có → vào Questionnaire
            profileDAO.getDocument(uid).get()
                    .addOnSuccessListener(snap -> {
                        if (snap.exists()) {
                            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            goToQuestionnaire(uid);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIRESTORE", "Check profile failed", e);
                        // fallback: vẫn cho vào Main
                        goToMain();
                    });
        }
    }
}