package fpt.fall2025.posetrainer.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;

public class LikeListDialog extends Dialog {
    private static final String TAG = "LikeListDialog";
    private final String postId;
    private RecyclerView rvLikes;
    private LinearLayout emptyState;
    private LikeUserAdapter adapter;
    private FirebaseFirestore db;

    public LikeListDialog(@NonNull Context context, String postId) {
        super(context);
        this.postId = postId;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_like_list);

        rvLikes = findViewById(R.id.rvLikes);
        emptyState = findViewById(R.id.emptyState);
        ImageButton btnClose = findViewById(R.id.btnClose);

        rvLikes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LikeUserAdapter();
        rvLikes.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());

        loadLikes();
    }

    private void loadLikes() {
        db.collection("community").document(postId).collection("likes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> userIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String uid = doc.getString("uid");
                        if (uid != null) {
                            userIds.add(uid);
                        }
                    }

                    if (userIds.isEmpty()) {
                        rvLikes.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvLikes.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                        adapter.setUserIds(userIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private class LikeUserAdapter extends RecyclerView.Adapter<LikeUserVH> {
        private List<String> userIds = new ArrayList<>();

        public void setUserIds(List<String> userIds) {
            this.userIds = userIds;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LikeUserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_like_user, parent, false);
            return new LikeUserVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LikeUserVH holder, int position) {
            holder.bind(userIds.get(position));
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }
    }

    private static class LikeUserVH extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail;
        private final android.widget.ImageView ivAvatar;

        public LikeUserVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }

        public void bind(String uid) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
                                tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

                                String photoUrl = user.getPhotoURL();
                                if (photoUrl != null && !photoUrl.isEmpty()) {
                                    Glide.with(itemView.getContext())
                                            .load(photoUrl)
                                            .placeholder(R.drawable.ic_person)
                                            .error(R.drawable.ic_person)
                                            .circleCrop()
                                            .into(ivAvatar);
                                } else {
                                    ivAvatar.setImageResource(R.drawable.ic_person);
                                }
                            }
                        } else {
                            tvName.setText("Người dùng");
                            tvEmail.setText("");
                            ivAvatar.setImageResource(R.drawable.ic_person);
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvName.setText("Người dùng");
                        tvEmail.setText("");
                        ivAvatar.setImageResource(R.drawable.ic_person);
                    });
        }
    }
}

