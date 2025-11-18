package fpt.fall2025.posetrainer.Data;

import android.util.Log;

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
    private static final String TAG = "CommunityRepository";
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

        // Thực hiện transaction để toggle like
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
            boolean isNewLike = !likeSnap.exists(); // true nếu đang like (chưa tồn tại)

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

            // Trả về Map chứa thông tin để tạo notification sau
            Map<String, Object> result = new HashMap<>();
            result.put("postUid", postSnap.getString("uid"));
            result.put("isNewLike", isNewLike);
            return result;
        }).continueWith(transactionTask -> {
            // Sau khi transaction thành công, tạo notification nếu cần
            if (transactionTask.isSuccessful() && transactionTask.getResult() != null) {
                Map<String, Object> result = transactionTask.getResult();
                String postUid = (String) result.get("postUid");
                Boolean isNewLike = (Boolean) result.get("isNewLike");
                
                // Chỉ tạo notification khi like mới (không phải unlike) và không phải chính mình
                if (Boolean.TRUE.equals(isNewLike) && postUid != null && !postUid.equals(uid)) {
                    // Không tạo notification cho chính mình
                    createLikeNotification(postId, postUid, uid)
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Lỗi tạo notification cho like: " + e.getMessage())
                            );
                }
            }
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
            
            // Trả về postUid để tạo notification sau transaction
            return postSnap.getString("uid");
        }).continueWith(task -> {
            // Sau khi transaction thành công, tạo notification nếu cần
            if (task.isSuccessful() && task.getResult() != null) {
                String postUid = task.getResult();
                if (!postUid.equals(uid)) {
                    // Không tạo notification cho chính mình
                    createCommentNotification(postId, postUid, uid, displayName, text.trim())
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Lỗi tạo notification cho comment: " + e.getMessage())
                            );
                }
            }
            return null;
        });
    }

    // =============================
    // 4️⃣ Tạo Notification khi có Like
    // =============================
    /**
     * Tạo notification cho chủ bài viết khi có người like bài viết của họ
     * @param postId ID của bài viết
     * @param postOwnerUid UID của người đăng bài viết
     * @param likerUid UID của người like bài viết
     * @return Task để track việc tạo notification
     */
    private Task<Void> createLikeNotification(String postId, String postOwnerUid, String likerUid) {
        // Lấy thông tin người like để hiển thị trong notification
        return db.collection("users").document(likerUid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.w(TAG, "Không thể lấy thông tin user like, dùng thông tin mặc định");
                        return "Ai đó";
                    }
                    
                    Map<String, Object> userData = task.getResult().getData();
                    if (userData != null && userData.containsKey("displayName")) {
                        return userData.get("displayName").toString();
                    }
                    return "Ai đó";
                })
                .continueWith(task -> {
                    String likerName = task.getResult();
                    
                    // Tạo notification document
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("uid", postOwnerUid); // Gửi cho chủ bài viết
                    notification.put("type", "social_like"); // Loại thông báo: like xã hội
                    notification.put("title", "Có người thích bài viết của bạn");
                    notification.put("body", likerName + " đã thích bài viết của bạn");
                    notification.put("sentAt", System.currentTimeMillis()); // Timestamp hiện tại
                    notification.put("read", false); // Chưa đọc
                    notification.put("isAiGenerated", false); // Không phải AI
                    notification.put("actionType", "open_post"); // Khi click vào sẽ mở post
                    notification.put("actionData", postId); // ID của post cần mở
                    
                    // Metadata để lưu thông tin bổ sung
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("postId", postId);
                    metadata.put("likerUid", likerUid);
                    metadata.put("likerName", likerName);
                    notification.put("metadata", metadata);
                    
                    // Lưu vào Firestore collection "notifications"
                    Task<DocumentReference> addTask = db.collection("notifications").add(notification);
                    
                    // Log kết quả
                    addTask.addOnSuccessListener(docRef -> {
                        String notificationId = docRef.getId();
                        Log.d(TAG, "✓ Đã tạo notification cho like bài viết: " + postId);
                        Log.d(TAG, "  - Notification ID: " + notificationId);
                        Log.d(TAG, "  - Post Owner UID: " + postOwnerUid);
                        Log.d(TAG, "  - Liker UID: " + likerUid);
                        Log.d(TAG, "  - Liker Name: " + likerName);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "✗ Lỗi tạo notification cho like: " + e.getMessage());
                        e.printStackTrace();
                    });
                    
                    return addTask;
                })
                .continueWith(task -> null); // Return null để hoàn thành task chain
    }

    // =============================
    // 5️⃣ Tạo Notification khi có Comment
    // =============================
    /**
     * Tạo notification cho chủ bài viết khi có người comment bài viết của họ
     * @param postId ID của bài viết
     * @param postOwnerUid UID của người đăng bài viết
     * @param commenterUid UID của người comment
     * @param commenterName Tên của người comment
     * @param commentText Nội dung comment
     * @return Task để track việc tạo notification
     */
    private Task<Void> createCommentNotification(String postId, String postOwnerUid, 
                                                  String commenterUid, String commenterName, 
                                                  String commentText) {
        // Tạo notification document
        Map<String, Object> notification = new HashMap<>();
        notification.put("uid", postOwnerUid); // Gửi cho chủ bài viết
        notification.put("type", "social_comment"); // Loại thông báo: comment xã hội
        notification.put("title", "Có người bình luận bài viết của bạn");
        
        // Rút ngắn nội dung comment nếu quá dài
        String previewText = commentText;
        if (commentText.length() > 50) {
            previewText = commentText.substring(0, 50) + "...";
        }
        notification.put("body", commenterName + " đã bình luận: \"" + previewText + "\"");
        notification.put("sentAt", System.currentTimeMillis()); // Timestamp hiện tại
        notification.put("read", false); // Chưa đọc
        notification.put("isAiGenerated", false); // Không phải AI
        notification.put("actionType", "open_post"); // Khi click vào sẽ mở post
        notification.put("actionData", postId); // ID của post cần mở
        
        // Metadata để lưu thông tin bổ sung
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("postId", postId);
        metadata.put("commenterUid", commenterUid);
        metadata.put("commenterName", commenterName);
        metadata.put("commentText", commentText);
        notification.put("metadata", metadata);
        
        // Lưu vào Firestore collection "notifications"
        Task<DocumentReference> addTask = db.collection("notifications").add(notification);
        
        // Log kết quả
        addTask.addOnSuccessListener(docRef -> {
            String notificationId = docRef.getId();
            Log.d(TAG, "✓ Đã tạo notification cho comment bài viết: " + postId);
            Log.d(TAG, "  - Notification ID: " + notificationId);
            Log.d(TAG, "  - Post Owner UID: " + postOwnerUid);
            Log.d(TAG, "  - Commenter UID: " + commenterUid);
            Log.d(TAG, "  - Commenter Name: " + commenterName);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "✗ Lỗi tạo notification cho comment: " + e.getMessage());
            e.printStackTrace();
        });
        
        return addTask.continueWith(task -> null); // Return Task<Void>
    }
}
