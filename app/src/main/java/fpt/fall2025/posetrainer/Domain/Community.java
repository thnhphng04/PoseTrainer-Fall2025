package fpt.fall2025.posetrainer.Domain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho một bài đăng (post) trong "community".
 * Map trực tiếp với Firestore collection "community/{postId}"
 */
public class Community {

    // ====== Các field map trực tiếp với Firestore ======
    public String id;               // ID bài đăng (docId)
    public String uid;              // UID người đăng
    public Author author;           // Thông tin người đăng tại thời điểm đăng
    public String content;          // Nội dung bài viết
    public String imageUrl;         // URL ảnh bài viết (backward compatibility - ảnh đơn)
    public String imagePath;        // Đường dẫn trong Storage (backward compatibility)
    public List<String> imageUrls; // Danh sách URL ảnh (hỗ trợ nhiều ảnh)
    public List<String> imagePaths; // Danh sách đường dẫn trong Storage
    public long likesCount;         // Tổng lượt thích
    public long commentsCount;      // Tổng lượt bình luận
    public Timestamp createdAt;     // Ngày tạo
    public Timestamp updatedAt;     // Ngày cập nhật
    public List<String> likedBy;    // Danh sách UID người đã like

    // ====== Constructor bắt buộc Firestore cần ======
    public Community() {
        // Khởi tạo các list để tránh NullPointerException
        this.imageUrls = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
        this.likedBy = new ArrayList<>();
    }

    public Community(String id, String uid, Author author, String content,
                     String imageUrl, String imagePath,
                     long likesCount, long commentsCount,
                     Timestamp createdAt, Timestamp updatedAt, List<String> likedBy) {
        this.id = id;
        this.uid = uid;
        this.author = author;
        this.content = content;
        this.imageUrl = imageUrl;
        this.imagePath = imagePath;
        this.imageUrls = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likedBy = likedBy != null ? likedBy : new ArrayList<>();
    }
    
    // Helper method để lấy danh sách ảnh (ưu tiên imageUrls, fallback về imageUrl)
    @Exclude
    public List<String> getImageUrls() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls;
        }
        // Backward compatibility: nếu có imageUrl đơn thì thêm vào list
        List<String> urls = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            urls.add(imageUrl);
        }
        return urls;
    }

    // ====== Các field chỉ dùng trong client, không lưu Firestore ======
    @Exclude public boolean likedByMe = false;     // người dùng hiện tại đã like chưa
    @Exclude public List<Comment> comments = new ArrayList<>(); // danh sách comment khi mở chi tiết

    // ====== Getter tiện ích cho UI ======
    @Exclude
    public String getDisplayName() {
        return (author != null && author.displayName != null && !author.displayName.isEmpty())
                ? author.displayName
                : "Người dùng";
    }

    @Exclude
    public String getPhotoURL() {
        return (author != null && author.photoURL != null) ? author.photoURL : null;
    }

    @Exclude
    public String getTimeString() {
        if (createdAt == null) return "";
        java.util.Date d = createdAt.toDate();
        return android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", d).toString();
    }

    // ====== Nested classes ======
    public static class Author {
        public String uid;
        public String displayName;
        public String photoURL;

        public Author() {}

        public Author(String uid, String displayName, String photoURL) {
            this.uid = uid;
            this.displayName = displayName;
            this.photoURL = photoURL;
        }
    }

    public static class Like {
        public String uid;
        public Timestamp createdAt;

        public Like() {}

        public Like(String uid, Timestamp createdAt) {
            this.uid = uid;
            this.createdAt = createdAt;
        }
    }

    public static class Comment {
        public String id;
        public String postId;
        public String uid;
        public String displayName;
        public String photoURL;
        public String text;
        public Timestamp createdAt;

        public Comment() {}

        public Comment(String id, String postId, String uid,
                       String displayName, String photoURL,
                       String text, Timestamp createdAt) {
            this.id = id;
            this.postId = postId;
            this.uid = uid;
            this.displayName = displayName;
            this.photoURL = photoURL;
            this.text = text;
            this.createdAt = createdAt;
        }
    }
}
