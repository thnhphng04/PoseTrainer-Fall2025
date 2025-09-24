package fpt.fall2025.posetrainer.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import fpt.fall2025.posetrainer.Adapter.WorkoutTemplateAdapter;
import fpt.fall2025.posetrainer.Domain.WorkoutTemplate;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ActivityMainBinding binding;
    private ArrayList<WorkoutTemplate> workoutTemplates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize data
        workoutTemplates = new ArrayList<>();

        // Setup RecyclerView
        binding.view1.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        
        // Load workout templates from Firestore
        loadWorkoutTemplates();
    }

    /**
     * Load workout templates from Firebase Firestore
     */
    private void loadWorkoutTemplates() {
        Log.d(TAG, "Loading workout templates from Firestore...");
        
        FirebaseService.getInstance().loadWorkoutTemplates(this, new FirebaseService.OnWorkoutTemplatesLoadedListener() {
            @Override
            public void onWorkoutTemplatesLoaded(ArrayList<WorkoutTemplate> templates) {
                workoutTemplates = templates;
                binding.view1.setAdapter(new WorkoutTemplateAdapter(workoutTemplates));
                Log.d(TAG, "Loaded " + workoutTemplates.size() + " workout templates");
            }
        });
    }
}