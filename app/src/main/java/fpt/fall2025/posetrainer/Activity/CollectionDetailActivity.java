package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fpt.fall2025.posetrainer.Adapter.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.Domain.Collection;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityCollectionDetailBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CollectionDetailActivity extends AppCompatActivity {
    private static final String TAG = "CollectionDetailActivity";
    private ActivityCollectionDetailBinding binding;
    private String collectionId;
    private String collectionTitle;
    private Collection collection;
    private ArrayList<WorkoutTemplate> workouts;
    private WorkoutTemplateAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollectionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        collectionId = getIntent().getStringExtra("collectionId");
        collectionTitle = getIntent().getStringExtra("collectionTitle");

        if (collectionId == null) {
            Log.e(TAG, "Collection ID is null");
            finish();
            return;
        }

        workouts = new ArrayList<>();

        setupRecyclerView();
        setupBackButton();
        loadCollection();
    }

    private void setupRecyclerView() {
        binding.rvWorkouts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new WorkoutTemplateAdapter(workouts);
        binding.rvWorkouts.setAdapter(adapter);
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void loadCollection() {
        Log.d(TAG, "Loading collection: " + collectionId);
        
        FirebaseFirestore.getInstance()
                .collection("collections")
                .document(collectionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        collection = documentSnapshot.toObject(Collection.class);
                        if (collection != null) {
                            collection.setId(documentSnapshot.getId());
                            updateUI();
                            loadWorkouts();
                        } else {
                            Log.e(TAG, "Failed to parse collection");
                            showError();
                        }
                    } else {
                        Log.e(TAG, "Collection not found");
                        showError();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading collection: " + e.getMessage());
                    showError();
                });
    }

    private void updateUI() {
        if (collection == null) return;

        String title = collection.getTitle() != null ? collection.getTitle() : collectionTitle;
        binding.tvTitle.setText(title);

        if (collection.getDescription() != null && !collection.getDescription().isEmpty()) {
            binding.tvDescription.setText(collection.getDescription());
            binding.tvDescription.setVisibility(View.VISIBLE);
        } else {
            binding.tvDescription.setVisibility(View.GONE);
        }

        int workoutCount = collection.getWorkoutTemplateIds() != null ? collection.getWorkoutTemplateIds().size() : 0;
        binding.tvWorkoutCount.setText(workoutCount + " bài tập");
    }

    private void loadWorkouts() {
        if (collection == null || collection.getWorkoutTemplateIds() == null || collection.getWorkoutTemplateIds().isEmpty()) {
            Log.d(TAG, "No workout template IDs in collection");
            adapter.updateList(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Loading " + collection.getWorkoutTemplateIds().size() + " workouts for collection");

        ArrayList<WorkoutTemplate> loadedWorkouts = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalCount = collection.getWorkoutTemplateIds().size();

        for (String workoutTemplateId : collection.getWorkoutTemplateIds()) {
            FirebaseService.getInstance().loadWorkoutTemplateById(
                    workoutTemplateId,
                    this,
                    workoutTemplate -> {
                        if (workoutTemplate != null) {
                            loadedWorkouts.add(workoutTemplate);
                        }
                        loadedCount[0]++;
                        
                        if (loadedCount[0] == totalCount) {
                            // All workouts loaded
                            workouts = loadedWorkouts;
                            adapter.updateList(workouts);
                            Log.d(TAG, "Loaded " + workouts.size() + " workouts");
                            
                            if (workouts.isEmpty()) {
                                binding.llEmptyState.setVisibility(View.VISIBLE);
                                binding.rvWorkouts.setVisibility(View.GONE);
                            } else {
                                binding.llEmptyState.setVisibility(View.GONE);
                                binding.rvWorkouts.setVisibility(View.VISIBLE);
                            }
                        }
                    }
            );
        }
    }

    private void showError() {
        binding.llEmptyState.setVisibility(View.VISIBLE);
        binding.rvWorkouts.setVisibility(View.GONE);
    }
}

