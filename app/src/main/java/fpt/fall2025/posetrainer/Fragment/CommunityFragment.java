package fpt.fall2025.posetrainer.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Activity.CreatePostActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.View.CommunityViewModel;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView imgAvatar;
    private FirestoreRecyclerAdapter<Community, PostVH> adapter;
    private LinearLayoutManager layoutManager;
    private CommunityViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        imgAvatar = v.findViewById(R.id.profile_image);

        // ViewModel để lưu vị trí scroll
        viewModel = new ViewModelProvider(requireActivity()).get(CommunityViewModel.class);

        // --- Load avatar user ---
        loadUserFromFirestore();

        // --- Setup RecyclerView ---
        RecyclerView rv = v.findViewById(R.id.rvFeed);
        layoutManager = new LinearLayoutManager(getContext());
        rv.setLayoutManager(layoutManager);
        rv.setItemAnimator(null);

        Query q = db.collection("community")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30);

        FirestoreRecyclerOptions<Community> opts = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(q, Community.class)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();

        adapter = new FirestoreRecyclerAdapter<Community, PostVH>(opts) {
            @NonNull
            @Override
            public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_post, parent, false);
                return new PostVH(item);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostVH h, int position, @NonNull Community p) {
                h.bind(p);
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                Toast.makeText(getContext(), "Lỗi feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        rv.setAdapter(adapter);

        // === Giữ vị trí cuộn khi quay lại ===
        rv.post(() -> {
            if (viewModel.lastScrollPosition > 0) {
                layoutManager.scrollToPositionWithOffset(viewModel.lastScrollPosition, 0);
            }
        });

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    viewModel.lastScrollPosition = layoutManager.findFirstVisibleItemPosition();
                }
            }
        });

        // --- Khi bấm "Bạn đang nghĩ gì?" ---
        TextView tvCreatePost = v.findViewById(R.id.tvCreatePost);
        tvCreatePost.setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(i);
        });

        // --- Nút quay lại ---
        ImageButton btnBack = v.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ===================== LOAD USER AVATAR =====================
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            String name = user.getDisplayName() != null ? user.getDisplayName() : "User";
                            String photoUrl = doc.contains("photoUrl") ? doc.getString("photoUrl") : null;

                            if (photoUrl == null || photoUrl.isEmpty()) {
                                if (currentUser.getPhotoUrl() != null) {
                                    photoUrl = currentUser.getPhotoUrl().toString();
                                }
                            }

                            bindUser(name, photoUrl);
                        }
                    } else {
                        bindFromAuth(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUserFromFirestore: " + e.getMessage());
                    bindFromAuth(currentUser);
                });
    }

    private void bindUser(String name, String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void bindFromAuth(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    // ---------------- ViewHolder ----------------
    public static class PostVH extends RecyclerView.ViewHolder {
        private final TextView tvAuthor, tvContent, tvCounts, tvTime, tvLike, tvComment;
        private final ImageView ivImage, iconLike, iconComment;
        private final LinearLayout btnLike, btnComment;
        private boolean isLiked = false;

        public PostVH(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCounts = itemView.findViewById(R.id.tvCounts);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);

            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            iconLike = itemView.findViewById(R.id.iconLike);
            iconComment = itemView.findViewById(R.id.iconComment);
            tvLike = itemView.findViewById(R.id.tvLike);
            tvComment = itemView.findViewById(R.id.tvComment);
        }

        public void bind(Community p) {
            String author = (p.author != null && p.author.displayName != null && !p.author.displayName.isEmpty())
                    ? p.author.displayName : "User";
            tvAuthor.setText(author);
            tvContent.setText(p.content != null ? p.content : "");
            tvCounts.setText("❤ " + p.likesCount + "   💬 " + p.commentsCount);

            if (p.createdAt != null) {
                java.util.Date d = p.createdAt.toDate();
                tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
            } else tvTime.setText("");

            if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                Glide.with(ivImage.getContext()).load(p.imageUrl).into(ivImage);
                ivImage.setVisibility(View.VISIBLE);
            } else ivImage.setVisibility(View.GONE);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && p.likedBy != null && p.likedBy.contains(currentUser.getUid())) {
                isLiked = true;
                iconLike.setImageResource(R.drawable.ic_favorite_filled);
                iconLike.setColorFilter(android.graphics.Color.parseColor("#E0245E"));
                tvLike.setTextColor(android.graphics.Color.parseColor("#E0245E"));
            } else {
                isLiked = false;
                iconLike.setImageResource(R.drawable.ic_favorite_border);
                iconLike.setColorFilter(android.graphics.Color.parseColor("#606770"));
                tvLike.setTextColor(android.graphics.Color.parseColor("#606770"));
            }

            // ⚡ Xử lý Like / Unlike
            btnLike.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(itemView.getContext(), "Bạn cần đăng nhập để thích bài viết", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = user.getUid();
                DocumentReference postRef = FirebaseFirestore.getInstance()
                        .collection("community")
                        .document(p.id);

                FirebaseFirestore.getInstance().runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(postRef);
                    Long likesCount = snapshot.getLong("likesCount");
                    if (likesCount == null) likesCount = 0L;

                    List<String> likedBy = (List<String>) snapshot.get("likedBy");
                    if (likedBy == null) likedBy = new ArrayList<>();

                    if (likedBy.contains(uid)) {
                        likedBy.remove(uid);
                        likesCount--;
                        isLiked = false;
                    } else {
                        likedBy.add(uid);
                        likesCount++;
                        isLiked = true;
                    }

                    transaction.update(postRef, "likedBy", likedBy);
                    transaction.update(postRef, "likesCount", likesCount);
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    if (isLiked) {
                        iconLike.setImageResource(R.drawable.ic_favorite_filled);
                        iconLike.setColorFilter(android.graphics.Color.parseColor("#E0245E"));
                        tvLike.setTextColor(android.graphics.Color.parseColor("#E0245E"));
                    } else {
                        iconLike.setImageResource(R.drawable.ic_favorite_border);
                        iconLike.setColorFilter(android.graphics.Color.parseColor("#606770"));
                        tvLike.setTextColor(android.graphics.Color.parseColor("#606770"));
                    }
                }).addOnFailureListener(e -> {
                    Log.e("LIKE", "Error updating like", e);
                    Toast.makeText(itemView.getContext(), "Lỗi cập nhật lượt thích", Toast.LENGTH_SHORT).show();
                });
            });

            // 💬 Nút Comment
            btnComment.setOnClickListener(v -> {
                Intent i = new Intent(itemView.getContext(), PostDetailActivity.class);
                i.putExtra(PostDetailActivity.EXTRA_POST_ID, p.id);
                itemView.getContext().startActivity(i);
            });
        }
    }
}
