package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        if (schedule == null) {
            Log.e(TAG, "Schedule không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("Schedule không hợp lệ")));
            }
            return;
        }
        
        // Nếu ID null hoặc empty, tạo mới
        if (schedule.getId() == null || schedule.getId().isEmpty()) {
            Log.d(TAG, "Tạo schedule mới (ID trống)");
            firestoreContext.getCollection(COLLECTION_NAME)
                .add(schedule)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Schedule created successfully: " + documentReference.getId());
                    schedule.setId(documentReference.getId());
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi tạo schedule mới", e);
                    if (listener != null) {
                        listener.onComplete(Tasks.<Void>forException(e));
                    }
                });
        } else {
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
    }
    
    /**
     * Save schedule với callback interface tương thích FirebaseService
     */
    public void saveSchedule(@NonNull Schedule schedule, @Nullable OnScheduleSavedListener listener) {
        save(schedule, task -> {
            if (listener != null) {
                listener.onScheduleSaved(task.isSuccessful());
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
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    // Nếu có nhiều documents, xóa các document cũ và chỉ giữ lại document đầu tiên
                    if (querySnapshot.size() > 1) {
                        Log.w(TAG, "Found " + querySnapshot.size() + " schedule documents for user, cleaning up duplicates...");
                        
                        DocumentSnapshot mainDocument = querySnapshot.getDocuments().get(0);
                        Schedule schedule = mainDocument.toObject(Schedule.class);
                        if (schedule != null) {
                            schedule.setId(mainDocument.getId());
                            
                            // Xóa các document còn lại
                            for (int i = 1; i < querySnapshot.size(); i++) {
                                DocumentSnapshot docToDelete = querySnapshot.getDocuments().get(i);
                                Log.d(TAG, "Deleting duplicate schedule document: " + docToDelete.getId());
                                docToDelete.getReference().delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted duplicate schedule: " + docToDelete.getId()))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete duplicate schedule: " + docToDelete.getId(), e));
                            }
                            
                            Log.d(TAG, "✅ Lấy schedule thành công: " + schedule.getTitle() + " (ID: " + schedule.getId() + ")");
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(schedule));
                            }
                        } else {
                            Log.w(TAG, "Schedule object is null");
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(null));
                            }
                        }
                    } else {
                        // Chỉ có 1 document
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Schedule schedule = document.toObject(Schedule.class);
                        if (schedule != null) {
                            schedule.setId(document.getId());
                            Log.d(TAG, "✅ Lấy schedule thành công: " + schedule.getTitle() + " (ID: " + schedule.getId() + ")");
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(schedule));
                            }
                        } else {
                            Log.w(TAG, "Schedule object is null");
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(null));
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "No schedule found for user");
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
     * Load user schedule với callback interface tương thích FirebaseService
     */
    public void loadUserSchedule(@NonNull String uid, @Nullable OnScheduleLoadedListener listener) {
        getByUserId(uid, task -> {
            if (task.isSuccessful()) {
                if (listener != null) {
                    listener.onScheduleLoaded(task.getResult());
                }
            } else {
                if (listener != null) {
                    listener.onScheduleLoaded(null);
                }
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
    
    // Interfaces tương thích với FirebaseService
    public interface OnScheduleLoadedListener {
        void onScheduleLoaded(Schedule schedule);
    }
    
    public interface OnScheduleSavedListener {
        void onScheduleSaved(boolean success);
    }
}

