package com.arena.app.ui.arena;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.models.BattleResult;
import com.google.android.material.button.MaterialButton;

public class BattleResultFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battle_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BattleResult result = BattleResult.getMockVictory();
        bindResult(view, result);
        animateIn(view);

        MaterialButton btnBack = view.findViewById(R.id.btn_back_to_arena);
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_battleResult_to_arena));
    }

    private void bindResult(View view, BattleResult result) {
        TextView titleText = view.findViewById(R.id.text_result_title);
        TextView timeText = view.findViewById(R.id.text_stat_time);
        TextView accuracyText = view.findViewById(R.id.text_stat_accuracy);
        TextView xpText = view.findViewById(R.id.text_stat_xp);
        TextView coinsText = view.findViewById(R.id.text_stat_coins);

        if (result.isWon()) {
            titleText.setText(R.string.victory);
            titleText.setTextColor(getResources().getColor(R.color.victory_teal, null));
        } else {
            titleText.setText(R.string.defeat);
            titleText.setTextColor(getResources().getColor(R.color.defeat_red, null));
        }

        timeText.setText(result.getTime());
        accuracyText.setText(result.getFormattedAccuracy());
        xpText.setText(result.getFormattedXp());
        coinsText.setText(result.getFormattedCoins());
    }

    private void animateIn(View root) {
        LinearLayout bottomSheet = root.findViewById(R.id.bottom_sheet);
        bottomSheet.setTranslationY(600f);
        bottomSheet.setAlpha(0f);
        bottomSheet.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(0.6f))
                .start();

        // Animate stats cards sequentially
        int[] statIds = {R.id.stat_time, R.id.stat_accuracy, R.id.stat_xp, R.id.stat_coins};
        for (int i = 0; i < statIds.length; i++) {
            View stat = root.findViewById(statIds[i]);
            stat.setScaleX(0f);
            stat.setScaleY(0f);
            stat.setAlpha(0f);
            stat.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(500 + (i * 100L))
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }
}
