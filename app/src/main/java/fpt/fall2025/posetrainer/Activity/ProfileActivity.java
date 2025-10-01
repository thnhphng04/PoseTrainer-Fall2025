package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    
    private ImageView profileImage, btnBack;
    private TextView profileName, profileEmail;
    private TextView tvDisplayName, tvEmail, tvPhotoUrl, tvCreatedAt, tvLastLogin, tvRoles;
    private Button btnLogout;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load user profile
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        profileImage = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhotoUrl = findViewById(R.id.tv_photo_url);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvLastLogin = findViewById(R.id.tv_last_login);
        tvRoles = findViewById(R.id.tv_roles);
        
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            // Navigate to intro screen
            Intent intent = new Intent(this, IntroActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user found");
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            updateProfileUI(user);
                        }
                    } else {
                        Log.w(TAG, "User document not found in Firestore");
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user profile", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileUI(User user) {
        // Update header profile info
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            profileName.setText(user.getDisplayName());
            tvDisplayName.setText(user.getDisplayName());
        } else {
            profileName.setText("User");
            tvDisplayName.setText("Not set");
        }

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            profileEmail.setText(user.getEmail());
            tvEmail.setText(user.getEmail());
        } else {
            profileEmail.setText("No email");
            tvEmail.setText("Not set");
        }

        // Update profile image
        if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoURL())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
            tvPhotoUrl.setText(user.getPhotoURL());
        } else {
            profileImage.setImageResource(R.drawable.profile);
            tvPhotoUrl.setText("Not set");
        }

        // Update timestamps
        if (user.getCreatedAt() > 0) {
            String createdDate = formatTimestamp(user.getCreatedAt());
            tvCreatedAt.setText(createdDate);
        } else {
            tvCreatedAt.setText("Unknown");
        }

        if (user.getLastLoginAt() > 0) {
            String lastLoginDate = formatTimestamp(user.getLastLoginAt());
            tvLastLogin.setText(lastLoginDate);
        } else {
            tvLastLogin.setText("Unknown");
        }

        // Update roles
        List<String> roles = user.getRoles();
        if (roles != null && !roles.isEmpty()) {
            tvRoles.setText(String.join(", ", roles));
        } else {
            tvRoles.setText("No roles assigned");
        }
    }

    private String formatTimestamp(long timestamp) {
        try {
            // Convert from seconds to milliseconds if needed
            long timestampMs = timestamp;
            if (timestamp < 10000000000L) { // If timestamp is in seconds
                timestampMs = timestamp * 1000;
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestampMs));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Invalid date";
        }
    }
}
