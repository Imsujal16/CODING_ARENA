package com.arena.app.ui.learn;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.adapters.ProblemListAdapter;
import com.arena.app.adapters.RoadmapAdapter;
import com.arena.app.adapters.TopicMasteryAdapter;
import com.arena.app.models.Problem;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.ui.solver.ProblemSolverFragment;

public class LearnFragment extends Fragment {

    private LearnViewModel viewModel;
    private RoadmapAdapter roadmapAdapter;
    private TopicMasteryAdapter topicMasteryAdapter;
    private ProblemListAdapter problemListAdapter;
    private LocalProfileStore localProfileStore;
    private User currentUser;
    private TextView loadingText;
    private java.util.List<Problem> allUpNextProblems = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LearnViewModel.class);
        localProfileStore = new LocalProfileStore(requireContext());
        loadingText = view.findViewById(R.id.text_learn_loading);

        setupRecyclerViews(view);
        setupActions(view);
        observeData(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && currentUser != null) {
            bindUser(getView(), localProfileStore.getEffectiveProfile(currentUser));
        }
    }

    private void setupRecyclerViews(View view) {
        RecyclerView recyclerRoadmaps = view.findViewById(R.id.recycler_roadmaps);
        roadmapAdapter = new RoadmapAdapter();
        recyclerRoadmaps.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerRoadmaps.setAdapter(roadmapAdapter);
        roadmapAdapter.setListener(roadmap -> {
            Navigation.findNavController(view).navigate(R.id.action_learn_to_roadmapDetail);
        });

        RecyclerView recyclerTopics = view.findViewById(R.id.recycler_topic_mastery);
        topicMasteryAdapter = new TopicMasteryAdapter();
        recyclerTopics.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTopics.setAdapter(topicMasteryAdapter);
        topicMasteryAdapter.setListener(topic -> {
            Problem problem = new Problem();
            problem.setTitle(topic.getNextProblem());
            problem.setTopic(topic.getName().replace("\n", " "));
            problem.setDifficulty("Medium");
            problem.setDescription("Practice the next recommended problem for this topic.");
            problem.setXpReward(20);
            Navigation.findNavController(view).navigate(
                    R.id.problemSolverFragment,
                    ProblemSolverFragment.createArgs(problem)
            );
        });

        RecyclerView recyclerUpNext = view.findViewById(R.id.recycler_up_next);
        problemListAdapter = new ProblemListAdapter();
        recyclerUpNext.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerUpNext.setAdapter(problemListAdapter);
        problemListAdapter.setOnProblemClickListener(problem ->
                Navigation.findNavController(view).navigate(
                        R.id.problemSolverFragment,
                        ProblemSolverFragment.createArgs(problem)
                ));
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btn_notification).setOnClickListener(v ->
                showComingSoon("Notifications"));
        view.findViewById(R.id.btn_search).setOnClickListener(v ->
                showComingSoon("Search"));
        view.findViewById(R.id.text_roadmaps_view_all).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_learn_to_roadmapDetail));
        view.findViewById(R.id.tab_all).setOnClickListener(v -> {
            problemListAdapter.setItems(allUpNextProblems);
            Toast.makeText(requireContext(), "Showing all problems", Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.tab_easy).setOnClickListener(v -> {
            java.util.List<Problem> easy = new java.util.ArrayList<>();
            for (Problem problem : allUpNextProblems) {
                if ("easy".equalsIgnoreCase(problem.getDifficulty())) {
                    easy.add(problem);
                }
            }
            problemListAdapter.setItems(easy);
            Toast.makeText(requireContext(), "Filtered to Easy", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeData(View view) {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getUsername() != null) {
                currentUser = user;
                bindUser(view, localProfileStore.getEffectiveProfile(user));
            }
        });

        viewModel.getRoadmaps().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                roadmapAdapter.setItems(list);
            }
        });

        viewModel.getTopicMasteries().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                topicMasteryAdapter.setItems(list);
            }
        });

        viewModel.getUpNextProblems().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                allUpNextProblems = new java.util.ArrayList<>(list);
                problemListAdapter.setItems(list);
                hideLoading();
            }
        });
    }

    private void showComingSoon(String feature) {
        Toast.makeText(requireContext(), feature + " is coming soon", Toast.LENGTH_SHORT).show();
    }

    private void hideLoading() {
        if (loadingText != null) {
            loadingText.setVisibility(View.GONE);
        }
    }

    private void bindUser(View view, User user) {
        TextView welcomeText = view.findViewById(R.id.text_learn_welcome);
        welcomeText.setText("Welcome back, " + user.getUsername());
    }
}
