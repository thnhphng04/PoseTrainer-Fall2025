package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

import fpt.fall2025.posetrainer.Domain.Schedule;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;

/**
 * ScheduleDAO - Data Access Object cho Schedule
 * Quản lý các thao tác CRUD với collection "schedules" trong Firestore
 */
public class ScheduleDAO {
    private static final String TAG = "ScheduleDAO";
    private static final String COLLECTION_NAME = "schedules";
    
    private FirebaseFirestoreContext firestoreContext;
    
    public ScheduleDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
    }
    
    /**
     * Lưu schedule vào Firestore
     */
    public void save(@NonNull Schedule schedule, @Nullable OnCompleteListener<Void> listener) {
        if (schedule == null || schedule.getId() == null) {
            Log.e(TAG, "Schedule hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Schedule hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu schedule: " + schedule.getId());
        firestoreContext.getDocument(COLLECTION_NAME, schedule.getId())
            .set(schedule)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu schedule thành công: " + schedule.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu schedule: " + schedule.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy schedule theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<Schedule> listener) {
        Log.d(TAG, "Đang lấy schedule: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule != null) {
                        Log.d(TAG, "✅ Lấy schedule thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(schedule));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse schedule");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Schedule>forException(new Exception("Không thể parse schedule")));
                        }
                    }
                } else {
                    Log.d(TAG, "Schedule không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy schedule: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Schedule>forException(e));
                }
            });
    }
    
    /**
     * Lấy schedule của user
     */
    public void getByUserId(@NonNull String uid, @Nullable OnCompleteListener<Schedule> listener) {
        Log.d(TAG, "Đang lấy schedule của user: " + uid);
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    Schedule schedule = querySnapshot.getDocuments().get(0).toObject(Schedule.class);
                    if (schedule != null) {
                        schedule.setId(querySnapshot.getDocuments().get(0).getId());
                        Log.d(TAG, "✅ Lấy schedule thành công");
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(schedule));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse schedule");
                        if (listener != null) {
                            listener.onComplete(Tasks.<Schedule>forException(new Exception("Không thể parse schedule")));
                        }
                    }
                } else {
                    Log.d(TAG, "Schedule không tồn tại cho user: " + uid);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy schedule của user: " + uid, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Schedule>forException(e));
                }
            });
    }
    
    /**
     * Xóa schedule
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa schedule: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa schedule thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa schedule: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy collection reference để query
     * @return CollectionReference cho schedules
     */
    @NonNull
    public com.google.firebase.firestore.CollectionReference getCollection() {
        return firestoreContext.getCollection(COLLECTION_NAME);
    }
    
    /**
     * Lấy document reference
     * @param scheduleId Schedule ID
     * @return DocumentReference
     */
    @NonNull
    public DocumentReference getDocument(@NonNull String scheduleId) {
        return firestoreContext.getDocument(COLLECTION_NAME, scheduleId);
    }
}

