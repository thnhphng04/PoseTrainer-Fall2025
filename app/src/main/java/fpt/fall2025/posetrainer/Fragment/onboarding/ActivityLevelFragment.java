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

import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentActivityLevelBinding;

public class ActivityLevelFragment extends Fragment {

    private FragmentActivityLevelBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialCardView selectedCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityLevelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.cardSedentary.setOnClickListener(v -> selectLevel("sedentary", binding.cardSedentary));
        binding.cardLightlyActive.setOnClickListener(v -> selectLevel("lightly_active", binding.cardLightlyActive));
        binding.cardModeratelyActive.setOnClickListener(v -> selectLevel("moderately_active", binding.cardModeratelyActive));
        binding.cardVeryActive.setOnClickListener(v -> selectLevel("very_active", binding.cardVeryActive));
    }

    private void selectLevel(String level, MaterialCardView card) {
        if (selectedCard != null) {
            selectedCard.setStrokeColor(getResources().getColor(android.R.color.darker_gray, null));
            selectedCard.setStrokeWidth(2);
        }

        selectedCard = card;
        selectedCard.setStrokeColor(getResources().getColor(android.R.color.holo_blue_dark, null));
        selectedCard.setStrokeWidth(4);

        viewModel.setActivityLevel(level);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}