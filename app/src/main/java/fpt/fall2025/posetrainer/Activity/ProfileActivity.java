package fpt.fall2025.posetrainer.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ImageView profileImage, btnBack;
    private TextView profileName, profileEmail;
    private TextView tvDisplayName, tvEmail, tvPhotoUrl, tvCreatedAt, tvLastLogin;
    private TextInputEditText etDisplayName;
    private Button btnEditProfile, btnSave, btnCancel, btnLogout;
    private LinearLayout layoutSaveCancel;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private boolean isEditMode = false;
    private Uri selectedImageUri;
    private String currentPhotoUrl;

    // Google sign-in client (for proper sign-out when account was Google)
    private GoogleSignInClient googleClient;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this)
                                .load(selectedImageUri)
                                .placeholder(R.drawable.profile)
                                .error(R.drawable.profile)
                                .into(profileImage);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Init GoogleSignInClient (safe even if user didn't sign in via Google)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupClickListeners();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        etDisplayName = findViewById(R.id.et_display_name);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnLogout = findViewById(R.id.btn_logout);

        layoutSaveCancel = findViewById(R.id.layout_save_cancel);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> enterEditMode());

        btnSave.setOnClickListener(v -> saveProfile());

        btnCancel.setOnClickListener(v -> exitEditMode());

        profileImage.setOnClickListener(v -> {
            if (isEditMode) openImagePicker();
        });

        // Use safe logout flow
        btnLogout.setOnClickListener(v -> logout());
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
        Log.d(TAG, "Loading profile for UID: " + uid);
        Log.d(TAG, "Current user email: " + currentUser.getEmail());
        Log.d(TAG, "Current user display name: " + currentUser.getDisplayName());

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) updateProfileUI(user);
                        else Log.e(TAG, "Failed to convert document to User");
                    } else {
                        Log.w(TAG, "User document not found in Firestore; fallback to Auth user");
                        updateProfileUIFromFirebaseAuth(mAuth.getCurrentUser());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user profile", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileUI(User user) {
        // Display name
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            profileName.setText(user.getDisplayName());
            tvDisplayName.setText(user.getDisplayName());
        } else {
            profileName.setText("User");
            tvDisplayName.setText("Not set");
        }

        // Email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            profileEmail.setText(user.getEmail());
            tvEmail.setText(user.getEmail());
        } else {
            profileEmail.setText("No email");
            tvEmail.setText("Not set");
        }

        // Photo
        if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
            currentPhotoUrl = user.getPhotoURL();
            Glide.with(this)
                    .load(user.getPhotoURL())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
            tvPhotoUrl.setText(user.getPhotoURL());
        } else {
            currentPhotoUrl = null;
            profileImage.setImageResource(R.drawable.profile);
            tvPhotoUrl.setText("Not set");
        }

        // Timestamps
        tvCreatedAt.setText(user.getCreatedAt() > 0 ? formatTimestamp(user.getCreatedAt()) : "Unknown");
        tvLastLogin.setText(user.getLastLoginAt() > 0 ? formatTimestamp(user.getLastLoginAt()) : "Unknown");
    }

    private void enterEditMode() {
        isEditMode = true;

        tvDisplayName.setVisibility(View.GONE);
        etDisplayName.setVisibility(View.VISIBLE);
        etDisplayName.setText(tvDisplayName.getText().toString());

        btnEditProfile.setVisibility(View.GONE);
        layoutSaveCancel.setVisibility(View.VISIBLE);

        profileImage.setClickable(true);
        profileImage.setFocusable(true);
    }

    private void exitEditMode() {
        isEditMode = false;

        tvDisplayName.setVisibility(View.VISIBLE);
        etDisplayName.setVisibility(View.GONE);

        btnEditProfile.setVisibility(View.VISIBLE);
        layoutSaveCancel.setVisibility(View.GONE);

        profileImage.setClickable(false);
        profileImage.setFocusable(false);

        selectedImageUri = null;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        String displayName = etDisplayName.getText().toString().trim();

        if (TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            return;
        }

        if (selectedImageUri != null) {
            uploadImageAndUpdateProfile(currentUser, displayName);
        } else {
            updateProfile(currentUser, displayName, currentPhotoUrl);
        }
    }

    private void uploadImageAndUpdateProfile(FirebaseUser currentUser, String displayName) {
        String uid = currentUser.getUid();
        StorageReference imageRef = storageRef.child("profile_images/" + uid + ".jpg");

        UploadTask uploadTask = imageRef.putFile(selectedImageUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(uri ->
                        updateProfile(currentUser, displayName, uri.toString())
                ).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                })
        ).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image", e);
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
        });
    }

    private void updateProfile(FirebaseUser currentUser, String displayName, String photoUrl) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(photoUrl != null ? Uri.parse(photoUrl) : null)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFirestoreUser(currentUser, displayName, photoUrl);
                    } else {
                        Log.e(TAG, "Failed to update Firebase Auth profile", task.getException());
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                    }
                });
    }

    private void updateFirestoreUser(FirebaseUser currentUser, String displayName, String photoUrl) {
        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .update("displayName", displayName, "photoURL", photoUrl)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    exitEditMode();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore user document", e);
                    Toast.makeText(this, "Profile updated but failed to sync with server", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    exitEditMode();
                    loadUserProfile();
                });
    }

    private String formatTimestamp(long timestamp) {
        try {
            long timestampMs = timestamp < 10000000000L ? timestamp * 1000 : timestamp;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestampMs));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Invalid date";
        }
    }

    private void updateProfileUIFromFirebaseAuth(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            profileName.setText(firebaseUser.getDisplayName());
            tvDisplayName.setText(firebaseUser.getDisplayName());
        } else {
            profileName.setText("User");
            tvDisplayName.setText("Not set");
        }

        if (firebaseUser.getEmail() != null && !firebaseUser.getEmail().isEmpty()) {
            profileEmail.setText(firebaseUser.getEmail());
            tvEmail.setText(firebaseUser.getEmail());
        } else {
            profileEmail.setText("No email");
            tvEmail.setText("Not set");
        }

        if (firebaseUser.getPhotoUrl() != null) {
            currentPhotoUrl = firebaseUser.getPhotoUrl().toString();
            Glide.with(this)
                    .load(firebaseUser.getPhotoUrl())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
            tvPhotoUrl.setText(currentPhotoUrl);
        } else {
            currentPhotoUrl = null;
            profileImage.setImageResource(R.drawable.profile);
            tvPhotoUrl.setText("Not set");
        }

        tvCreatedAt.setText("Unknown");
        tvLastLogin.setText("Unknown");

        createUserDocumentInBackground(firebaseUser);
    }

    private void createUserDocumentInBackground(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

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

        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(unused -> Log.d(TAG, "User document created successfully in background"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user document in background", e));
    }

    // ===== Logout flow (safe) =====

    private void logout() {
        // Avoid double clicks
        btnLogout.setEnabled(false);
        showLoggingOut(true);

        boolean isGoogleSignedIn = GoogleSignIn.getLastSignedInAccount(this) != null;

        if (isGoogleSignedIn && googleClient != null) {
            // Sign out Google first (safe even if token expired)
            googleClient.signOut()
                    .addOnCompleteListener(task -> {
                        // Do NOT call revokeAccess unless you need to fully disconnect OAuth
                        safeFirebaseSignOut();
                        navigateToLogin();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Google signOut failed: " + e.getMessage());
                        safeFirebaseSignOut();
                        navigateToLogin();
                    });
        } else {
            // Not a Google session → just Firebase signOut
            safeFirebaseSignOut();
            navigateToLogin();
        }
    }

    private void safeFirebaseSignOut() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            Log.w(TAG, "Firebase signOut error: " + e.getMessage());
        }
    }

    private void navigateToLogin() {
        showLoggingOut(false);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoggingOut(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnLogout.setEnabled(!show);
        btnEditProfile.setEnabled(!show);
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
    }
}
