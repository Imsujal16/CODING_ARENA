package com.arena.app.models;

import android.os.Bundle;

public class BattleResult {
    // Core result
    private boolean won;
    private String  time;
    private int     accuracy;
    private int     xpGained;
    private int     coins;
    // Real battle fields
    private String  opponentName;
    private String  problemTitle;
    private String  problemSlug;
    private String  reason;

    // ── Bundle keys ──────────────────────────────────────────────────────────
    private static final String KEY_WON           = "won";
    private static final String KEY_TIME          = "time";
    private static final String KEY_ACCURACY      = "accuracy";
    private static final String KEY_XP            = "xpGained";
    private static final String KEY_COINS         = "coins";
    private static final String KEY_OPPONENT_NAME = "opponentName";
    private static final String KEY_PROBLEM_TITLE = "problemTitle";
    private static final String KEY_PROBLEM_SLUG  = "problemSlug";
    private static final String KEY_REASON        = "reason";

    public BattleResult() {}

    public BattleResult(boolean won, String time, int accuracy, int xpGained, int coins) {
        this.won      = won;
        this.time     = time;
        this.accuracy = accuracy;
        this.xpGained = xpGained;
        this.coins    = coins;
    }

    // ── Factory: from Bundle (navigation args) ───────────────────────────────
    public static BattleResult fromBundle(Bundle args) {
        if (args == null) return getMockVictory();
        BattleResult r = new BattleResult();
        r.won          = args.getBoolean(KEY_WON,       false);
        r.time         = args.getString(KEY_TIME,       "00:00");
        r.accuracy     = args.getInt(KEY_ACCURACY,      0);
        r.xpGained     = args.getInt(KEY_XP,            25);
        r.coins        = args.getInt(KEY_COINS,         0);
        r.opponentName = args.getString(KEY_OPPONENT_NAME, "Opponent");
        r.problemTitle = args.getString(KEY_PROBLEM_TITLE, "");
        r.problemSlug  = args.getString(KEY_PROBLEM_SLUG,  "");
        r.reason       = args.getString(KEY_REASON,    "");
        return r;
    }

    // ── Convert to Bundle (for navigation) ───────────────────────────────────
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putBoolean(KEY_WON,           won);
        b.putString(KEY_TIME,           time  != null ? time  : "00:00");
        b.putInt(KEY_ACCURACY,          accuracy);
        b.putInt(KEY_XP,                xpGained);
        b.putInt(KEY_COINS,             coins);
        b.putString(KEY_OPPONENT_NAME,  opponentName  != null ? opponentName  : "Opponent");
        b.putString(KEY_PROBLEM_TITLE,  problemTitle  != null ? problemTitle  : "");
        b.putString(KEY_PROBLEM_SLUG,   problemSlug   != null ? problemSlug   : "");
        b.putString(KEY_REASON,         reason        != null ? reason        : "");
        return b;
    }

    // ── Legacy mocks (only used as a last fallback, never in real battle) ────
    public static BattleResult getMockVictory() {
        BattleResult r = new BattleResult(true, "12:45", 98, 250, 50);
        r.opponentName = "Opponent";
        r.problemTitle = "Daily Challenge";
        return r;
    }

    public static BattleResult getMockDefeat() {
        BattleResult r = new BattleResult(false, "15:30", 72, 50, 10);
        r.opponentName = "Opponent";
        r.problemTitle = "Daily Challenge";
        return r;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public boolean isWon()          { return won; }
    public void setWon(boolean won) { this.won = won; }

    public String getTime()             { return time; }
    public void setTime(String time)    { this.time = time; }

    public int getAccuracy()                { return accuracy; }
    public void setAccuracy(int accuracy)   { this.accuracy = accuracy; }

    public int getXpGained()                { return xpGained; }
    public void setXpGained(int xpGained)   { this.xpGained = xpGained; }

    public int getCoins()               { return coins; }
    public void setCoins(int coins)     { this.coins = coins; }

    public String getOpponentName()                     { return opponentName; }
    public void setOpponentName(String opponentName)    { this.opponentName = opponentName; }

    public String getProblemTitle()                     { return problemTitle; }
    public void setProblemTitle(String problemTitle)    { this.problemTitle = problemTitle; }

    public String getProblemSlug()                      { return problemSlug; }
    public void setProblemSlug(String problemSlug)      { this.problemSlug = problemSlug; }

    public String getReason()               { return reason; }
    public void setReason(String reason)    { this.reason = reason; }

    // ── Formatted display ────────────────────────────────────────────────────
    public String getFormattedAccuracy() { return accuracy + "%"; }
    public String getFormattedXp()       { return (xpGained >= 0 ? "+" : "") + xpGained; }
    public String getFormattedCoins()    { return (coins >= 0 ? "+" : "") + coins; }
}
