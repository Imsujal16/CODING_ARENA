package com.arena.app.models;

import java.util.List;

public class Battle {
    private String id;
    private String opponentId;
    private String opponentName;
    private String opponentRank;
    private String difficulty;
    private List<String> topics;
    private int bet;
    private String status; // "searching", "matched", "in_progress", "completed"

    public Battle() {}

    public Battle(String id, String opponentId, String opponentName, String opponentRank,
                  String difficulty, List<String> topics, int bet, String status) {
        this.id = id;
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        this.opponentRank = opponentRank;
        this.difficulty = difficulty;
        this.topics = topics;
        this.bet = bet;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOpponentId() { return opponentId; }
    public void setOpponentId(String opponentId) { this.opponentId = opponentId; }

    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }

    public String getOpponentRank() { return opponentRank; }
    public void setOpponentRank(String opponentRank) { this.opponentRank = opponentRank; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }

    public int getBet() { return bet; }
    public void setBet(int bet) { this.bet = bet; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
