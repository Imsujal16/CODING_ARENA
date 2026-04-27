package com.arena.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arena.app.models.LeetcodeSyncRequest;
import com.arena.app.models.LeetcodeSyncResponse;
import com.arena.app.models.LinkedLeetcodeResponse;
import com.arena.app.models.User;
import com.arena.app.models.UserStatusResponse;
import com.arena.app.network.ApiClient;
import com.arena.app.network.ApiService;
import com.arena.app.utils.ClerkAuthHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;
    private final ClerkAuthHelper authHelper;
    private final LocalProfileStore localProfileStore;

    public UserRepository(Context context) {
        Context appContext = context.getApplicationContext();
        apiService = ApiClient.getInstance(appContext).getApiService();
        authHelper = new ClerkAuthHelper(appContext);
        localProfileStore = new LocalProfileStore(appContext);
    }

    public LiveData<User> getUserProfile() {
        return loadUser(true);
    }

    public LiveData<User> getUserStats() {
        return loadUser(false);
    }

    private LiveData<User> loadUser(boolean detailedProfile) {
        MutableLiveData<User> data = new MutableLiveData<>();
        data.setValue(localProfileStore.getEffectiveProfile(localProfileStore.getDefaultProfile()));

        String cachedUsername = normalizeUsername(authHelper.getLeetcodeUsername());
        if (cachedUsername != null) {
            syncUserFromApis(cachedUsername, data, detailedProfile);
        }

        if (authHelper.shouldUseAuthenticatedApis()) {
            apiService.getLinkedLeetcodeUsername().enqueue(new Callback<LinkedLeetcodeResponse>() {
                @Override
                public void onResponse(Call<LinkedLeetcodeResponse> call,
                                       Response<LinkedLeetcodeResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }

                    String linkedUsername = normalizeUsername(response.body().getLeetcodeUsername());
                    if (linkedUsername == null) {
                        return;
                    }

                    authHelper.saveLeetcodeUsername(linkedUsername);
                    syncUserFromApis(linkedUsername, data, detailedProfile);
                }

                @Override
                public void onFailure(Call<LinkedLeetcodeResponse> call, Throwable t) {
                    // Public endpoints still use any cached username if present.
                }
            });
        }

        return data;
    }

    private void syncUserFromApis(String leetcodeUsername,
                                  MutableLiveData<User> data,
                                  boolean detailedProfile) {
        apiService.getUserStatus(leetcodeUsername).enqueue(new Callback<UserStatusResponse>() {
            @Override
            public void onResponse(Call<UserStatusResponse> call, Response<UserStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User updated = copyCurrentUser(data);
                    if (authHelper.getUserId() != null) {
                        updated.setId(authHelper.getUserId());
                    }
                    updated.setStreak(response.body().getCurrentStreak());
                    data.postValue(localProfileStore.getEffectiveProfile(updated));
                }
            }

            @Override
            public void onFailure(Call<UserStatusResponse> call, Throwable t) {
                // Keep the current state.
            }
        });

        apiService.syncLeetcode(new LeetcodeSyncRequest(leetcodeUsername))
                .enqueue(new Callback<LeetcodeSyncResponse>() {
                    @Override
                    public void onResponse(Call<LeetcodeSyncResponse> call,
                                           Response<LeetcodeSyncResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().getMatchedUser() == null) {
                            return;
                        }

                        User updated = copyCurrentUser(data);
                        LeetcodeSyncResponse.MatchedUser matchedUser = response.body().getMatchedUser();

                        if (authHelper.getUserId() != null) {
                            updated.setId(authHelper.getUserId());
                        }

                        String displayName = matchedUser.getUsername();
                        if (matchedUser.getProfile() != null
                                && matchedUser.getProfile().getRealName() != null
                                && !matchedUser.getProfile().getRealName().trim().isEmpty()) {
                            displayName = matchedUser.getProfile().getRealName().trim();
                        }

                        updated.setUsername(displayName);
                        updated.setHandle("@" + matchedUser.getUsername());

                        if (matchedUser.getProfile() != null) {
                            updated.setAvatarUrl(matchedUser.getProfile().getUserAvatar());
                            if (matchedUser.getProfile().getRanking() > 0) {
                                updated.setRank("LC #" + matchedUser.getProfile().getRanking());
                            }
                        }

                        if (matchedUser.getUserCalendar() != null && matchedUser.getUserCalendar().getStreak() > 0) {
                            updated.setStreak(matchedUser.getUserCalendar().getStreak());
                        }

                        if (detailedProfile) {
                            int solvedCount = getSolvedCount(response.body());
                            if (solvedCount > 0) {
                                updated.setPoints(solvedCount);
                            }
                        }

                        data.postValue(localProfileStore.getEffectiveProfile(updated));
                    }

                    @Override
                    public void onFailure(Call<LeetcodeSyncResponse> call, Throwable t) {
                        // Keep fallback/mock values for fields this API would enrich.
                    }
                });
    }

    private int getSolvedCount(LeetcodeSyncResponse response) {
        if (response.getMatchedUser() == null
                || response.getMatchedUser().getSubmitStatsGlobal() == null
                || response.getMatchedUser().getSubmitStatsGlobal().getAcSubmissionNum() == null) {
            return 0;
        }

        for (LeetcodeSyncResponse.SubmissionCount count
                : response.getMatchedUser().getSubmitStatsGlobal().getAcSubmissionNum()) {
            if (count != null && "All".equalsIgnoreCase(count.getDifficulty())) {
                return count.getCount();
            }
        }

        return 0;
    }

    private User copyCurrentUser(MutableLiveData<User> data) {
        User current = data.getValue();
        return current == null
                ? localProfileStore.getEffectiveProfile(localProfileStore.getDefaultProfile())
                : new User(current);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }

        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
