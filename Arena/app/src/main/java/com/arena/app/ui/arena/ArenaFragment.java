package com.arena.app.ui.arena;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class ArenaFragment extends Fragment {

    private String selectedDifficulty = "Medium";
    private int betAmount = 500;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_arena, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDifficultyToggle(view);
        setupBetSlider(view);
        setupFightButton(view);
    }

    private void setupDifficultyToggle(View view) {
        TextView chipEasy = view.findViewById(R.id.chip_easy);
        TextView chipMedium = view.findViewById(R.id.chip_medium);
        TextView chipHard = view.findViewById(R.id.chip_hard);

        View.OnClickListener listener = v -> {
            // Reset all
            chipEasy.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipEasy.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chipMedium.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipMedium.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chipHard.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipHard.setTextColor(getResources().getColor(R.color.text_secondary, null));

            // Select clicked
            ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
            ((TextView) v).setTextColor(getResources().getColor(R.color.text_white, null));

            if (v.getId() == R.id.chip_easy) selectedDifficulty = "Easy";
            else if (v.getId() == R.id.chip_medium) selectedDifficulty = "Medium";
            else selectedDifficulty = "Hard";
        };

        chipEasy.setOnClickListener(listener);
        chipMedium.setOnClickListener(listener);
        chipHard.setOnClickListener(listener);
    }

    private void setupBetSlider(View view) {
        Slider slider = view.findViewById(R.id.slider_bet);
        TextView textBet = view.findViewById(R.id.text_bet_amount);

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            betAmount = (int) value;
            textBet.setText(String.valueOf(betAmount));
        });
    }

    private void setupFightButton(View view) {
        MaterialButton btnFight = view.findViewById(R.id.btn_fight);
        btnFight.setOnClickListener(v -> {
            // Navigate to battle result (with mock victory)
            Navigation.findNavController(v).navigate(R.id.action_arena_to_battleResult);
        });
    }
}
