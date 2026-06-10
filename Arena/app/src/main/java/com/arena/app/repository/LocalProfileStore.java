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

        String profileId = resolveProfileStorageId(user);
        if (!hasText(profileId)) {
            return;
        }

        user.setId(profileId);
        prefs.edit()
                .putString(getProfileOverrideKey(profileId), gson.toJson(user))
                .apply();
    }

    public User getSavedProfile() {
        String currentUserId = getCurrentUserId();
        User savedProfile = getSavedProfileForUserId(currentUserId);
        if (savedProfile != null) {
            return savedProfile;
        }

        User legacyProfile = readUser(Constants.PREF_PROFILE_OVERRIDE);
        if (legacyProfile == null) {
            return null;
        }

        if (!hasText(currentUserId)
                || !hasText(legacyProfile.getId())
                || currentUserId.equals(legacyProfile.getId())) {
            if (hasText(currentUserId)) {
                legacyProfile.setId(currentUserId);
                saveProfile(legacyProfile);
            }
            return legacyProfile;
        }
        return null;
    }

    public User getSavedProfileForUserId(String userId) {
        if (!hasText(userId)) {
            return null;
        }
        return readUser(getProfileOverrideKey(userId));
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
            if (!hasText(effectiveProfile.getBannerUrl())) {
                effectiveProfile.setBannerUrl(resolvedBase.getBannerUrl());
            }
            if (!hasText(effectiveProfile.getEmail())) {
                effectiveProfile.setEmail(resolvedBase.getEmail());
            }
            if (!hasText(effectiveProfile.getAvatarColor())) {
                effectiveProfile.setAvatarColor(hasText(resolvedBase.getAvatarColor())
                        ? resolvedBase.getAvatarColor()
                        : "#1D75D8");
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

    private String resolveProfileStorageId(User user) {
        String currentUserId = getCurrentUserId();
        if (hasText(currentUserId)) {
            return currentUserId;
        }
        if (user != null && hasText(user.getId())) {
            return user.getId();
        }
        return null;
    }

    private String getProfileOverrideKey(String userId) {
        return Constants.PREF_PROFILE_OVERRIDE + "_" + userId;
    }

    private boolean isGuestModeEnabled() {
        return prefs.getBoolean(Constants.PREF_GUEST_MODE, false);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
