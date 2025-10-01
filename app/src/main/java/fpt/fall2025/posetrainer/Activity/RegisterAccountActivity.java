// Updated Register.java
package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RegisterAccountActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    TextInputEditText editTextDisplayName, editTextPhotoUrl, editTextFcmToken;
    TextInputLayout layoutDisplayName, layoutPhotoUrl, layoutFcmToken;
    Button buttonReg;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    boolean allowNotifications = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Gán view
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextDisplayName = findViewById(R.id.editTextDisplayName);
        editTextPhotoUrl = findViewById(R.id.editTextPhotoUrl);


        layoutDisplayName = findViewById(R.id.layoutDisplayName);
        layoutPhotoUrl = findViewById(R.id.layoutPhotoUrl);



        buttonReg = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progessBar);
        textView = findViewById(R.id.loginNow);


        textView.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // No dynamic role-based field toggling needed; roles are checkboxes


        buttonReg.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);

            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Invalid email format");
                return;
            }

            if (password.length() < 6) {
                showToast("Password must be at least 6 characters");
                return;
            }

            String displayName = Objects.requireNonNull(editTextDisplayName.getText()).toString().trim();
            String photoUrl = Objects.requireNonNull(editTextPhotoUrl.getText()).toString().trim();
            // Bỏ editTextFcmToken (chưa có trong XML)
            String fcmToken = null;

            if (TextUtils.isEmpty(displayName)) {
                showToast("Enter display name");
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                long now = System.currentTimeMillis() / 1000; // seconds
                                User.NotificationSettings notification = new User.NotificationSettings(fcmToken, true);

                                User newUser = new User(
                                        uid,
                                        email,
                                        TextUtils.isEmpty(displayName) ? "" : displayName,
                                        TextUtils.isEmpty(photoUrl) ? "" : photoUrl,
                                        Arrays.asList("password"),
                                        now,
                                        now,
                                        notification,
                                        Arrays.asList("user")   // default role
                                );

                                db.collection("users").document(uid).set(newUser)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegisterAccountActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("FIRESTORE", "Failed to save user", e);
                                            showToast("Failed to save user: " + e.getMessage());
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e != null) {
                                Log.e("RegisterActivity", "Auth failed", e);
                                showToast("Authentication failed: " + e.getMessage());
                            } else {
                                showToast("Authentication failed: unknown error");
                            }
                        }
                    });
        });

    }

    private void showToast(String message) {
        Toast.makeText(RegisterAccountActivity.this, message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }
}
