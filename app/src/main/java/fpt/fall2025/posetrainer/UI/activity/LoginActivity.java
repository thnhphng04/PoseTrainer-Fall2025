package fpt.fall2025.posetrainer.UI.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpt.fall2025.posetrainer.R;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.textfield.TextInputEditText;

import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.UserDAO;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin, buttonGoogleSignIn;
    TextView textViewRegister, textViewForgotPassword;

    private AuthService authService;
    private UserDAO userDAO;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;


    @Override
    public void onStart() {
        super.onStart();
        authService = new AuthService();
        // Check if user is already logged in
        checkCurrentUser();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new AuthService();
        userDAO = new UserDAO();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        editTextEmail = findViewById(R.id.et_email);
        editTextPassword = findViewById(R.id.et_password);
        buttonLogin = findViewById(R.id.btn_login);
        buttonGoogleSignIn = findViewById(R.id.btn_google_signin);
        textViewForgotPassword = findViewById(R.id.tv_forgot_password);
        textViewRegister = findViewById(R.id.tv_register_link);

        textViewForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterAccountActivity.class));
            finish();
        });

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            buttonLogin.setEnabled(false);

            authService.signInWithEmailPassword(email, password, task -> {
                buttonLogin.setEnabled(true);
                if (task.isSuccessful()) {
                    // Check if user is active
                    FirebaseUser user = authService.getCurrentUser();
                    if (user != null) {
                        checkUserActiveStatus(user.getUid(), () -> {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                        });
                    }
                } else {
                    String errorMessage = task.getException() != null
                            ? task.getException().getMessage()
                            : "Xác thực thất bại";
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("FIREBASE_AUTH", "Sign-in error: " + errorMessage);
                }
            });
        });

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
                boolean isNewUser = result != null && result.getAdditionalUserInfo() != null && result.getAdditionalUserInfo().isNewUser();
                FirebaseUser firebaseUser = authService.getCurrentUser();
                if (firebaseUser != null) {
                    Log.d("GOOGLE_AUTH", "Firebase user authenticated: " + firebaseUser.getUid());
                    Log.d("GOOGLE_AUTH", "Is new user: " + isNewUser);

                    if (isNewUser) {
                        Log.d("GOOGLE_AUTH", "Creating new user document");
                        createUserDocumentForGoogle(firebaseUser);
                    } else {
                        Log.d("GOOGLE_AUTH", "Updating existing user login time");
                        updateLastLoginAndProceed(firebaseUser);
                    }
                } else {
                    Log.e("GOOGLE_AUTH", "Firebase user is null after authentication");
                    Toast.makeText(LoginActivity.this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                }
            } else {
                Exception e = task.getException();
                Log.e("FIREBASE_AUTH", "Google credential sign-in error", e);
                Toast.makeText(LoginActivity.this, "Xác thực thất bại: " + (e != null ? e.getMessage() : "unknown"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createUserDocumentForGoogle(FirebaseUser firebaseUser) {
        Log.d("GOOGLE_AUTH", "Creating user document for UID: " + firebaseUser.getUid());
        
        authService.createUserDocument(firebaseUser, java.util.Arrays.asList("google.com"), task -> {
            if (task.isSuccessful()) {
                Log.d("FIRESTORE", "User document saved successfully");
                Toast.makeText(LoginActivity.this, "Đăng ký Google thành công", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            } else {
                Log.e("FIRESTORE", "Failed to save user", task.getException());
                Toast.makeText(LoginActivity.this, "Lưu thông tin người dùng thất bại: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                    Toast.LENGTH_LONG).show();
                // Still proceed to MainActivity even if save fails
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        });
    }

    private void updateLastLoginAndProceed(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        Log.d("GOOGLE_AUTH", "Updating last login time for UID: " + uid);

        // Check if user is active before proceeding
        checkUserActiveStatus(uid, () -> {
            authService.updateLastLogin(uid, task -> {
                if (task.isSuccessful()) {
                    Log.d("GOOGLE_AUTH", "Last login time updated successfully");
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                } else {
                    Log.w("GOOGLE_AUTH", "Failed to update last login, creating new user document");
                    // If update fails, try to create user document
                    createUserDocumentForGoogle(firebaseUser);
                }
            });
        });
    }

    /**
     * Check if user is already logged in and navigate to MainActivity if true
     */
    private void checkCurrentUser() {
        FirebaseUser currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, check if active
            Log.d("FIREBASE_AUTH", "User already logged in: " + currentUser.getUid());
            checkUserActiveStatus(currentUser.getUid(), () -> {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            });
        }
    }

    /**
     * Check if user is active, if not sign out and show message
     * @param uid User ID to check
     * @param onActive Callback to execute if user is active
     */
    private void checkUserActiveStatus(String uid, Runnable onActive) {
        userDAO.getById(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                fpt.fall2025.posetrainer.Domain.User user = task.getResult();
                if (user.isActive()) {
                    onActive.run();
                } else {
                    // User is deactivated
                    authService.signOut();
                    Toast.makeText(LoginActivity.this, "Tài khoản của bạn đã bị vô hiệu hóa.", Toast.LENGTH_LONG).show();
                }
            } else {
                // User document not found, proceed (new user or error)
                onActive.run();
            }
        });
    }
}