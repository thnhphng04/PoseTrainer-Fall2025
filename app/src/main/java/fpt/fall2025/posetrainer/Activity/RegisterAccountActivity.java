package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.Objects;

public class RegisterAccountActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    TextInputEditText editTextDisplayName, editTextPhotoUrl;
    TextInputLayout layoutEmail, layoutPassword, layoutConfirmPassword;
    TextInputLayout layoutDisplayName, layoutPhotoUrl;
    Button buttonReg, buttonGoogleSignIn;
    TextView textViewBackToLogin;
    FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Gán view theo layout mới
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

        // Quay lại đăng nhập
        textViewBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // Đăng ký bằng Email/Password
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

            // Validation
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

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        buttonReg.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

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

                                db.collection("users").document(uid).set(newUser)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegisterAccountActivity.this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FIRESTORE", "Failed to save user", e);
                                            Toast.makeText(RegisterAccountActivity.this, "Lỗi khi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            String errorMsg = e != null ? e.getMessage() : "Lỗi không xác định";
                            Log.e("RegisterActivity", "Auth failed", e);
                            Toast.makeText(RegisterAccountActivity.this, "Lỗi đăng ký: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Đăng ký bằng Google
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
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    buttonGoogleSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        AuthResult result = task.getResult();
                        boolean isNewUser = result != null && result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser();
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            Log.d("GOOGLE_AUTH", "Firebase user authenticated: " + firebaseUser.getUid());
                            Log.d("GOOGLE_AUTH", "Is new user: " + isNewUser);

                            if (isNewUser) {
                                Log.d("GOOGLE_AUTH", "Creating new user document");
                                createUserDocumentForGoogle(firebaseUser);
                            } else {
                                Log.d("GOOGLE_AUTH", "User already exists, proceeding to MainActivity");
                                Toast.makeText(RegisterAccountActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();
                            }
                        } else {
                            Log.e("GOOGLE_AUTH", "Firebase user is null after authentication");
                            Toast.makeText(RegisterAccountActivity.this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("FIREBASE_AUTH", "Google credential sign-in error", e);
                        Toast.makeText(RegisterAccountActivity.this, "Xác thực thất bại: " + (e != null ? e.getMessage() : "unknown"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserDocumentForGoogle(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        Log.d("GOOGLE_AUTH", "Creating user document for UID: " + uid);
        Log.d("GOOGLE_AUTH", "Email: " + email);
        Log.d("GOOGLE_AUTH", "Display Name: " + displayName);
        Log.d("GOOGLE_AUTH", "Photo URL: " + photoUrl);

        long now = System.currentTimeMillis() / 1000;
        User.NotificationSettings notification = new User.NotificationSettings(null, true);
        User newUser = new User(
                uid,
                email,
                displayName,
                photoUrl,
                Arrays.asList("google.com"),
                now,
                now,
                notification,
                Arrays.asList("user")
        );

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("FIRESTORE", "Attempting to save user to Firestore...");
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(unused -> {
                    Log.d("FIRESTORE", "User document saved successfully");
                    Toast.makeText(RegisterAccountActivity.this, "Đăng ký Google thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Failed to save user", e);
                    Toast.makeText(RegisterAccountActivity.this, "Lưu thông tin người dùng thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still proceed to MainActivity even if save fails
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                });
    }
}