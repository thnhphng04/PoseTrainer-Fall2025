package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import fpt.fall2025.posetrainer.Domain.Profile;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * ProfileDAO - Data Access Object cho Profile
 * Quản lý các thao tác CRUD với collection "profiles" trong Firestore
 */
public class ProfileDAO {
    private static final String TAG = "ProfileDAO";
    private static final String COLLECTION_NAME = "profiles";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public ProfileDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu profile vào Firestore
     */
    public void save(@NonNull Profile profile, @Nullable OnCompleteListener<Void> listener) {
        if (profile == null || profile.getUid() == null) {
            Log.e(TAG, "Profile hoặc UID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Profile hoặc UID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu profile: " + profile.getUid());
        firestoreContext.getDocument(COLLECTION_NAME, profile.getUid())
            .set(profile)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu profile thành công: " + profile.getUid());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu profile: " + profile.getUid(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy profile theo UID
     */
    public void getById(@NonNull String uid, @Nullable OnCompleteListener<Profile> listener) {
        Log.d(TAG, "Đang lấy profile: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Profile profile = documentSnapshot.toObject(Profile.class);
                    if (profile != null) {
                        Log.d(TAG, "✅ Lấy profile thành công: " + uid);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(profile));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse profile");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Profile>forException(new Exception("Không thể parse profile")));
                        }
                    }
                } else {
                    Log.d(TAG, "Profile không tồn tại: " + uid);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy profile: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Profile>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật profile
     */
    public void update(@NonNull String uid, @NonNull Profile profile, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật profile: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .set(profile)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật profile thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật profile: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Xóa profile
     */
    public void delete(@NonNull String uid, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa profile: " + uid);
        firestoreContext.getDocument(COLLECTION_NAME, uid)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa profile thành công: " + uid);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa profile: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
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
     * Lấy document snapshot (để bind profile data)
     * @param uid User ID
     * @return Task<DocumentSnapshot>
     */
    @NonNull
    public com.google.android.gms.tasks.Task<DocumentSnapshot> getDocumentSnapshot(@NonNull String uid) {
        return getDocument(uid).get();
    }
    
    /**
     * Cập nhật profile với SetOptions.merge()
     * @param profile Profile object
     * @param listener Callback
     */
    public void saveWithMerge(@NonNull Profile profile, @Nullable OnCompleteListener<Void> listener) {
        if (profile == null || profile.getUid() == null) {
            Log.e(TAG, "Profile hoặc UID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Profile hoặc UID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu profile với merge: " + profile.getUid());
        getDocument(profile.getUid())
            .set(profile, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu profile thành công: " + profile.getUid());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu profile: " + profile.getUid(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
}

