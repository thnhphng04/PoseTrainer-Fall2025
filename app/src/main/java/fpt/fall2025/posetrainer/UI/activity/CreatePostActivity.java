package fpt.fall2025.posetrainer.UI.activity;

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.AuthService;
import fpt.fall2025.posetrainer.DAL.CommunityDAO;
import fpt.fall2025.posetrainer.Domain.Community;

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

    private AuthService authService;
    private CommunityDAO communityDAO;

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

        authService = new AuthService();
        communityDAO = new CommunityDAO();

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
        FirebaseUser user = authService.getCurrentUser();
        if (user != null) {
            String displayName = authService.getCurrentUserDisplayName();
            String email = authService.getCurrentUserEmail();
            tvUserName.setText(displayName != null ? displayName : (email != null ? email : "Người dùng"));
            String photoUrl = authService.getCurrentUserPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into(ivUserAvatar);
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
        FirebaseUser user = authService.getCurrentUser();
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

        String uid = user.getUid();
        String postId = java.util.UUID.randomUUID().toString();

        // Tạo Community object
        String displayName = authService.getCurrentUserDisplayName() != null ? authService.getCurrentUserDisplayName()
                : (authService.getCurrentUserEmail() != null ? authService.getCurrentUserEmail() : "User");
        String photoURL = authService.getCurrentUserPhotoUrl() != null ? authService.getCurrentUserPhotoUrl() : "";
        Community.Author author = new Community.Author(uid, displayName, photoURL);

        Community post = new Community();
        post.id = postId;
        post.uid = uid;
        post.author = author;
        post.content = content;
        post.imageUrl = ""; // Backward compatibility
        post.imagePath = ""; // Backward compatibility
        post.imageUrls = new ArrayList<>(); // Danh sách ảnh mới
        post.imagePaths = new ArrayList<>(); // Danh sách đường dẫn
        post.likesCount = 0L;
        post.commentsCount = 0L;
        post.likedBy = new ArrayList<>();
        post.isVisible = true;
        post.createdAt = new com.google.firebase.Timestamp(new java.util.Date());
        post.updatedAt = new com.google.firebase.Timestamp(new java.util.Date());

        // Upload ảnh trước (nếu có)
            if (selectedImages.isEmpty()) {
            // Không có ảnh, lưu post luôn
            communityDAO.save(post, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Error creating post: " + task.getException().getMessage(), task.getException());
                    Toast.makeText(this, "Lỗi đăng bài: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    setUiEnabled(true);
                    progress.setVisibility(ProgressBar.GONE);
            }
            });
        } else {
            // Upload ảnh trước
            communityDAO.uploadPostImages(uid, postId, selectedImages, uploadTask -> {
                if (uploadTask.isSuccessful()) {
                    CommunityDAO.UploadResult result = uploadTask.getResult();
                    post.imageUrls = result.imageUrls;
                    post.imagePaths = result.imagePaths;
                
                // Backward compatibility: set ảnh đầu tiên vào imageUrl
                    if (!result.imageUrls.isEmpty()) {
                        post.imageUrl = result.imageUrls.get(0);
                        post.imagePath = result.imagePaths.get(0);
                }
                
                    post.updatedAt = new com.google.firebase.Timestamp(new java.util.Date());
                    
                    // Lưu post với ảnh
                    communityDAO.save(post, saveTask -> {
                        if (saveTask.isSuccessful()) {
            Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
            finish();
                        } else {
                            Log.e(TAG, "Error creating post: " + saveTask.getException().getMessage(), saveTask.getException());
                            Toast.makeText(this, "Lỗi đăng bài: " + saveTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                            setUiEnabled(true);
                            progress.setVisibility(ProgressBar.GONE);
                        }
                    });
                } else {
                    Log.e(TAG, "Error uploading images: " + uploadTask.getException().getMessage(), uploadTask.getException());
                    Toast.makeText(this, "Lỗi upload ảnh: " + uploadTask.getException().getMessage(), Toast.LENGTH_LONG).show();
            setUiEnabled(true);
            progress.setVisibility(ProgressBar.GONE);
                }
        });
        }
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
