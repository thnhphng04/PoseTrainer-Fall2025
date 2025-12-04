package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * SessionDAO - Data Access Object cho Session
 * Quản lý các thao tác CRUD với collection "sessions" trong Firestore
 */
public class SessionDAO {
    private static final String TAG = "SessionDAO";
    private static final String COLLECTION_NAME = "sessions";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public SessionDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu session vào Firestore
     */
    public void save(@NonNull Session session, @Nullable OnCompleteListener<Void> listener) {
        if (session == null || session.getId() == null) {
            Log.e(TAG, "Session hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Session hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu session: " + session.getId());
        firestoreContext.getDocument(COLLECTION_NAME, session.getId())
            .set(session)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu session thành công: " + session.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu session: " + session.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy session theo ID
     */
    public void getById(@NonNull String sessionId, @Nullable OnCompleteListener<Session> listener) {
        Log.d(TAG, "Đang lấy session: " + sessionId);
        firestoreContext.getDocument(COLLECTION_NAME, sessionId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Session session = documentSnapshot.toObject(Session.class);
                    if (session != null) {
                        session.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy session thành công: " + sessionId);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(session));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse session");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Session>forException(new Exception("Không thể parse session")));
                        }
                    }
                } else {
                    Log.d(TAG, "Session không tồn tại: " + sessionId);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy session: " + sessionId, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Session>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả sessions của user
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<Session>> listener) {
        Log.d(TAG, "Đang lấy sessions của user: " + uid);
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Session> sessions = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Session session = doc.toObject(Session.class);
                    if (session != null) {
                        session.setId(doc.getId());
                        sessions.add(session);
                    }
                }
                Log.d(TAG, "✅ Lấy " + sessions.size() + " sessions thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(sessions));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy sessions của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<Session>>forException(e));
                }
            });
    }
    
    /**
     * Xóa session
     */
    public void delete(@NonNull String sessionId, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa session: " + sessionId);
        firestoreContext.getDocument(COLLECTION_NAME, sessionId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa session thành công: " + sessionId);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa session: " + sessionId, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật session
     */
    public void update(@NonNull String sessionId, @NonNull Session session, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật session: " + sessionId);
        firestoreContext.getDocument(COLLECTION_NAME, sessionId)
            .set(session)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật session thành công: " + sessionId);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật session: " + sessionId, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy collection reference để query
     * @return CollectionReference cho sessions
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lấy document reference
     * @param sessionId ID của session
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String sessionId) {
        return firestoreContext.getDocument(COLLECTION_NAME, sessionId);
    }
    
    /**
     * Lấy query với whereEqualTo và orderBy
     * @param field Field để filter
     * @param value Value để filter
     * @param orderByField Field để order
     * @param direction Direction (ASCENDING/DESCENDING)
     * @return Query
     */
    @NonNull
    public Query getQuery(@NonNull String field, @NonNull Object value, 
                         @NonNull String orderByField, @NonNull Query.Direction direction) {
        return firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo(field, value)
            .orderBy(orderByField, direction);
    }
    
    /**
     * Lấy query với whereEqualTo, whereGreaterThan và orderBy
     * @param field1 Field để filter (whereEqualTo)
     * @param value1 Value để filter (whereEqualTo)
     * @param field2 Field để filter (whereGreaterThan)
     * @param value2 Value để filter (whereGreaterThan)
     * @return Query
     */
    @NonNull
    public Query getQueryWithGreaterThan(@NonNull String field1, @NonNull Object value1,
                                        @NonNull String field2, @NonNull Object value2) {
        return firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo(field1, value1)
            .whereGreaterThan(field2, value2);
    }
    
    /**
     * Lắng nghe real-time updates cho sessions query
     * @param query Query để lắng nghe
     * @param listener Listener để xử lý updates
     * @return ListenerRegistration để có thể remove listener
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull Query query,
                                                   @NonNull com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        return query.addSnapshotListener(listener);
    }
}

