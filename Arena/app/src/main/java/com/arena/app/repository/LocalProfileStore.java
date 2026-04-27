package com.arena.app.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.arena.app.models.User;
import com.arena.app.utils.Constants;
import com.google.gson.Gson;

import java.util.Random;

public class LocalProfileStore {
    private static final String[] GUEST_PREFIXES = {
            "Guest", "Coder", "Arena", "Player", "Solver"
    };

    private final SharedPreferences prefs;
    private final Gson gson;
    private final Random random;

    public LocalProfileStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        random = new Random();
    }

    public void saveProfile(User user) {
        if (user == null) {
            return;
        }

        if (!hasText(user.getId())) {
            String currentUserId = getCurrentUserId();
            if (hasText(currentUserId)) {
                user.setId(currentUserId);
            }
        }

        prefs.edit()
                .putString(Constants.PREF_PROFILE_OVERRIDE, gson.toJson(user))
                .apply();
    }

    public User getSavedProfile() {
        String json = prefs.getString(Constants.PREF_PROFILE_OVERRIDE, null);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(json, User.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public void saveGuestProfile(User user) {
        if (user == null) {
            return;
        }

        prefs.edit()
                .putString(Constants.PREF_GUEST_PROFILE, gson.toJson(user))
                .apply();
    }

    public User getGuestProfile() {
        return readUser(Constants.PREF_GUEST_PROFILE);
    }

    public void clearGuestProfile() {
        prefs.edit().remove(Constants.PREF_GUEST_PROFILE).apply();
    }

    public User createAndSaveRandomGuestProfile() {
        String username = GUEST_PREFIXES[random.nextInt(GUEST_PREFIXES.length)]
                + (1000 + random.nextInt(9000));
        User guestUser = User.createGuestUser(username);
        saveGuestProfile(guestUser);
        return guestUser;
    }

    public User getDefaultProfile() {
        if (isGuestModeEnabled()) {
            User guestUser = getGuestProfile();
            return guestUser != null ? guestUser : createAndSaveRandomGuestProfile();
        }

        String currentUserId = getCurrentUserId();
        if (hasText(currentUserId)) {
            return User.createSignedInPlaceholder(currentUserId);
        }

        return User.getMockUser();
    }

    public User getEffectiveProfile(User baseUser) {
        if (isGuestModeEnabled()) {
            User guestUser = getGuestProfile();
            return guestUser != null ? guestUser : createAndSaveRandomGuestProfile();
        }

        User resolvedBase = baseUser == null ? getDefaultProfile() : baseUser;
        User savedProfile = getSavedProfile();
        if (savedProfile == null) {
            return resolvedBase;
        }

        String currentUserId = getCurrentUserId();
        if (hasText(currentUserId)
                && hasText(savedProfile.getId())
                && !currentUserId.equals(savedProfile.getId())) {
            return resolvedBase;
        }

        User effectiveProfile = new User(savedProfile);
        if (resolvedBase != null) {
            if (!hasText(effectiveProfile.getId())) {
                effectiveProfile.setId(resolvedBase.getId());
            }
            if (!hasText(effectiveProfile.getAvatarUrl())) {
                effectiveProfile.setAvatarUrl(resolvedBase.getAvatarUrl());
            }
            if (!hasText(effectiveProfile.getEmail())) {
                effectiveProfile.setEmail(resolvedBase.getEmail());
            }

            // These are synced/live values and should not be overridden by local edits.
            effectiveProfile.setLevel(resolvedBase.getLevel());
            effectiveProfile.setRank(resolvedBase.getRank());
            effectiveProfile.setPoints(resolvedBase.getPoints());
            effectiveProfile.setStreak(resolvedBase.getStreak());
            effectiveProfile.setFollowing(resolvedBase.getFollowing());
            effectiveProfile.setFollowers(resolvedBase.getFollowers());
        }
        return effectiveProfile;
    }

    private User readUser(String prefKey) {
        String json = prefs.getString(prefKey, null);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(json, User.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String getCurrentUserId() {
        return prefs.getString(Constants.PREF_USER_ID, null);
    }

    private boolean isGuestModeEnabled() {
        return prefs.getBoolean(Constants.PREF_GUEST_MODE, false);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
