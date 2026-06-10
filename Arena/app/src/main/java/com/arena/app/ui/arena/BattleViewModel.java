package com.arena.app.ui.arena;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arena.app.models.BattleResult;
import com.arena.app.network.SocketManager;
import com.arena.app.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity-scoped ViewModel — survives navigation between ArenaFragment,
 * ProblemSolverFragment, and BattleResultFragment so socket events are
 * never dropped mid-battle.
 */
public class BattleViewModel extends AndroidViewModel {

    private static final String TAG = "BattleViewModel";

    public enum BattleStatus {
        IDLE, CONNECTING, WAITING, OPPONENT_JOINED, VOTING, IN_PROGRESS, ENDED, ERROR
    }

    private final MutableLiveData<BattleStatus> status       = new MutableLiveData<>(BattleStatus.IDLE);
    private final MutableLiveData<String>        roomCode     = new MutableLiveData<>();
    private final MutableLiveData<String>        opponentName = new MutableLiveData<>();
    private final MutableLiveData<JSONObject>    problem      = new MutableLiveData<>();
    private final MutableLiveData<BattleResult>  result       = new MutableLiveData<>();
    private final MutableLiveData<String>        error        = new MutableLiveData<>();
    
    // MCQ specific LiveData
    private final MutableLiveData<java.util.List<String>> votingTopics = new MutableLiveData<>();
    private final MutableLiveData<String> selectedTopic = new MutableLiveData<>();
    private final MutableLiveData<String> battleMode = new MutableLiveData<>(); // "coding" or "mcq"
    private final MutableLiveData<org.json.JSONArray> mcqQuestions = new MutableLiveData<>();

    private final SocketManager socket;
    private boolean listenersRegistered = false;

    public BattleViewModel(@NonNull Application application) {
        super(application);
        socket = SocketManager.getInstance();
    }

