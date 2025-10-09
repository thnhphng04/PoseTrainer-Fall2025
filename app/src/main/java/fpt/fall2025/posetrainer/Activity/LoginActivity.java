package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpt.fall2025.posetrainer.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
 
import com.google.android.material.textfield.TextInputEditText;
import fpt.fall2025.posetrainer.Domain.User;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    ProgressBar progressBar;
    TextView textView, forgetPassword;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
 

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Ánh xạ view
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progessBar);
        textView = findViewById(R.id.registerNow);
        forgetPassword = findViewById(R.id.forgetPassword);

//        // ===== Quên mật khẩu =====
//        forgetPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPassword.class)));

        // ===== Chuyển sang đăng ký =====
        textView.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterAccountActivity.class));
            finish();
        });

        // ===== Đăng nhập bằng Email/Password =====
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                        } else {
                            String errorMessage = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Authentication failed";
                            Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("FIREBASE_AUTH", "Sign-in error: " + errorMessage);
                        }
                    });
        });

        // ===== Đăng nhập bằng Google =====
        findViewById(R.id.btn_google_signin).setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
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
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Log.e("GOOGLE_SIGN_IN", "SignIn failed", e);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
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
                                Log.d("GOOGLE_AUTH", "Updating existing user login time");
                                updateLastLoginAndProceed(firebaseUser);
                            }
                        } else {
                            Log.e("GOOGLE_AUTH", "Firebase user is null after authentication");
                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("FIREBASE_AUTH", "Google credential sign-in error", e);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + (e != null ? e.getMessage() : "unknown"), Toast.LENGTH_LONG).show();
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
                java.util.Arrays.asList("google.com"),
                now,
                now,
                notification,
                java.util.Arrays.asList("user")
        );

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("FIRESTORE", "Attempting to save user to Firestore...");
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(unused -> {
                    Log.d("FIRESTORE", "User document saved successfully");
                    Toast.makeText(LoginActivity.this, "Đăng ký Google thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Failed to save user", e);
                    Toast.makeText(LoginActivity.this, "Lưu thông tin người dùng thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still proceed to MainActivity even if save fails
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                });
    }

    private void updateLastLoginAndProceed(FirebaseUser firebaseUser) {
        long now = System.currentTimeMillis() / 1000;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = firebaseUser.getUid();
        
        Log.d("GOOGLE_AUTH", "Checking if user document exists for UID: " + uid);
        
        // First check if user document exists
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("GOOGLE_AUTH", "User document exists, updating lastLoginAt");
                        // Update last login time
                        db.collection("users").document(uid)
                                .update("lastLoginAt", now)
                                .addOnCompleteListener(unused -> {
                                    Log.d("GOOGLE_AUTH", "Last login time updated successfully");
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                });
                    } else {
                        Log.w("GOOGLE_AUTH", "User document not found, creating new one");
                        // Create user document if it doesn't exist
                        createUserDocumentForGoogle(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GOOGLE_AUTH", "Failed to check user document", e);
                    // If check fails, try to create user document
                    createUserDocumentForGoogle(firebaseUser);
                });
    }


}
