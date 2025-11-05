package fpt.fall2025.posetrainer.Fragment.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentPlanReadyBinding;

public class PlanReadyFragment extends Fragment {

    private FragmentPlanReadyBinding binding;
    private OnboardingViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlanReadyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        // Display plan based on user's goal
        updatePlanDisplay();
    }

    private void updatePlanDisplay() {
        String goal = viewModel.getData().getGoal();
        String bodyPart = viewModel.getData().getBodyPart();

        // Customize plan title based on selections
        if (bodyPart != null && bodyPart.equals("fullbody")) {
            binding.tvPlanTitle.setText("TOÀN THÂN THỬ THÁCH");
        } else if (bodyPart != null) {
            binding.tvPlanTitle.setText(bodyPart.toUpperCase() + " THỬ THÁCH");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}