package fpt.fall2025.posetrainer.Data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

public class CommunityRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference postRef(String postId) {
        return db.collection("community").document(postId);
    }

    // --- 1) Toggle Like ---
    public Task<Void> toggleLike(String postId) {
        if (auth.getCurrentUser() == null) return Tasks.forException(new Exception("Not signed in"));
        String uid = auth.getCurrentUser().getUid();

        DocumentReference likeRef = postRef(postId).collection("likes").document(uid);
        DocumentReference pRef    = postRef(postId);

        return db.runTransaction(trx -> {
            DocumentSnapshot likeSnap = trx.get(likeRef);
            DocumentSnapshot postSnap = trx.get(pRef);
            long likes = postSnap.contains("likesCount") ? postSnap.getLong("likesCount") : 0L;

            if (likeSnap.exists()) {
                trx.delete(likeRef);
                trx.update(pRef, "likesCount", Math.max(0, likes - 1));
            } else {
                Map<String, Object> like = new HashMap<>();
                like.put("uid", uid);
                like.put("createdAt", FieldValue.serverTimestamp());
                trx.set(likeRef, like);
                trx.update(pRef, "likesCount", likes + 1);
            }
            return null;
        });
    }

    // --- 2) Kiểm tra đã like chưa (1 lần) ---
    public Task<Boolean> isLikedByMe(String postId) {
        if (auth.getCurrentUser() == null) return Tasks.forResult(false);
        String uid = auth.getCurrentUser().getUid();
        return postRef(postId).collection("likes").document(uid).get()
                .continueWith(t -> t.isSuccessful() && t.getResult().exists());
    }

    // --- 3) Thêm Comment ---
    public Task<Void> addComment(String postId, String text) {
        if (auth.getCurrentUser() == null) return Tasks.forException(new Exception("Not signed in"));
        String uid = auth.getCurrentUser().getUid();
        String displayName = auth.getCurrentUser().getDisplayName() != null
                ? auth.getCurrentUser().getDisplayName()
                : (auth.getCurrentUser().getEmail() != null ? auth.getCurrentUser().getEmail() : "User");
        String photoURL = auth.getCurrentUser().getPhotoUrl() != null
                ? auth.getCurrentUser().getPhotoUrl().toString() : "";

        DocumentReference pRef = postRef(postId);
        DocumentReference cRef = pRef.collection("comments").document();
        Map<String, Object> cmt = new HashMap<>();
        cmt.put("id", cRef.getId());
        cmt.put("postId", postId);
        cmt.put("uid", uid);
        cmt.put("displayName", displayName);
        cmt.put("photoURL", photoURL);
        cmt.put("text", text.trim());
        cmt.put("createdAt", FieldValue.serverTimestamp());

        return db.runTransaction(trx -> {
            DocumentSnapshot postSnap = trx.get(pRef);
            long count = postSnap.contains("commentsCount") ? postSnap.getLong("commentsCount") : 0L;
            trx.set(cRef, cmt);
            trx.update(pRef, "commentsCount", count + 1);
            return null;
        });
    }
}

