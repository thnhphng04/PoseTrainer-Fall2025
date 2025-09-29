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
import fpt.fall2025.posetrainer.databinding.FragmentHomeBinding;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ArrayList<WorkoutTemplate> workoutTemplates;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize data
        workoutTemplates = new ArrayList<>();

        // Setup RecyclerView
        binding.view1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        // Load workout templates from Firestore
        loadWorkoutTemplates();
    }

    /**
     * Load workout templates from Firebase Firestore
     */
    private void loadWorkoutTemplates() {
        Log.d(TAG, "Loading workout templates from Firestore...");
        
        FirebaseService.getInstance().loadWorkoutTemplates((androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnWorkoutTemplatesLoadedListener() {
            @Override
            public void onWorkoutTemplatesLoaded(ArrayList<WorkoutTemplate> templates) {
                workoutTemplates = templates;
                binding.view1.setAdapter(new WorkoutTemplateAdapter(workoutTemplates));
                Log.d(TAG, "Loaded " + workoutTemplates.size() + " workout templates");
            }
        });
    }
}
