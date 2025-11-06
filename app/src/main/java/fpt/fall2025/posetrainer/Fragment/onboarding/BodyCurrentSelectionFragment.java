
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
import fpt.fall2025.posetrainer.databinding.FragmentBodyCurrentSelectionBinding;

public class BodyCurrentSelectionFragment extends Fragment {

    private FragmentBodyCurrentSelectionBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialCardView selectedCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBodyCurrentSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.cardBody1.setOnClickListener(v -> selectBodyType("very_lean", binding.cardBody1));
        binding.cardBody2.setOnClickListener(v -> selectBodyType("lean", binding.cardBody2));
        binding.cardBody3.setOnClickListener(v -> selectBodyType("normal", binding.cardBody3));
        binding.cardBody4.setOnClickListener(v -> selectBodyType("overweight", binding.cardBody4));
        binding.cardBody5.setOnClickListener(v -> selectBodyType("obese", binding.cardBody5));
    }

    private void selectBodyType(String bodyType, MaterialCardView card) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setStrokeColor(getResources().getColor(R.color.darkBlue, null));
            selectedCard.setStrokeWidth(2);
        }

        // Select new
        selectedCard = card;
        selectedCard.setStrokeColor(getResources().getColor(android.R.color.holo_blue_light, null));
        selectedCard.setStrokeWidth(4);

        // Save to ViewModel - use bodyPart field
        viewModel.setBodyPart(bodyType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}