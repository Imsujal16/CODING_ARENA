package com.arena.app.utils;

import android.os.Build;

import com.arena.app.BuildConfig;

public class Constants {
    // The Android app talks to the deployed Next.js API described in the guide.
    public static final String BASE_URL = "https://leetcodee-sigma.vercel.app/api/";

    // Socket.IO still runs separately for local battle testing.
    public static final String SOCKET_URL = normalizeSocketUrl(
            isEmulator() ? BuildConfig.EMULATOR_SOCKET_URL : BuildConfig.DEVICE_SOCKET_URL
    );

    // Clerk Authentication
    public static final String CLERK_PUBLISHABLE_KEY = "pk_test_c3BlY2lhbC1za3Vuay05Ni5jbGVyay5hY2NvdW50cy5kZXYk";

    // Compiler
    public static final String COMPILER_BASE_URL = "https://ce.judge0.com/";

    // Assets
    public static final String STRIVER_SHEET_ASSET = "striver_a2z.json";

    // SharedPreferences
    public static final String PREFS_NAME = "arena_prefs";
    public static final String PREF_AUTH_TOKEN = "auth_token";
    public static final String PREF_ONBOARDING_COMPLETED = "onboarding_completed";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_LEETCODE_USERNAME = "leetcode_username";
    public static final String PREF_GUEST_MODE = "guest_mode";
    public static final String PREF_PROFILE_OVERRIDE = "profile_override";
    public static final String PREF_GUEST_PROFILE = "guest_profile";
    public static final String PREF_BATTLE_DEMO_MODE = "battle_demo_mode";

    // Request timeout (seconds)
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk".equals(Build.PRODUCT);
    }

    private static String normalizeSocketUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
