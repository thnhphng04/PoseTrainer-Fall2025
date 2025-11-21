package fpt.fall2025.posetrainer.Fragment.Registration;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import fpt.fall2025.posetrainer.R;

public class ExperienceFragment extends Fragment {

    private MaterialCardView[] experienceCards;
    private String selectedExperienceLevel = null;
    private ExperienceListener listener;

    private final String[] expLevels = {"beginner", "intermediate", "advanced"};

    public interface ExperienceListener {
        void onExperienceSelected(String experienceLevel);
    }

    public void setListener(ExperienceListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_experience, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupExperienceGrid();
    }

    private void bindViews(View view) {
        experienceCards = new MaterialCardView[]{
                view.findViewById(R.id.card_experience_1),
                view.findViewById(R.id.card_experience_2),
                view.findViewById(R.id.card_experience_3)
        };
    }

    private void setupExperienceGrid() {
        for (int i = 0; i < experienceCards.length; i++) {
            experienceCards[i].setTag(expLevels[i]);
            final int idx = i;
            experienceCards[i].setOnClickListener(v -> selectExperienceCard(idx));
        }
    }

    private void selectExperienceCard(int index) {
        for (int i = 0; i < experienceCards.length; i++) {
            setCardStroke(experienceCards[i], i == index);
        }
        selectedExperienceLevel = String.valueOf(experienceCards[index].getTag());

        if (listener != null) {
            listener.onExperienceSelected(selectedExperienceLevel);
        }
    }

    private void setCardStroke(MaterialCardView card, boolean selected) {
        int strokeWidth = dp(selected ? 2 : 1);
        card.setStrokeWidth(strokeWidth);
        card.setStrokeColor(resolveAttr(selected
                ? com.google.android.material.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutline));
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private int resolveAttr(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    public boolean validate() {
        if (selectedExperienceLevel == null) {
            return false;
        }
        return true;
    }

    public String getSelectedExperienceLevel() {
        return selectedExperienceLevel;
    }
}


