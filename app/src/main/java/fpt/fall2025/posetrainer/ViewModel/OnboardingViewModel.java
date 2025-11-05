package fpt.fall2025.posetrainer.ViewModel;

import androidx.lifecycle.ViewModel;
import fpt.fall2025.posetrainer.Domain.OnboardingData;

public class OnboardingViewModel extends ViewModel {
    private final OnboardingData data = new OnboardingData();

    public OnboardingData getData() {
        return data;
    }

    public void setGender(String gender) {
        data.setGender(gender);
    }

    public void setBodyPart(String bodyPart) {
        data.setBodyPart(bodyPart);
    }

    public void setGoal(String goal) {
        data.setGoal(goal);
    }

    public void setActivityLevel(String activityLevel) {
        data.setActivityLevel(activityLevel);
    }

    public void setWeeklyGoal(int weeklyGoal) {
        data.setWeeklyGoal(weeklyGoal);
    }

    public void setWeight(float weight) {
        data.setWeight(weight);
    }

    public void setHeight(float height) {
        data.setHeight(height);
    }
}