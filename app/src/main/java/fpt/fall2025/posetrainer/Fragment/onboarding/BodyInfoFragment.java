package fpt.fall2025.posetrainer.Fragment.onboarding;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Calendar;

import fpt.fall2025.posetrainer.ViewModel.OnboardingViewModel;
import fpt.fall2025.posetrainer.databinding.FragmentBodyInfoBinding;

public class BodyInfoFragment extends Fragment {

    private FragmentBodyInfoBinding binding;
    private OnboardingViewModel viewModel;

    private final String[] expLevels = {"Mới bắt đầu", "Trung bình", "Nâng cao"};
    private final String[] expValues = {"beginner", "intermediate", "advanced"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBodyInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupDropdown();
        setupDatePicker();
        setupListeners();
    }

    private void setupDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                expLevels
        );
        binding.ddExperience.setAdapter(adapter);
        binding.ddExperience.setText(expLevels[0], false);
        viewModel.setExperienceLevel(expValues[0]);

        binding.ddExperience.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.setExperienceLevel(expValues[position]);
        });
    }

    private void setupDatePicker() {
        binding.etBirthday.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR) - 20;
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                String date = String.format("%04d-%02d-%02d", y, m + 1, d);
                binding.etBirthday.setText(date);
                viewModel.setBirthday(date);
            }, year, month, day).show();
        });
    }

    private void setupListeners() {
        // Weight
        binding.etWeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float weight = Float.parseFloat(s.toString());
                    viewModel.setWeight(weight);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Height
        binding.etHeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float height = Float.parseFloat(s.toString());
                    viewModel.setHeight(height);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Daily minutes
        binding.etDailyMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int minutes = Integer.parseInt(s.toString());
                    viewModel.setDailyMinutes(minutes);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Target weight
        binding.etTargetWeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float targetWeight = Float.parseFloat(s.toString());
                    viewModel.setTargetWeight(targetWeight);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}