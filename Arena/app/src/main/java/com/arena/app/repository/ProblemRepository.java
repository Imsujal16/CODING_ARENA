package com.arena.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arena.app.models.ContinueLearning;
import com.arena.app.models.DailyChallengeResponse;
import com.arena.app.models.Problem;
import com.arena.app.models.Roadmap;
import com.arena.app.models.SheetProgressResponse;
import com.arena.app.models.TopicMastery;
import com.arena.app.network.ApiClient;
import com.arena.app.network.ApiService;
import com.arena.app.utils.ClerkAuthHelper;
import com.arena.app.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProblemRepository {
    private static final int PREVIEW_COUNT = 8;
    private static final int CONTINUE_LEARNING_COUNT = 3;

    private final Context appContext;
    private final ApiService apiService;
    private final ClerkAuthHelper authHelper;
    private final ExecutorService executor;
    private final Gson gson;

    private volatile List<Problem> cachedStriverProblems;

    public ProblemRepository(Context context) {
        appContext = context.getApplicationContext();
        apiService = ApiClient.getInstance(appContext).getApiService();
        authHelper = new ClerkAuthHelper(appContext);
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();
    }

    public LiveData<Problem> getDailyChallenge() {
        MutableLiveData<Problem> data = new MutableLiveData<>();
        data.setValue(Problem.getDailyChallenge());

        apiService.getDailyChallenge().enqueue(new Callback<DailyChallengeResponse>() {
            @Override
            public void onResponse(Call<DailyChallengeResponse> call, Response<DailyChallengeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(mapDailyChallenge(response.body()));
                }
            }

            @Override
            public void onFailure(Call<DailyChallengeResponse> call, Throwable t) {
                // Keep the fallback already posted.
            }
        });

        return data;
    }

    public LiveData<List<ContinueLearning>> getContinueLearningList() {
        MutableLiveData<List<ContinueLearning>> data = new MutableLiveData<>();
        loadSheetDerived(data, this::buildContinueLearning, ContinueLearning.getMockData());
        return data;
    }

    public LiveData<List<Problem>> getUpNextProblems() {
        MutableLiveData<List<Problem>> data = new MutableLiveData<>();
        loadSheetDerived(data, this::buildUpNextProblems, Problem.getUpNextProblems());
        return data;
    }

    public LiveData<List<Roadmap>> getRoadmaps() {
        MutableLiveData<List<Roadmap>> data = new MutableLiveData<>();
        loadSheetDerived(data, this::buildRoadmaps, Roadmap.getMockRoadmaps());
        return data;
    }

    public LiveData<List<TopicMastery>> getTopicMasteries() {
        MutableLiveData<List<TopicMastery>> data = new MutableLiveData<>();
        loadSheetDerived(data, this::buildTopicMasteries, TopicMastery.getMockTopics());
        return data;
    }

    public LiveData<List<Problem>> getRecentActivity() {
        MutableLiveData<List<Problem>> data = new MutableLiveData<>();
        loadSheetDerived(data, this::buildRecentActivity, Problem.getRecentActivity());
        return data;
    }

    private <T> void loadSheetDerived(MutableLiveData<T> liveData,
                                      BiFunction<List<Problem>, SheetProgressSnapshot, T> mapper,
                                      T fallback) {
        executor.execute(() -> {
            List<Problem> problems = loadLocalStriverProblems();
            if (problems.isEmpty()) {
                liveData.postValue(fallback);
                return;
            }

            liveData.postValue(mapper.apply(problems, SheetProgressSnapshot.empty()));

            apiService.getStriverProgress().enqueue(new Callback<SheetProgressResponse>() {
                @Override
                public void onResponse(Call<SheetProgressResponse> call, Response<SheetProgressResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        liveData.postValue(mapper.apply(problems, resolveCurrentUserProgress(response.body())));
                    }
                }

                @Override
                public void onFailure(Call<SheetProgressResponse> call, Throwable t) {
                    // Local JSON fallback is already displayed.
                }
            });
        });
    }

    private List<Problem> loadLocalStriverProblems() {
        if (cachedStriverProblems != null) {
            return cachedStriverProblems;
        }

        synchronized (this) {
            if (cachedStriverProblems != null) {
                return cachedStriverProblems;
            }

            try (InputStream inputStream = appContext.getAssets().open(Constants.STRIVER_SHEET_ASSET)) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<Problem>>() {}.getType();
                List<Problem> parsedProblems = gson.fromJson(json, listType);
                cachedStriverProblems = normalizeProblems(parsedProblems);
                return cachedStriverProblems;
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }
    }

    private List<Problem> normalizeProblems(List<Problem> parsedProblems) {
        if (parsedProblems == null) {
            return Collections.emptyList();
        }

        List<Problem> normalized = new ArrayList<>();
        for (int index = 0; index < parsedProblems.size(); index++) {
            Problem raw = parsedProblems.get(index);
            if (raw == null || raw.getTitle() == null || raw.getTitle().trim().isEmpty()) {
                continue;
            }

            Problem uiProblem = new Problem();
            uiProblem.setId(index + 1);
            uiProblem.setNumber(index + 1);
            uiProblem.setTitle(raw.getTitle().trim());
            uiProblem.setSlug(normalizeSlug(raw.getSlug(), raw.getTitle()));
            uiProblem.setUrl(raw.getUrl());
            uiProblem.setPlatform(raw.getPlatform());
            uiProblem.setStep(raw.getStep());
            uiProblem.setDescription(raw.getDescription());
            uiProblem.setTopic(raw.getTopic());
            uiProblem.setDifficulty("Medium");
            uiProblem.setXpReward(20);
            normalized.add(uiProblem);
        }
        return normalized;
    }

    private SheetProgressSnapshot resolveCurrentUserProgress(SheetProgressResponse response) {
        if (response == null || response.getUsers() == null || response.getUsers().isEmpty()) {
            return SheetProgressSnapshot.empty();
        }

        String currentUserId = safeLower(authHelper.getUserId());
        String linkedUsername = safeLower(authHelper.getLeetcodeUsername());

        for (SheetProgressResponse.SheetUserProgress user : response.getUsers()) {
            if (user == null) {
                continue;
            }

            boolean matchesUserId = currentUserId != null
                    && (currentUserId.equals(safeLower(user.getUserId()))
                    || currentUserId.equals(safeLower(user.getClerkId())));
            boolean matchesUsername = linkedUsername != null
                    && linkedUsername.equals(safeLower(user.getUsername()));

            if (matchesUserId || matchesUsername) {
                return new SheetProgressSnapshot(
                        toSet(user.getCompleted()),
                        toSet(user.getInProgress())
                );
            }
        }

        return SheetProgressSnapshot.empty();
    }

    private Problem mapDailyChallenge(DailyChallengeResponse response) {
        Problem problem = new Problem();
        problem.setId(parseInt(response.getQuestionId()));
        problem.setNumber(parseInt(response.getQuestionId()));
        problem.setTitle(response.getTitle());
        problem.setSlug(response.getTitleSlug());
        problem.setDifficulty(response.getDifficulty());
        problem.setTopic("Daily Challenge");
        problem.setDescription("Solve today's LeetCode daily challenge and keep your streak alive.");
        problem.setXpReward(defaultXp(problem.getDifficulty()));
        return problem;
    }

    private List<ContinueLearning> buildContinueLearning(List<Problem> problems,
                                                         SheetProgressSnapshot snapshot) {
        Map<String, List<Problem>> topics = groupProblemsByTopic(problems);
        List<ContinueLearning> items = new ArrayList<>();

        for (Map.Entry<String, List<Problem>> entry : topics.entrySet()) {
            if (items.size() >= CONTINUE_LEARNING_COUNT) {
                break;
            }

            List<Problem> topicProblems = entry.getValue();
            Problem nextProblem = findNextProblem(topicProblems, snapshot.completed);
            int completedCount = countCompleted(topicProblems, snapshot.completed);
            int progressPercent = toPercent(completedCount, topicProblems.size());

            items.add(new ContinueLearning(
                    entry.getKey(),
                    formatTopicTitle(entry.getKey()),
                    nextProblem.getTitle(),
                    compactTopicTag(entry.getKey()),
                    progressPercent
            ));
        }

        return items.isEmpty() ? ContinueLearning.getMockData() : items;
    }

    private List<Roadmap> buildRoadmaps(List<Problem> problems, SheetProgressSnapshot snapshot) {
        List<Roadmap> roadmaps = Roadmap.getMockRoadmaps();
        if (roadmaps.isEmpty()) {
            return roadmaps;
        }

        int completedCount = countCompleted(problems, snapshot.completed);
        int progressPercent = toPercent(completedCount, problems.size());

        Roadmap striverRoadmap = roadmaps.get(0);
        striverRoadmap.setProgress(progressPercent);
        striverRoadmap.setSubtitle(String.format("%d of %d bundled Striver problems completed.",
                completedCount, problems.size()));

        return roadmaps;
    }

    private List<TopicMastery> buildTopicMasteries(List<Problem> problems,
                                                   SheetProgressSnapshot snapshot) {
        Map<String, List<Problem>> topics = groupProblemsByTopic(problems);
        List<TopicMastery> items = new ArrayList<>();

        for (Map.Entry<String, List<Problem>> entry : topics.entrySet()) {
            List<Problem> topicProblems = entry.getValue();
            int completedCount = countCompleted(topicProblems, snapshot.completed);
            Problem nextProblem = findNextProblem(topicProblems, snapshot.completed);

            items.add(new TopicMastery(
                    entry.getKey(),
                    formatTopicTitle(entry.getKey()),
                    completedCount,
                    topicProblems.size(),
                    completedCount == topicProblems.size() ? "All problems solved" : nextProblem.getTitle()
            ));
        }

        return items.isEmpty() ? TopicMastery.getMockTopics() : items;
    }

    private List<Problem> buildUpNextProblems(List<Problem> problems, SheetProgressSnapshot snapshot) {
        List<Problem> items = new ArrayList<>();

        for (Problem problem : problems) {
            if (items.size() >= PREVIEW_COUNT) {
                break;
            }

            if (!snapshot.completed.contains(problem.getSlug())) {
                items.add(toUiProblem(problem, false, null));
            }
        }

        if (items.isEmpty()) {
            for (int index = 0; index < problems.size() && items.size() < PREVIEW_COUNT; index++) {
                items.add(toUiProblem(problems.get(index), false, null));
            }
        }

        return items.isEmpty() ? Problem.getUpNextProblems() : items;
    }

    private List<Problem> buildRecentActivity(List<Problem> problems, SheetProgressSnapshot snapshot) {
        if (snapshot.completed.isEmpty()) {
            return Problem.getRecentActivity();
        }

        List<Problem> items = new ArrayList<>();
        for (int index = problems.size() - 1; index >= 0 && items.size() < 4; index--) {
            Problem problem = problems.get(index);
            if (snapshot.completed.contains(problem.getSlug())) {
                items.add(toUiProblem(problem, true, "Synced completion"));
            }
        }

        return items.isEmpty() ? Problem.getRecentActivity() : items;
    }

    private Problem toUiProblem(Problem source, boolean solved, String solvedTimeAgo) {
        Problem item = new Problem();
        item.setId(source.getId());
        item.setNumber(source.getNumber());
        item.setTitle(source.getTitle());
        item.setSlug(source.getSlug());
        item.setUrl(source.getUrl());
        item.setPlatform(source.getPlatform());
        item.setStep(source.getStep());
        item.setDescription(source.getDescription());
        item.setDifficulty(source.getDifficulty());
        item.setTopic(formatTopicTitle(source.getTopic()));
        item.setXpReward(source.getXpReward());
        item.setSolved(solved);
        item.setSolvedTimeAgo(solvedTimeAgo);
        return item;
    }

    private Map<String, List<Problem>> groupProblemsByTopic(List<Problem> problems) {
        Map<String, List<Problem>> grouped = new LinkedHashMap<>();
        for (Problem problem : problems) {
            grouped.computeIfAbsent(problem.getTopic(), key -> new ArrayList<>()).add(problem);
        }
        return grouped;
    }

    private Problem findNextProblem(List<Problem> problems, Set<String> completedSlugs) {
        for (Problem problem : problems) {
            if (!completedSlugs.contains(problem.getSlug())) {
                return problem;
            }
        }
        return problems.isEmpty() ? Problem.getDailyChallenge() : problems.get(0);
    }

    private int countCompleted(List<Problem> problems, Set<String> completedSlugs) {
        int count = 0;
        for (Problem problem : problems) {
            if (completedSlugs.contains(problem.getSlug())) {
                count++;
            }
        }
        return count;
    }

    private int toPercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 100f) / denominator);
    }

    private String formatTopicTitle(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return "Striver Sheet";
        }

        switch (topic) {
            case "Learn the basics":
                return "Basics";
            case "Solve Problems on Arrays [Easy -> Medium -> Hard]":
                return "Arrays";
            case "Binary Search [1D, 2D Arrays, Search Space]":
                return "Binary Search";
            case "Strings [Basic and Medium]":
                return "Strings";
            case "Learn LinkedList [Single LL, Double LL, Medium, Hard Problems]":
                return "Linked List";
            case "Recursion [PatternWise]":
                return "Recursion";
            case "Bit Manipulation [Concepts & Problems]":
                return "Bit Manipulation";
            case "Stack and Queues [Learning, Pre-In-Post-fix, Monotonic Stack, Implementation]":
                return "Stacks & Queues";
            case "Sliding Window & Two Pointer Combined Problems":
                return "Sliding Window";
            case "Heaps [Learning, Medium, Hard Problems]":
                return "Heaps";
            case "Greedy Algorithms [Easy, Medium/Hard]":
                return "Greedy";
            case "Binary Trees [Traversals, Medium and Hard Problems]":
                return "Binary Trees";
            case "Binary Search Trees [Concept and Problems]":
                return "BST";
            case "Graphs [Concepts & Problems]":
                return "Graphs";
            case "Dynamic Programming [Patterns and Problems]":
                return "Dynamic Programming";
            case "Strings [Hard Problems]":
                return "Strings";
            default:
                int bracketIndex = topic.indexOf('[');
                return bracketIndex > 0 ? topic.substring(0, bracketIndex).trim() : topic.trim();
        }
    }

    private String compactTopicTag(String topic) {
        String formatted = formatTopicTitle(topic);
        if (formatted.length() <= 18) {
            return formatted;
        }

        switch (formatted) {
            case "Dynamic Programming":
                return "DP";
            case "Bit Manipulation":
                return "Bits";
            case "Stacks & Queues":
                return "Stacks";
            default:
                return formatted.substring(0, 18).trim();
        }
    }

    private String normalizeSlug(String slug, String title) {
        if (slug != null && !slug.trim().isEmpty()) {
            return slug.trim();
        }

        return title.toLowerCase()
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private int defaultXp(String difficulty) {
        if (difficulty == null) {
            return 20;
        }

        switch (difficulty.toLowerCase()) {
            case "easy":
                return 10;
            case "hard":
                return 30;
            case "medium":
            default:
                return 20;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Set<String> toSet(List<String> values) {
        return values == null ? Collections.emptySet() : new HashSet<>(values);
    }

    private String safeLower(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static class SheetProgressSnapshot {
        private final Set<String> completed;
        private final Set<String> inProgress;

        private SheetProgressSnapshot(Set<String> completed, Set<String> inProgress) {
            this.completed = completed;
            this.inProgress = inProgress;
        }

        private static SheetProgressSnapshot empty() {
            return new SheetProgressSnapshot(Collections.emptySet(), Collections.emptySet());
        }
    }
}
