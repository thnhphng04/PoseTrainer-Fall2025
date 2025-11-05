package fpt.fall2025.posetrainer.Fragment.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentGoalSelectionBinding;

public class GoalSelectionFragment extends Fragment {

    private FragmentGoalSelectionBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialCardView selectedCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGoalSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.cardLoseWeight.setOnClickListener(v -> selectGoal("lose_weight", binding.cardLoseWeight));
        binding.cardBuildMuscle.setOnClickListener(v -> selectGoal("build_muscle", binding.cardBuildMuscle));
        binding.cardKeepFit.setOnClickListener(v -> selectGoal("keep_fit", binding.cardKeepFit));
    }

    private void selectGoal(String goal, MaterialCardView card) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setStrokeColor(getResources().getColor(fpt.fall2025.posetrainer.R.color.darkBlue, null));
            selectedCard.setStrokeWidth(2);
        }

        // Select new
        selectedCard = card;
        selectedCard.setStrokeColor(getResources().getColor(android.R.color.holo_blue_light, null));
        selectedCard.setStrokeWidth(4);

        viewModel.setGoal(goal);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}