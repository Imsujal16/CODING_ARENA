package com.arena.app.network;

import com.arena.app.models.Battle;
import com.arena.app.models.BattleResult;
import com.arena.app.models.DailyChallengeResponse;
import com.arena.app.models.LeaderboardResponse;
import com.arena.app.models.LeetcodeSyncRequest;
import com.arena.app.models.LeetcodeSyncResponse;
import com.arena.app.models.LinkedLeetcodeResponse;
import com.arena.app.models.SheetProgressResponse;
import com.arena.app.models.UserStatusResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("user/status/today")
    Call<UserStatusResponse> getUserStatus(@Query("username") String username);

    @GET("user/leetcode")
    Call<LinkedLeetcodeResponse> getLinkedLeetcodeUsername();

    @GET("leaderboard")
    Call<LeaderboardResponse> getLeaderboard(
            @Query("tier") String tier,
            @Query("limit") Integer limit
    );

    @POST("leetcode/sync")
    Call<LeetcodeSyncResponse> syncLeetcode(@Body LeetcodeSyncRequest request);

    @GET("leetcode/daily")
    Call<DailyChallengeResponse> getDailyChallenge();

    @POST("leetcode/check-solved")
    Call<Map<String, Object>> checkSolved(@Body Map<String, Object> request);

    @GET("sheets/striver/progress")
    Call<SheetProgressResponse> getStriverProgress();

    @GET("coins")
    Call<Map<String, Object>> getCoins();

    @POST("coins")
    Call<Map<String, Object>> createCoinTransaction(@Body Map<String, Object> transaction);

    @GET("shop")
    Call<Map<String, Object>> getShop();

    @GET("friends")
    Call<Map<String, Object>> getFriends();

    @POST("battle/create")
    Call<Battle> createBattle(@Body Map<String, Object> config);

    @GET("battle/{id}/result")
    Call<BattleResult> getBattleResult(@Path("id") String battleId);
}
