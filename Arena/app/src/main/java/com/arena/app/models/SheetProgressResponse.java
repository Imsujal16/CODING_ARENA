package com.arena.app.models;

import java.util.List;

public class SheetProgressResponse {
    private String sheetId;
    private List<SheetUserProgress> users;

    public String getSheetId() { return sheetId; }
    public List<SheetUserProgress> getUsers() { return users; }

    public static class SheetUserProgress {
        private String userId;
        private String username;
        private String avatarUrl;
        private String clerkId;
        private List<String> completed;
        private List<String> inProgress;

        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getClerkId() { return clerkId; }
        public List<String> getCompleted() { return completed; }
        public List<String> getInProgress() { return inProgress; }
    }
}
