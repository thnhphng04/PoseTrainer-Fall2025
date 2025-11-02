package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * ONE domain to bind UI:
 * - Trực tiếp map được với document "community/{postId}" (các field Post)
 * - Kèm các cấu trúc phụ (comments, likedByMe) để hiển thị — dùng @Exclude để không serialize lên Firestore
 */
public class Community {

    // ====== Fields map trực tiếp với community/{postId} ======
    public String id;               // = postId (docId)
    public String uid;              // uid người đăng
    public Author author;           // thông tin người đăng tại thời điểm post
    public String content;          // <= 500 chars
    public String imageUrl;         // có thể rỗng
    public String imagePath;        // đường dẫn trong Storage
    public long likesCount;         // đếm nhanh
    public long commentsCount;      // đếm nhanh
    public Timestamp createdAt;     // serverTimestamp()
    public Timestamp updatedAt;     // serverTimestamp()

    public Community() {}

    public Community(String id, String uid, Author author, String content,
                     String imageUrl, String imagePath,
                     long likesCount, long commentsCount,
                     Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.uid = uid;
        this.author = author;
        this.content = content;
        this.imageUrl = imageUrl;
        this.imagePath = imagePath;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ====== Phần bổ trợ cho UI (KHÔNG lưu Firestore) ======
    @Exclude public boolean likedByMe = false;     // client tính từ likes/{myUid}
    @Exclude public List<Comment> comments = new ArrayList<>(); // load lazy khi mở chi tiết

    // ====== Nested types ======
    public static class Author {
        public String uid;
        public String displayName;
        public String photoURL;
        public Author() {}
        public Author(String uid, String displayName, String photoURL) {
            this.uid = uid; this.displayName = displayName; this.photoURL = photoURL;
        }
    }

    public static class Like {
        public String uid;          // docId = uid
        public Timestamp createdAt; // serverTimestamp()
        public Like() {}
        public Like(String uid, Timestamp createdAt) { this.uid = uid; this.createdAt = createdAt; }
    }

    public static class Comment {
        public String id;           // = commentId (docId)
        public String postId;       // id bài post
        public String uid;          // ai bình luận
        public String displayName;
        public String photoURL;
        public String text;         // <= 500 chars
        public Timestamp createdAt; // serverTimestamp()
        public Comment() {}
        public Comment(String id, String postId, String uid,
                       String displayName, String photoURL,
                       String text, Timestamp createdAt) {
            this.id = id; this.postId = postId; this.uid = uid;
            this.displayName = displayName; this.photoURL = photoURL;
            this.text = text; this.createdAt = createdAt;
        }
    }
}