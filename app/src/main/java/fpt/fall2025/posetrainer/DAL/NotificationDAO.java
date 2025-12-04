package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * NotificationDAO - Data Access Object cho Notification
 * Quản lý các thao tác CRUD với collection "notifications" trong Firestore
 */
public class NotificationDAO {
    private static final String TAG = "NotificationDAO";
    private static final String COLLECTION_NAME = "notifications";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public NotificationDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu notification vào Firestore
     */
    public void save(@NonNull Notification notification, @Nullable OnCompleteListener<Void> listener) {
        if (notification == null || notification.getId() == null) {
            Log.e(TAG, "Notification hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Notification hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu notification: " + notification.getId());
        firestoreContext.getDocument(COLLECTION_NAME, notification.getId())
            .set(notification)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu notification thành công: " + notification.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu notification: " + notification.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy notification theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Notification> listener) {
        Log.d(TAG, "Đang lấy notification: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Notification notification = documentSnapshot.toObject(Notification.class);
                    if (notification != null) {
                        notification.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy notification thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(notification));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse notification");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Notification>forException(new Exception("Không thể parse notification")));
                        }
                    }
                } else {
                    Log.d(TAG, "Notification không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy notification: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Notification>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả notifications của user
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<Notification>> listener) {
        Log.d(TAG, "Đang lấy notifications của user: " + uid);
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Notification> notifications = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notification.setId(doc.getId());
                        notifications.add(notification);
                    }
                }
                Log.d(TAG, "✅ Lấy " + notifications.size() + " notifications thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(notifications));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy notifications của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Notification>>forException(e));
                }
            });
    }
    
    /**
     * Đánh dấu notification là đã đọc
     */
    public void markAsRead(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang đánh dấu notification đã đọc: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .update("read", true)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Đánh dấu notification đã đọc thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi đánh dấu notification đã đọc: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Xóa notification
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa notification: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa notification thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa notification: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy collection reference để query
     * @return CollectionReference cho notifications
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lắng nghe real-time updates cho notifications query
     * @param query Query để lắng nghe
     * @param listener Listener để xử lý updates
     * @return ListenerRegistration để có thể remove listener
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull Query query,
                                                   @NonNull com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        return query.addSnapshotListener(listener);
    }
}

