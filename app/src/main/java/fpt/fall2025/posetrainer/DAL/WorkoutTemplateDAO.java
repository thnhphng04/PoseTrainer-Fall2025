package fpt.fall2025.posetrainer.DAL;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseFirestoreContext;
import fpt.fall2025.posetrainer.FirebaseContext.FirebaseStorageContext;

/**
 * WorkoutTemplateDAO - Data Access Object cho WorkoutTemplate
 * Quản lý các thao tác CRUD với collection "workouts_templates" trong Firestore
 */
public class WorkoutTemplateDAO {
    private static final String TAG = "WorkoutTemplateDAO";
    private static final String COLLECTION_NAME = "workouts_templates";
    
    private FirebaseFirestoreContext firestoreContext;
    private FirebaseStorageContext storageContext;
    
    public WorkoutTemplateDAO() {
        this.firestoreContext = FirebaseFirestoreContext.getInstance();
        this.storageContext = FirebaseStorageContext.getInstance();
    }
    
    /**
     * Lưu workout template vào Firestore
     */
    public void save(@NonNull WorkoutTemplate template, @Nullable OnCompleteListener<Void> listener) {
        if (template == null || template.getId() == null) {
            Log.e(TAG, "WorkoutTemplate hoặc ID không hợp lệ");
            if (listener != null) {
                listener.onComplete(Tasks.<Void>forException(new IllegalArgumentException("WorkoutTemplate hoặc ID không hợp lệ")));
            }
            return;
        }
        
        Log.d(TAG, "Đang lưu workout template: " + template.getId());
        firestoreContext.getDocument(COLLECTION_NAME, template.getId())
            .set(template)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Lưu workout template thành công: " + template.getId());
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lưu workout template: " + template.getId(), e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy workout template theo ID
     */
    public void getById(@NonNull String id, @Nullable OnCompleteListener<WorkoutTemplate> listener) {
        Log.d(TAG, "Đang lấy workout template: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    WorkoutTemplate template = documentSnapshot.toObject(WorkoutTemplate.class);
                    if (template != null) {
                        template.setId(documentSnapshot.getId());
                        Log.d(TAG, "✅ Lấy workout template thành công: " + id);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(template));
                        }
                    } else {
                        Log.e(TAG, "❌ Không thể parse workout template");
                        if (listener != null) {
                            listener.onComplete(Tasks.<WorkoutTemplate>forException(new Exception("Không thể parse workout template")));
                        }
                    }
                } else {
                    Log.d(TAG, "Workout template không tồn tại: " + id);
                    if (listener != null) {
                        listener.onComplete(Tasks.forResult(null));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy workout template: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<WorkoutTemplate>forException(e));
                }
            });
    }
    
    /**
     * Lấy tất cả workout templates công khai
     */
    public void getPublicTemplates(@Nullable OnCompleteListener<List<WorkoutTemplate>> listener) {
        Log.d(TAG, "Đang lấy public workout templates");
        firestoreContext.getCollection(COLLECTION_NAME)
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<WorkoutTemplate> templates = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    WorkoutTemplate template = doc.toObject(WorkoutTemplate.class);
                    if (template != null) {
                        template.setId(doc.getId());
                        templates.add(template);
                    }
                }
                Log.d(TAG, "✅ Lấy " + templates.size() + " workout templates thành công");
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(templates));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi lấy public workout templates", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<List<WorkoutTemplate>>forException(e));
                }
            });
    }
    
    /**
     * Xóa workout template
     */
    public void delete(@NonNull String id, @Nullable OnCompleteListener<Void> listener) {
        Log.d(TAG, "Đang xóa workout template: " + id);
        firestoreContext.getDocument(COLLECTION_NAME, id)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Xóa workout template thành công: " + id);
                if (listener != null) {
                    listener.onComplete(Tasks.forResult(null));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi xóa workout template: " + id, e);
                if (listener != null) {
                    listener.onComplete(Tasks.<Void>forException(e));
                }
            });
    }
    
    /**
     * Lấy StorageReference cho workout images
     * @param workoutId Workout ID
     * @param fileName Tên file
     * @return StorageReference đến workout image path
     */
    @NonNull
    private StorageReference getWorkoutImageReference(@NonNull String workoutId, @NonNull String fileName) {
        return storageContext.getReference().child("workouts").child(workoutId).child(fileName);
    }
    
    /**
     * Upload ảnh cho workout template
     * @param workoutId Workout ID
     * @param imageUri Uri của ảnh
     * @param fileName Tên file
     * @param listener Callback trả về download URL
     */
    public void uploadWorkoutImage(@NonNull String workoutId, @NonNull Uri imageUri, 
                                  @NonNull String fileName,
                                  @Nullable OnCompleteListener<String> listener) {
        Log.d(TAG, "Đang upload ảnh cho workout: " + workoutId);
        StorageReference storageRef = getWorkoutImageReference(workoutId, fileName);
        
        StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build();
        
        storageRef.putFile(imageUri, metadata)
            .addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "✅ Upload ảnh thành công, đang lấy download URL");
                storageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "✅ Lấy download URL thành công: " + downloadUrl);
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(downloadUrl));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Lỗi lấy download URL", e);
                        if (listener != null) {
                            listener.onComplete(Tasks.<String>forException(e));
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Lỗi upload ảnh", e);
                if (listener != null) {
                    listener.onComplete(Tasks.<String>forException(e));
                }
            });
    }
}

