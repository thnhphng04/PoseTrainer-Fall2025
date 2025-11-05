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
import fpt.fall2025.posetrainer.databinding.FragmentGenderSelectionBinding;

public class GenderSelectionFragment extends Fragment {

    private FragmentGenderSelectionBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialCardView selectedCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGenderSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.cardMale.setOnClickListener(v -> selectGender("male", binding.cardMale));
        binding.cardFemale.setOnClickListener(v -> selectGender("female", binding.cardFemale));
    }

    private void selectGender(String gender, MaterialCardView card) {
        // Deselect previous
        if (selectedCard != null) {
            selectedCard.setStrokeColor(getResources().getColor(R.color.darkBlue, null));
            selectedCard.setStrokeWidth(2);
        }

        // Select new
        selectedCard = card;
        selectedCard.setStrokeColor(getResources().getColor(android.R.color.holo_blue_light, null));
        selectedCard.setStrokeWidth(4);

        viewModel.setGender(gender);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}