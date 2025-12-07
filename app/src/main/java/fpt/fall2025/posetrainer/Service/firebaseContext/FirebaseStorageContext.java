package fpt.fall2025.posetrainer.Service.firebaseContext;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * FirebaseStorageContext - Context class để quản lý FirebaseStorage
 * Sử dụng Singleton pattern để đảm bảo chỉ có 1 instance trong toàn bộ app
 * 
 * Cung cấp:
 * - Khởi tạo FirebaseStorage instance
 * - Truy cập storage để upload/download files
 * - Các method tiện ích để tạo StorageReference
 */
public class FirebaseStorageContext {
    private static final String TAG = "FirebaseStorageContext";
    private static FirebaseStorageContext instance;
    
    private FirebaseStorage storage;
    
    /**
     * Private constructor để đảm bảo Singleton pattern
     */
    private FirebaseStorageContext() {
        storage = FirebaseStorage.getInstance();
        Log.d(TAG, "FirebaseStorage instance initialized");
    }
    
    /**
     * Lấy instance duy nhất của FirebaseStorageContext (Singleton)
     */
    public static FirebaseStorageContext getInstance() {
        if (instance == null) {
            synchronized (FirebaseStorageContext.class) {
                if (instance == null) {
                    instance = new FirebaseStorageContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy FirebaseStorage instance
     * @return FirebaseStorage instance
     */
    @NonNull
    public FirebaseStorage getStorage() {
        return storage;
    }
    
    /**
     * Lấy root StorageReference
     * @return StorageReference đến root
     */
    @NonNull
    public StorageReference getReference() {
        return storage.getReference();
    }
    
    /**
     * Lấy StorageReference từ path
     * @param path Đường dẫn file trong storage
     * @return StorageReference
     */
    @NonNull
    public StorageReference getReference(@NonNull String path) {
        return storage.getReference(path);
    }
    
    /**
     * Lấy StorageReference từ Uri
     * @param uri Uri của file
     * @return StorageReference
     */
    @NonNull
    public StorageReference getReferenceFromUrl(@NonNull Uri uri) {
        return storage.getReferenceFromUrl(uri.toString());
    }
    
}

