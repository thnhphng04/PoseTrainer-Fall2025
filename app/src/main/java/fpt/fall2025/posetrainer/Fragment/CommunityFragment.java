package fpt.fall2025.posetrainer.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.*;

import fpt.fall2025.posetrainer.Activity.CreatePostActivity;
import fpt.fall2025.posetrainer.Activity.PostDetailActivity;
import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.R;

public class CommunityFragment extends Fragment {

    private FirestoreRecyclerAdapter<Community, PostVH> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        RecyclerView rv = v.findViewById(R.id.rvFeed);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setItemAnimator(null); // <-- quan trọng: tắt animator để tránh crash khi realtime reorders

        Query q = FirebaseFirestore.getInstance()
                .collection("community")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30);

        FirestoreRecyclerOptions<Community> opts = new FirestoreRecyclerOptions.Builder<Community>()
                .setQuery(q, Community.class)
                .setLifecycleOwner(getViewLifecycleOwner()) // <-- để FirebaseUI tự quản lý
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
            public void onError(@NonNull com.google.firebase.firestore.FirebaseFirestoreException e) {
                super.onError(e);
                Toast.makeText(getContext(), "Lỗi feed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        rv.setAdapter(adapter);

        v.findViewById(R.id.fabCreate).setOnClickListener(v1 ->
                startActivity(new Intent(getActivity(), CreatePostActivity.class))
        );
    }

    @Override public void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override public void onStop()  { super.onStop();  if (adapter != null) adapter.stopListening();  }

    // --------- ViewHolder ----------
    public static class PostVH extends RecyclerView.ViewHolder {
        private final android.widget.TextView tvAuthor, tvContent, tvCounts, tvTime;
        private final android.widget.ImageView ivImage;

        public PostVH(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCounts  = itemView.findViewById(R.id.tvCounts);
            tvTime    = itemView.findViewById(R.id.tvTime);
            ivImage   = itemView.findViewById(R.id.ivImage);
        }

        public void bind(Community p) {
            String author = (p.author != null && p.author.displayName != null && !p.author.displayName.isEmpty())
                    ? p.author.displayName : "User";
            tvAuthor.setText(author);
            tvContent.setText(p.content != null ? p.content : "");
            tvCounts.setText("❤ " + p.likesCount + "   💬 " + p.commentsCount);

            // thời gian tương đối (đơn giản)
            if (p.createdAt != null) {
                java.util.Date d = p.createdAt.toDate();
                tvTime.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d));
            } else {
                tvTime.setText("");
            }

            if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(ivImage.getContext()).load(p.imageUrl).into(ivImage);
                ivImage.setVisibility(View.VISIBLE);
            } else {
                ivImage.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(itemView.getContext(), fpt.fall2025.posetrainer.Activity.PostDetailActivity.class);
                i.putExtra(PostDetailActivity.EXTRA_POST_ID, p.id);
                itemView.getContext().startActivity(i);
            });
        }
    }
}
