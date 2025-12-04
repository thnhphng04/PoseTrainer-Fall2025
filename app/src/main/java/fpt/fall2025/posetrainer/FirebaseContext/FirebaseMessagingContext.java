package fpt.fall2025.posetrainer.FirebaseContext;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * FirebaseMessagingContext - Context class để quản lý FirebaseMessaging (FCM)
 * Sử dụng Singleton pattern để đảm bảo chỉ có 1 instance trong toàn bộ app
 * 
 * Cung cấp:
 * - Khởi tạo FirebaseMessaging instance
 * - Truy cập FCM để lấy token, subscribe/unsubscribe topics
 */
public class FirebaseMessagingContext {
    private static final String TAG = "FirebaseMessagingContext";
    private static FirebaseMessagingContext instance;
    
    private FirebaseMessaging messaging;
    
    /**
     * Private constructor để đảm bảo Singleton pattern
     */
    private FirebaseMessagingContext() {
        messaging = FirebaseMessaging.getInstance();
        Log.d(TAG, "FirebaseMessaging instance initialized");
    }
    
    /**
     * Lấy instance duy nhất của FirebaseMessagingContext (Singleton)
     */
    public static FirebaseMessagingContext getInstance() {
        if (instance == null) {
            synchronized (FirebaseMessagingContext.class) {
                if (instance == null) {
                    instance = new FirebaseMessagingContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy FirebaseMessaging instance
     * @return FirebaseMessaging instance
     */
    @NonNull
    public FirebaseMessaging getMessaging() {
        return messaging;
    }
    
    /**
     * Lấy FCM token của thiết bị hiện tại
     * @return Task<String> chứa FCM token
     */
    @NonNull
    public Task<String> getToken() {
        return messaging.getToken();
    }
    
    /**
     * Xóa FCM token (thường dùng khi logout)
     * @return Task<Void>
     */
    @NonNull
    public Task<Void> deleteToken() {
        return messaging.deleteToken();
    }
    
    /**
     * Subscribe vào một topic để nhận notifications
     * @param topic Tên topic
     * @return Task<Void>
     */
    @NonNull
    public Task<Void> subscribeToTopic(@NonNull String topic) {
        Log.d(TAG, "Subscribing to topic: " + topic);
        return messaging.subscribeToTopic(topic);
    }
    
    /**
     * Unsubscribe khỏi một topic
     * @param topic Tên topic
     * @return Task<Void>
     */
    @NonNull
    public Task<Void> unsubscribeFromTopic(@NonNull String topic) {
        Log.d(TAG, "Unsubscribing from topic: " + topic);
        return messaging.unsubscribeFromTopic(topic);
    }
}

