package fpt.fall2025.posetrainer.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.R;

public class CreatePostActivity extends AppCompatActivity {

    private EditText edtContent;
    private ImageView ivPreview, ivUserAvatar;
    private ProgressBar progress;
    private Button btnPickImage, btnPost;
    private ImageButton btnBack;
    private TextView tvUserName;

    private Uri pickedImage = null;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            pickedImage = uri;
                            ivPreview.setImageURI(uri);
                            ivPreview.setVisibility(ImageView.VISIBLE);
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupUserInfo();

        btnPickImage.setOnClickListener(v -> selectImage());
        btnPost.setOnClickListener(v -> createPost());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        edtContent = findViewById(R.id.edtContent);
        ivPreview = findViewById(R.id.ivPreview);
        progress = findViewById(R.id.progress);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnPost = findViewById(R.id.btnPost);
        btnBack = findViewById(R.id.btnBack);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvUserName = findViewById(R.id.tvUserName);
    }

    private void setupUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName()
                    : (user.getEmail() != null ? user.getEmail() : "Người dùng"));
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(ivUserAvatar);
            }
        }
    }

    private void selectImage() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
                return;
            }
        }
        pickImage.launch("image/*");
    }

    private void setUiEnabled(boolean enabled) {
        btnPickImage.setEnabled(enabled);
        btnPost.setEnabled(enabled);
        edtContent.setEnabled(enabled);
    }

    private void createPost() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String content = edtContent.getText().toString().trim();
        if (content.isEmpty() && pickedImage == null) {
            Toast.makeText(this, "Vui lòng nhập nội dung hoặc chọn ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        setUiEnabled(false);
        progress.setVisibility(ProgressBar.VISIBLE);

        DocumentReference postRef = db.collection("community").document();
        String postId = postRef.getId();

        Map<String, Object> author = new HashMap<>();
        author.put("uid", user.getUid());
        author.put("displayName", user.getDisplayName() != null ? user.getDisplayName()
                : (user.getEmail() != null ? user.getEmail() : "User"));
        author.put("photoURL", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

        Map<String, Object> post = new HashMap<>();
        post.put("id", postId);
        post.put("uid", user.getUid());
        post.put("author", author);
        post.put("content", content);
        post.put("imageUrl", "");
        post.put("imagePath", "");
        post.put("likesCount", 0L);
        post.put("commentsCount", 0L);
        post.put("createdAt", FieldValue.serverTimestamp());
        post.put("updatedAt", FieldValue.serverTimestamp());

        Task<Void> chain = postRef.set(post);

        if (pickedImage != null) {
            String path = "community/" + user.getUid() + "/" + postId + "/image.jpg";
            StorageReference ref = storage.getReference().child(path);
            chain = chain.onSuccessTask(v ->
                    ref.putFile(pickedImage)
                            .continueWithTask(t -> {
                                if (!t.isSuccessful()) throw t.getException();
                                return ref.getDownloadUrl();
                            }).onSuccessTask(uri -> {
                                Map<String, Object> upd = new HashMap<>();
                                upd.put("imageUrl", uri.toString());
                                upd.put("imagePath", path);
                                upd.put("updatedAt", FieldValue.serverTimestamp());
                                return postRef.update(upd);
                            })
            );
        }

        chain.addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi đăng bài: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setUiEnabled(true);
                    progress.setVisibility(ProgressBar.GONE);
                });
    }
}
