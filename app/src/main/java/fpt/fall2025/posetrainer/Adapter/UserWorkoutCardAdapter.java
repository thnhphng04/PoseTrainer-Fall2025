package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import fpt.fall2025.posetrainer.Activity.UserWorkoutDetailActivity;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ViewholderUserWorkoutBinding;

import java.util.ArrayList;

public class UserWorkoutCardAdapter extends RecyclerView.Adapter<UserWorkoutCardAdapter.Viewholder> {
    private final ArrayList<UserWorkout> list;
    private Context context;
    private OnUserWorkoutDeletedListener onUserWorkoutDeletedListener;
    private FirebaseFirestore db;
    private String cachedTrainingStartTime;
    private String cachedTrainingEndTime;
    private boolean isProfileLoaded = false;

    public interface OnUserWorkoutDeletedListener {
        void onUserWorkoutDeleted();
    }

    public UserWorkoutCardAdapter(ArrayList<UserWorkout> list) {
        this.list = list;
        this.db = FirebaseFirestore.getInstance();
        // ✅ Load profile ngay khi adapter được tạo để cache sẵn
        loadProfileForCache();
    }

    /**
     * Load profile một lần để cache thời gian tập luyện
     */
    private void loadProfileForCache() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || isProfileLoaded) {
            return;
        }

        String uid = currentUser.getUid();
        db.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cachedTrainingStartTime = documentSnapshot.getString("trainingStartTime");
                        cachedTrainingEndTime = documentSnapshot.getString("trainingEndTime");
                        isProfileLoaded = true;
                    }
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi load profile, bỏ qua
                });
    }

    public void setOnUserWorkoutDeletedListener(OnUserWorkoutDeletedListener listener) {
        this.onUserWorkoutDeletedListener = listener;
    }

    @NonNull
    @Override
    public UserWorkoutCardAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderUserWorkoutBinding binding = ViewholderUserWorkoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserWorkoutCardAdapter.Viewholder holder, int position) {
        UserWorkout userWorkout = list.get(position);
        
        System.out.println("UserWorkoutCardAdapter: Binding position " + position + " with workout: " + userWorkout.getTitle());
        
        // Set workout title
        holder.binding.titleTxt.setText(userWorkout.getTitle());
        
        // Set workout image
        int resId = getImageResourceForWorkout(userWorkout);
        Glide.with(holder.itemView.getContext())
                .load(resId)
                .centerCrop()
                .into(holder.binding.pic);

        // Set exercise count
        int exerciseCount = userWorkout.getItems() != null ? userWorkout.getItems().size() : 0;
        if (exerciseCount == 0) {
            holder.binding.excerciseTxt.setText("Chưa có bài tập");
        } else if (exerciseCount == 1) {
            holder.binding.excerciseTxt.setText("1 bài tập");
        } else {
            holder.binding.excerciseTxt.setText(exerciseCount + " bài tập");
        }
        
        // Set source badge
        String source = userWorkout.getSource() != null ? userWorkout.getSource() : "custom";
        String sourceText = getSourceText(source);
        holder.binding.sourceBadge.setText(sourceText);
        
        // Set creation date
        holder.binding.durationTxt.setText("Tạo " + formatRelativeDateVietnamese(userWorkout.getCreatedAt()));

        // ✅ Hiển thị thời gian tập luyện cho AI workouts
        if ("ai".equals(source)) {
            // Load profile để lấy thời gian tập luyện (chỉ load một lần và cache)
            loadProfileAndShowTrainingTime(holder, userWorkout);
        } else {
            // Ẩn training time layout cho non-AI workouts
            if (holder.binding.trainingTimeLayout != null) {
                holder.binding.trainingTimeLayout.setVisibility(android.view.View.GONE);
            }
        }

        // Set click listener on the root view to start workout
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, UserWorkoutDetailActivity.class);
                intent.putExtra("userWorkoutId", userWorkout.getId());
                intent.putExtra("fromMyWorkoutFragment", true);
                System.out.println("UserWorkoutCardAdapter: Starting UserWorkoutDetailActivity with ID: " + userWorkout.getId() + " from MyWorkoutFragment");
                context.startActivity(intent);
            } else {
                System.out.println("UserWorkoutCardAdapter: Context is null, cannot start activity");
            }
        });
        
        // Set long click listener for delete option
        holder.binding.getRoot().setOnLongClickListener(v -> {
            showDeleteDialog(userWorkout, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteDialog(UserWorkout userWorkout, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bài tập")
                .setMessage("Bạn có chắc chắn muốn xóa \"" + userWorkout.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteUserWorkout(userWorkout.getId(), position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Delete user workout from Firebase
     */
    private void deleteUserWorkout(String userWorkoutId, int position) {
        FirebaseService.getInstance().deleteUserWorkout(userWorkoutId, new FirebaseService.OnUserWorkoutDeletedListener() {
            @Override
            public void onUserWorkoutDeleted(boolean success) {
                if (success) {
                    // Remove from local list
                    list.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, list.size());
                    
                    Toast.makeText(context, "Đã xóa bài tập thành công", Toast.LENGTH_SHORT).show();
                    
                    // Notify parent fragment
                    if (onUserWorkoutDeletedListener != null) {
                        onUserWorkoutDeletedListener.onUserWorkoutDeleted();
                    }
                } else {
                    Toast.makeText(context, "Không thể xóa bài tập", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Get image resource based on workout source/type
     */
    private int getImageResourceForWorkout(UserWorkout userWorkout) {
        // Default image
        int defaultResId = context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
        
        if (userWorkout.getSource() != null) {
            switch (userWorkout.getSource()) {
                case "template":
                    return context.getResources().getIdentifier("pic_2", "drawable", context.getPackageName());
                case "ai":
                    return context.getResources().getIdentifier("pic_3", "drawable", context.getPackageName());
                case "custom":
                default:
                    return defaultResId;
            }
        }
        
        return defaultResId;
    }

    /**
     * Format timestamp to relative date in Vietnamese (e.g., "2 ngày trước")
     */
    private String formatRelativeDateVietnamese(long timestamp) {
        if (timestamp == 0) return "Không xác định";
        
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;
        
        if (diff < 60) {
            return "vừa xong";
        } else if (diff < 3600) {
            long minutes = diff / 60;
            return minutes + " phút trước";
        } else if (diff < 86400) {
            long hours = diff / 3600;
            return hours + " giờ trước";
        } else if (diff < 2592000) {
            long days = diff / 86400;
            return days + " ngày trước";
        } else if (diff < 31104000) {
            long months = diff / 2592000;
            return months + " tháng trước";
        } else {
            long years = diff / 31104000;
            return years + " năm trước";
        }
    }

    /**
     * Get Vietnamese text for source
     */
    private String getSourceText(String source) {
        if (source == null) return "Tùy chỉnh";
        
        switch (source.toLowerCase()) {
            case "template":
                return "Mẫu";
            case "ai":
                return "AI";
            case "custom":
            default:
                return "Tùy chỉnh";
        }
    }

    /**
     * Load profile và hiển thị thời gian tập luyện cho AI workout
     */
    private void loadProfileAndShowTrainingTime(Viewholder holder, UserWorkout workout) {
        // Nếu đã load profile rồi, sử dụng cache
        if (isProfileLoaded && cachedTrainingStartTime != null && cachedTrainingEndTime != null) {
            showTrainingTime(holder, cachedTrainingStartTime, cachedTrainingEndTime);
            return;
        }

        // Load profile từ Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (holder.binding.trainingTimeLayout != null) {
                holder.binding.trainingTimeLayout.setVisibility(android.view.View.GONE);
            }
            return;
        }

        String uid = currentUser.getUid();
        db.collection("profiles").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String trainingStartTime = documentSnapshot.getString("trainingStartTime");
                        String trainingEndTime = documentSnapshot.getString("trainingEndTime");
                        
                        // Cache lại
                        cachedTrainingStartTime = trainingStartTime;
                        cachedTrainingEndTime = trainingEndTime;
                        isProfileLoaded = true;
                        
                        // Hiển thị thời gian
                        showTrainingTime(holder, trainingStartTime, trainingEndTime);
                    } else {
                        // Không có profile, ẩn training time
                        if (holder.binding.trainingTimeLayout != null) {
                            holder.binding.trainingTimeLayout.setVisibility(android.view.View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi load profile, ẩn training time
                    if (holder.binding.trainingTimeLayout != null) {
                        holder.binding.trainingTimeLayout.setVisibility(android.view.View.GONE);
                    }
                });
    }

    /**
     * Hiển thị thời gian tập luyện trong holder
     */
    private void showTrainingTime(Viewholder holder, String trainingStartTime, String trainingEndTime) {
        if (holder.binding.trainingTimeLayout == null || holder.binding.trainingTimeTxt == null) {
            return;
        }

        if (trainingStartTime != null && !trainingStartTime.isEmpty() && 
            trainingEndTime != null && !trainingEndTime.isEmpty()) {
            String timeText = trainingStartTime + " - " + trainingEndTime;
            holder.binding.trainingTimeTxt.setText(timeText);
            holder.binding.trainingTimeLayout.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.trainingTimeLayout.setVisibility(android.view.View.GONE);
        }
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderUserWorkoutBinding binding;

        public Viewholder(ViewholderUserWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
