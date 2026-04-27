package com.arena.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ClerkAuthHelper {
    private final SharedPreferences prefs;

    public ClerkAuthHelper(Context context) {
        prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.PREF_AUTH_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.PREF_AUTH_TOKEN, null);
    }

    public void clearToken() {
        prefs.edit()
                .remove(Constants.PREF_AUTH_TOKEN)
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_LEETCODE_USERNAME)
                .remove(Constants.PREF_GUEST_PROFILE)
                .apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null || getUserId() != null;
    }

    public boolean shouldUseAuthenticatedApis() {
        return !isGuestModeEnabled() && isLoggedIn();
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(Constants.PREF_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(Constants.PREF_USER_ID, null);
    }

    public void saveLeetcodeUsername(String username) {
        String normalized = username == null ? null : username.trim();
        prefs.edit().putString(Constants.PREF_LEETCODE_USERNAME, normalized).apply();
    }

    public String getLeetcodeUsername() {
        return prefs.getString(Constants.PREF_LEETCODE_USERNAME, null);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETED, false);
    }

    public void setGuestModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_GUEST_MODE, enabled).apply();
    }

    public boolean isGuestModeEnabled() {
        return prefs.getBoolean(Constants.PREF_GUEST_MODE, false);
    }
}
