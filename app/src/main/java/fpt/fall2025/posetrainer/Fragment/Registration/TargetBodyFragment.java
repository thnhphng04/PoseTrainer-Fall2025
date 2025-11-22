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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import fpt.fall2025.posetrainer.R;

public class TargetBodyFragment extends Fragment {

    private MaterialCardView[] targetCards;
    private ImageView[] targetImages;
    private TextView[] targetTexts;
    private TextInputLayout tilTargetWeight;
    private TextInputEditText etTargetWeight;
    private String targetBodyType = null;
    private TargetBodyListener listener;

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

    public interface TargetBodyListener {
        void onTargetBodySelected(String bodyType);
        void onTargetWeightChanged(String weight);
        String getGender();
    }

    public void setListener(TargetBodyListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_target_body, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupTargetGrid();
        updateBodyImages();
        setupTextWatcher();
    }

    private void bindViews(View view) {
        targetCards = new MaterialCardView[]{
                view.findViewById(R.id.card_target_1),
                view.findViewById(R.id.card_target_2),
                view.findViewById(R.id.card_target_3),
                view.findViewById(R.id.card_target_4),
                view.findViewById(R.id.card_target_5)
        };
        targetImages = new ImageView[]{
                view.findViewById(R.id.ivTarget1),
                view.findViewById(R.id.ivTarget2),
                view.findViewById(R.id.ivTarget3),
                view.findViewById(R.id.ivTarget4),
                view.findViewById(R.id.ivTarget5)
        };
        targetTexts = new TextView[]{
                view.findViewById(R.id.tvTarget1),
                view.findViewById(R.id.tvTarget2),
                view.findViewById(R.id.tvTarget3),
                view.findViewById(R.id.tvTarget4),
                view.findViewById(R.id.tvTarget5)
        };

        tilTargetWeight = view.findViewById(R.id.til_target_weight);
        etTargetWeight = view.findViewById(R.id.et_target_weight);
    }

    private void setupTargetGrid() {
        if (targetCards == null) {
            return;
        }
        for (int i = 0; i < targetCards.length; i++) {
            if (targetCards[i] != null && i < bodyTypes.length) {
                targetCards[i].setTag(bodyTypes[i]);
                final int idx = i;
                targetCards[i].setOnClickListener(v -> selectTargetCard(idx));
            }
        }
    }

    public void updateBodyImages() {
        // Check if views are initialized
        if (targetImages == null || targetTexts == null || targetCards == null) {
            return;
        }
        
        String gender = listener != null ? listener.getGender() : "female";
        if (TextUtils.isEmpty(gender)) gender = "female";

        int[] drawables = "male".equalsIgnoreCase(gender) ? maleBodies : femaleBodies;

        for (int i = 0; i < 5; i++) {
            if (targetImages[i] != null) {
                targetImages[i].setImageResource(drawables[i]);
            }
            if (targetTexts[i] != null) {
                targetTexts[i].setText(bodyLabels[i]);
            }
            if (targetCards[i] != null) {
                setCardStroke(targetCards[i], false);
            }
        }
        targetBodyType = null;
    }

    private void selectTargetCard(int index) {
        if (targetCards == null || index < 0 || index >= targetCards.length) {
            return;
        }
        for (int i = 0; i < targetCards.length; i++) {
            if (targetCards[i] != null) {
                setCardStroke(targetCards[i], i == index);
            }
        }
        if (targetCards[index] != null) {
            targetBodyType = String.valueOf(targetCards[index].getTag());
        }

        if (listener != null) {
            listener.onTargetBodySelected(targetBodyType);
        }
    }

    private void setupTextWatcher() {
        if (etTargetWeight == null) {
            return;
        }
        etTargetWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && listener != null) {
                String weight = etTargetWeight.getText() != null ? etTargetWeight.getText().toString().trim() : "";
                listener.onTargetWeightChanged(weight);
            }
        });
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
        // Check if views are initialized
        if (etTargetWeight == null || tilTargetWeight == null) {
            return false;
        }
        
        clearErrors();

        if (targetBodyType == null) {
            return false;
        }

        String targetWeightStr = etTargetWeight.getText() != null ? etTargetWeight.getText().toString().trim() : "";
        if (TextUtils.isEmpty(targetWeightStr)) {
            tilTargetWeight.setError("Nhập cân nặng mục tiêu");
            return false;
        }

        try {
            int targetW = Integer.parseInt(targetWeightStr);
            if (targetW <= 0) {
                tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ");
                return false;
            }
        } catch (NumberFormatException e) {
            tilTargetWeight.setError("Cân nặng mục tiêu không hợp lệ");
            return false;
        }

        return true;
    }

    private void clearErrors() {
        if (tilTargetWeight != null) {
            tilTargetWeight.setError(null);
        }
    }

    public String getTargetBodyType() {
        return targetBodyType;
    }

    public String getTargetWeight() {
        if (etTargetWeight == null) {
            return "";
        }
        return etTargetWeight.getText() != null ? etTargetWeight.getText().toString().trim() : "";
    }
}


