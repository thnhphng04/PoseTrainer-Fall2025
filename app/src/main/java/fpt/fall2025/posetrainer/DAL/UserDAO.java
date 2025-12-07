package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseStorageContext;

/**
 * UserDAO - Data Access Object cho User
 * Quản lý các thao tác CRUD với collection "users" trong Firestore
 */
public class UserDAO {
    private static final String TAG = "UserDAO";
    private static final String COLLECTION_NAME = "users";
    
    private FirebaseFirestoreContext firestoreContext;
    private FirebaseStorageContext storageContext;
    
    public UserDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
        this.storageContext = FirebaseStorageContext.getInstance();
    }
    
    /**
     * Lưu user vào Firestore
     */
    public void save(@NonNull User user, @Nullable OnCompleteListener<Void> listener) {
        if (user == null || user.getUid() == null) {
            Log.e(TAG, "User hoặc UID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("User hoặc UID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu user: " + user.getUid());
        firestoreContext.getDocument(COLLECTION_NAME, user.getUid())
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu user thành công: " + user.getUid());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu user: " + user.getUid(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy user theo UID
     */
    public void getById(@NonNull String uid, @Nullable OnCompleteListener<User> listener) {
        Log.d(TAG, "Đang lấy user: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Log.d(TAG, "✅ Lấy user thành công: " + uid);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(user));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse user");
                        if (listener != null) {
                              listener.onComplete(Tasks.<User>forException(new Exception("Không thể parse user")));
                        }
                    }
                } else {
                    Log.d(TAG, "User không tồn tại: " + uid);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy user: " + uid, e);
                if (listener != null) {
                      listener.onComplete(Tasks.<User>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật user
     */
    public void update(@NonNull String uid, @NonNull User user, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật user: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .set(user)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật user thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Xóa user
     */
    public void delete(@NonNull String uid, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa user: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa user thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy StorageReference cho user avatar
     * @param uid User ID
     * @param fileName Tên file (optional, nếu null sẽ dùng "avatar.jpg")
     * @return StorageReference đến avatar path
     */
    @NonNull
    private StorageReference getUserAvatarReference(@NonNull String uid, @Nullable String fileName) {
        String fileNameToUse = fileName != null ? fileName : "avatar.jpg";
        return storageContext.getReference().child("avatars").child(uid).child(fileNameToUse);
    }
    
    /**
     * Upload avatar cho user
     * @param uid User ID
     * @param imageUri Uri của ảnh
     * @param listener Callback trả về download URL
     */
    public void uploadAvatar(@NonNull String uid, @NonNull Uri imageUri, 
                            @Nullable OnCompleteListener<String> listener) {
        Log.d(TAG, "Đang upload avatar cho user: " + uid);
        String fileName = "avatar_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = getUserAvatarReference(uid, fileName);
        
        StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build();
        
        storageRef.putFile(imageUri, metadata)
            .addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "✅ Upload avatar thành công, đang lấy download URL");
                storageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "✅ Lấy download URL thành công: " + downloadUrl);
                        if (listener != null) {
                              listener.onComplete(Tasks.forResult(downloadUrl));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Lỗi lấy download URL", e);
                        if (listener != null) {
                            listener.onComplete(Tasks.<String>forException(e));
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi upload avatar", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<String>forException(e));
                }
            });
    }
    
    /**
     * Lấy document reference
     * @param uid User ID
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String uid) {
        return firestoreContext.getDocument(COLLECTION_NAME, uid);
    }
    
    /**
     * Lấy sub-collection reference (following/followers)
     * @param uid User ID
     * @param subCollectionName Tên sub-collection ("following" hoặc "followers")
     * @return CollectionReference cho sub-collection
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getSubCollection(@NonNull String uid, 
                                                                              @NonNull String subCollectionName) {
        return getDocument(uid).collection(subCollectionName);
    }
    
    /**
     * Lấy document reference trong sub-collection
     * @param uid User ID
     * @param subCollectionName Tên sub-collection ("following" hoặc "followers")
     * @param targetUserId ID của user target
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getSubCollectionDocument(@NonNull String uid, 
                                                      @NonNull String subCollectionName,
                                                      @NonNull String targetUserId) {
        return getSubCollection(uid, subCollectionName).document(targetUserId);
    }
    
    /**
     * Lấy following collection reference
     * @param uid User ID
     * @return CollectionReference cho following
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getFollowingCollection(@NonNull String uid) {
        return getSubCollection(uid, "following");
    }
    
    /**
     * Lấy followers collection reference
     * @param uid User ID
     * @return CollectionReference cho followers
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getFollowersCollection(@NonNull String uid) {
        return getSubCollection(uid, "followers");
    }
}

