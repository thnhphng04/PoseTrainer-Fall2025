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
import fpt.fall2025.posetrainer.Activity.WorkoutActivity;
import fpt.fall2025.posetrainer.Domain.UserWorkout;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ViewholderUserWorkoutBinding;

import java.util.ArrayList;

public class UserWorkoutCardAdapter extends RecyclerView.Adapter<UserWorkoutCardAdapter.Viewholder> {
    private final ArrayList<UserWorkout> list;
    private Context context;
    private OnUserWorkoutDeletedListener onUserWorkoutDeletedListener;

    public interface OnUserWorkoutDeletedListener {
        void onUserWorkoutDeleted();
    }

    public UserWorkoutCardAdapter(ArrayList<UserWorkout> list) {
        this.list = list;
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
        holder.binding.excerciseTxt.setText(exerciseCount + " Exercise" + (exerciseCount != 1 ? "s" : ""));
        
        // Set source badge
        String source = userWorkout.getSource() != null ? userWorkout.getSource() : "custom";
        holder.binding.sourceBadge.setText(capitalizeFirst(source));
        
        // Set creation date
        holder.binding.durationTxt.setText("Created " + formatRelativeDate(userWorkout.getCreatedAt()));

        // Set click listener on the root view to start workout
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, WorkoutActivity.class);
                intent.putExtra("userWorkoutId", userWorkout.getId());
                intent.putExtra("fromFavoriteFragment", true);
                System.out.println("UserWorkoutCardAdapter: Starting WorkoutActivity with ID: " + userWorkout.getId() + " from FavoriteFragment");
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
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to delete \"" + userWorkout.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteUserWorkout(userWorkout.getId(), position);
                })
                .setNegativeButton("Cancel", null)
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
                    
                    Toast.makeText(context, "Workout deleted successfully", Toast.LENGTH_SHORT).show();
                    
                    // Notify parent fragment
                    if (onUserWorkoutDeletedListener != null) {
                        onUserWorkoutDeletedListener.onUserWorkoutDeleted();
                    }
                } else {
                    Toast.makeText(context, "Failed to delete workout", Toast.LENGTH_SHORT).show();
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
     * Format timestamp to relative date (e.g., "2 days ago")
     */
    private String formatRelativeDate(long timestamp) {
        if (timestamp == 0) return "unknown";
        
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;
        
        if (diff < 60) {
            return "just now";
        } else if (diff < 3600) {
            long minutes = diff / 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
        } else if (diff < 86400) {
            long hours = diff / 3600;
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        } else if (diff < 2592000) {
            long days = diff / 86400;
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        } else {
            long months = diff / 2592000;
            return months + " month" + (months != 1 ? "s" : "") + " ago";
        }
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderUserWorkoutBinding binding;

        public Viewholder(ViewholderUserWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
