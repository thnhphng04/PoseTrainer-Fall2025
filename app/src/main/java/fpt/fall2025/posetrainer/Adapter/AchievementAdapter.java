package fpt.fall2025.posetrainer.Adapter;

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
        badgeKeys.add("streak_3");
        badgeKeys.add("streak_7");
        badgeKeys.add("streak_14");
        badgeKeys.add("workout_1");
        badgeKeys.add("workout_10");
        badgeKeys.add("workout_30");
        
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

