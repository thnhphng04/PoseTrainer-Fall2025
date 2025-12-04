package fpt.fall2025.posetrainer.FirebaseContext;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FirebaseFirestoreContext - Context class để quản lý FirebaseFirestore
 * Sử dụng Singleton pattern để đảm bảo chỉ có 1 instance trong toàn bộ app
 * 
 * Cung cấp:
 * - Khởi tạo FirebaseFirestore instance
 * - Truy cập database để thực hiện các operations
 */
public class FirebaseFirestoreContext {
    private static final String TAG = "FirebaseFirestoreContext";
    private static FirebaseFirestoreContext instance;
    
    private FirebaseFirestore db;
    
    /**
     * Private constructor để đảm bảo Singleton pattern
     */
    private FirebaseFirestoreContext() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "FirebaseFirestore instance initialized");
    }
    
    /**
     * Lấy instance duy nhất của FirebaseFirestoreContext (Singleton)
     */
    public static FirebaseFirestoreContext getInstance() {
        if (instance == null) {
            synchronized (FirebaseFirestoreContext.class) {
                if (instance == null) {
                    instance = new FirebaseFirestoreContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lấy FirebaseFirestore instance
     * @return FirebaseFirestore instance
     */
    @NonNull
    public FirebaseFirestore getFirestore() {
        return db;
    }
    
    /**
     * Lấy collection reference
     * @param collectionPath Đường dẫn collection
     * @return CollectionReference
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection(@NonNull String collectionPath) {
        return db.collection(collectionPath);
    }
    
    /**
     * Lấy document reference
     * @param collectionPath Đường dẫn collection
     * @param documentPath Đường dẫn document
     * @return DocumentReference
     */
    @NonNull
    public com.google.firebase.firestore.DocumentReference getDocument(@NonNull String collectionPath, @NonNull String documentPath) {
        return db.collection(collectionPath).document(documentPath);
    }
    
    /**
     * Lấy batch để thực hiện multiple writes
     * @return WriteBatch
     */
    @NonNull
    public com.google.firebase.firestore.WriteBatch getBatch() {
        return db.batch();
    }
    
    /**
     * Enable/disable network persistence
     * @param enabled true để enable, false để disable
     */
    public void setPersistenceEnabled(boolean enabled) {
        try {
            db.setFirestoreSettings(
                new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(enabled)
                    .build()
            );
            Log.d(TAG, "Persistence " + (enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Log.e(TAG, "Error setting persistence: " + e.getMessage());
        }
    }
}

