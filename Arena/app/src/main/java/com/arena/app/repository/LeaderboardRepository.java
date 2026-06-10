package com.arena.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arena.app.models.LeaderboardEntry;
import com.arena.app.models.LeaderboardResponse;
import com.arena.app.models.User;
import com.arena.app.network.ApiClient;
import com.arena.app.network.ApiService;
import com.arena.app.utils.ClerkAuthHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardRepository {
    private final ApiService apiService;
    private final ClerkAuthHelper authHelper;
    private final LocalProfileStore localProfileStore;

    public LeaderboardRepository(Context context) {
        Context appContext = context.getApplicationContext();
        apiService = ApiClient.getInstance(appContext).getApiService();
        authHelper = new ClerkAuthHelper(appContext);
        localProfileStore = new LocalProfileStore(appContext);
    }

    public LiveData<List<LeaderboardEntry>> getLeaderboard(String tier, int limit) {
        MutableLiveData<List<LeaderboardEntry>> data = new MutableLiveData<>();

        apiService.getLeaderboard(tier, limit).enqueue(new Callback<LeaderboardResponse>() {
            @Override
            public void onResponse(Call<LeaderboardResponse> call, Response<LeaderboardResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getLeaderboard() == null
                        || response.body().getLeaderboard().isEmpty()) {
                    emitFallback(data);
                    return;
                }

                List<LeaderboardEntry> entries = response.body().getLeaderboard();
                markCurrentUser(entries);
                data.setValue(entries);
            }

            @Override
            public void onFailure(Call<LeaderboardResponse> call, Throwable t) {
                emitFallback(data);
            }
        });

        return data;
    }

    private void emitFallback(MutableLiveData<List<LeaderboardEntry>> data) {
        List<LeaderboardEntry> fallback = LeaderboardEntry.getMockLeaderboard();
        markCurrentUser(fallback);
        data.setValue(fallback);
    }

    public LeaderboardEntry findCurrentUser(List<LeaderboardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return LeaderboardEntry.getCurrentUserEntry();
        }

        markCurrentUser(entries);

        for (LeaderboardEntry entry : entries) {
            if (entry.isCurrentUser()) {
                return entry;
            }
        }

        return LeaderboardEntry.getCurrentUserEntry();
    }

    private void markCurrentUser(List<LeaderboardEntry> entries) {
        String currentUserId = safeLower(authHelper.getUserId());
        String linkedUsername = safeLower(authHelper.getLeetcodeUsername());

        for (LeaderboardEntry entry : entries) {
            boolean matchesId = currentUserId != null && currentUserId.equals(safeLower(entry.getUserId()));
            boolean matchesUsername = linkedUsername != null
                    && linkedUsername.equals(safeLower(entry.getUsername()));
            boolean isCurrentUser = matchesId || matchesUsername;
            entry.setCurrentUser(isCurrentUser);
            if (isCurrentUser) {
                applyLocalProfile(entry, currentUserId);
            }
        }
    }

    private void applyLocalProfile(LeaderboardEntry entry, String currentUserId) {
        User savedProfile = localProfileStore.getSavedProfileForUserId(currentUserId);
        if (savedProfile == null) {
            savedProfile = localProfileStore.getSavedProfile();
        }
        if (savedProfile == null) {
            return;
        }

        if (hasText(savedProfile.getUsername())) {
            entry.setUsername(savedProfile.getUsername());
        }
        if (hasText(savedProfile.getAvatarUrl())) {
            entry.setAvatarUrl(savedProfile.getAvatarUrl());
        }
        if (hasText(savedProfile.getId())) {
            entry.setUserId(savedProfile.getId());
        }
    }

    private String safeLower(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
