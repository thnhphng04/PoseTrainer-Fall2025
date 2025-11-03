package fpt.fall2025.posetrainer.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import fpt.fall2025.posetrainer.R;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private ImageView imgProfile;
    private EditText etName;
    private TextView tvEmail;
    private Button btnEditProfile;

    private Uri selectedImageUri;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        imgProfile = findViewById(R.id.imgProfile);
        etName = findViewById(R.id.etName);
        tvEmail = findViewById(R.id.tvEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (user != null) {
            etName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .into(imgProfile);
            }
        }

        imgProfile.setOnClickListener(v -> openImagePicker());
        btnEditProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .into(imgProfile);
            }
        }
    }

    private void saveProfileChanges() {
        if (user == null) return;

        // ✅ Khai báo newName ở đầu hàm — để các listener bên trong có thể dùng được
        String newName = etName.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên hiển thị!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            // ✅ Upload ảnh đúng theo Storage Rules (/avatars/{uid}/{filename})
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String fileName = "avatar_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference()
                    .child("avatars/" + user.getUid() + "/" + fileName);

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String photoUrl = uri.toString();

                                UserProfileChangeRequest profileUpdates =
                                        new UserProfileChangeRequest.Builder()
                                                .setDisplayName(newName)
                                                .setPhotoUri(uri)
                                                .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                user.reload().addOnCompleteListener(t -> {
                                                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                                                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                                                    db.collection("users")
                                                            .document(refreshedUser.getUid())
                                                            .update("displayName", newName,
                                                                    "photoUrl", photoUrl)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(this, "✅ Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                                                                finish();
                                                            })
                                                            .addOnFailureListener(e ->
                                                                    Toast.makeText(this, "❌ Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                });
                                            } else {
                                                Toast.makeText(this, "❌ Lỗi Auth: " + task.getException(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Không đổi ảnh, chỉ đổi tên
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.reload().addOnCompleteListener(t -> {
                                db.collection("users")
                                        .document(user.getUid())
                                        .update("displayName", newName)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "✅ Lưu thay đổi thành công!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            });
                        }
                    });
        }
    }
}
