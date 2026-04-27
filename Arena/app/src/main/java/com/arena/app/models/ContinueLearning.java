package com.arena.app.models;

import java.util.ArrayList;
import java.util.List;

public class ContinueLearning {
    private String id;
    private String title;
    private String nextProblem;
    private String topicTag;
    private int progressPercent;

    public ContinueLearning() {}

    public ContinueLearning(String id, String title, String nextProblem,
                            String topicTag, int progressPercent) {
        this.id = id;
        this.title = title;
        this.nextProblem = nextProblem;
        this.topicTag = topicTag;
        this.progressPercent = progressPercent;
    }

    public static List<ContinueLearning> getMockData() {
        List<ContinueLearning> list = new ArrayList<>();
        list.add(new ContinueLearning("cl1", "Data Structures 101",
                "Dynamic Arrays Implementation", "Arrays", 40));
        list.add(new ContinueLearning("cl2", "Advanced Algorithms",
                "Breadth-First Search", "Graphs", 65));
        list.add(new ContinueLearning("cl3", "Dynamic Programming",
                "Longest Common Subsequence", "DP", 25));
        return list;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNextProblem() { return nextProblem; }
    public void setNextProblem(String nextProblem) { this.nextProblem = nextProblem; }

    public String getTopicTag() { return topicTag; }
    public void setTopicTag(String topicTag) { this.topicTag = topicTag; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
}
