package com.arena.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arena.app.models.Battle;
import com.arena.app.models.BattleResult;
import com.arena.app.network.ApiClient;
import com.arena.app.network.ApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BattleRepository {
    private final ApiService apiService;

    public BattleRepository(Context context) {
        apiService = ApiClient.getInstance(context).getApiService();
    }

    public LiveData<Battle> createBattle(String difficulty, List<String> topics, int bet) {
        MutableLiveData<Battle> data = new MutableLiveData<>();

        Map<String, Object> config = new HashMap<>();
        config.put("difficulty", difficulty);
        config.put("topics", topics);
        config.put("bet", bet);

        apiService.createBattle(config).enqueue(new Callback<Battle>() {
            @Override
            public void onResponse(Call<Battle> call, Response<Battle> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Battle> call, Throwable t) {
                // Return null on failure
                data.setValue(null);
            }
        });

        return data;
    }

    public LiveData<BattleResult> getBattleResult(String battleId) {
        MutableLiveData<BattleResult> data = new MutableLiveData<>();

        apiService.getBattleResult(battleId).enqueue(new Callback<BattleResult>() {
            @Override
            public void onResponse(Call<BattleResult> call, Response<BattleResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body());
                } else {
                    data.setValue(BattleResult.getMockVictory());
                }
            }

            @Override
            public void onFailure(Call<BattleResult> call, Throwable t) {
                data.setValue(BattleResult.getMockVictory());
            }
        });

        return data;
    }
}