    // ── Register socket listeners once ───────────────────────────────────────
    private void ensureListeners() {
        if (listenersRegistered) return;
        listenersRegistered = true;

        socket.on("room:created", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                JSONObject room = data.getJSONObject("room");
                roomCode.postValue(room.getString("code"));
                status.postValue(BattleStatus.WAITING);
                Log.d(TAG, "Room created: " + room.getString("code"));
            } catch (JSONException e) {
                Log.e(TAG, "room:created parse error", e);
            }
        });

        socket.on("room:joined", args -> {
            try {
                JSONObject data   = (JSONObject) args[0];
                JSONObject player = data.getJSONObject("player");
                opponentName.postValue(player.optString("username", "Opponent"));
                status.postValue(BattleStatus.OPPONENT_JOINED);
            } catch (JSONException e) {
                Log.e(TAG, "room:joined parse error", e);
            }
        });

        socket.on("match:waiting", args -> status.postValue(BattleStatus.WAITING));

        socket.on("battle:voting_start", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                org.json.JSONArray topicsArray = data.getJSONArray("topics");
                java.util.List<String> list = new java.util.ArrayList<>();
                for (int i=0; i<topicsArray.length(); i++) list.add(topicsArray.getString(i));
                votingTopics.postValue(list);
                status.postValue(BattleStatus.VOTING);
            } catch (JSONException e) {
                Log.e(TAG, "battle:voting_start parse error", e);
            }
        });

        socket.on("topic:selected", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                selectedTopic.postValue(data.getString("topic"));
            } catch (JSONException e) {
                Log.e(TAG, "topic:selected parse error", e);
            }
        });

        socket.on("battle:start", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String mode = data.optString("mode", "coding");
                battleMode.postValue(mode);
                
                if (mode.equals("mcq")) {
                    mcqQuestions.postValue(data.getJSONArray("questions"));
                } else {
                    problem.postValue(data.getJSONObject("problem"));
                }
                
                status.postValue(BattleStatus.IN_PROGRESS);
                Log.d(TAG, "Battle started in mode: " + mode);
            } catch (JSONException e) {
                Log.e(TAG, "battle:start parse error", e);
            }
        });

        socket.on("battle:end", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                BattleResult r  = new BattleResult(
                        data.optBoolean("won",      false),
                        data.optString("time",      "00:00"),
                        data.optInt("accuracy",     0),
                        data.optInt("xpGained",     25),
                        data.optInt("coins",        0)
                );
                r.setOpponentName(data.optString("opponentName", "Opponent"));
                r.setProblemTitle(data.optString("problemTitle", ""));
                r.setProblemSlug(data.optString("problemSlug",   ""));
                r.setReason(data.optString("reason", ""));
                result.postValue(r);
                status.postValue(BattleStatus.ENDED);
            } catch (Exception e) {
                Log.e(TAG, "battle:end parse error", e);
            }
        });

        socket.on("room:error", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                error.postValue(data.optString("message", "An error occurred"));
                status.postValue(BattleStatus.ERROR);
            } catch (Exception ignored) {}
        });
    }

    // ── Public API ───────────────────────────────────────────────────────────
    public void createRoom(String userId, String username, String avatarUrl,
                           String leetcodeUsername, String difficulty, int entryFee, String mode) {
        status.postValue(BattleStatus.CONNECTING);
        socket.connect();
        ensureListeners();

        try {
            JSONObject payload = new JSONObject();
            payload.put("userId",          userId);
            payload.put("username",        username);
            payload.put("avatarUrl",       avatarUrl       != null ? avatarUrl       : "");
            payload.put("leetcodeUsername",leetcodeUsername!= null ? leetcodeUsername: username);
            payload.put("difficulty",      difficulty);
            payload.put("entryFee",        entryFee);
            payload.put("mode",            mode != null ? mode : "coding");
            payload.put("duration",        30);
            payload.put("isHardcore",      false);
            socket.emit("room:create", payload);
        } catch (JSONException e) {
            Log.e(TAG, "room:create emit failed", e);
            error.postValue("Failed to connect to battle server");
            status.postValue(BattleStatus.ERROR);
        }
    }

    public void joinRoom(String code, String userId, String username,
                         String avatarUrl, String leetcodeUsername) {
        status.postValue(BattleStatus.CONNECTING);
        socket.connect();
        ensureListeners();

        try {
            JSONObject payload = new JSONObject();
            payload.put("roomCode",        code);
            payload.put("userId",          userId);
            payload.put("username",        username);
            payload.put("avatarUrl",       avatarUrl       != null ? avatarUrl       : "");
            payload.put("leetcodeUsername",leetcodeUsername!= null ? leetcodeUsername: username);
            socket.emit("room:join", payload);
        } catch (JSONException e) {
            Log.e(TAG, "room:join emit failed", e);
            error.postValue("Failed to join room");
            status.postValue(BattleStatus.ERROR);
        }
    }

    public void findMatch(String userId, String username, String avatarUrl,
                          String leetcodeUsername, String difficulty, int entryFee, String mode) {
        status.postValue(BattleStatus.CONNECTING);
        socket.connect();
        ensureListeners();

        try {
            JSONObject payload = new JSONObject();
            payload.put("userId",          userId);
            payload.put("username",        username);
            payload.put("avatarUrl",       avatarUrl       != null ? avatarUrl       : "");
            payload.put("leetcodeUsername",leetcodeUsername!= null ? leetcodeUsername: username);
            payload.put("difficulty",      difficulty);
            payload.put("entryFee",        entryFee);
            payload.put("mode",            mode != null ? mode : "coding");
            socket.emit("match:find", payload);
        } catch (JSONException e) {
            Log.e(TAG, "match:find emit failed", e);
            error.postValue("Failed to find opponent");
            status.postValue(BattleStatus.ERROR);
        }
    }

    public void reset() {
        socket.disconnect();
        // Remove listeners
        socket.off("room:created");
        socket.off("room:joined");
        socket.off("battle:start");
        socket.off("battle:end");
        socket.off("battle:voting_start");
        socket.off("topic:selected");
        socket.off("match:waiting");
        socket.off("room:error");
        listenersRegistered = false;
        status.postValue(BattleStatus.IDLE);
        roomCode.postValue(null);
        opponentName.postValue(null);
        problem.postValue(null);
        result.postValue(null);
        error.postValue(null);
        votingTopics.postValue(null);
        selectedTopic.postValue(null);
        battleMode.postValue(null);
        mcqQuestions.postValue(null);
    }

    public void prepareDemoMatch(String mode, String opponent) {
        battleMode.setValue(mode != null ? mode : "coding");
        opponentName.setValue(opponent != null ? opponent : "CodeWarrior_42");
        status.setValue(BattleStatus.OPPONENT_JOINED);
    }

    public void startDemoBattle(String mode, String topic, String opponent) {
        String resolvedMode = mode != null ? mode : "coding";
        battleMode.setValue(resolvedMode);
        opponentName.setValue(opponent != null ? opponent : "CodeWarrior_42");
        selectedTopic.setValue(topic);

        try {
            if ("mcq".equals(resolvedMode)) {
                mcqQuestions.setValue(buildDemoMcqQuestions(topic));
            } else {
                problem.setValue(buildDemoProblem(topic));
            }
            status.setValue(BattleStatus.IN_PROGRESS);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to start demo battle", e);
            error.setValue("Unable to start demo battle");
            status.setValue(BattleStatus.ERROR);
        }
    }

    // ── LiveData getters ──────────────────────────────────────────────────────
    public LiveData<BattleStatus> getStatus()       { return status;       }
    public LiveData<String>       getRoomCode()     { return roomCode;     }
    public LiveData<String>       getOpponentName() { return opponentName; }
    public LiveData<JSONObject>   getProblem()      { return problem;      }
    public LiveData<BattleResult> getResult()       { return result;       }
    public LiveData<String>       getError()        { return error;        }
    
    public LiveData<java.util.List<String>> getVotingTopics() { return votingTopics; }
    public LiveData<String>                 getSelectedTopic() { return selectedTopic; }
    public LiveData<String>                 getBattleMode() { return battleMode; }
    public LiveData<org.json.JSONArray>     getMcqQuestions() { return mcqQuestions; }
    
    public void submitTopicVote(String topic) {
        try {
            JSONObject p = new JSONObject();
            p.put("topic", topic);
            socket.emit("topic:vote", p);
        } catch (Exception ignored) {}
    }

    private JSONObject buildDemoProblem(String topic) throws JSONException {
        JSONObject p = new JSONObject();
        p.put("title", "Two Sum");
        p.put("titleSlug", "two-sum");
        p.put("difficulty", "Easy");
        p.put("topic", topic != null ? topic : "Arrays & Hashing");
        p.put("url", "https://leetcode.com/problems/two-sum/");
        return p;
    }

    private org.json.JSONArray buildDemoMcqQuestions(String topic) throws JSONException {
        org.json.JSONArray questions = new org.json.JSONArray();
        questions.put(new JSONObject()
                .put("question", "Which data structure is usually best for O(1) lookup in Two Sum?")
                .put("options", new org.json.JSONArray()
                        .put("HashMap")
                        .put("Stack")
                        .put("Queue")
                        .put("LinkedList"))
                .put("answer", "HashMap"));
        questions.put(new JSONObject()
                .put("question", "What does dynamic programming primarily avoid?")
                .put("options", new org.json.JSONArray()
                        .put("Repeated subproblems")
                        .put("All loops")
                        .put("Variables")
                        .put("Function calls"))
                .put("answer", "Repeated subproblems"));
        questions.put(new JSONObject()
                .put("question", "Which traversal uses a FIFO queue?")
                .put("options", new org.json.JSONArray()
                        .put("BFS")
                        .put("DFS")
                        .put("Binary search")
                        .put("Merge sort"))
                .put("answer", "BFS"));
        questions.put(new JSONObject()
                .put("question", "Selected demo topic: " + (topic != null ? topic : "Arrays & Hashing") + ". Which choice means logarithmic time?")
                .put("options", new org.json.JSONArray()
                        .put("O(log n)")
                        .put("O(n)")
                        .put("O(n^2)")
                        .put("O(1)"))
                .put("answer", "O(log n)"));
        return questions;
    }
    
    public void submitMcqScore(int score) {
        try {
            JSONObject p = new JSONObject();
            p.put("score", score);
            socket.emit("mcq:submit", p);
        } catch (Exception ignored) {}

        if (!isDemoMode()) {
            return;
        }

        org.json.JSONArray questions = mcqQuestions.getValue();
        if (questions != null) {
            int total = Math.max(questions.length(), 1);
            BattleResult demoResult = new BattleResult(
                    score >= Math.ceil(total / 2.0),
                    "01:00",
                    Math.round((score * 100f) / total),
                    50 + (score * 25),
                    score * 10
            );
            demoResult.setOpponentName(opponentName.getValue() != null
                    ? opponentName.getValue()
                    : "CodeWarrior_42");
            demoResult.setProblemTitle(selectedTopic.getValue() != null
                    ? selectedTopic.getValue()
                    : "Topic Vote");
            demoResult.setReason("Demo battle complete");
            result.setValue(demoResult);
            status.setValue(BattleStatus.ENDED);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        reset();
    }

    private boolean isDemoMode() {
        return getApplication().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_BATTLE_DEMO_MODE, true);
    }
}
