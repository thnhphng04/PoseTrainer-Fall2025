package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class Schedule implements Serializable {
    private String id;
    private String uid;
    private String title;
    private String timezone;
    private List<ScheduleItem> scheduleItems;
    private NotificationSettings notification;

    public Schedule() {}

    public Schedule(String id, String uid, String title, String timezone, 
                   List<ScheduleItem> scheduleItems, NotificationSettings notification) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.timezone = timezone;
        this.scheduleItems = scheduleItems;
        this.notification = notification;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<ScheduleItem> getScheduleItems() {
        return scheduleItems;
    }

    public void setScheduleItems(List<ScheduleItem> scheduleItems) {
        this.scheduleItems = scheduleItems;
    }

    public NotificationSettings getNotification() {
        return notification;
    }

    public void setNotification(NotificationSettings notification) {
        this.notification = notification;
    }

    // Inner class for ScheduleItem
    public static class ScheduleItem implements Serializable {
        private List<Integer> dayOfWeek; // 1-7 (Monday-Sunday)
        private String timeLocal; // "HH:mm" format
        private String workoutId;

        public ScheduleItem() {}

        public ScheduleItem(List<Integer> dayOfWeek, String timeLocal, String workoutId) {
            this.dayOfWeek = dayOfWeek;
            this.timeLocal = timeLocal;
            this.workoutId = workoutId;
        }

        public List<Integer> getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(List<Integer> dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getTimeLocal() {
            return timeLocal;
        }

        public void setTimeLocal(String timeLocal) {
            this.timeLocal = timeLocal;
        }

        public String getWorkoutId() {
            return workoutId;
        }

        public void setWorkoutId(String workoutId) {
            this.workoutId = workoutId;
        }
    }

    // Inner class for NotificationSettings
    public static class NotificationSettings implements Serializable {
        private boolean enabled;
        private int remindBeforeMin;
        private String sound;

        public NotificationSettings() {}

        public NotificationSettings(boolean enabled, int remindBeforeMin, String sound) {
            this.enabled = enabled;
            this.remindBeforeMin = remindBeforeMin;
            this.sound = sound;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRemindBeforeMin() {
            return remindBeforeMin;
        }

        public void setRemindBeforeMin(int remindBeforeMin) {
            this.remindBeforeMin = remindBeforeMin;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(String sound) {
            this.sound = sound;
        }
    }
}

