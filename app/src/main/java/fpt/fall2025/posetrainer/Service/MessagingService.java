package fpt.fall2025.posetrainer.Service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;

import fpt.fall2025.posetrainer.DAL.UserDAO;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseMessagingContext;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

/**
 * MessagingService - Service để quản lý FCM token và topics
 * Xử lý lấy token, cập nhật token lên Firestore, subscribe/unsubscribe topics
 */
public class MessagingService {
    private static final String TAG = "MessagingService";
    
    private FirebaseMessagingContext messagingContext;
    private UserDAO userDAO;
    
    public MessagingService() {
        this.messagingContext = FirebaseMessagingContext.getInstance();
        this.userDAO = new UserDAO();
    }
    
    /**
     * Lấy FCM token của thiết bị
     */
    public void getFcmToken(@Nullable OnCompleteListener<String> listener) {
        Log.d(TAG, "Đang lấy FCM token");
        messagingContext.getToken()
            .addOnSuccessListener(token -> {
                Log.d(TAG, "✅ Lấy FCM token thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(token));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy FCM token", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<String>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật FCM token lên Firestore
     */
    public void updateFcmToken(@NonNull String uid, @NonNull String token,
                               @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật FCM token cho user: " + uid);
        
        // Sử dụng Firestore để cập nhật token
        // Có thể dùng UserDAO hoặc trực tiếp Firestore
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
        notificationMap.put("fcmToken", token);
        updates.put("notification", notificationMap);
        
        // Tạm thời dùng Firestore trực tiếp vì UserDAO chưa có update field cụ thể
        FirebaseFirestoreContext.getInstance()
            .getDocument("users", uid)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật FCM token thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật FCM token", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Xóa FCM token (thường dùng khi logout)
     */
    public void deleteToken(@Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa FCM token");
        messagingContext.deleteToken()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa FCM token thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa FCM token", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Subscribe vào một topic
     */
    public void subscribeToTopic(@NonNull String topic, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang subscribe vào topic: " + topic);
        messagingContext.subscribeToTopic(topic)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Subscribe topic thành công: " + topic);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi subscribe topic: " + topic, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Unsubscribe khỏi một topic
     */
    public void unsubscribeFromTopic(@NonNull String topic, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang unsubscribe khỏi topic: " + topic);
        messagingContext.unsubscribeFromTopic(topic)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Unsubscribe topic thành công: " + topic);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi unsubscribe topic: " + topic, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
}

