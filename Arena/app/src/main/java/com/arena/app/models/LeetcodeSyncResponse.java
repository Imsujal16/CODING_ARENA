package com.arena.app.models;

import java.util.List;

public class LeetcodeSyncResponse {
    private MatchedUser matchedUser;
    private List<RecentSubmission> recentSubmissionList;

    public MatchedUser getMatchedUser() { return matchedUser; }
    public List<RecentSubmission> getRecentSubmissionList() { return recentSubmissionList; }

    public static class MatchedUser {
        private String username;
        private Profile profile;
        private SubmitStatsGlobal submitStatsGlobal;
        private UserCalendar userCalendar;

        public String getUsername() { return username; }
        public Profile getProfile() { return profile; }
        public SubmitStatsGlobal getSubmitStatsGlobal() { return submitStatsGlobal; }
        public UserCalendar getUserCalendar() { return userCalendar; }
    }

    public static class Profile {
        private String realName;
        private String userAvatar;
        private int ranking;

        public String getRealName() { return realName; }
        public String getUserAvatar() { return userAvatar; }
        public int getRanking() { return ranking; }
    }

    public static class SubmitStatsGlobal {
        private List<SubmissionCount> acSubmissionNum;

        public List<SubmissionCount> getAcSubmissionNum() { return acSubmissionNum; }
    }

    public static class SubmissionCount {
        private String difficulty;
        private int count;

        public String getDifficulty() { return difficulty; }
        public int getCount() { return count; }
    }

    public static class UserCalendar {
        private int streak;
        private int totalActiveDays;
        private String submissionCalendar;

        public int getStreak() { return streak; }
        public int getTotalActiveDays() { return totalActiveDays; }
        public String getSubmissionCalendar() { return submissionCalendar; }
    }

    public static class RecentSubmission {
        private String title;
        private String titleSlug;
        private String timestamp;
        private String statusDisplay;
        private String lang;

        public String getTitle() { return title; }
        public String getTitleSlug() { return titleSlug; }
        public String getTimestamp() { return timestamp; }
        public String getStatusDisplay() { return statusDisplay; }
        public String getLang() { return lang; }
    }
}
