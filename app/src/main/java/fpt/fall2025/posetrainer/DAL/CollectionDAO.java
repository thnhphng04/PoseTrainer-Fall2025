package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Collection;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * CollectionDAO - Data Access Object cho Collection
 * Quản lý các thao tác CRUD với collection "collections" trong Firestore
 */
public class CollectionDAO {
    private static final String TAG = "CollectionDAO";
    private static final String COLLECTION_NAME = "collections";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public CollectionDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu collection vào Firestore
     */
    public void save(@NonNull Collection collection, @Nullable OnCompleteListener<Void> listener) {
        if (collection == null || collection.getId() == null) {
            Log.e(TAG, "Collection hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Collection hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu collection: " + collection.getId());
        firestoreContext.getDocument(COLLECTION_NAME, collection.getId())
            .set(collection)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu collection thành công: " + collection.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu collection: " + collection.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy collection theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Collection> listener) {
        Log.d(TAG, "Đang lấy collection: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Collection collection = documentSnapshot.toObject(Collection.class);
                    if (collection != null) {
                        collection.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy collection thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(collection));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse collection");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Collection>forException(new Exception("Không thể parse collection")));
                        }
                    }
                } else {
                    Log.d(TAG, "Collection không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy collection: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Collection>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả collections công khai
     */
    public void getPublicCollections(@Nullable OnCompleteListener<List<Collection>> listener) {
        Log.d(TAG, "Đang lấy public collections");
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Collection> collections = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Collection collection = doc.toObject(Collection.class);
                    if (collection != null) {
                        collection.setId(doc.getId());
                        collections.add(collection);
                    }
                }
                Log.d(TAG, "✅ Lấy " + collections.size() + " collections thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(collections));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy public collections", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Collection>>forException(e));
                }
            });
    }
    
    /**
     * Xóa collection
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa collection: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa collection thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa collection: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy document reference
     * @param collectionId Collection ID
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String collectionId) {
        return firestoreContext.getDocument(COLLECTION_NAME, collectionId);
    }
}

