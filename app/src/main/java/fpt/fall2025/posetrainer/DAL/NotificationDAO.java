package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Notification;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

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
        if (notification == null) {
            Log.e(TAG, "Notification không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Notification không hợp lệ")));
            }
            return;
        }
        
        // Nếu ID null hoặc empty, tạo mới
        if (notification.getId() == null || notification.getId().isEmpty()) {
            Log.d(TAG, "Tạo notification mới (ID trống)");
            firestoreContext.getCollection(COLLECTION_NAME)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Notification saved successfully: " + documentReference.getId());
                    notification.setId(documentReference.getId());
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi tạo notification mới", e);
                    if (listener != null) {
                        listener.onComplete(Tasks.<Void>forException(e));
                    }
                });
        } else {
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
    }
    
    /**
     * Save notification với callback interface tương thích FirebaseService
     */
    public void saveNotification(@NonNull Notification notification, @Nullable OnNotificationSavedListener listener) {
        save(notification, task -> {
            if (listener != null) {
                listener.onNotificationSaved(task.isSuccessful());
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
    
    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     * Tương thích với FirebaseService interface
     */
    public void markAllNotificationsAsRead(@NonNull String uid, @Nullable OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Đánh dấu tất cả thông báo đã đọc cho user: " + uid);
        
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Không có thông báo chưa đọc");
                    if (listener != null) {
                        listener.onNotificationUpdated(true);
                    }
                    return;
                }
                
                int totalNotifications = queryDocumentSnapshots.size();
                final int[] updatedCount = {0};
                
                queryDocumentSnapshots.forEach(document -> {
                    document.getReference().update("read", true)
                        .addOnCompleteListener(task -> {
                            updatedCount[0]++;
                            if (updatedCount[0] == totalNotifications) {
                                Log.d(TAG, "✓ Đã đánh dấu " + totalNotifications + " thông báo là đã đọc");
                                if (listener != null) {
                                    listener.onNotificationUpdated(true);
                                }
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi đánh dấu thông báo: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onNotificationUpdated(false);
                }
            });
    }
    
    /**
     * Gửi feedback cho thông báo AI (accepted, ignored, dismissed)
     * Tương thích với FirebaseService interface
     */
    public void sendNotificationFeedback(@NonNull String notificationId, @NonNull String feedback, 
                                        @Nullable OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Gửi feedback cho thông báo: " + notificationId + ", feedback: " + feedback);
        
        firestoreContext.getDocument(COLLECTION_NAME, notificationId)
            .update("feedback", feedback)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ Đã gửi feedback thành công");
                if (listener != null) {
                    listener.onNotificationUpdated(true);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi gửi feedback: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onNotificationUpdated(false);
                }
            });
    }
    
    /**
     * Đếm số thông báo chưa đọc của user
     * Tương thích với FirebaseService interface
     */
    public void countUnreadNotifications(@NonNull String uid, @Nullable OnUnreadCountLoadedListener listener) {
        Log.d(TAG, "Đếm thông báo chưa đọc cho user: " + uid);
        
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = queryDocumentSnapshots.size();
                Log.d(TAG, "✓ Có " + count + " thông báo chưa đọc");
                if (listener != null) {
                    listener.onUnreadCountLoaded(count);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi đếm thông báo: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onUnreadCountLoaded(0);
                }
            });
    }
    
    /**
     * Load thông báo AI (chỉ lấy thông báo do AI tạo)
     * Tương thích với FirebaseService interface
     */
    public void loadAiNotifications(@NonNull String uid, @Nullable OnNotificationsLoadedListener listener) {
        Log.d(TAG, "Loading AI notifications for userId: " + uid);
        
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .whereEqualTo("isAiGenerated", true)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                ArrayList<Notification> notifications = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Notification notification = document.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(document.getId());
                            notifications.add(notification);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing AI notification: " + e.getMessage());
                    }
                }
                Log.d(TAG, "Loaded " + notifications.size() + " AI notifications");
                if (listener != null) {
                    listener.onNotificationsLoaded(notifications);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading AI notifications", e);
                if (listener != null) {
                    listener.onNotificationsLoaded(new ArrayList<>());
                }
            });
    }
    
    /**
     * Load notifications với callback interface tương thích FirebaseService
     */
    public void loadUserNotifications(@NonNull String uid, @Nullable OnNotificationsLoadedListener listener) {
        getByUserId(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ArrayList<Notification> notifications = new ArrayList<>(task.getResult());
                if (listener != null) {
                    listener.onNotificationsLoaded(notifications);
                }
            } else {
                if (listener != null) {
                    listener.onNotificationsLoaded(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * Cập nhật FCM token cho user
     * Tương thích với FirebaseService interface
     */
    public void updateFcmToken(@NonNull String uid, @NonNull String fcmToken, 
                               @Nullable OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Cập nhật FCM token cho user: " + uid);
        
        firestoreContext.getDocument("users", uid)
            .update("notification.fcmToken", fcmToken)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ Đã cập nhật FCM token thành công");
                if (listener != null) {
                    listener.onNotificationUpdated(true);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi cập nhật FCM token: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onNotificationUpdated(false);
                }
            });
    }
    
    /**
     * Cập nhật cài đặt thông báo AI cho user
     * Tương thích với FirebaseService interface
     */
    public void updateAiNotificationSettings(@NonNull String uid, @NonNull java.util.Map<String, Object> settings, 
                                            @Nullable OnNotificationUpdatedListener listener) {
        Log.d(TAG, "Cập nhật cài đặt AI notification cho user: " + uid);
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Object> entry : settings.entrySet()) {
            updates.put("notification." + entry.getKey(), entry.getValue());
        }
        
        firestoreContext.getDocument("users", uid)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ Đã cập nhật cài đặt thành công");
                if (listener != null) {
                    listener.onNotificationUpdated(true);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Lỗi cập nhật cài đặt: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onNotificationUpdated(false);
                }
            });
    }
    
    // Interfaces tương thích với FirebaseService
    public interface OnNotificationSavedListener {
        void onNotificationSaved(boolean success);
    }
    
    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(ArrayList<Notification> notifications);
    }
    
    public interface OnNotificationUpdatedListener {
        void onNotificationUpdated(boolean success);
    }
    
    public interface OnUnreadCountLoadedListener {
        void onUnreadCountLoaded(int count);
    }
}

