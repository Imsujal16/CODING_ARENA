package com.arena.app.ui.arena;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.repository.LocalProfileStore;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCQBattleFragment extends Fragment {

    private BattleViewModel battleVm;
    private JSONArray questionsArray;
    private int currentQuestionIndex = 0;
    private int score = 0;

    private TextView textProgress;
    private TextView textScore;
    private TextView textQuestion;
    private TextView textStatus;
    private TextView textTimer;
    private TextView textOpponentScore;
    private View layoutOptions;
    private MaterialButton[] btnOptions = new MaterialButton[4];
    private CountDownTimer timer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mcq_battle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        battleVm = new ViewModelProvider(requireActivity()).get(BattleViewModel.class);

        textProgress = view.findViewById(R.id.text_mcq_progress);
        textScore = view.findViewById(R.id.text_mcq_score);
        textQuestion = view.findViewById(R.id.text_question);
        textStatus = view.findViewById(R.id.text_mcq_status);
        textTimer = view.findViewById(R.id.text_timer);
        textOpponentScore = view.findViewById(R.id.text_opponent_score);
        layoutOptions = view.findViewById(R.id.layout_options);

        bindDuelHeader(view);
        startTimer();

        btnOptions[0] = view.findViewById(R.id.btn_option_0);
        btnOptions[1] = view.findViewById(R.id.btn_option_1);
        btnOptions[2] = view.findViewById(R.id.btn_option_2);
        btnOptions[3] = view.findViewById(R.id.btn_option_3);

        for (MaterialButton btn : btnOptions) {
            btn.setOnClickListener(this::onOptionSelected);
        }

        battleVm.getMcqQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && questionsArray == null) {
                questionsArray = questions;
                loadQuestion();
            }
        });

        battleVm.getStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == BattleViewModel.BattleStatus.ENDED) {
                if (Navigation.findNavController(view).getCurrentDestination().getId() == R.id.mcqBattleFragment) {
                    com.arena.app.models.BattleResult result = battleVm.getResult().getValue();
                    Bundle args = result != null ? result.toBundle() : null;
                    Navigation.findNavController(view).navigate(R.id.action_mcqBattle_to_battleResult, args);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
    }

    private void loadQuestion() {
        if (questionsArray == null || currentQuestionIndex >= questionsArray.length()) {
            finishQuiz();
            return;
        }

        try {
            JSONObject q = questionsArray.getJSONObject(currentQuestionIndex);
            textQuestion.setText(q.getString("question"));
            textProgress.setText("QUESTION " + (currentQuestionIndex + 1) + " / " + questionsArray.length());
            textScore.setText(String.valueOf(score));
            textOpponentScore.setText(String.valueOf(Math.max(0, currentQuestionIndex - score)));

            JSONArray options = q.getJSONArray("options");
            List<String> shuffledOptions = new ArrayList<>();
            for (int i = 0; i < options.length(); i++) {
                shuffledOptions.add(options.getString(i));
            }
            // Shuffle to prevent predictable patterns
            Collections.shuffle(shuffledOptions);

            for (int i = 0; i < 4; i++) {
                btnOptions[i].setText(shuffledOptions.get(i));
                btnOptions[i].setEnabled(true);
                btnOptions[i].setStrokeColorResource(R.color.divider);
                btnOptions[i].setAlpha(1f);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onOptionSelected(View v) {
        MaterialButton selectedBtn = (MaterialButton) v;
        String selectedAnswer = selectedBtn.getText().toString();

        try {
            JSONObject q = questionsArray.getJSONObject(currentQuestionIndex);
            String correctAnswer = q.getString("answer");

            if (selectedAnswer.equals(correctAnswer)) {
                score++;
                selectedBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.GREEN));
            } else {
                selectedBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.RED));
                // Find correct one and highlight it
                for (MaterialButton btn : btnOptions) {
                    if (btn.getText().toString().equals(correctAnswer)) {
                        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.GREEN));
                    }
                }
            }

            // Disable all buttons to prevent double tapping
            for (MaterialButton btn : btnOptions) {
                btn.setEnabled(false);
            }

            textScore.setText(String.valueOf(score));

            // Move to next after a delay
            v.postDelayed(() -> {
                currentQuestionIndex++;
                loadQuestion();
            }, 1000);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void finishQuiz() {
        if (timer != null) timer.cancel();
        layoutOptions.setVisibility(View.GONE);
        textQuestion.setVisibility(View.GONE);
        textProgress.setVisibility(View.GONE);
        textStatus.setVisibility(View.VISIBLE);
        
        battleVm.submitMcqScore(score);
    }

    private void bindDuelHeader(View view) {
        LocalProfileStore store = new LocalProfileStore(requireContext());
        String playerName = store.getDefaultProfile().getUsername();
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "You";
        }
        String opponentName = battleVm.getOpponentName().getValue();
        if (opponentName == null || opponentName.trim().isEmpty()) {
            opponentName = "CodeWarrior";
        }

        ((TextView) view.findViewById(R.id.text_player_name)).setText(playerName);
        ((TextView) view.findViewById(R.id.avatar_you)).setText(playerName.substring(0, 1).toUpperCase(java.util.Locale.US));
        ((TextView) view.findViewById(R.id.text_opponent_name)).setText(opponentName);
        ((TextView) view.findViewById(R.id.avatar_opponent)).setText(opponentName.substring(0, 1).toUpperCase(java.util.Locale.US));
    }

    private void startTimer() {
        timer = new CountDownTimer(45000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                textTimer.setText(String.format(java.util.Locale.US, "0:%02d", seconds));
                if (seconds <= 10) {
                    textTimer.setTextColor(Color.parseColor("#FF4D4D"));
                }
            }

            @Override
            public void onFinish() {
                textTimer.setText("0:00");
                finishQuiz();
            }
        };
        timer.start();
    }
}
