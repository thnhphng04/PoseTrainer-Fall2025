package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;

public class RegisterAccountActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputEditText editTextDisplayName, editTextPhotoUrl;
    private TextInputLayout layoutEmail, layoutPassword, layoutConfirmPassword;
    private TextInputLayout layoutDisplayName, layoutPhotoUrl;
    private Button buttonReg, buttonGoogleSignIn;
    private TextView textViewBackToLogin;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

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
        editTextPhotoUrl = findViewById(R.id.et_photo_url);

        layoutEmail = findViewById(R.id.til_email);
        layoutPassword = findViewById(R.id.til_password);
        layoutConfirmPassword = findViewById(R.id.til_confirm_password);
        layoutDisplayName = findViewById(R.id.til_display_name);
        layoutPhotoUrl = findViewById(R.id.til_photo_url);

        buttonReg = findViewById(R.id.btn_register);
        buttonGoogleSignIn = findViewById(R.id.btn_google_signin);
        textViewBackToLogin = findViewById(R.id.tv_back_to_login);

        // Back to login
        textViewBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // Email/Password register with pre-check
        buttonReg.setOnClickListener(v -> {
            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();
            String confirmPassword = Objects.requireNonNull(editTextConfirmPassword.getText()).toString().trim();
            String displayName = Objects.requireNonNull(editTextDisplayName.getText()).toString().trim();
            String photoUrl = Objects.requireNonNull(editTextPhotoUrl.getText()).toString().trim();

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

            buttonReg.setEnabled(false);

            // 1) Pre-check: email đã tồn tại trên Firebase Auth chưa?
            mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(checkTask -> {
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

                // 2) Tạo tài khoản
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(createTask -> {
                    buttonReg.setEnabled(true);
                    if (createTask.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // users/{uid}
                            createUserDoc(uid, email, displayName, photoUrl, Arrays.asList("password"));

                            // profiles/{uid} rỗng + defaults, rồi vào Questionnaire
                            ensureEmptyProfile(uid)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                                        goToQuestionnaire(uid);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Lỗi khởi tạo profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        goToQuestionnaire(uid); // vẫn cho tiếp tục
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
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            buttonGoogleSignIn.setEnabled(true);
            if (task.isSuccessful()) {
                AuthResult result = task.getResult();
                boolean isNewUser = result != null
                        && result.getAdditionalUserInfo() != null
                        && result.getAdditionalUserInfo().isNewUser();
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
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
                               java.util.List<String> providers) {
        long now = System.currentTimeMillis() / 1000;
        User.NotificationSettings notification = new User.NotificationSettings(null, true);
        User newUser = new User(uid, email, displayName, photoUrl, providers, now, now, notification, Arrays.asList("user"));
        FirebaseFirestore.getInstance().collection("users").document(uid).set(newUser);
    }

    /** Tạo profiles/{uid} nếu chưa có: rỗng + defaults */
    private com.google.android.gms.tasks.Task<Void> ensureEmptyProfile(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("profiles").document(uid);

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
            FirebaseFirestore.getInstance().collection("profiles").document(uid).get()
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