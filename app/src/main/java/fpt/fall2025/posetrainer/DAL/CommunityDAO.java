package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Community;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseStorageContext;

/**
 * CommunityDAO - Data Access Object cho Community (Posts)
 * Quản lý các thao tác CRUD với collection "community" trong Firestore
 */
public class CommunityDAO {
    private static final String TAG = "CommunityDAO";
    private static final String COLLECTION_NAME = "community";
    
    private FirebaseFirestoreContext firestoreContext;
    private FirebaseStorageContext storageContext;
    
    public CommunityDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
        this.storageContext = FirebaseStorageContext.getInstance();
    }
    
    /**
     * Lưu post vào Firestore
     */
    public void save(@NonNull Community post, @Nullable OnCompleteListener<Void> listener) {
        if (post == null || post.id == null) {
            Log.e(TAG, "Community post hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Community post hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu post: " + post.id);
        firestoreContext.getDocument(COLLECTION_NAME, post.id)
            .set(post)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu post thành công: " + post.id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu post: " + post.id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy post theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Community> listener) {
        Log.d(TAG, "Đang lấy post: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Community post = documentSnapshot.toObject(Community.class);
                    if (post != null) {
                        post.id = documentSnapshot.getId();
                        Log.d(TAG, "✅ Lấy post thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(post));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse post");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Community>forException(new Exception("Không thể parse post")));
                        }
                    }
                } else {
                    Log.d(TAG, "Post không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy post: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Community>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả posts (feed)
     */
    public void getAllPosts(@Nullable OnCompleteListener<List<Community>> listener) {
        Log.d(TAG, "Đang lấy tất cả posts");
        firestoreContext.getCollection(COLLECTION_NAME)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Community> posts = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Community post = doc.toObject(Community.class);
                    if (post != null) {
                        post.id = doc.getId();
                        posts.add(post);
                    }
                }
                Log.d(TAG, "✅ Lấy " + posts.size() + " posts thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(posts));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy posts", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Community>>forException(e));
                }
            });
    }
    
    /**
     * Xóa post
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa post: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa post thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa post: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy StorageReference cho community post images
     * @param uid User ID
     * @param postId Post ID
     * @param imageIndex Index của ảnh (0, 1, 2, ...)
     * @return StorageReference đến post image path
     */
    @NonNull
    private StorageReference getPostImageReference(@NonNull String uid, @NonNull String postId, int imageIndex) {
        return storageContext.getReference().child("community").child(uid).child(postId).child("image_" + imageIndex + ".jpg");
    }
    
    /**
     * Upload nhiều ảnh cho post
     * @param uid User ID
     * @param postId Post ID
     * @param imageUris Danh sách Uri của ảnh
     * @param listener Callback trả về danh sách download URLs và paths
     */
    public void uploadPostImages(@NonNull String uid, @NonNull String postId, 
                                @NonNull List<Uri> imageUris,
                                @Nullable OnCompleteListener<UploadResult> listener) {
        if (imageUris.isEmpty()) {
            Log.d(TAG, "Không có ảnh để upload");
            if (listener != null) {
                listener.onComplete(Tasks.forResult(new UploadResult(new ArrayList<>(), new ArrayList<>())));
            }
            return;
        }
        
        Log.d(TAG, "Đang upload " + imageUris.size() + " ảnh cho post: " + postId);
        
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        List<String> imagePaths = new ArrayList<>();
        
        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            StorageReference ref = getPostImageReference(uid, postId, i);
            
            StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
            
            Task<Uri> uploadTask = ref.putFile(imageUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    synchronized (imageUrls) {
                        imageUrls.add(uri.toString());
                        imagePaths.add(ref.getPath());
                    }
                });
            
            uploadTasks.add(uploadTask);
        }
        
        // Đợi tất cả ảnh upload xong
        Tasks.whenAllComplete(uploadTasks)
            .addOnSuccessListener(allTasks -> {
                Log.d(TAG, "✅ Upload " + imageUrls.size() + " ảnh thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(new UploadResult(imageUrls, imagePaths)));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi upload ảnh", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<UploadResult>forException(e));
                }
            });
    }
    
    /**
     * Class để trả về kết quả upload
     */
    public static class UploadResult {
        public final List<String> imageUrls;
        public final List<String> imagePaths;
        
        public UploadResult(List<String> imageUrls, List<String> imagePaths) {
            this.imageUrls = imageUrls;
            this.imagePaths = imagePaths;
        }
    }
    
    /**
     * Lấy collection reference để query
     * @return CollectionReference cho community
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lấy document reference
     * @param postId ID của post
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String postId) {
        return firestoreContext.getDocument(COLLECTION_NAME, postId);
    }
    
    /**
     * Lấy query với whereEqualTo và orderBy
     * @param field Field để filter
     * @param value Value để filter
     * @param orderByField Field để order
     * @param direction Direction (ASCENDING/DESCENDING)
     * @param limit Limit số lượng kết quả
     * @return Query
     */
    @NonNull
    public Query getQuery(@NonNull String field, @NonNull Object value,
                         @NonNull String orderByField, @NonNull Query.Direction direction, int limit) {
        return firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo(field, value)
            .orderBy(orderByField, direction)
            .limit(limit);
    }
    
    /**
     * Lắng nghe real-time updates cho post document
     * @param postId ID của post
     * @param listener Listener để xử lý updates
     * @return ListenerRegistration để có thể remove listener
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull String postId,
                                                   @NonNull com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot> listener) {
        return getDocument(postId).addSnapshotListener(listener);
    }
    
    /**
     * Lấy sub-collection reference (comments)
     * @param postId ID của post
     * @return CollectionReference cho comments
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCommentsCollection(@NonNull String postId) {
        return getDocument(postId).collection("comments");
    }
}

