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
import fpt.fall2025.posetrainer.databinding.FragmentBodyTargetSelectionBinding;

public class BodyTargetSelectionFragment extends Fragment {

    private FragmentBodyTargetSelectionBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialCardView selectedCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBodyTargetSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.cardTarget1.setOnClickListener(v -> selectTargetBody("very_lean", binding.cardTarget1));
        binding.cardTarget2.setOnClickListener(v -> selectTargetBody("lean", binding.cardTarget2));
        binding.cardTarget3.setOnClickListener(v -> selectTargetBody("normal", binding.cardTarget3));
        binding.cardTarget4.setOnClickListener(v -> selectTargetBody("overweight", binding.cardTarget4));
        binding.cardTarget5.setOnClickListener(v -> selectTargetBody("obese", binding.cardTarget5));
    }

    private void selectTargetBody(String targetBodyType, MaterialCardView card) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setStrokeColor(getResources().getColor(R.color.darkBlue, null));
            selectedCard.setStrokeWidth(2);
        }

        // Select new
        selectedCard = card;
        selectedCard.setStrokeColor(getResources().getColor(android.R.color.holo_blue_light, null));
        selectedCard.setStrokeWidth(4);

        // Save to ViewModel
        viewModel.setTargetBodyType(targetBodyType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}