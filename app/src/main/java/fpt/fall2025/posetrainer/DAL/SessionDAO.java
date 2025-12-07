package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

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
        
        // Kiểm tra uid trước khi lưu
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Không thể lưu session: người dùng chưa đăng nhập");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalStateException("Người dùng chưa đăng nhập")));
            }
            return;
        }
        
        String currentUid = currentUser.getUid();
        String sessionUid = session.getUid();
        
        // Đảm bảo uid được set đúng
        if (sessionUid == null || sessionUid.isEmpty()) {
            Log.w(TAG, "⚠️ Session UID trống, đang set thành UID của người dùng hiện tại: " + currentUid);
            session.setUid(currentUid);
        } else if (!sessionUid.equals(currentUid)) {
            Log.w(TAG, "⚠️ Session UID (" + sessionUid + ") không khớp với UID hiện tại (" + currentUid + "), đang cập nhật...");
            session.setUid(currentUid);
        }
        
        Log.d(TAG, "💾 Đang lưu session vào Firestore: " + session.getId() + ", UID: " + session.getUid());
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
     * Lưu session với callback interface tương thích FirebaseService
     */
    public void saveSession(@NonNull Session session, @Nullable OnSessionSavedListener listener) {
        save(session, task -> {
            if (listener != null) {
                listener.onSessionSaved(task.isSuccessful());
            }
        });
    }
    
    /**
     * Load session by ID với callback interface tương thích FirebaseService
     */
    public void loadSessionById(@NonNull String sessionId, @Nullable OnSessionLoadedListener listener) {
        Log.d(TAG, "Loading session by ID: " + sessionId);
        
        getById(sessionId, task -> {
            if (task.isSuccessful()) {
                Session session = task.getResult();
                Log.d(TAG, "Loaded session by ID: " + (session != null ? session.getId() : "null"));
                if (listener != null) {
                    listener.onSessionLoaded(session);
                }
            } else {
                Log.e(TAG, "Error loading session by ID", task.getException());
                if (listener != null) {
                    listener.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                }
            }
        });
    }
    
    /**
     * Load active session for a specific workout
     * Tương thích với FirebaseService interface
     */
    public void loadActiveSession(@NonNull String workoutId, @Nullable OnSessionLoadedListener listener) {
        Log.d(TAG, "Loading active session for workout: " + workoutId);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            if (listener != null) {
                listener.onError("User not authenticated");
            }
            return;
        }
        
        String uid = currentUser.getUid();
        Log.d(TAG, "Loading active session for user: " + uid);
        
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("workoutId", workoutId)
            .whereEqualTo("uid", uid)
            .whereEqualTo("endedAt", 0)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No active session found for workout: " + workoutId);
                    if (listener != null) {
                        listener.onSessionLoaded(null);
                    }
                } else {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    Session session = document.toObject(Session.class);
                    if (session != null) {
                        session.setId(document.getId());
                        Log.d(TAG, "Loaded active session: " + session.getId());
                        if (listener != null) {
                            listener.onSessionLoaded(session);
                        }
                    } else {
                        Log.e(TAG, "Failed to parse session object");
                        if (listener != null) {
                            listener.onError("Failed to parse session");
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading active session", e);
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    // Interfaces tương thích với FirebaseService
    public interface OnSessionSavedListener {
        void onSessionSaved(boolean success);
    }
    
    public interface OnSessionLoadedListener {
        void onSessionLoaded(Session session);
        void onError(String error);
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

