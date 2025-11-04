package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.*;

import fpt.fall2025.posetrainer.Data.CommunityRepository;
import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.R;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";

    private String postId;
    private ImageView ivImage, btnLike;
    private TextView tvAuthor, tvTime, tvContent, tvLikeCount, tvCommentCount;
    private EditText edtComment;
    private View btnSend;
    private boolean likedByMe = false;

    private CommunityRepository repo;
    private FirebaseFirestore db;
    private ListenerRegistration postListener;

    private FirestoreRecyclerAdapter<Community.Comment, CmtVH> cmtAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) { finish(); return; }

        repo = new CommunityRepository();
        db = FirebaseFirestore.getInstance();

        ivImage         = findViewById(R.id.ivImage);
        btnLike         = findViewById(R.id.btnLike);
        tvAuthor        = findViewById(R.id.tvAuthor);
        tvTime          = findViewById(R.id.tvTime);
        tvContent       = findViewById(R.id.tvContent);
        tvLikeCount     = findViewById(R.id.tvLikeCount);
        tvCommentCount  = findViewById(R.id.tvCommentCount);
        edtComment      = findViewById(R.id.edtComment);
        btnSend         = findViewById(R.id.btnSend);

        // --- 1) Listen Post realtime ---
        postListener = db.collection("community").document(postId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    Community p = snap.toObject(Community.class);
                    bindPost(p);
                });

        // --- 2) Initial like state ---
        repo.isLikedByMe(postId).addOnSuccessListener(b -> {
            likedByMe = b;
            renderLikeIcon();
        });

        // --- 3) Like toggle ---
        btnLike.setOnClickListener(v ->
                repo.toggleLike(postId)
                        .addOnSuccessListener(x -> repo.isLikedByMe(postId).addOnSuccessListener(b -> { likedByMe = b; renderLikeIcon(); }))
                        .addOnFailureListener(err -> Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show())
        );

        // --- 4) Comments list ---
        // --- 4) Comments list ---
        RecyclerView rv = findViewById(R.id.rvComments);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Query q = FirebaseFirestore.getInstance()
                .collection("community").document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Community.Comment> opts =
                new FirestoreRecyclerOptions.Builder<Community.Comment>()
                        .setQuery(q, Community.Comment.class)
                        .build();

        cmtAdapter = new FirestoreRecyclerAdapter<Community.Comment, CmtVH>(opts) {
            @Override
            public void onBindViewHolder(@NonNull CmtVH h, int pos,
                                         @NonNull Community.Comment c) {
                h.bind(c);
            }

            @NonNull
            @Override
            public CmtVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_comment, parent, false);
                return new CmtVH(item);
            }
        };
        rv.setAdapter(cmtAdapter);


        // --- 5) Send comment ---
        btnSend.setOnClickListener(v -> {
            String text = edtComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            repo.addComment(postId, text)
                    .addOnSuccessListener(x -> edtComment.setText(""))
                    .addOnFailureListener(err -> Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show());
        });
        //Nút back
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

    }

    private void bindPost(@Nullable Community p) {
        if (p == null) return;
        tvAuthor.setText(p.author != null && p.author.displayName != null ? p.author.displayName : "User");
        tvContent.setText(p.content != null ? p.content : "");
        if (p.createdAt != null) {
            java.util.Date d = p.createdAt.toDate();
            tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
        } else tvTime.setText("");
        tvLikeCount.setText(String.valueOf(p.likesCount));
        tvCommentCount.setText(String.valueOf(p.commentsCount));

        if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(p.imageUrl).into(ivImage);
            ivImage.setVisibility(View.VISIBLE);
        } else ivImage.setVisibility(View.GONE);
    }

    private void renderLikeIcon() {
        // dùng 2 icon: ic_heart_fill & ic_heart_outline
        btnLike.setImageResource(likedByMe ? R.drawable.ic_favorite_filled : R.drawable.ic_heart_outline);
    }

    @Override protected void onStart() { super.onStart(); if (cmtAdapter != null) cmtAdapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (cmtAdapter != null) cmtAdapter.stopListening(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (postListener != null) postListener.remove();
    }

    // --- ViewHolder comment ---
    // --- ViewHolder comment ---
    static class CmtVH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvText, tvTime;

        CmtVH(View item) {
            super(item);
            ivAvatar = item.findViewById(R.id.ivAvatar);
            tvName   = item.findViewById(R.id.tvName);
            tvText   = item.findViewById(R.id.tvText);
            tvTime   = item.findViewById(R.id.tvTime);
        }

        void bind(Community.Comment c) {
            tvName.setText(c.displayName != null ? c.displayName : "User");
            tvText.setText(c.text != null ? c.text : "");

            if (c.createdAt != null) {
                java.util.Date d = c.createdAt.toDate();
                tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
            } else {
                tvTime.setText("");
            }

            // Load avatar người bình luận
            if (c.photoURL != null && !c.photoURL.isEmpty()) {
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(c.photoURL)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }
    }

}

