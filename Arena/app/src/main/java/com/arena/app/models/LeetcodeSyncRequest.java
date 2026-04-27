package com.arena.app.models;

public class LeetcodeSyncRequest {
    private final String username;

    public LeetcodeSyncRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
