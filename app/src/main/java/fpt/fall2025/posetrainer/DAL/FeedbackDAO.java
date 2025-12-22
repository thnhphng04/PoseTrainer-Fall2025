package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.Feedback;
import fpt.fall2025.posetrainer.Service.firebaseContext.FirebaseFirestoreContext;

/**
 * FeedbackDAO - Data Access Object cho Feedback
 * Quản lý các thao tác CRUD với collection "feedbacks" trong Firestore
 */
public class FeedbackDAO {
    private static final String TAG = "FeedbackDAO";
    private static final String COLLECTION_NAME = "feedbacks";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public FeedbackDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu feedback vào Firestore
     */
    public void save(@NonNull Feedback feedback, @Nullable OnCompleteListener<Void> listener) {
        if (feedback == null) {
            Log.e(TAG, "Feedback không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Feedback không hợp lệ")));
            }
            return;
        }
        
        // Tạo ID nếu chưa có
        if (feedback.getId() == null || feedback.getId().isEmpty()) {
            String id = firestoreContext.getCollection(COLLECTION_NAME).document().getId();
            feedback.setId(id);
        }
        
        Log.d(TAG, "Đang lưu feedback: " + feedback.getId());
        firestoreContext.getDocument(COLLECTION_NAME, feedback.getId())
            .set(feedback)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu feedback thành công: " + feedback.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu feedback: " + feedback.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy feedback theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Feedback> listener) {
        Log.d(TAG, "Đang lấy feedback: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Feedback feedback = documentSnapshot.toObject(Feedback.class);
                    if (feedback != null) {
                        feedback.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy feedback thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(feedback));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse feedback");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Feedback>forException(new Exception("Không thể parse feedback")));
                        }
                    }
                } else {
                    Log.d(TAG, "Feedback không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy feedback: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Feedback>forException(e));
                }
            });
    }
    
    /**
     * Cập nhật feedback
     */
    public void update(@NonNull String id, @NonNull Feedback feedback, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang cập nhật feedback: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .set(feedback)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Cập nhật feedback thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi cập nhật feedback: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy document reference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String id) {
        return firestoreContext.getDocument(COLLECTION_NAME, id);
    }
    
    /**
     * Lấy collection reference
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lấy danh sách feedback của user theo UID
     * Sắp xếp theo thời gian tạo mới nhất trước
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<List<Feedback>> listener) {
        Log.d(TAG, "Đang lấy feedback của user: " + uid);
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Feedback> feedbacks = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Feedback feedback = doc.toObject(Feedback.class);
                    if (feedback != null) {
                        feedback.setId(doc.getId());
                        feedbacks.add(feedback);
                    }
                }
                Log.d(TAG, "✅ Lấy " + feedbacks.size() + " feedback thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(feedbacks));
                }
            })
            .addOnFailureListener(e -> {
                // Nếu thiếu index, thử query đơn giản hơn và sort client-side
                if (e.getMessage() != null && (e.getMessage().contains("index") || e.getMessage().contains("requires an index"))) {
                    Log.w(TAG, "⚠ Thiếu index cho feedback query, dùng query đơn giản cho user " + uid);
                    firestoreContext.getCollection(COLLECTION_NAME)
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<Feedback> feedbacks = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                Feedback feedback = doc.toObject(Feedback.class);
                                if (feedback != null) {
                                    feedback.setId(doc.getId());
                                    feedbacks.add(feedback);
                                }
                            }
                            // Sort client-side theo createdAt giảm dần
                            feedbacks.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                            Log.d(TAG, "✅ Lấy " + feedbacks.size() + " feedback thành công (client-side sort)");
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(feedbacks));
                            }
                        })
                        .addOnFailureListener(fallbackError -> {
                            Log.e(TAG, "❌ Lỗi lấy feedback của user: " + uid, fallbackError);
                            if (listener != null) {
                                listener.onComplete(Tasks.<List<Feedback>>forException(fallbackError));
                            }
                        });
                } else {
                    Log.e(TAG, "❌ Lỗi lấy feedback của user: " + uid, e);
                    if (listener != null) {
                        listener.onComplete(Tasks.<List<Feedback>>forException(e));
                    }
                }
            });
    }
    
    /**
     * Lắng nghe real-time updates cho danh sách feedback của user theo UID
     * Sắp xếp theo thời gian tạo mới nhất trước
     * @param uid UID của user
     * @param listener Listener để xử lý updates (nhận danh sách Feedback mỗi khi có thay đổi)
     * @return ListenerRegistration để có thể remove listener khi không cần nữa
     */
    @NonNull
    public ListenerRegistration listenByUserId(@NonNull String uid, 
                                               @NonNull com.google.firebase.firestore.EventListener<QuerySnapshot> listener) {
        Log.d(TAG, "Đang lắng nghe feedback của user: " + uid);
        
        Query query = firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING);
        
        ListenerRegistration registration = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                // Nếu lỗi do thiếu index, log cảnh báo và vẫn gọi listener với error
                if (error.getMessage() != null && (error.getMessage().contains("index") || error.getMessage().contains("requires an index"))) {
                    Log.w(TAG, "⚠ Thiếu index cho feedback query với orderBy. Có thể cần tạo index hoặc dùng query đơn giản hơn.");
                }
                Log.e(TAG, "❌ Lỗi lắng nghe feedback của user: " + uid, error);
                listener.onEvent(null, error);
                return;
            }
            
            if (querySnapshot != null) {
                listener.onEvent(querySnapshot, null);
            }
        });
        
        return registration;
    }
}

