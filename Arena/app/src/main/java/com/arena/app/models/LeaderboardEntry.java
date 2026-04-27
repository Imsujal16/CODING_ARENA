package com.arena.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardEntry {
    private int rank;
    @SerializedName("id")
    private String userId;
    private String username;
    private String avatarUrl;
    private int xp;
    private int level;
    private String leagueTier;
    private int currentStreak;
    private int totalSolved;
    private int coins;
    private boolean isCurrentUser;

    public LeaderboardEntry() {}

    public LeaderboardEntry(int rank, String userId, String username, String avatarUrl,
                            int xp, boolean isCurrentUser) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.xp = xp;
        this.isCurrentUser = isCurrentUser;
    }

    public static List<LeaderboardEntry> getMockLeaderboard() {
        List<LeaderboardEntry> list = new ArrayList<>();
        list.add(new LeaderboardEntry(1, "u1", "Gautam", null, 15400, false));
        list.add(new LeaderboardEntry(2, "u2", "Sujal", null, 14200, false));
        list.add(new LeaderboardEntry(3, "u3", "Yash", null, 13800, false));
        list.add(new LeaderboardEntry(4, "u4", "Prisha", null, 12800, false));
        list.add(new LeaderboardEntry(5, "u5", "Arnav", null, 11540, false));
        list.add(new LeaderboardEntry(6, "u6", "Prayansh", null, 10900, false));
        return list;
    }

    public static LeaderboardEntry getCurrentUserEntry() {
        return new LeaderboardEntry(42, "user_001", "Hacker", null, 5400, true);
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getLeagueTier() { return leagueTier; }
    public void setLeagueTier(String leagueTier) { this.leagueTier = leagueTier; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getTotalSolved() { return totalSolved; }
    public void setTotalSolved(int totalSolved) { this.totalSolved = totalSolved; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isCurrentUser() { return isCurrentUser; }
    public void setCurrentUser(boolean currentUser) { isCurrentUser = currentUser; }

    public String getFormattedXp() {
        return String.format("%,d XP", xp);
    }
}
