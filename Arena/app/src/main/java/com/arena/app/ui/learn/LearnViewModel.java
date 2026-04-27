package com.arena.app.ui.learn;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.arena.app.models.Problem;
import com.arena.app.models.Roadmap;
import com.arena.app.models.TopicMastery;
import com.arena.app.models.User;
import com.arena.app.repository.ProblemRepository;
import com.arena.app.repository.UserRepository;

import java.util.List;

public class LearnViewModel extends AndroidViewModel {

    private final LiveData<User> user;
    private final LiveData<List<Roadmap>> roadmaps;
    private final LiveData<List<TopicMastery>> topicMasteries;
    private final LiveData<List<Problem>> upNextProblems;

    public LearnViewModel(@NonNull Application application) {
        super(application);

        UserRepository userRepository = new UserRepository(application);
        ProblemRepository problemRepository = new ProblemRepository(application);

        user = userRepository.getUserStats();
        roadmaps = problemRepository.getRoadmaps();
        topicMasteries = problemRepository.getTopicMasteries();
        upNextProblems = problemRepository.getUpNextProblems();
    }

    public LiveData<User> getUser() { return user; }
    public LiveData<List<Roadmap>> getRoadmaps() { return roadmaps; }
    public LiveData<List<TopicMastery>> getTopicMasteries() { return topicMasteries; }
    public LiveData<List<Problem>> getUpNextProblems() { return upNextProblems; }
}
