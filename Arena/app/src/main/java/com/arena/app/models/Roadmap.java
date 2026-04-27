package com.arena.app.models;

import java.util.ArrayList;
import java.util.List;

public class Roadmap {
    private String id;
    private String title;
    private String subtitle;
    private int progress; // 0-100
    private String badge; // "MOST POPULAR", "NEW", etc.
    private boolean isBlueGradient; // true = blue, false = green

    public Roadmap() {}

    public Roadmap(String id, String title, String subtitle, int progress,
                   String badge, boolean isBlueGradient) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.progress = progress;
        this.badge = badge;
        this.isBlueGradient = isBlueGradient;
    }

    public static List<Roadmap> getMockRoadmaps() {
        List<Roadmap> list = new ArrayList<>();
        list.add(new Roadmap("r1", "Striver's A2Z\nDSA Course",
                "Complete mastery from basics to advanced.", 35, "MOST POPULAR", true));
        list.add(new Roadmap("r2", "Neetcode 150\nPatterns",
                "The essential coding interview patterns.", 20, "NEW", false));
        list.add(new Roadmap("r3", "System Design\nMastery",
                "Learn scalable system design principles.", 10, null, true));
        return list;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public boolean isBlueGradient() { return isBlueGradient; }
    public void setBlueGradient(boolean blueGradient) { isBlueGradient = blueGradient; }
}
