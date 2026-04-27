package com.arena.app.models;

import java.util.ArrayList;
import java.util.List;

public class TopicMastery {
    private String id;
    private String name;
    private int completedCount;
    private int totalCount;
    private String nextProblem;

    public TopicMastery() {}

    public TopicMastery(String id, String name, int completedCount,
                        int totalCount, String nextProblem) {
        this.id = id;
        this.name = name;
        this.completedCount = completedCount;
        this.totalCount = totalCount;
        this.nextProblem = nextProblem;
    }

    public static List<TopicMastery> getMockTopics() {
        List<TopicMastery> list = new ArrayList<>();
        list.add(new TopicMastery("t1", "Arrays &\nHashing", 6, 10, "Valid Sudoku"));
        list.add(new TopicMastery("t2", "Two Pointers", 4, 8, "3Sum"));
        list.add(new TopicMastery("t3", "Sliding Window", 3, 7, "Min Window Substring"));
        list.add(new TopicMastery("t4", "Stack", 5, 9, "Largest Rectangle"));
        return list;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public String getNextProblem() { return nextProblem; }
    public void setNextProblem(String nextProblem) { this.nextProblem = nextProblem; }

    public String getProgressText() {
        return completedCount + "/" + totalCount;
    }

    public int getProgressPercent() {
        return totalCount > 0 ? (completedCount * 100 / totalCount) : 0;
    }
}
