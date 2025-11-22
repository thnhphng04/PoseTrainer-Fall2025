package fpt.fall2025.posetrainer.Fragment.Registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import fpt.fall2025.posetrainer.R;

public class CurrentBodyFragment extends Fragment {

    private MaterialCardView[] bodyCards;
    private ImageView[] bodyImages;
    private TextView[] bodyTexts;
    private String currentBodyType = null;
    private CurrentBodyListener listener;

    private final String[] bodyTypes = {"very_lean", "lean", "normal", "overweight", "obese"};
    private final String[] bodyLabels = {"Rất gầy", "Gầy", "Bình thường", "Thừa cân", "Béo phì"};

    private final int[] maleBodies = {
            R.drawable.male_body_very_lean,
            R.drawable.male_body_lean,
            R.drawable.male_body_normal,
            R.drawable.male_body_overweight,
            R.drawable.male_body_obese
    };
    private final int[] femaleBodies = {
            R.drawable.female_body_very_lean,
            R.drawable.female_body_lean,
            R.drawable.female_body_normal,
            R.drawable.female_body_overweight,
            R.drawable.female_body_obese
    };

    public interface CurrentBodyListener {
        void onCurrentBodySelected(String bodyType);
        String getGender();
    }

    public void setListener(CurrentBodyListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_body, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupCurrentGrid();
        updateBodyImages();
    }

    private void bindViews(View view) {
        bodyCards = new MaterialCardView[]{
                view.findViewById(R.id.card_body_1),
                view.findViewById(R.id.card_body_2),
                view.findViewById(R.id.card_body_3),
                view.findViewById(R.id.card_body_4),
                view.findViewById(R.id.card_body_5)
        };
        bodyImages = new ImageView[]{
                view.findViewById(R.id.ivBody1),
                view.findViewById(R.id.ivBody2),
                view.findViewById(R.id.ivBody3),
                view.findViewById(R.id.ivBody4),
                view.findViewById(R.id.ivBody5)
        };
        bodyTexts = new TextView[]{
                view.findViewById(R.id.tvBody1),
                view.findViewById(R.id.tvBody2),
                view.findViewById(R.id.tvBody3),
                view.findViewById(R.id.tvBody4),
                view.findViewById(R.id.tvBody5)
        };
    }

    private void setupCurrentGrid() {
        if (bodyCards == null) {
            return;
        }
        for (int i = 0; i < bodyCards.length; i++) {
            if (bodyCards[i] != null && i < bodyTypes.length) {
                bodyCards[i].setTag(bodyTypes[i]);
                final int idx = i;
                bodyCards[i].setOnClickListener(v -> selectCurrentCard(idx));
            }
        }
    }

    public void updateBodyImages() {
        // Check if views are initialized
        if (bodyImages == null || bodyTexts == null || bodyCards == null) {
            return;
        }
        
        String gender = listener != null ? listener.getGender() : "female";
        if (TextUtils.isEmpty(gender)) gender = "female";

        int[] drawables = "male".equalsIgnoreCase(gender) ? maleBodies : femaleBodies;

        for (int i = 0; i < 5; i++) {
            if (bodyImages[i] != null) {
                bodyImages[i].setImageResource(drawables[i]);
            }
            if (bodyTexts[i] != null) {
                bodyTexts[i].setText(bodyLabels[i]);
            }
            if (bodyCards[i] != null) {
                setCardStroke(bodyCards[i], false);
            }
        }
        currentBodyType = null;
    }

    private void selectCurrentCard(int index) {
        if (bodyCards == null || index < 0 || index >= bodyCards.length) {
            return;
        }
        for (int i = 0; i < bodyCards.length; i++) {
            if (bodyCards[i] != null) {
                setCardStroke(bodyCards[i], i == index);
            }
        }
        if (bodyCards[index] != null) {
            currentBodyType = String.valueOf(bodyCards[index].getTag());
        }

        if (listener != null) {
            listener.onCurrentBodySelected(currentBodyType);
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
        return currentBodyType != null;
    }

    public String getCurrentBodyType() {
        return currentBodyType;
    }
}


