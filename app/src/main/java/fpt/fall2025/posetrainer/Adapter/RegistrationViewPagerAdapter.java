package fpt.fall2025.posetrainer.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.HashMap;
import java.util.Map;

import fpt.fall2025.posetrainer.Fragment.Registration.BasicInfoFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.CurrentBodyFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.ExperienceFragment;
import fpt.fall2025.posetrainer.Fragment.Registration.TargetBodyFragment;

public class RegistrationViewPagerAdapter extends FragmentStateAdapter {

    private Map<Integer, Fragment> fragmentMap = new HashMap<>();

    public RegistrationViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new BasicInfoFragment();
                break;
            case 1:
                fragment = new ExperienceFragment();
                break;
            case 2:
                fragment = new CurrentBodyFragment();
                break;
            case 3:
                fragment = new TargetBodyFragment();
                break;
            default:
                fragment = new BasicInfoFragment();
        }
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    public Fragment getFragment(int position) {
        return fragmentMap.get(position);
    }
}

