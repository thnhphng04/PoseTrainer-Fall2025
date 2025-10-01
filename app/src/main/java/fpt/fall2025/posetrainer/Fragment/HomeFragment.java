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
import fpt.fall2025.posetrainer.Domain.User;
import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

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
        
        // Load current user info
        loadCurrentUserInfo();
        
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

    /**
     * Load current user information from Firestore
     */
    private void loadCurrentUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user found");
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            updateUserUI(user);
                        }
                    } else {
                        Log.w(TAG, "User document not found in Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user info", e);
                });
    }

    /**
     * Update UI with user information
     */
    private void updateUserUI(User user) {
        // Update display name
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            binding.textView5.setText(user.getDisplayName());
        } else {
            binding.textView5.setText("User");
        }

        // Update profile image
        if (user.getPhotoURL() != null && !user.getPhotoURL().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoURL())
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(binding.imageView2);
        } else {
            // Keep default profile image
            binding.imageView2.setImageResource(R.drawable.profile);
        }
    }
}
