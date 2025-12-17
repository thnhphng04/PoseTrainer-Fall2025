package fpt.fall2025.posetrainer.UI.adapter.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fpt.fall2025.posetrainer.Domain.Achievement;
import fpt.fall2025.posetrainer.Manager.AchievementManager;
import fpt.fall2025.posetrainer.R;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {
    private List<String> badgeKeys;
    private Achievement userAchievement;
    private AchievementManager achievementManager;

    public AchievementAdapter() {
        this.badgeKeys = new ArrayList<>();
        this.achievementManager = AchievementManager.getInstance();
    }

    public void setAchievements(Achievement achievement) {
        this.userAchievement = achievement;
        
        // Get all available badge keys from AchievementManager
        badgeKeys.clear();
        
        // Streak achievements
        badgeKeys.add("streak_3");
        badgeKeys.add("streak_7");
        badgeKeys.add("streak_14");
        badgeKeys.add("streak_30");
        badgeKeys.add("streak_60");
        badgeKeys.add("streak_100");
        
        // Workout count achievements
        badgeKeys.add("workout_1");
        badgeKeys.add("workout_10");
        badgeKeys.add("workout_30");
        badgeKeys.add("workout_50");
        badgeKeys.add("workout_100");
        badgeKeys.add("workout_200");
        badgeKeys.add("workout_500");
        
        // Duration achievements
        badgeKeys.add("duration_1h");
        badgeKeys.add("duration_10h");
        badgeKeys.add("duration_50h");
        badgeKeys.add("duration_100h");
        
        // Calories achievements
        badgeKeys.add("calories_1000");
        badgeKeys.add("calories_5000");
        badgeKeys.add("calories_10000");
        badgeKeys.add("calories_50000");
        
        // Weekly achievements
        badgeKeys.add("week_1");
        badgeKeys.add("week_4");
        badgeKeys.add("week_12");
        
        // Special achievements
        badgeKeys.add("early_bird");
        badgeKeys.add("night_owl");
        badgeKeys.add("weekend_warrior");
        badgeKeys.add("perfectionist");
        badgeKeys.add("marathon");
        
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        String badgeKey = badgeKeys.get(position);
        AchievementManager.AchievementInfo info = achievementManager.getAchievementInfo(badgeKey);
        
        if (info != null) {
            holder.tvAchievementName.setText(info.name);
            holder.tvAchievementDescription.setText(info.description);
            
            // Show emoji or icon
            if (info.drawableResId != 0) {
                holder.ivAchievementIcon.setImageResource(info.drawableResId);
                holder.ivAchievementIcon.setVisibility(View.VISIBLE);
                holder.tvAchievementEmoji.setVisibility(View.GONE);
            } else if (info.emoji != null && !info.emoji.isEmpty()) {
                holder.tvAchievementEmoji.setText(info.emoji);
                holder.tvAchievementEmoji.setVisibility(View.VISIBLE);
                holder.ivAchievementIcon.setVisibility(View.GONE);
            }
        }
        
        // Check if unlocked
        boolean isUnlocked = userAchievement != null && userAchievement.isBadgeUnlocked(badgeKey);
        
        if (isUnlocked) {
            holder.tvUnlockedBadge.setVisibility(View.VISIBLE);
            holder.ivLocked.setVisibility(View.GONE);
            // Make unlocked items more visible
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.tvUnlockedBadge.setVisibility(View.GONE);
            holder.ivLocked.setVisibility(View.VISIBLE);
            // Make locked items less visible
            holder.itemView.setAlpha(0.6f);
        }
    }

    @Override
    public int getItemCount() {
        return badgeKeys.size();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        TextView tvAchievementEmoji;
        ImageView ivAchievementIcon;
        TextView tvAchievementName;
        TextView tvAchievementDescription;
        ImageView ivLocked;
        TextView tvUnlockedBadge;

        AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAchievementEmoji = itemView.findViewById(R.id.tv_achievement_emoji);
            ivAchievementIcon = itemView.findViewById(R.id.iv_achievement_icon);
            tvAchievementName = itemView.findViewById(R.id.tv_achievement_name);
            tvAchievementDescription = itemView.findViewById(R.id.tv_achievement_description);
            ivLocked = itemView.findViewById(R.id.iv_locked);
            tvUnlockedBadge = itemView.findViewById(R.id.tv_unlocked_badge);
        }
    }
}

