package com.arena.app.models;

public class User {
    private static final String DEFAULT_AUTH_USERNAME = "Coder";

    private String id;
    private String username;
    private String handle;
    private String email;
    private String avatarUrl;
    private int level;
    private int xp;
    private String rank;
    private int streak;
    private int points;
    private String bio;
    private String location;
    private String joinedDate;
    private String birthday;
    private int following;
    private int followers;
    private boolean verified;

    public User() {}

    public User(User other) {
        this(
                other.id,
                other.username,
                other.handle,
                other.email,
                other.avatarUrl,
                other.level,
                other.xp,
                other.rank,
                other.streak,
                other.points,
                other.bio,
                other.location,
                other.joinedDate,
                other.birthday,
                other.following,
                other.followers,
                other.verified
        );
    }

    public User(String id, String username, String handle, String email, String avatarUrl, int level,
                int xp, String rank, int streak, int points, String bio, String location,
                String joinedDate, String birthday, int following, int followers, boolean verified) {
        this.id = id;
        this.username = username;
        this.handle = handle;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.level = level;
        this.xp = xp;
        this.rank = rank;
        this.streak = streak;
        this.points = points;
        this.bio = bio;
        this.location = location;
        this.joinedDate = joinedDate;
        this.birthday = birthday;
        this.following = following;
        this.followers = followers;
        this.verified = verified;
    }

    // Static mock data
    public static User getMockUser() {
        return new User(
            "user_001", "Gautam", "@ggupta78", "ggupta78@example.com", null, 24,
            12450, "Platinum", 12, 1200, "Co-Founder - Sevenfold",
            "Delhi, India", "Joined Jan 2026", "Born June 25, 1996",
            50, 50, true
        );
    }

    public static User createSignedInPlaceholder(String userId) {
        return new User(
                userId,
                DEFAULT_AUTH_USERNAME,
                "",
                "",
                null,
                0,
                0,
                "",
                0,
                0,
                "",
                "",
                "",
                "",
                0,
                0,
                false
        );
    }

    public static User createGuestUser(String username) {
        String safeUsername = username == null || username.trim().isEmpty()
                ? "Guest"
                : username.trim();

        return new User(
                "guest_" + safeUsername.toLowerCase(),
                safeUsername,
                "",
                "",
                null,
                0,
                0,
                "",
                0,
                0,
                "",
                "",
                "",
                "",
                0,
                0,
                false
        );
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public int getFollowing() { return following; }
    public void setFollowing(int following) { this.following = following; }

    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getFormattedPoints() {
        if (points >= 1000) {
            return String.format("%.1fk", points / 1000.0);
        }
        return String.valueOf(points);
    }

    public String getFormattedXp() {
        return String.format("%,d", xp);
    }
}
