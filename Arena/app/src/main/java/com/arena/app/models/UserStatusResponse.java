package com.arena.app.models;

public class UserStatusResponse {
    private boolean hasSolved;
    private String timestamp;
    private int problemsToday;
    private int currentStreak;

    public boolean isHasSolved() { return hasSolved; }
    public String getTimestamp() { return timestamp; }
    public int getProblemsToday() { return problemsToday; }
    public int getCurrentStreak() { return currentStreak; }
}
