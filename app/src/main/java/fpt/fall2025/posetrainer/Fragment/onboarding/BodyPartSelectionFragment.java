package fpt.fall2025.posetrainer.Fragment.onboarding;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import fpt.fall2025.posetrainer.R;
import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentBodyPartSelectionBinding;

public class BodyPartSelectionFragment extends Fragment {

    private FragmentBodyPartSelectionBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialButton selectedButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBodyPartSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupClicks();
    }

    private void setupClicks() {
        binding.btnFullbody.setOnClickListener(v -> selectBodyPart("fullbody", binding.btnFullbody));
        binding.btnArm.setOnClickListener(v -> selectBodyPart("arm", binding.btnArm));
        binding.btnChest.setOnClickListener(v -> selectBodyPart("chest", binding.btnChest));
        binding.btnAbs.setOnClickListener(v -> selectBodyPart("abs", binding.btnAbs));
        binding.btnLeg.setOnClickListener(v -> selectBodyPart("leg", binding.btnLeg));
    }

    private void selectBodyPart(String bodyPart, MaterialButton button) {
        // Deselect previous
        if (selectedButton != null) {
            selectedButton.setStrokeWidth(1);
            selectedButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(fpt.fall2025.posetrainer.R.color.darkBlue, null)));
        }

        // Select new
        selectedButton = button;
        selectedButton.setStrokeWidth(4);
        selectedButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_light, null)));

        viewModel.setBodyPart(bodyPart);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}