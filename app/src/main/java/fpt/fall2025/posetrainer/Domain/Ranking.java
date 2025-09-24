package fpt.fall2025.posetrainer.Domain;

import java.io.Serializable;
import java.util.List;

public class Ranking implements Serializable {
    private String scope; // "daily", "weekly", "monthly"
    private String dateKey; // "YYYY-MM-DD" format
    private String metric; // "reps", "duration", "kcal", etc.
    private List<RankingEntry> entries;
    private long generatedAt;

    public Ranking() {}

    public Ranking(String scope, String dateKey, String metric, 
                  List<RankingEntry> entries, long generatedAt) {
        this.scope = scope;
        this.dateKey = dateKey;
        this.metric = metric;
        this.entries = entries;
        this.generatedAt = generatedAt;
    }

    // Getters and Setters
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public List<RankingEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<RankingEntry> entries) {
        this.entries = entries;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    // Inner class for RankingEntry
    public static class RankingEntry implements Serializable {
        private int rank;
        private String uid;
        private int value;
        private String displayName;
        private String photoURL;

        public RankingEntry() {}

        public RankingEntry(int rank, String uid, int value, String displayName, String photoURL) {
            this.rank = rank;
            this.uid = uid;
            this.value = value;
            this.displayName = displayName;
            this.photoURL = photoURL;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPhotoURL() {
            return photoURL;
        }

        public void setPhotoURL(String photoURL) {
            this.photoURL = photoURL;
        }
    }
}

