package fpt.fall2025.posetrainer.Dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import fpt.fall2025.posetrainer.Manager.AchievementManager;
import fpt.fall2025.posetrainer.R;

public class AchievementUnlockedDialog extends DialogFragment {
    private static final String TAG = "AchievementUnlockedDialog";
    private static final String ARG_BADGE_KEY = "badge_key";
    
    private TextView tvAchievementEmoji;
    private ImageView ivAchievementIcon;
    private TextView tvAchievementName;
    private TextView tvAchievementDescription;
    private Button btnClose;
    
    private String badgeKey;
    private Handler autoDismissHandler;
    private Runnable autoDismissRunnable;

    public static AchievementUnlockedDialog newInstance(String badgeKey) {
        AchievementUnlockedDialog dialog = new AchievementUnlockedDialog();
        Bundle args = new Bundle();
        args.putString(ARG_BADGE_KEY, badgeKey);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.FullScreenDialogStyle);
        
        if (getArguments() != null) {
            badgeKey = getArguments().getString(ARG_BADGE_KEY);
        }
        
        autoDismissHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_achievement_unlocked, container, false);
        
        tvAchievementEmoji = view.findViewById(R.id.tv_achievement_emoji);
        ivAchievementIcon = view.findViewById(R.id.iv_achievement_icon);
        tvAchievementName = view.findViewById(R.id.tv_achievement_name);
        tvAchievementDescription = view.findViewById(R.id.tv_achievement_description);
        btnClose = view.findViewById(R.id.btn_close);
        
        // Get achievement info
        AchievementManager.AchievementInfo info = AchievementManager.getInstance().getAchievementInfo(badgeKey);
        if (info != null) {
            if (info.drawableResId != 0) {
                ivAchievementIcon.setImageResource(info.drawableResId);
                ivAchievementIcon.setVisibility(View.VISIBLE);
                tvAchievementEmoji.setVisibility(View.GONE);
            } else if (info.emoji != null && !info.emoji.isEmpty()) {
                tvAchievementEmoji.setText(info.emoji);
                tvAchievementEmoji.setVisibility(View.VISIBLE);
                ivAchievementIcon.setVisibility(View.GONE);
            }
            
            tvAchievementName.setText(info.name);
            tvAchievementDescription.setText(info.description);
        } else {
            // Fallback
            tvAchievementEmoji.setText("🏆");
            tvAchievementName.setText("Thành tích mới!");
            tvAchievementDescription.setText("Bạn đã mở khóa một thành tích mới!");
        }
        
        btnClose.setOnClickListener(v -> dismiss());
        
        // Auto dismiss after 3 seconds
        autoDismissRunnable = () -> {
            if (isAdded() && !isRemoving()) {
                dismiss();
            }
        };
        autoDismissHandler.postDelayed(autoDismissRunnable, 3000);
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Entrance animation
        View view = getView();
        if (view != null) {
            view.setAlpha(0f);
            view.setScaleX(0.5f);
            view.setScaleY(0.5f);
            
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f);
            
            fadeIn.setDuration(300);
            scaleX.setDuration(300);
            scaleY.setDuration(300);
            
            scaleX.setInterpolator(new DecelerateInterpolator());
            scaleY.setInterpolator(new DecelerateInterpolator());
            
            fadeIn.start();
            scaleX.start();
            scaleY.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autoDismissHandler != null && autoDismissRunnable != null) {
            autoDismissHandler.removeCallbacks(autoDismissRunnable);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }
}

