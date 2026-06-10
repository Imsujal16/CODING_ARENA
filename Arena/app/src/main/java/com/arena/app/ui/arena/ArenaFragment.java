package com.arena.app.ui.arena;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.arena.app.R;
import com.arena.app.models.BattleResult;
import com.arena.app.models.Problem;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.ui.solver.ProblemSolverFragment;
import com.arena.app.utils.ClerkAuthHelper;
import com.arena.app.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ArenaFragment extends Fragment {

    private BattleViewModel battleVm;
    private ClerkAuthHelper authHelper;
    private LocalProfileStore profileStore;
    private String selectedDifficulty = "MEDIUM";
    private String selectedMode = "coding";
    private int betAmount = 10;
    private AlertDialog matchmakingDialog;
    private SharedPreferences prefs;
    private boolean demoMode = true;
    private boolean navigatingFromBattleState = false;
    private boolean autoMatchRequested = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_arena, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Activity-scoped so it survives navigation to ProblemSolver
        battleVm     = new ViewModelProvider(requireActivity()).get(BattleViewModel.class);
        authHelper   = new ClerkAuthHelper(requireContext());
        profileStore = new LocalProfileStore(requireContext());
        prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        demoMode = prefs.getBoolean(Constants.PREF_BATTLE_DEMO_MODE, true);

        bindPlayerSummary(view);
        setupDeveloperBattleMode(view);
        setupDifficultySelection(view);
        setupModeSelection(view);
        setupBetSlider(view);
        setupFightButton(view);
        observeBattleState(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
            matchmakingDialog.dismiss();
        }
    }

    // â”€â”€ Difficulty chips â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupDifficultySelection(View view) {
        TextView chipEasy   = view.findViewById(R.id.chip_easy);
        TextView chipMedium = view.findViewById(R.id.chip_medium);
        TextView chipHard   = view.findViewById(R.id.chip_hard);

        View.OnClickListener listener = v -> {
            chipEasy.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipEasy.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chipMedium.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipMedium.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chipHard.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipHard.setTextColor(getResources().getColor(R.color.text_secondary, null));

            ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
            ((TextView) v).setTextColor(getResources().getColor(R.color.text_white, null));

            if (v.getId() == R.id.chip_easy)        selectedDifficulty = "EASY";
            else if (v.getId() == R.id.chip_medium) selectedDifficulty = "MEDIUM";
            else                                    selectedDifficulty = "HARD";
        };

        chipEasy.setOnClickListener(listener);
        chipMedium.setOnClickListener(listener);
        chipHard.setOnClickListener(listener);
    }

    // â”€â”€ Bet slider â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupBetSlider(View view) {
        Slider   slider  = view.findViewById(R.id.slider_bet);
        TextView textBet = view.findViewById(R.id.text_bet_amount);
        betAmount = (int) slider.getValue();
        textBet.setText(String.valueOf(betAmount));
        slider.addOnChangeListener((s, value, fromUser) -> {
            betAmount = (int) value;
            textBet.setText(String.valueOf(betAmount));
        });
    }

    // â”€â”€ Mode selection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupModeSelection(View view) {
        selectedMode = "coding";

        MaterialCardView cardCoding = view.findViewById(R.id.card_mode_coding);
        MaterialCardView cardMcq = view.findViewById(R.id.card_mode_mcq);
        TextView codingBadge = view.findViewById(R.id.text_coding_badge);
        TextView mcqBadge = view.findViewById(R.id.text_mcq_badge);

        View.OnClickListener cardListener = v -> {
            selectedMode = v.getId() == R.id.card_mode_mcq ? "mcq" : "coding";
            updateModeCards(cardCoding, cardMcq, codingBadge, mcqBadge);
        };
        cardCoding.setOnClickListener(cardListener);
        cardMcq.setOnClickListener(cardListener);

        com.google.android.material.chip.ChipGroup group = view.findViewById(R.id.chipGroup_mode);
        if (group != null) {
            group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_mode_mcq) {
                    selectedMode = "mcq";
                } else {
                    selectedMode = "coding";
                }
                updateModeCards(cardCoding, cardMcq, codingBadge, mcqBadge);
            });
        }
        updateModeCards(cardCoding, cardMcq, codingBadge, mcqBadge);
    }

    private void updateModeCards(MaterialCardView cardCoding, MaterialCardView cardMcq,
                                 TextView codingBadge, TextView mcqBadge) {
        boolean coding = "coding".equals(selectedMode);
        applyModeCardStyle(cardCoding, coding);
        applyModeCardStyle(cardMcq, !coding);

        codingBadge.setText(coding ? "SELECTED" : "TAP");
        codingBadge.setTextColor(ContextCompat.getColor(requireContext(),
                coding ? R.color.text_white : R.color.text_secondary));
        codingBadge.setBackgroundResource(coding ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);

        mcqBadge.setText(coding ? "TAP" : "SELECTED");
        mcqBadge.setTextColor(ContextCompat.getColor(requireContext(),
                coding ? R.color.text_secondary : R.color.text_white));
        mcqBadge.setBackgroundResource(coding ? R.drawable.bg_chip_unselected : R.drawable.bg_chip_selected);
    }

    private void applyModeCardStyle(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? 2 : 1);
        card.setStrokeColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.primary_blue : R.color.divider));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.leaderboard_highlight : R.color.card_background));
    }

    // â”€â”€ Fight button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void setupFightButton(View view) {
        MaterialButton btnFight = view.findViewById(R.id.btn_fight);
        btnFight.setOnClickListener(v -> startMatchmaking());
        view.findViewById(R.id.btn_join_room).setOnClickListener(v -> joinRealRoom(view));
    }

    private void setupDeveloperBattleMode(View view) {
        SwitchMaterial switchReal = view.findViewById(R.id.switch_real_battle);
        View joinLayout = view.findViewById(R.id.layout_real_join);
        TextView statusPill = view.findViewById(R.id.text_status_pill);
        TextView subtitle = view.findViewById(R.id.text_dev_mode_subtitle);
        MaterialButton fightButton = view.findViewById(R.id.btn_fight);

        switchReal.setChecked(!demoMode);
        updateDeveloperModeUi(statusPill, subtitle, joinLayout, fightButton);
        switchReal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            demoMode = !isChecked;
            prefs.edit().putBoolean(Constants.PREF_BATTLE_DEMO_MODE, demoMode).apply();
            updateDeveloperModeUi(statusPill, subtitle, joinLayout, fightButton);
        });
    }

    private void updateDeveloperModeUi(TextView statusPill, TextView subtitle, View joinLayout,
                                       MaterialButton fightButton) {
        statusPill.setText(demoMode ? "DEMO" : "REAL");
        subtitle.setText(demoMode
                ? "Practice against the demo opponent with the full vote and battle flow."
                : "Create a private room, join by code, or auto-match with another player.");
        joinLayout.setVisibility(demoMode ? View.GONE : View.VISIBLE);
        fightButton.setText(demoMode ? "Find Demo Opponent" : "Start Live Battle");
    }

    private void bindPlayerSummary(View view) {
        com.arena.app.models.User user = profileStore.getDefaultProfile();
        String username = user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? user.getUsername().trim()
                : "Player";

        ((TextView) view.findViewById(R.id.text_user_name)).setText(username);
        ((TextView) view.findViewById(R.id.text_user_rank)).setText(user != null ? user.getRank() : "Ready for battle");
        ((TextView) view.findViewById(R.id.badge_user_level)).setText(user != null ? "LVL " + user.getLevel() : "LVL 1");
        ((TextView) view.findViewById(R.id.text_user_avatar_letter)).setText(username.substring(0, 1).toUpperCase(java.util.Locale.US));
    }

    private void startMatchmaking() {
        if (!demoMode) {
            showRealBattleOptions();
            return;
        }

        // Reset any previous session
        battleVm.reset();
        navigatingFromBattleState = false;
        autoMatchRequested = false;

        String userId           = authHelper.getUserId();
        String leetcodeUsername = authHelper.getLeetcodeUsername();
        String username         = resolveUsername(leetcodeUsername);

        if (userId == null) userId = "guest_" + System.currentTimeMillis();

        showMatchmakingDialog(username);
    }

    private void showRealBattleOptions() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Start live battle")
                .setMessage("Choose how you want to find your opponent.")
                .setPositiveButton("Auto match", (dialog, which) -> startAutoMatch())
                .setNegativeButton("Create room", (dialog, which) -> startPrivateRoom())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void startPrivateRoom() {
        battleVm.reset();
        navigatingFromBattleState = false;
        autoMatchRequested = false;

        String userId           = authHelper.getUserId();
        String leetcodeUsername = authHelper.getLeetcodeUsername();
        String username         = resolveUsername(leetcodeUsername);
        if (userId == null) userId = "guest_" + System.currentTimeMillis();

        showMatchmakingDialog(username);
        battleVm.createRoom(userId, username, null, leetcodeUsername,
                selectedDifficulty.toLowerCase(java.util.Locale.US), betAmount, selectedMode);
    }

    private void startAutoMatch() {
        battleVm.reset();
        navigatingFromBattleState = false;
        autoMatchRequested = true;

        String userId           = authHelper.getUserId();
        String leetcodeUsername = authHelper.getLeetcodeUsername();
        String username         = resolveUsername(leetcodeUsername);
        if (userId == null) userId = "guest_" + System.currentTimeMillis();

        showMatchmakingDialog(username);
        battleVm.findMatch(userId, username, null, leetcodeUsername,
                selectedDifficulty.toLowerCase(java.util.Locale.US), betAmount, selectedMode);
    }

    // â”€â”€ Matchmaking dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void showMatchmakingDialog(String myUsername) {
        matchmakingDialog = new AlertDialog.Builder(requireContext())
                .setTitle(demoMode
                        ? "Finding Demo Opponent"
                        : autoMatchRequested ? "Finding Live Opponent" : "Creating Live Room")
                .setMessage(demoMode
                        ? "Preparing a demo opponent for " + myUsername + "."
                        : autoMatchRequested
                                ? "Looking for another player with the same battle setup..."
                                : "Connecting to the live quiz server...")
                .setNegativeButton("Cancel", (d, w) -> {
                    battleVm.reset();
                    d.dismiss();
                })
                .setCancelable(false)
                .create();
        matchmakingDialog.show();

        if (!demoMode) {
            return;
        }

        battleVm.prepareDemoMatch(selectedMode, "CodeWarrior_42");

        // Demo mode moves forward locally without waiting for the server.
        requireView().postDelayed(() -> {
            if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
                matchmakingDialog.dismiss();
            }
            if (!isAdded() || getView() == null) return;
            int dest = Navigation.findNavController(requireView())
                    .getCurrentDestination().getId();
            if (dest == R.id.navigation_arena) {
                Bundle args = new Bundle();
                args.putString(MatchFoundFragment.ARG_MODE, selectedMode);
                args.putString(MatchFoundFragment.ARG_OPPONENT, "CodeWarrior_42");
                args.putString(MatchFoundFragment.ARG_OPP_RANK, "Silver II");
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_arena_to_matchFound, args);
            }
        }, 2000);
    }

    private void joinRealRoom(View view) {
        if (demoMode) {
            return;
        }

        TextInputEditText input = view.findViewById(R.id.input_room_code);
        String code = input.getText() == null
                ? ""
                : input.getText().toString().trim().toUpperCase(java.util.Locale.US);
        if (code.isEmpty()) {
            input.setError("Enter room code");
            return;
        }
        input.setError(null);

        battleVm.reset();
        navigatingFromBattleState = false;
        autoMatchRequested = false;
        String userId = authHelper.getUserId();
        if (userId == null) userId = "guest_" + System.currentTimeMillis();
        String leetcodeUsername = authHelper.getLeetcodeUsername();
        String username = resolveUsername(leetcodeUsername);
        showMatchmakingDialog(username);
        battleVm.joinRoom(code, userId, username, null, leetcodeUsername);
    }

    private void updateMatchmakingMessage(String msg) {
        if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
            matchmakingDialog.setMessage(msg);
        }
    }

    // â”€â”€ Observe battle state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void observeBattleState(View view) {
        battleVm.getStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;
            switch (status) {
                case CONNECTING:
                    updateMatchmakingMessage("Connecting to battle server...");
                    break;

                case WAITING:
                    String code = battleVm.getRoomCode().getValue();
                    if (autoMatchRequested) {
                        updateMatchmakingMessage("Looking for a live opponent...\n\nKeep this screen open. The battle starts automatically when someone matches.");
                    } else {
                        updateMatchmakingMessage(
                                "Room created.\n\nCode: " + code +
                                "\n\nShare this code with your friend. The battle starts when they join.");
                    }
                    if (!autoMatchRequested && matchmakingDialog != null) {
                        // Make message tappable to copy room code
                        matchmakingDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Copy Code",
                                (d, w) -> copyToClipboard(code));
                    }
                    break;

                case OPPONENT_JOINED:
                    if (!demoMode && !navigatingFromBattleState
                            && Navigation.findNavController(requireView()).getCurrentDestination().getId() == R.id.navigation_arena) {
                        navigatingFromBattleState = true;
                        if (matchmakingDialog != null) matchmakingDialog.dismiss();
                        Bundle args = new Bundle();
                        args.putString(MatchFoundFragment.ARG_MODE, selectedMode);
                        args.putString(MatchFoundFragment.ARG_OPPONENT,
                                battleVm.getOpponentName().getValue() != null
                                        ? battleVm.getOpponentName().getValue()
                                        : "Opponent");
                        args.putString(MatchFoundFragment.ARG_OPP_RANK, "Online");
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_arena_to_matchFound, args);
                    }
                    updateMatchmakingMessage("Opponent found. Loading battle...");
                    break;

                case VOTING:
                    if (matchmakingDialog != null) matchmakingDialog.dismiss();
                    if (Navigation.findNavController(requireView()).getCurrentDestination().getId() == R.id.navigation_arena) {
                        Navigation.findNavController(requireView()).navigate(R.id.action_arena_to_topicVoting);
                    }
                    break;
                case IN_PROGRESS:
                    if (matchmakingDialog != null) matchmakingDialog.dismiss();
                    if (Navigation.findNavController(requireView()).getCurrentDestination().getId() == R.id.navigation_arena) {
                        String mode = battleVm.getBattleMode().getValue();
                        if ("mcq".equals(mode)) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_arena_to_mcqBattle);
                        } else {
                            navigateToProblemSolver();
                        }
                    }
                    break;

                case ERROR:
                    if (matchmakingDialog != null && matchmakingDialog.isShowing()) {
                        matchmakingDialog.dismiss();
                    }
                    String errMsg = battleVm.getError().getValue();
                    Toast.makeText(requireContext(),
                            errMsg != null ? errMsg : "Battle error", Toast.LENGTH_LONG).show();
                    break;

                default:
                    break;
            }
        });

        // Also observe battle:end if user somehow ends up back on ArenaFragment
        battleVm.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && battleVm.getStatus().getValue() == BattleViewModel.BattleStatus.ENDED) {
                navigateToBattleResult(result);
            }
        });
    }

    // â”€â”€ Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void navigateToProblemSolver() {
        JSONObject problemJson = battleVm.getProblem().getValue();
        if (problemJson == null) return;

        Problem problem = new Problem();
        problem.setTitle(problemJson.optString("title", "Daily Challenge"));
        problem.setSlug(problemJson.optString("titleSlug", "two-sum"));
        problem.setUrl(problemJson.optString("url",
                "https://leetcode.com/problems/" + problemJson.optString("titleSlug", "two-sum") + "/"));
        problem.setDifficulty(problemJson.optString("difficulty", "Medium"));
        problem.setDescription("Solve this LeetCode problem to win the battle.\n\n" +
                "The server checks your LeetCode submissions every 15 seconds.\n" +
                "First player to submit an Accepted solution wins!");
        problem.setXpReward(250);

        Bundle args = ProblemSolverFragment.createArgs(problem);
        args.putBoolean("isBattleMode", true);
        args.putString("opponentName", battleVm.getOpponentName().getValue());

        Navigation.findNavController(requireView())
                .navigate(R.id.problemSolverFragment, args);
    }

    private void navigateToBattleResult(BattleResult result) {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_arena_to_battleResult, result.toBundle());
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private String resolveUsername(String leetcodeUsername) {
        if (leetcodeUsername != null && !leetcodeUsername.trim().isEmpty()) {
            return leetcodeUsername.trim();
        }
        return profileStore.getDefaultProfile().getUsername() != null
                ? profileStore.getDefaultProfile().getUsername()
                : "Player";
    }

    private void copyToClipboard(String text) {
        if (text == null) return;
        ClipboardManager cm = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Room Code", text));
        Toast.makeText(requireContext(), "Room code copied!", Toast.LENGTH_SHORT).show();
    }
}
