package com.arena.app.ui.arena;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.models.Problem;
import com.arena.app.ui.solver.ProblemSolverFragment;
import com.arena.app.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TopicVotingFragment extends Fragment {

    private static final List<String> TOPICS = Arrays.asList(
            "Arrays & Hashing", "Strings", "Dynamic Programming",
            "Trees & Graphs", "Math & Logic", "Sorting & Searching"
    );
    private static final int VOTE_SECONDS = 30;

    private BattleViewModel battleVm;
    private String selectedTopic = null;
    private MaterialButton btnLockIn;
    private LinearLayout layoutWaiting;
    private TextView textMyStatus, textOppStatus, textTimer, textPreview;
    private CountDownTimer countDownTimer;
    private boolean lockedIn = false;
    private boolean hasProceeded = false;
    private boolean demoMode = true;
    private String opponentName = "CodeWarrior_42";
    private String battleMode = "coding";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topic_voting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        battleVm = new ViewModelProvider(requireActivity()).get(BattleViewModel.class);
        demoMode = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_BATTLE_DEMO_MODE, true);

        // Read args
        if (getArguments() != null) {
            opponentName = getArguments().getString("opponentName", "CodeWarrior_42");
            battleMode = getArguments().getString(MatchFoundFragment.ARG_MODE, "coding");
        }

        // Bind views
        ChipGroup chipGroup  = view.findViewById(R.id.chipGroup_voting);
        btnLockIn            = view.findViewById(R.id.btn_lock_in);
        layoutWaiting        = view.findViewById(R.id.layout_waiting);
        textMyStatus         = view.findViewById(R.id.text_my_vote_status);
        textOppStatus        = view.findViewById(R.id.text_opp_vote_status);
        textTimer            = view.findViewById(R.id.text_timer);
        textPreview          = view.findViewById(R.id.text_selected_preview);

        textOppStatus.setText(opponentName + ": thinking...");

        // Build topic chips dynamically
        buildTopicChips(chipGroup);

        // Lock in listener
        btnLockIn.setOnClickListener(v -> onLockIn());

        // Start 30-second countdown
        startCountdown();

        if (demoMode) {
            simulateOpponentVote();
        }

        // Observe real server state too (if connected)
        observeServerState(view);
    }

    private void buildTopicChips(ChipGroup group) {
        group.removeAllViews();
        for (String topic : TOPICS) {
            Chip chip = new Chip(requireContext());
            chip.setText(topic);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_unselected_bg);
            chip.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chip.setCheckedIconTint(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.primary_accent, null)));
            group.addView(chip);
        }

        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = g.findViewById(checkedIds.get(0));
                selectedTopic = chip.getText().toString();
                textMyStatus.setText("You: " + selectedTopic + " ✓");
                textPreview.setText("\"" + selectedTopic + "\" selected");
                textPreview.setTextColor(
                        getResources().getColor(R.color.primary_accent, null));
            } else {
                selectedTopic = null;
                textMyStatus.setText("You: choosing...");
                textPreview.setText("Tap a topic to select it");
                textPreview.setTextColor(
                        getResources().getColor(R.color.text_secondary, null));
            }
        });
    }

    private void onLockIn() {
        if (selectedTopic == null) {
            Toast.makeText(getContext(), "Select a topic first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lockedIn) return;
        lockedIn = true;
        btnLockIn.setEnabled(false);
        btnLockIn.setText("VOTED ✓");
        layoutWaiting.setVisibility(View.VISIBLE);

        // Submit to server if connected
        battleVm.submitTopicVote(selectedTopic);

        if (demoMode) {
            requireView().postDelayed(this::proceedToDemoBattle, 2500);
        } else {
            textPreview.setText("Vote locked. Waiting for the real room...");
        }
    }

    private void proceedToDemoBattle() {
        if (!isAdded() || getView() == null) return;
        if (hasProceeded) return;
        hasProceeded = true;
        if (countDownTimer != null) countDownTimer.cancel();
        int dest = Navigation.findNavController(requireView()).getCurrentDestination().getId();
        if (dest == R.id.topicVotingFragment) {
            battleVm.startDemoBattle(battleMode, selectedTopic, opponentName);
            if ("mcq".equals(battleMode)) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_topicVoting_to_mcqBattle);
            } else {
                Problem problem = new Problem();
                problem.setTitle("Two Sum");
                problem.setSlug("two-sum");
                problem.setUrl("https://leetcode.com/problems/two-sum/");
                problem.setDifficulty("Easy");
                problem.setTopic(selectedTopic != null ? selectedTopic : "Arrays & Hashing");
                problem.setDescription("Solve this demo battle problem after the topic vote.");
                problem.setXpReward(250);

                Bundle args = ProblemSolverFragment.createArgs(problem);
                args.putBoolean("isBattleMode", true);
                args.putString("opponentName", opponentName);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_topicVoting_to_problemSolver, args);
            }
        }
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(VOTE_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) (millisUntilFinished / 1000);
                textTimer.setText(String.valueOf(sec));
                if (sec <= 10) {
                    textTimer.setTextColor(
                            getResources().getColor(R.color.defeat_red, null));
                }
            }

            @Override
            public void onFinish() {
                textTimer.setText("0");
                // Auto-pick random if not voted
                if (!lockedIn) {
                    if (selectedTopic == null) {
                        selectedTopic = TOPICS.get(new Random().nextInt(TOPICS.size()));
                    }
                    onLockIn();
                }
            }
        }.start();
    }

    private void simulateOpponentVote() {
        // Opponent votes between 5-10 seconds
        long delay = 5000 + new Random().nextInt(5000);
        requireView().postDelayed(() -> {
            if (!isAdded()) return;
            String oppTopic = TOPICS.get(new Random().nextInt(TOPICS.size()));
            textOppStatus.setText(opponentName + ": " + oppTopic + " ✓");
            textOppStatus.setTextColor(
                    getResources().getColor(R.color.primary_accent, null));
        }, delay);
    }

    private void observeServerState(View view) {
        // Real server state: IN_PROGRESS means voting resolved, move to MCQ
        battleVm.getStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == BattleViewModel.BattleStatus.IN_PROGRESS && !hasProceeded) {
                if (demoMode) {
                    proceedToDemoBattle();
                } else {
                    proceedToRealBattle();
                }
            }
        });
    }

    private void proceedToRealBattle() {
        if (!isAdded() || getView() == null || hasProceeded) return;
        hasProceeded = true;
        if (countDownTimer != null) countDownTimer.cancel();
        int dest = Navigation.findNavController(requireView()).getCurrentDestination().getId();
        if (dest != R.id.topicVotingFragment) return;
        String mode = battleVm.getBattleMode().getValue();
        if ("mcq".equals(mode)) {
            Navigation.findNavController(requireView()).navigate(R.id.action_topicVoting_to_mcqBattle);
            return;
        }
        org.json.JSONObject problemJson = battleVm.getProblem().getValue();
        if (problemJson == null) {
            Toast.makeText(requireContext(), "Waiting for battle problem...", Toast.LENGTH_SHORT).show();
            hasProceeded = false;
            return;
        }
        Problem problem = new Problem();
        problem.setTitle(problemJson.optString("title", "Battle Problem"));
        problem.setSlug(problemJson.optString("titleSlug", "two-sum"));
        problem.setUrl(problemJson.optString("url",
                "https://leetcode.com/problems/" + problem.getSlug() + "/"));
        problem.setDifficulty(problemJson.optString("difficulty", "Medium"));
        problem.setTopic(battleVm.getSelectedTopic().getValue() != null
                ? battleVm.getSelectedTopic().getValue()
                : selectedTopic);
        problem.setDescription("Solve this real battle problem before your opponent.");
        problem.setXpReward(250);

        Bundle args = ProblemSolverFragment.createArgs(problem);
        args.putBoolean("isBattleMode", true);
        args.putString("opponentName", battleVm.getOpponentName().getValue());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_topicVoting_to_problemSolver, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
