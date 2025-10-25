package fpt.fall2025.posetrainer.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import fpt.fall2025.posetrainer.Adapter.SessionAdapter;
import fpt.fall2025.posetrainer.Domain.Session;
import fpt.fall2025.posetrainer.Service.FirebaseService;
import fpt.fall2025.posetrainer.databinding.FragmentDailyBinding; // ✅ Đổi sang binding đúng file

public class DailyFragment extends Fragment {
    private static final String TAG = "DailyFragment";
    private FragmentDailyBinding binding; // ✅ Đổi kiểu binding tương ứng
    private ArrayList<Session> sessions;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDailyBinding.inflate(inflater, container, false); // ✅ Inflate đúng layout fragment_daily.xml
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize data
        sessions = new ArrayList<>();

        // Setup RecyclerView
        binding.sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load sessions from Firestore
        loadSessions();
    }

    /**
     * Load sessions from Firebase Firestore
     */
    private void loadSessions() {
        Log.d(TAG, "=== LOADING SESSIONS ===");
        Log.d(TAG, "Loading sessions for uid_1 from Firestore...");

        FirebaseService.getInstance().loadUserSessions("uid_1", (androidx.appcompat.app.AppCompatActivity) getActivity(), new FirebaseService.OnSessionsLoadedListener() {
            @Override
            public void onSessionsLoaded(ArrayList<Session> loadedSessions) {
                Log.d(TAG, "=== SESSIONS LOADED ===");
                Log.d(TAG, "Received " + (loadedSessions != null ? loadedSessions.size() : "null") + " sessions");

                if (loadedSessions != null) {
                    for (int i = 0; i < loadedSessions.size(); i++) {
                        Session session = loadedSessions.get(i);
                        Log.d(TAG, "Session " + i + ": ID=" + session.getId() +
                                ", UID=" + session.getUid() +
                                ", StartedAt=" + session.getStartedAt());
                    }
                }

                sessions = loadedSessions != null ? loadedSessions : new ArrayList<>();
                binding.sessionsRecyclerView.setAdapter(new SessionAdapter(sessions));
                Log.d(TAG, "Adapter set with " + sessions.size() + " sessions");
                Log.d(TAG, "=== END LOADING SESSIONS ===");
            }
        });
    }
}
