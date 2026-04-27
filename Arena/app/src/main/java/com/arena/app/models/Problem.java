package com.arena.app.models;

import java.util.ArrayList;
import java.util.List;

public class Problem {
    private int id;
    private int number;
    private String title;
    private String slug;
    private String url;
    private String platform;
    private String step;
    private String description;
    private String difficulty; // "Easy", "Medium", "Hard"
    private String topic;
    private int xpReward;
    private boolean isLocked;
    private boolean isSolved;
    private String solvedTimeAgo;

    public Problem() {}

    public Problem(int id, int number, String title, String description, String difficulty,
                   String topic, int xpReward, boolean isLocked, boolean isSolved, String solvedTimeAgo) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.topic = topic;
        this.xpReward = xpReward;
        this.isLocked = isLocked;
        this.isSolved = isSolved;
        this.solvedTimeAgo = solvedTimeAgo;
    }

    // Mock data
    public static Problem getDailyChallenge() {
        return new Problem(1, 124, "Binary Tree Maximum Path Sum",
                "Find the maximum path sum in a binary tree where the path may start and end at any node.",
                "Medium", "Trees", 50, false, false, null);
    }

    public static List<Problem> getUpNextProblems() {
        List<Problem> list = new ArrayList<>();
        list.add(new Problem(1, 1, "Two Sum", null, "Easy", "Arrays", 10, false, false, null));
        list.add(new Problem(2, 242, "Valid Anagram", null, "Easy", "Strings", 10, false, false, null));
        list.add(new Problem(3, 49, "Group Anagrams", null, "Medium", "Hashing", 20, false, false, null));
        list.add(new Problem(4, 215, "Top K Elements", null, "Medium", "Heap", 25, false, false, null));
        return list;
    }

    public static List<Problem> getRecentActivity() {
        List<Problem> list = new ArrayList<>();
        list.add(new Problem(1, 1, "Two Sum", null, "Easy", "Arrays", 10, false, true, "2 hours ago"));
        list.add(new Problem(2, 146, "LRU Cache", null, "Medium", "Design", 20, false, true, "5 hours ago"));
        list.add(new Problem(3, 42, "Trapping Rain Water", null, "Hard", "Arrays", 30, false, true, "1 day ago"));
        list.add(new Problem(4, 3, "Longest Substring", null, "Medium", "Strings", 20, false, true, "2 days ago"));
        return list;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() {
        return difficulty == null || difficulty.trim().isEmpty() ? "Medium" : difficulty;
    }

    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getTopic() {
        return topic == null || topic.trim().isEmpty() ? "Striver Sheet" : topic;
    }

    public void setTopic(String topic) { this.topic = topic; }

    public int getXpReward() {
        if (xpReward > 0) {
            return xpReward;
        }

        switch (getDifficulty().toLowerCase()) {
            case "easy":
                return 10;
            case "hard":
                return 30;
            case "medium":
            default:
                return 20;
        }
    }

    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public boolean isSolved() { return isSolved; }
    public void setSolved(boolean solved) { isSolved = solved; }

    public String getSolvedTimeAgo() { return solvedTimeAgo; }
    public void setSolvedTimeAgo(String solvedTimeAgo) { this.solvedTimeAgo = solvedTimeAgo; }

    public String getDisplayTitle() {
        return number > 0 ? number + ". " + title : title;
    }

    public String getResolvedUrl() {
        if (url != null && !url.trim().isEmpty()) {
            return url.trim();
        }
        if (slug != null && !slug.trim().isEmpty()) {
            return "https://leetcode.com/problems/" + slug.trim() + "/";
        }
        return null;
    }
}
