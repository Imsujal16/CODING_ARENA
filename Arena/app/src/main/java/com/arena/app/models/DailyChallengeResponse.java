package com.arena.app.models;

public class DailyChallengeResponse {
    private String date;
    private String title;
    private String titleSlug;
    private String difficulty;
    private String questionId;

    public String getDate() { return date; }
    public String getTitle() { return title; }
    public String getTitleSlug() { return titleSlug; }
    public String getDifficulty() { return difficulty; }
    public String getQuestionId() { return questionId; }
}
