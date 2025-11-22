package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;

public class UserProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "user_id";

    private String userId;
    private ImageView ivAvatar;
    private TextView tvName, tvEmail;
    private Button btnFollow;
    private RecyclerView rvPosts;
    private LinearLayout emptyState;
    private FirestoreRecyclerAdapter<Community, PostVH> adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null) {
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivAvatar = findViewById(R.id.ivAvatar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        btnFollow = findViewById(R.id.btnFollow);
        rvPosts = findViewById(R.id.rvPosts);
        emptyState = findViewById(R.id.emptyState);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(userId)) {
            btnFollow.setVisibility(View.GONE);
        } else {
            btnFollow.setVisibility(View.VISIBLE);
            checkFollowStatus();
            btnFollow.setOnClickListener(v -> toggleFollow());
        }

        loadUserInfo();
        setupPosts();
    }

    private void loadUserInfo() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
                            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

                            String photoUrl = user.getPhotoURL();
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .circleCrop()
                                        .into(ivAvatar);
                            } else {
                                ivAvatar.setImageResource(R.drawable.ic_person);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkFollowStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .collection("following").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    isFollowing = doc.exists();
                    updateFollowButton();
                });
    }

    private void toggleFollow() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();
        DocumentReference followRef = db.collection("users").document(currentUserId)
                .collection("following").document(userId);

        if (isFollowing) {
            // Unfollow
            followRef.delete()
                    .addOnSuccessListener(v -> {
                        isFollowing = false;
                        updateFollowButton();
                        // Xóa khỏi followers của user
                        db.collection("users").document(userId)
                                .collection("followers").document(currentUserId)
                                .delete();
                    });
        } else {
            // Follow
            followRef.set(new java.util.HashMap<>())
                    .addOnSuccessListener(v -> {
                        isFollowing = true;
                        updateFollowButton();
                        // Thêm vào followers của user
                        db.collection("users").document(userId)
                                .collection("followers").document(currentUserId)
                                .set(new java.util.HashMap<>());
                    });
        }
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText("Đang theo dõi");
            btnFollow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            btnFollow.setText("Theo dõi");
            btnFollow.setBackgroundColor(getResources().getColor(R.color.purple_500));
        }
    }

    private void setupPosts() {
        Query query = db.collection("community")
                .whereEqualTo("uid", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50);

        FirestoreRecyclerOptions<Community> options = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(query, Community.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new FirestoreRecyclerAdapter<Community, PostVH>(options) {
            @NonNull
            @Override
            public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_post, parent, false);
                return new PostVH(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostVH holder, int position, @NonNull Community post) {
                holder.bind(post);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) {
                    rvPosts.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvPosts.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            }
        };

        rvPosts.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    // Simplified PostVH - reuse from CommunityFragment if possible
    private static class PostVH extends RecyclerView.ViewHolder {
        private final TextView tvAuthor, tvContent;
        private final ImageView ivImage;

        public PostVH(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            ivImage = itemView.findViewById(R.id.ivImage);
        }

        public void bind(Community post) {
            tvAuthor.setText(post.getDisplayName());
            tvContent.setText(post.content != null ? post.content : "");

            if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
                Glide.with(itemView.getContext()).load(post.imageUrl).into(ivImage);
                ivImage.setVisibility(View.VISIBLE);
            } else {
                ivImage.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                Intent i = new Intent(itemView.getContext(), PostDetailActivity.class);
                i.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id);
                itemView.getContext().startActivity(i);
            });
        }
    }
}

