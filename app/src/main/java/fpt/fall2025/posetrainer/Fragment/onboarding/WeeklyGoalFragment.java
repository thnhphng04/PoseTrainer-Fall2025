package fpt.fall2025.posetrainer.Fragment.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentWeeklyGoalBinding;

public class WeeklyGoalFragment extends Fragment {

    private FragmentWeeklyGoalBinding binding;
    private OnboardingViewModel viewModel;
    private MaterialButton selectedButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWeeklyGoalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        // Set default selection
        selectDays(4, binding.btn4);

        setupClicks();
    }

    private void setupClicks() {
        binding.btn1.setOnClickListener(v -> selectDays(1, binding.btn1));
        binding.btn2.setOnClickListener(v -> selectDays(2, binding.btn2));
        binding.btn3.setOnClickListener(v -> selectDays(3, binding.btn3));
        binding.btn4.setOnClickListener(v -> selectDays(4, binding.btn4));
        binding.btn5.setOnClickListener(v -> selectDays(5, binding.btn5));
        binding.btn6.setOnClickListener(v -> selectDays(6, binding.btn6));
        binding.btn7.setOnClickListener(v -> selectDays(7, binding.btn7));
    }

    private void selectDays(int days, MaterialButton button) {
        if (selectedButton != null) {
            selectedButton.setStrokeWidth(1);
        }

        selectedButton = button;
        selectedButton.setStrokeWidth(4);

        viewModel.setWeeklyGoal(days);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}