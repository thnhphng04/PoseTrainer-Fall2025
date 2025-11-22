package fpt.fall2025.posetrainer.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fpt.fall2025.posetrainer.R;

public class CreatePostActivity extends AppCompatActivity {

    private static final String TAG = "CreatePostActivity";
    private static final int MAX_IMAGES = 10;

    private EditText edtContent;
    private RecyclerView rvSelectedImages;
    private ProgressBar progress;
    private Button btnPickImage, btnPost;
    private ImageButton btnBack;
    private TextView tvUserName;
    private ImageView ivUserAvatar;

    private List<Uri> selectedImages = new ArrayList<>();
    private SelectedImagesAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final ActivityResultLauncher<String> pickMultipleImages =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),
                    uris -> {
                        if (uris != null && !uris.isEmpty()) {
                            int remainingSlots = MAX_IMAGES - selectedImages.size();
                            int toAdd = Math.min(remainingSlots, uris.size());
                            
                            for (int i = 0; i < toAdd; i++) {
                                selectedImages.add(uris.get(i));
                            }
                            
                            if (uris.size() > toAdd) {
                                Toast.makeText(this, "Chỉ có thể thêm tối đa " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
                            }
                            
                            adapter.notifyDataSetChanged();
                            updateImageListVisibility();
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
        setupImageRecyclerView();

        btnPickImage.setOnClickListener(v -> selectImages());
        btnPost.setOnClickListener(v -> createPost());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        edtContent = findViewById(R.id.edtContent);
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
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

    private void setupImageRecyclerView() {
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new SelectedImagesAdapter();
        rvSelectedImages.setAdapter(adapter);
        updateImageListVisibility();
    }

    private void updateImageListVisibility() {
        if (selectedImages.isEmpty()) {
            rvSelectedImages.setVisibility(View.GONE);
        } else {
            rvSelectedImages.setVisibility(View.VISIBLE);
        }
    }

    private void selectImages() {
        if (selectedImages.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Bạn đã chọn tối đa " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
                return;
            }
        }
        pickMultipleImages.launch("image/*");
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
        if (content.isEmpty() && selectedImages.isEmpty()) {
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
        post.put("imageUrl", ""); // Backward compatibility
        post.put("imagePath", ""); // Backward compatibility
        post.put("imageUrls", new ArrayList<String>()); // Danh sách ảnh mới
        post.put("imagePaths", new ArrayList<String>()); // Danh sách đường dẫn
        post.put("likesCount", 0L);
        post.put("commentsCount", 0L);
        post.put("likedBy", new ArrayList<String>());
        post.put("createdAt", FieldValue.serverTimestamp());
        post.put("updatedAt", FieldValue.serverTimestamp());

        // Tạo post trước
        postRef.set(post).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            // Upload tất cả ảnh
            if (selectedImages.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<Task<Uri>> uploadTasks = new ArrayList<>();
            List<String> imageUrls = new ArrayList<>();
            List<String> imagePaths = new ArrayList<>();

            for (int i = 0; i < selectedImages.size(); i++) {
                Uri imageUri = selectedImages.get(i);
                String fileName = "image_" + i + ".jpg";
                String path = "community/" + user.getUid() + "/" + postId + "/" + fileName;
                StorageReference ref = storage.getReference().child(path);

                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build();

                Task<Uri> uploadTask = ref.putFile(imageUri, metadata)
                        .continueWithTask(t -> {
                            if (!t.isSuccessful()) {
                                throw t.getException();
                            }
                            return ref.getDownloadUrl();
                        })
                        .addOnSuccessListener(uri -> {
                            imageUrls.add(uri.toString());
                            imagePaths.add(path);
                        });

                uploadTasks.add(uploadTask);
            }

            // Đợi tất cả ảnh upload xong
            return Tasks.whenAllComplete(uploadTasks).continueWithTask(allTasks -> {
                // Cập nhật post với danh sách ảnh
                Map<String, Object> updates = new HashMap<>();
                updates.put("imageUrls", imageUrls);
                updates.put("imagePaths", imagePaths);
                
                // Backward compatibility: set ảnh đầu tiên vào imageUrl
                if (!imageUrls.isEmpty()) {
                    updates.put("imageUrl", imageUrls.get(0));
                    updates.put("imagePath", imagePaths.get(0));
                }
                
                updates.put("updatedAt", FieldValue.serverTimestamp());
                return postRef.update(updates);
            });
        }).addOnSuccessListener(v -> {
            Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error creating post: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi đăng bài: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setUiEnabled(true);
            progress.setVisibility(ProgressBar.GONE);
        });
    }

    // Adapter cho RecyclerView hiển thị ảnh đã chọn
    private class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImageVH> {
        @NonNull
        @Override
        public SelectedImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_selected_image, parent, false);
            return new SelectedImageVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectedImageVH holder, int position) {
            holder.bind(selectedImages.get(position), position);
        }

        @Override
        public int getItemCount() {
            return selectedImages.size();
        }
    }

    private class SelectedImageVH extends RecyclerView.ViewHolder {
        private final ImageView ivImage;
        private final ImageButton btnRemove;

        public SelectedImageVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        public void bind(Uri imageUri, int position) {
            Glide.with(itemView.getContext())
                    .load(imageUri)
                    .centerCrop()
                    .into(ivImage);

            btnRemove.setOnClickListener(v -> {
                selectedImages.remove(position);
                adapter.notifyDataSetChanged();
                updateImageListVisibility();
            });
        }
    }
}
