package com.arena.app.models;

import java.util.List;

public class LeaderboardResponse {
    private List<LeaderboardEntry> leaderboard;
    private String weekStart;
    private int totalUsers;

    public List<LeaderboardEntry> getLeaderboard() { return leaderboard; }
    public String getWeekStart() { return weekStart; }
    public int getTotalUsers() { return totalUsers; }
}
