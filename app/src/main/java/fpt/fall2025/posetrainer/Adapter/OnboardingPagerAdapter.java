package fpt.fall2025.posetrainer.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import fpt.fall2025.posetrainer.Fragment.onboarding.*;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new GenderSelectionFragment();
            case 1: return new BodyPartSelectionFragment();
            case 2: return new GoalSelectionFragment();
            case 3: return new ActivityLevelFragment();
            case 4: return new WeeklyGoalFragment();
            case 5: return new BodyInfoFragment();
            case 6: return new PlanReadyFragment();
            default: return new GenderSelectionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 7;
    }
}