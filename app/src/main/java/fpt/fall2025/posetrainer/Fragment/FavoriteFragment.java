package fpt.fall2025.posetrainer.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.Adapter.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentFavoriteBinding;

import java.util.ArrayList;

/**
 * FavoriteFragment - Fragment hiển thị danh sách workout templates đã lưu của user
 */
public class FavoriteFragment extends Fragment {
    private static final String TAG = "FavoriteFragment";
    private FragmentFavoriteBinding binding;
    private ArrayList<WorkoutTemplate> userWorkoutTemplates;
    private String userId = "uid_1"; // TODO: Get from authenticated user

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize data
        userWorkoutTemplates = new ArrayList<>();

        // Setup RecyclerView
        binding.userWorkoutsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        // Load user workout templates
        loadUserWorkoutTemplates();
    }

    /**
     * Load user workout templates from Firebase
     */
    private void loadUserWorkoutTemplates() {
        Log.d(TAG, "Loading user workout templates for userId: " + userId);
        
        FirebaseService.getInstance().loadUserWorkoutTemplates(userId, (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnWorkoutTemplatesLoadedListener() {
            @Override
            public void onWorkoutTemplatesLoaded(ArrayList<WorkoutTemplate> templates) {
                userWorkoutTemplates = templates;
                
                if (userWorkoutTemplates == null || userWorkoutTemplates.isEmpty()) {
                    // Show empty state
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    binding.userWorkoutsRecyclerView.setVisibility(View.GONE);
                    binding.emptyStateText.setText("No saved workouts yet.\nEdit a workout to save it here!");
                } else {
                    // Show workouts
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.userWorkoutsRecyclerView.setVisibility(View.VISIBLE);
                    binding.userWorkoutsRecyclerView.setAdapter(new WorkoutTemplateAdapter(userWorkoutTemplates));
                }
                
                Log.d(TAG, "Loaded " + userWorkoutTemplates.size() + " user workout templates");
            }
        });
    }

    /**
     * Refresh the list when returning to this fragment
     */
    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        loadUserWorkoutTemplates();
    }
}
