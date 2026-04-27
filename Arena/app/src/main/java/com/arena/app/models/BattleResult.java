package com.arena.app.models;

public class BattleResult {
    private boolean won;
    private String time;
    private int accuracy;
    private int xpGained;
    private int coins;

    public BattleResult() {}

    public BattleResult(boolean won, String time, int accuracy, int xpGained, int coins) {
        this.won = won;
        this.time = time;
        this.accuracy = accuracy;
        this.xpGained = xpGained;
        this.coins = coins;
    }

    public static BattleResult getMockVictory() {
        return new BattleResult(true, "12:45", 98, 250, 50);
    }

    public static BattleResult getMockDefeat() {
        return new BattleResult(false, "15:30", 72, 50, 10);
    }

    // Getters and Setters
    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getAccuracy() { return accuracy; }
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }

    public int getXpGained() { return xpGained; }
    public void setXpGained(int xpGained) { this.xpGained = xpGained; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public String getFormattedAccuracy() { return accuracy + "%"; }
    public String getFormattedXp() { return "+" + xpGained; }
    public String getFormattedCoins() { return "+" + coins; }
}
