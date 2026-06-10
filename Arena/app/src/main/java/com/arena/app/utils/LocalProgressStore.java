package com.arena.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class LocalProgressStore {
    private static final String PREF_NAME = "arena_progress_store";
    private static final String KEY_SOLVED_PROBLEMS = "solved_problems_slugs";
    
    private final SharedPreferences prefs;

    public LocalProgressStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getSolvedSlugs() {
        return new HashSet<>(prefs.getStringSet(KEY_SOLVED_PROBLEMS, new HashSet<>()));
    }

    public void setProblemSolved(String slug, boolean solved) {
        Set<String> current = getSolvedSlugs();
        if (solved) {
            current.add(slug);
        } else {
            current.remove(slug);
        }
        prefs.edit().putStringSet(KEY_SOLVED_PROBLEMS, current).apply();
    }

    public boolean isProblemSolved(String slug) {
        return getSolvedSlugs().contains(slug);
    }
}
