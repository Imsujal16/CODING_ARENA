package com.arena.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arena.app.R;
import com.arena.app.adapters.ContinueLearningAdapter;
import com.arena.app.models.Problem;
import com.arena.app.models.User;
import com.arena.app.repository.LocalProfileStore;
import com.arena.app.ui.solver.ProblemSolverFragment;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private ContinueLearningAdapter adapter;
    private LocalProfileStore localProfileStore;
    private User currentUser;
    private Problem currentProblem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        localProfileStore = new LocalProfileStore(requireContext());

        setupRecyclerView(view);
        setupGreeting(view);
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

    private void setupGreeting(View view) {
        TextView textGreeting = view.findViewById(R.id.text_greeting);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning,";
        } else if (hour < 17) {
            greeting = "Good Afternoon,";
        } else {
            greeting = "Good Evening,";
        }
        textGreeting.setText(greeting);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_continue_learning);
        adapter = new ContinueLearningAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btn_solve_challenge).setOnClickListener(v -> {
            if (currentProblem == null) {
                return;
            }

            Navigation.findNavController(v).navigate(
                    R.id.problemSolverFragment,
                    ProblemSolverFragment.createArgs(currentProblem)
            );
        });
    }

    private void observeData(View view) {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                bindUser(view, localProfileStore.getEffectiveProfile(user));
            }
        });

        viewModel.getDailyChallenge().observe(getViewLifecycleOwner(), problem -> {
            if (problem != null) {
                currentProblem = problem;
                ((TextView) view.findViewById(R.id.text_problem_title)).setText(problem.getTitle());
                ((TextView) view.findViewById(R.id.text_problem_desc)).setText(problem.getDescription());

                TextView chipDifficulty = view.findViewById(R.id.chip_difficulty);
                chipDifficulty.setText(problem.getDifficulty());
                setDifficultyChipStyle(chipDifficulty, problem.getDifficulty());

                ((TextView) view.findViewById(R.id.chip_topic)).setText(problem.getTopic());
                ((TextView) view.findViewById(R.id.text_xp_reward))
                        .setText(String.format("+%d XP", problem.getXpReward()));
            }
        });

        viewModel.getContinueLearningList().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.setItems(list);
            }
        });
    }

    private void bindUser(View view, User user) {
        ((TextView) view.findViewById(R.id.text_username)).setText(user.getUsername());
        ((TextView) view.findViewById(R.id.text_level)).setText(String.valueOf(user.getLevel()));
        ((TextView) view.findViewById(R.id.text_xp)).setText(user.getFormattedXp());
        ((TextView) view.findViewById(R.id.text_rank)).setText(user.getRank());
        ((TextView) view.findViewById(R.id.text_streak_count)).setText(String.valueOf(user.getStreak()));

        TextView avatarInitial = view.findViewById(R.id.text_avatar_initial);
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            avatarInitial.setText(String.valueOf(user.getUsername().charAt(0)));
        }
    }

    private void setDifficultyChipStyle(TextView chip, String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "easy":
                chip.setBackgroundResource(R.drawable.bg_chip_easy);
                chip.setTextColor(getResources().getColor(R.color.difficulty_easy, null));
                break;
            case "medium":
                chip.setBackgroundResource(R.drawable.bg_chip_medium);
                chip.setTextColor(getResources().getColor(R.color.difficulty_medium, null));
                break;
            case "hard":
                chip.setBackgroundResource(R.drawable.bg_chip_hard);
                chip.setTextColor(getResources().getColor(R.color.difficulty_hard, null));
                break;
            default:
                chip.setBackgroundResource(R.drawable.bg_chip_medium);
                chip.setTextColor(getResources().getColor(R.color.difficulty_medium, null));
                break;
        }
    }
}
