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
import fpt.fall2025.posetrainer.databinding.ViewholderWorktoutBinding;

import java.util.ArrayList;

public class UserWorkoutAdapter extends RecyclerView.Adapter<UserWorkoutAdapter.Viewholder> {
    private final ArrayList<UserWorkout> list;
    private Context context;
    private OnUserWorkoutDeletedListener onUserWorkoutDeletedListener;

    public interface OnUserWorkoutDeletedListener {
        void onUserWorkoutDeleted();
    }

    public UserWorkoutAdapter(ArrayList<UserWorkout> list) {
        this.list = list;
    }

    public void setOnUserWorkoutDeletedListener(OnUserWorkoutDeletedListener listener) {
        this.onUserWorkoutDeletedListener = listener;
    }

    @NonNull
    @Override
    public UserWorkoutAdapter.Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderWorktoutBinding binding = ViewholderWorktoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserWorkoutAdapter.Viewholder holder, int position) {
        UserWorkout userWorkout = list.get(position);
        
        System.out.println("UserWorkoutAdapter: Binding position " + position + " with workout: " + userWorkout.getTitle());
        
        holder.binding.titleTxt.setText(userWorkout.getTitle());
        
        // Set default image based on workout type or use a default
        int resId = getImageResourceForWorkout(userWorkout);
        Glide.with(holder.itemView.getContext())
                .load(resId)
                .into(holder.binding.pic);

        // Show number of exercises
        int exerciseCount = userWorkout.getItems() != null ? userWorkout.getItems().size() : 0;
        holder.binding.excerciseTxt.setText(exerciseCount + " Exercise");
        
        // Show source and creation info
        String source = userWorkout.getSource() != null ? userWorkout.getSource() : "custom";
        holder.binding.durationTxt.setText(source + " • " + formatDate(userWorkout.getCreatedAt()));

        // Set click listener on the root view to start workout
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, WorkoutActivity.class);
                intent.putExtra("userWorkoutId", userWorkout.getId());
                intent.putExtra("fromMyWorkoutFragment", true); // Flag để biết từ MyWorkoutFragment
                System.out.println("UserWorkoutAdapter: Starting WorkoutActivity with ID: " + userWorkout.getId() + " from MyWorkoutFragment");
                context.startActivity(intent);
            } else {
                System.out.println("UserWorkoutAdapter: Context is null, cannot start activity");
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
     * Format timestamp to readable date
     */
    private String formatDate(long timestamp) {
        if (timestamp == 0) return "Unknown";
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp * 1000)); // Convert from seconds to milliseconds
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderWorktoutBinding binding;

        public Viewholder(ViewholderWorktoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

