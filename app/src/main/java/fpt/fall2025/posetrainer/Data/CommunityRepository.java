package fpt.fall2025.posetrainer.Data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference postRef(String postId) {
        return db.collection("community").document(postId);
    }

    // =============================
    // 1️⃣ Toggle Like (field likedBy)
    // =============================
    public Task<Void> toggleLike(String postId) {
        if (auth.getCurrentUser() == null) return Tasks.forException(new Exception("Not signed in"));
        String uid = auth.getCurrentUser().getUid();

        DocumentReference likeRef = postRef(postId).collection("likes").document(uid);
        DocumentReference pRef = postRef(postId);

        return db.runTransaction(trx -> {
            DocumentSnapshot likeSnap = trx.get(likeRef);
            DocumentSnapshot postSnap = trx.get(pRef);
            long likes = postSnap.contains("likesCount") ? postSnap.getLong("likesCount") : 0L;
            
            // Lấy danh sách likedBy hiện tại và tạo ArrayList mới để đảm bảo tính nhất quán
            List<String> currentLikedBy = (List<String>) postSnap.get("likedBy");
            List<String> likedBy = new ArrayList<>();
            if (currentLikedBy != null) {
                likedBy.addAll(currentLikedBy);
            }

            Map<String, Object> updates = new HashMap<>();

            if (likeSnap.exists()) {
                // Unlike: xóa khỏi subcollection và likedBy array
                trx.delete(likeRef);
                likedBy.remove(uid); // Xóa uid khỏi array
                updates.put("likesCount", Math.max(0, likes - 1));
            } else {
                // Like: thêm vào subcollection và likedBy array
                Map<String, Object> like = new HashMap<>();
                like.put("uid", uid);
                like.put("createdAt", FieldValue.serverTimestamp());
                trx.set(likeRef, like);
                if (!likedBy.contains(uid)) {
                    likedBy.add(uid); // Thêm uid vào array
                }
                updates.put("likesCount", likes + 1);
            }

            // Cập nhật field likedBy array trong document chính
            updates.put("likedBy", likedBy);
            // ⚠️ Rule yêu cầu có updatedAt
            updates.put("updatedAt", FieldValue.serverTimestamp());
            trx.update(pRef, updates);

            return null;
        });
    }


    // =============================
    // 2️⃣ Kiểm tra user đã like chưa
    // =============================
    public Task<Boolean> isLikedByMe(String postId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forResult(false);

        String uid = user.getUid();
        return postRef(postId).get().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return false;
            List<String> likedBy = (List<String>) task.getResult().get("likedBy");
            return likedBy != null && likedBy.contains(uid);
        });
    }

    // =============================
    // 3️⃣ Thêm Comment
    // =============================
    public Task<Void> addComment(String postId, String text) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new Exception("Not signed in"));

        String uid = user.getUid();
        String displayName = user.getDisplayName() != null ? user.getDisplayName()
                : (user.getEmail() != null ? user.getEmail() : "User");
        String photoURL = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

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
            Long count = postSnap.getLong("commentsCount");
            if (count == null) count = 0L;
            trx.set(cRef, cmt);
            trx.update(pRef, "commentsCount", count + 1);
            return null;
        });
    }
}
