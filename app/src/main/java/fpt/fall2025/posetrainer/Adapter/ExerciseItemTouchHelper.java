package fpt.fall2025.posetrainer.Adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemTouchHelper for handling drag and drop functionality in EditWorkoutAdapter
 */
public class ExerciseItemTouchHelper extends ItemTouchHelper.SimpleCallback {
    private EditWorkoutAdapter adapter;

    public ExerciseItemTouchHelper(EditWorkoutAdapter adapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, 
                         @NonNull RecyclerView.ViewHolder viewHolder, 
                         @NonNull RecyclerView.ViewHolder target) {
        
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        
        if (fromPosition == toPosition) {
            return false;
        }
        
        // Move the item in the adapter
        adapter.moveExercise(fromPosition, toPosition);
        
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // We don't handle swipe actions, only drag and drop
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }
    
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Add visual feedback when dragging starts
            if (viewHolder != null) {
                viewHolder.itemView.setAlpha(0.8f);
                viewHolder.itemView.setScaleX(1.05f);
                viewHolder.itemView.setScaleY(1.05f);
                // Add elevation for better visual separation
                viewHolder.itemView.setElevation(8f);
            }
        }
    }
    
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        
        // Reset visual feedback when dragging ends
        viewHolder.itemView.setAlpha(1.0f);
        viewHolder.itemView.setScaleX(1.0f);
        viewHolder.itemView.setScaleY(1.0f);
        viewHolder.itemView.setElevation(0f);
        
        // Update order numbers after drag is complete
        adapter.updateOrderNumbers();
    }
    
    @Override
    public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
        // Reduce threshold for more sensitive movement
        return 0.1f;
    }
    
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Enable smooth movement in both directions
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }
}
