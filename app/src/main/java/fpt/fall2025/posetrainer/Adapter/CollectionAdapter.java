package fpt.fall2025.posetrainer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fpt.fall2025.posetrainer.Activity.CollectionDetailActivity;
import fpt.fall2025.posetrainer.Domain.Collection;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.databinding.ItemCollectionBinding;

import java.util.ArrayList;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.Viewholder> {
    private ArrayList<Collection> list;
    private Context context;

    public CollectionAdapter(ArrayList<Collection> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    public void updateList(ArrayList<Collection> newList) {
        if (newList == null) {
            newList = new ArrayList<>();
        }
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ItemCollectionBinding binding = ItemCollectionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        Collection collection = list.get(position);
        
        holder.binding.tvCollectionTitle.setText(collection.getTitle());
        
        if (collection.getDescription() != null && !collection.getDescription().isEmpty()) {
            holder.binding.tvCollectionDescription.setText(collection.getDescription());
            holder.binding.tvCollectionDescription.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvCollectionDescription.setVisibility(android.view.View.GONE);
        }
        
        // Set workout count
        int workoutCount = collection.getWorkoutTemplateIds() != null ? collection.getWorkoutTemplateIds().size() : 0;
        holder.binding.tvWorkoutCount.setText(workoutCount + " bài tập");
        
        // Load thumbnail image
        if (collection.getThumbnailUrl() != null && !collection.getThumbnailUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(collection.getThumbnailUrl())
                    .placeholder(R.drawable.blue_bg)
                    .into(holder.binding.ivThumbnail);
        } else {
            // Default image based on category
            int resId = getImageResourceForCollection(collection);
            Glide.with(holder.itemView.getContext())
                    .load(resId)
                    .into(holder.binding.ivThumbnail);
        }
        
        // Set click listener
        holder.binding.getRoot().setOnClickListener(v -> {
            if (context != null) {
                Intent intent = new Intent(context, CollectionDetailActivity.class);
                intent.putExtra("collectionId", collection.getId());
                intent.putExtra("collectionTitle", collection.getTitle());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private int getImageResourceForCollection(Collection collection) {
        // Default image
        int defaultResId = context.getResources().getIdentifier("pic_1", "drawable", context.getPackageName());
        
        if (collection.getCategory() != null && !collection.getCategory().isEmpty()) {
            String category = collection.getCategory().toLowerCase();
            if (category.contains("cardio")) {
                return context.getResources().getIdentifier("pic_3", "drawable", context.getPackageName());
            } else if (category.contains("strength") || category.contains("power")) {
                return context.getResources().getIdentifier("pic_2", "drawable", context.getPackageName());
            }
        }
        
        return defaultResId;
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ItemCollectionBinding binding;

        public Viewholder(ItemCollectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

