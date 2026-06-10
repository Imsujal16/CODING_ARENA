package com.arena.app.ui.arena;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.utils.Constants;

public class MatchFoundFragment extends Fragment {

    public static final String ARG_MODE = "mode";
    public static final String ARG_OPPONENT = "opponent";
    public static final String ARG_OPP_RANK = "opp_rank";

    private CountDownTimer countDownTimer;
    private BattleViewModel battleVm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_found, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        battleVm = new ViewModelProvider(requireActivity()).get(BattleViewModel.class);

        String mode = getArguments() != null ? getArguments().getString(ARG_MODE, "coding") : "coding";
        String opponentName = getArguments() != null
                ? getArguments().getString(ARG_OPPONENT, "CodeWarrior_42")
                : "CodeWarrior_42";
        String oppRank = getArguments() != null
                ? getArguments().getString(ARG_OPP_RANK, "Silver II")
                : "Silver II";

        LocalProfileStore store = new LocalProfileStore(requireContext());
        TextView textPlayerName = view.findViewById(R.id.text_player_name);
        TextView textPlayerRank = view.findViewById(R.id.text_player_rank);
        TextView textOppName = view.findViewById(R.id.text_opponent_name);
        TextView textOppRank = view.findViewById(R.id.text_opponent_rank);
        TextView textMode = view.findViewById(R.id.text_battle_mode);
        TextView textCountdown = view.findViewById(R.id.text_countdown);

        String myName = store.getDefaultProfile().getUsername();
        textPlayerName.setText(myName != null ? myName : "You");
        textPlayerRank.setText("Challenger");
        textOppName.setText(opponentName);
        textOppRank.setText(oppRank);
        textMode.setText("mcq".equals(mode) ? "MCQ LOGIC BATTLE" : "CODING BATTLE");

        boolean demoMode = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_BATTLE_DEMO_MODE, true);
        if (demoMode) {
            battleVm.prepareDemoMatch(mode, opponentName);
        }
        animateIn(view);

        countDownTimer = new CountDownTimer(3500, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) Math.ceil(millisUntilFinished / 1000.0);
                if (sec > 0) {
                    textCountdown.setText(String.valueOf(sec));
                    textCountdown.setScaleX(1.4f);
                    textCountdown.setScaleY(1.4f);
                    textCountdown.animate().scaleX(1f).scaleY(1f).setDuration(600)
                            .setInterpolator(new OvershootInterpolator()).start();
                }
            }

            @Override
            public void onFinish() {
                textCountdown.setText("GO!");
                textCountdown.setTextColor(getResources().getColor(R.color.primary_accent, null));
                textCountdown.animate().scaleX(1.5f).scaleY(1.5f).setDuration(400)
                        .withEndAction(() -> {
                            if (!isAdded() || getView() == null) return;
                            int dest = Navigation.findNavController(requireView())
                                    .getCurrentDestination().getId();
                            if (dest == R.id.matchFoundFragment) {
                                Bundle args = new Bundle();
                                args.putString(ARG_MODE, mode);
                                args.putString("opponentName", opponentName);
                                Navigation.findNavController(requireView())
                                        .navigate(R.id.action_matchFound_to_topicVoting, args);
                            }
                        }).start();
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void animateIn(View root) {
        View title = root.findViewById(R.id.text_match_title);
        View cardPlayer = root.findViewById(R.id.card_player);
        View textVs = root.findViewById(R.id.text_vs);
        View cardOpp = root.findViewById(R.id.card_opponent);
        View modeBadge = root.findViewById(R.id.text_battle_mode);

        title.animate().alpha(1f).setDuration(400).setStartDelay(100).start();

        cardPlayer.setTranslationX(-200f);
        cardPlayer.animate().alpha(1f).translationX(0f).setDuration(500)
                .setStartDelay(300).setInterpolator(new OvershootInterpolator(0.8f)).start();

        textVs.animate().alpha(1f).setDuration(300).setStartDelay(600).start();

        cardOpp.setTranslationX(200f);
        cardOpp.animate().alpha(1f).translationX(0f).setDuration(500)
                .setStartDelay(300).setInterpolator(new OvershootInterpolator(0.8f)).start();

        modeBadge.animate().alpha(1f).setDuration(400).setStartDelay(700).start();
    }
}
